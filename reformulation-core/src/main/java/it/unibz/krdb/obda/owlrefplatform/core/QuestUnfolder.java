package it.unibz.krdb.obda.owlrefplatform.core;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import it.unibz.krdb.obda.model.*;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.OBDAVocabulary;
import it.unibz.krdb.obda.model.impl.TermUtils;
import it.unibz.krdb.obda.ontology.ClassAssertion;
import it.unibz.krdb.obda.ontology.DataPropertyAssertion;
import it.unibz.krdb.obda.ontology.ObjectPropertyAssertion;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.*;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.mappingprocessing.MappingDataTypeRepair;
import it.unibz.krdb.obda.owlrefplatform.core.mappingprocessing.MappingSameAs;
import it.unibz.krdb.obda.owlrefplatform.core.mappingprocessing.TMappingExclusionConfig;
import it.unibz.krdb.obda.owlrefplatform.core.mappingprocessing.TMappingProcessor;
import it.unibz.krdb.obda.owlrefplatform.core.unfolding.DatalogUnfolder;
import it.unibz.krdb.obda.parser.PreprocessProjection;
import it.unibz.krdb.obda.utils.Mapping2DatalogConverter;
import it.unibz.krdb.obda.utils.MappingSplitter;
import it.unibz.krdb.obda.utils.MetaMappingExpander;
import it.unibz.krdb.sql.DBMetadata;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class QuestUnfolder {

    /* The active unfolding engine */
    private DatalogUnfolder unfolder;

    private final DBMetadata metadata;

    private final Multimap<Predicate, List<Integer>> pkeys;

    private final CQContainmentCheckUnderLIDs foreignKeyCQC;

    /*
	 * These are pattern matchers that will help transforming the URI's in
	 * queries into Functions, used by the SPARQL translator.
	 */
    private UriTemplateMatcher uriTemplateMatcher = new UriTemplateMatcher();

    protected List<CQIE> ufp;

    // for TESTS ONLY
    private static final Logger log = LoggerFactory.getLogger(QuestUnfolder.class);

    private static final OBDADataFactory fac = OBDADataFactoryImpl.getInstance();

    private Set<Predicate> dataPropertiesAndClassesMapped = new HashSet<>();

    private Set<Predicate> objectPropertiesMapped = new HashSet<>();

    ;

    public QuestUnfolder(DBMetadata metadata) {
        this.metadata = metadata;
        this.pkeys = DBMetadataUtil.extractPKs(metadata);
        LinearInclusionDependencies foreignKeyRules = DBMetadataUtil.generateFKRules(metadata);
        this.foreignKeyCQC = new CQContainmentCheckUnderLIDs(foreignKeyRules);
    }

    public void setupInVirtualMode(Collection<OBDAMappingAxiom> mappings, Connection localConnection, VocabularyValidator vocabularyValidator, TBoxReasoner reformulationReasoner, Ontology inputOntology, TMappingExclusionConfig excludeFromTMappings) throws SQLException, JSQLParserException, OBDAException {
        mappings = vocabularyValidator.replaceEquivalences(mappings);
        preprocessProjection(mappings, metadata);
        Collection<OBDAMappingAxiom> splittedMappings = MappingSplitter.splitMappings(mappings);
        MetaMappingExpander metaMappingExpander = new MetaMappingExpander(localConnection, metadata.getQuotedIDFactory());
        Collection<OBDAMappingAxiom> expandedMappings = metaMappingExpander.expand(splittedMappings);
        List<CQIE> unfoldingProgram = Mapping2DatalogConverter.constructDatalogProgram(expandedMappings, metadata);
        log.debug("Original mapping size: {}", unfoldingProgram.size());
        normalizeMappings(unfoldingProgram);
        unfoldingProgram = applyTMappings(unfoldingProgram, reformulationReasoner, true, excludeFromTMappings);
        addAssertionsAsFacts(unfoldingProgram, inputOntology.getClassAssertions(), inputOntology.getObjectPropertyAssertions(), inputOntology.getDataPropertyAssertions());
        extendTypesWithMetadataAndAddNOTNULL(unfoldingProgram, reformulationReasoner, vocabularyValidator);
        addSameAsMapping(unfoldingProgram);
        uriTemplateMatcher = UriTemplateMatcher.create(unfoldingProgram);
        unfoldingProgram.addAll(generateTripleMappings(unfoldingProgram));
        log.debug("Final set of mappings: \n {}", Joiner.on("\n").join(unfoldingProgram));
        unfolder = new DatalogUnfolder(unfoldingProgram, pkeys);
        this.ufp = unfoldingProgram;
    }

    /**
	 * Setting up the unfolder and SQL generation
	 * @param reformulationReasoner
	 * @param collection
	 * @throws OBDAException
	 */
    public void setupInSemanticIndexMode(Collection<OBDAMappingAxiom> mappings, TBoxReasoner reformulationReasoner) throws OBDAException {
        List<CQIE> unfoldingProgram = Mapping2DatalogConverter.constructDatalogProgram(mappings, metadata);
        unfoldingProgram = applyTMappings(unfoldingProgram, reformulationReasoner, false, TMappingExclusionConfig.empty());
        uriTemplateMatcher = UriTemplateMatcher.create(unfoldingProgram);
        unfoldingProgram.addAll(generateTripleMappings(unfoldingProgram));
        log.debug("Final set of mappings: \n {}", Joiner.on("\n").join(unfoldingProgram));
        unfolder = new DatalogUnfolder(unfoldingProgram, pkeys);
        this.ufp = unfoldingProgram;
    }

    private List<CQIE> applyTMappings(List<CQIE> unfoldingProgram, TBoxReasoner reformulationReasoner, boolean full, TMappingExclusionConfig excludeFromTMappings) throws OBDAException {
        final long startTime = System.currentTimeMillis();
        unfoldingProgram = TMappingProcessor.getTMappings(unfoldingProgram, reformulationReasoner, full, foreignKeyCQC, excludeFromTMappings);
        final long endTime = System.currentTimeMillis();
        log.debug("TMapping size: {}", unfoldingProgram.size());
        log.debug("TMapping processing time: {} ms", (endTime - startTime));
        return unfoldingProgram;
    }

    /***
	 * Adding data typing on the mapping axioms.
	 * Adding NOT NULL conditions to the variables used in the head
	 * of all mappings to preserve SQL-RDF semantics
	 */
    private void extendTypesWithMetadataAndAddNOTNULL(List<CQIE> unfoldingProgram, TBoxReasoner tboxReasoner, VocabularyValidator qvv) throws OBDAException {
        MappingDataTypeRepair typeRepair = new MappingDataTypeRepair(metadata, tboxReasoner, qvv);
        for (CQIE mapping : unfoldingProgram) {
            typeRepair.insertDataTyping(mapping);
            Set<Variable> headvars = new HashSet<>();
            TermUtils.addReferencedVariablesTo(headvars, mapping.getHead());
            for (Variable var : headvars) {
                Function notnull = fac.getFunctionIsNotNull(var);
                List<Function> body = mapping.getBody();
                if (!body.contains(notnull)) {
                    body.add(notnull);
                }
            }
        }
    }

    /**
	 * Normalize language tags (make them lower-case) and equalities 
	 * (remove them by replacing all equivalent terms with one representative)
	 */
    private void normalizeMappings(List<CQIE> unfoldingProgram) {
        for (CQIE mapping : unfoldingProgram) {
            Function head = mapping.getHead();
            for (Term term : head.getTerms()) {
                if (!(term instanceof Function)) {
                    continue;
                }
                Function typedTerm = (Function) term;
                if (typedTerm.getTerms().size() == 2 && typedTerm.getFunctionSymbol().getName().equals(OBDAVocabulary.RDFS_LITERAL_URI)) {
                    Term originalLangTag = typedTerm.getTerm(1);
                    if (originalLangTag instanceof ValueConstant) {
                        ValueConstant originalLangConstant = (ValueConstant) originalLangTag;
                        Term normalizedLangTag = fac.getConstantLiteral(originalLangConstant.getValue().toLowerCase(), originalLangConstant.getType());
                        typedTerm.setTerm(1, normalizedLangTag);
                    }
                }
            }
        }
        for (CQIE cq : unfoldingProgram) {
            EQNormalizer.enforceEqualities(cq);
        }
    }

    /***
	 * Adding ontology assertions (ABox) as rules (facts, head with no body).
	 */
    private void addAssertionsAsFacts(List<CQIE> unfoldingProgram, Iterable<ClassAssertion> cas, Iterable<ObjectPropertyAssertion> pas, Iterable<DataPropertyAssertion> das) {
        int count = 0;
        for (ClassAssertion ca : cas) {
            URIConstant c = (URIConstant) ca.getIndividual();
            Predicate p = ca.getConcept().getPredicate();
            Function head = fac.getFunction(p, fac.getUriTemplate(fac.getConstantLiteral(c.getURI())));
            CQIE rule = fac.getCQIE(head, Collections.<Function>emptyList());
            unfoldingProgram.add(rule);
            count++;
        }
        log.debug("Appended {} class assertions from ontology as fact rules", count);
        count = 0;
        for (ObjectPropertyAssertion pa : pas) {
            URIConstant s = (URIConstant) pa.getSubject();
            URIConstant o = (URIConstant) pa.getObject();
            Predicate p = pa.getProperty().getPredicate();
            Function head = fac.getFunction(p, fac.getUriTemplate(fac.getConstantLiteral(s.getURI())), fac.getUriTemplate(fac.getConstantLiteral(o.getURI())));
            CQIE rule = fac.getCQIE(head, Collections.<Function>emptyList());
            unfoldingProgram.add(rule);
            count++;
        }
        log.debug("Appended {} object property assertions as fact rules", count);
    }

    /***
	 * Creates mappings with heads as "triple(x,y,z)" from mappings with binary
	 * and unary atoms"
	 *
	 * @return
	 */
    private static List<CQIE> generateTripleMappings(List<CQIE> unfoldingProgram) {
        List<CQIE> newmappings = new LinkedList<CQIE>();
        for (CQIE mapping : unfoldingProgram) {
            Function newhead = null;
            Function currenthead = mapping.getHead();
            if (currenthead.getArity() == 1) {
                Function rdfTypeConstant = fac.getUriTemplate(fac.getConstantLiteral(OBDAVocabulary.RDF_TYPE));
                String classname = currenthead.getFunctionSymbol().getName();
                Term classConstant = fac.getUriTemplate(fac.getConstantLiteral(classname));
                newhead = fac.getTripleAtom(currenthead.getTerm(0), rdfTypeConstant, classConstant);
            } else {
                if (currenthead.getArity() == 2) {
                    String propname = currenthead.getFunctionSymbol().getName();
                    Function propConstant = fac.getUriTemplate(fac.getConstantLiteral(propname));
                    newhead = fac.getTripleAtom(currenthead.getTerm(0), propConstant, currenthead.getTerm(1));
                }
            }
            CQIE newmapping = fac.getCQIE(newhead, mapping.getBody());
            newmappings.add(newmapping);
        }
        return newmappings;
    }

    public UriTemplateMatcher getUriTemplateMatcher() {
        return uriTemplateMatcher;
    }

    public DatalogProgram unfold(DatalogProgram query) throws OBDAException {
        return unfolder.unfold(query);
    }

    /***
	 * Expands a SELECT * into a SELECT with all columns implicit in the *
	 *
	 * @throws java.sql.SQLException
	 */
    private static void preprocessProjection(Collection<OBDAMappingAxiom> mappings, DBMetadata metadata) throws SQLException {
        for (OBDAMappingAxiom axiom : mappings) {
            try {
                String sourceString = axiom.getSourceQuery().toString();
                Select select = (Select) CCJSqlParserUtil.parse(sourceString);
                List<Function> targetQuery = axiom.getTargetQuery();
                Set<Variable> variables = new HashSet<>();
                for (Function atom : targetQuery) {
                    TermUtils.addReferencedVariablesTo(variables, atom);
                }
                PreprocessProjection ps = new PreprocessProjection(metadata);
                String query = ps.getMappingQuery(select, variables);
                axiom.setSourceQuery(fac.getSQLQuery(query));
            } catch (JSQLParserException e) {
                log.debug("SQL Query cannot be preprocessed by the parser");
            }
        }
    }

    /**
     * Store information about owl:sameAs
     */
    public void addSameAsMapping(List<CQIE> unfoldingProgram) throws OBDAException {
        MappingSameAs msa = new MappingSameAs(unfoldingProgram);
        dataPropertiesAndClassesMapped = msa.getDataPropertiesAndClassesWithSameAs();
        objectPropertiesMapped = msa.getObjectPropertiesWithSameAs();
    }

    public Set<Predicate> getSameAsDataPredicatesAndClasses() {
        return dataPropertiesAndClassesMapped;
    }

    public Set<Predicate> getSameAsObjectPredicates() {
        return objectPropertiesMapped;
    }
}
