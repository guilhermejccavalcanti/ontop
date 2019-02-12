package it.unibz.krdb.obda.owlapi3;

import com.google.common.collect.ImmutableMap;
import it.unibz.krdb.obda.ontology.*;
import it.unibz.krdb.obda.ontology.impl.ClassImpl;
import it.unibz.krdb.obda.ontology.impl.DataPropertyExpressionImpl;
import it.unibz.krdb.obda.ontology.impl.DatatypeImpl;
import it.unibz.krdb.obda.ontology.impl.OntologyFactoryImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 
 * @author Roman Kontchakov
 */

public class OWLAPITranslatorOWL2QL implements OWLAxiomVisitor {

	
	
	// If we need to construct auxiliary subclass axioms for A ISA exists R.C we
	// put them in this map to avoid generating too many auxiliary roles/classes
	private final Map<OWLObjectPropertyExpression, Map<OWLClassExpression, ObjectSomeValuesFrom>> auxiliaryClassProperties = new HashMap<>();

	private static final OntologyFactory ofac = OntologyFactoryImpl.getInstance();

	private static final Logger log = LoggerFactory.getLogger(OWLAPITranslatorOWL2QL.class);
	
	private static final String INCONSISTENT_ONTOLOGY = "The OWL 2 QL ontology is inconsistent due to axiom {}";
	private static final String INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE = "Inconsistency due to axiom ";
	private static final String NOT_SUPPORTED = "Axiom does not belong to OWL 2 QL: {}";
	private static final String NOT_SUPPORTED_EXT = "Axiom does not belong to OWL 2 QL: {} ({})";
	
	
	private final Ontology dl_onto;
	private final OWLAPITranslatorHelper helper;
	
	private final boolean functionalityAxioms = true; // TEMPORARY FIX
	private final boolean minCardinalityClassExpressions = true; // TEMPORARY FIX
	private final boolean nestedQualifiedExistentials = true; // TEMPORARY FIX
	
	public OWLAPITranslatorOWL2QL(Collection<OWLOntology> owls) {
		dl_onto = createOntology(owls);
		helper = new OWLAPITranslatorHelper(dl_onto.getVocabulary());
	}
	
	public ImmutableOntologyVocabulary getVocabulary() {
		return dl_onto.getVocabulary();
	}
	
	public Ontology getOntology() {
		return dl_onto;
	}

	public static class TranslationException extends Exception {

		private static final long serialVersionUID = 7917688953760608030L;

		public TranslationException() {
		}
		
		public TranslationException(String msg) {
			super(msg);
		}

	}
	
	
	/**
	 * (1)
	 * 
	 * SubClassOf := 'SubClassOf' '(' axiomAnnotations subClassExpression superClassExpression ')'
	 * subClassExpression := Class | subObjectSomeValuesFrom | subObjectSomeValuesFrom
	 * subObjectSomeValuesFrom := 'ObjectSomeValuesFrom' '(' ObjectPropertyExpression owl:Thing ')'
	 * DataSomeValuesFrom := 'DataSomeValuesFrom' '(' DataPropertyExpression { DataPropertyExpression } DataRange ')'
	 * superClassExpression := Class | superObjectIntersectionOf | superObjectComplementOf |  
	 *                         superObjectSomeValuesFrom | DataSomeValuesFrom
	 * superObjectIntersectionOf := 'ObjectIntersectionOf' '(' superClassExpression superClassExpression 
	 *                           { superClassExpression } ')'                        
	 * superObjectComplementOf := 'ObjectComplementOf' '(' subClassExpression ')'
	 * superObjectSomeValuesFrom := 'ObjectSomeValuesFrom' '(' ObjectPropertyExpression Class ')'                          
	 */
	
	@Override
	public void visit(OWLSubClassOfAxiom ax) {
		try {
			ClassExpression subDescription = getSubclassExpression(ax.getSubClass());
			addSubClassAxioms(subDescription, ax.getSuperClass());
		} 
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	
	/**
	 * (2)
	 * 
	 * EquivalentClasses := 'EquivalentClasses' '(' axiomAnnotations 
	 * 						subClassExpression subClassExpression { subClassExpression } ')'
	 * 
	 * replaced by SubClassOfAxiom (rule [R1])
	 */
	
	@Override
	public void visit(OWLEquivalentClassesAxiom ax) {
		try {
			Iterator<OWLClassExpression> it = ax.getClassExpressions().iterator();
			ClassExpression first = getSubclassExpression(it.next());
			ClassExpression previous = first;
			while (it.hasNext()) {
				ClassExpression current = getSubclassExpression(it.next());
				dl_onto.addSubClassOfAxiom(previous, current);
				previous = current;
			}
			dl_onto.addSubClassOfAxiom(previous, first);
		} 
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}
	
	/**
	 * (3)
	 * 
	 * DisjointClasses := 'DisjointClasses' '(' axiomAnnotations subClassExpression subClassExpression 
	 *                     { subClassExpression } ')'
	 */
	
	@Override
	public void visit(OWLDisjointClassesAxiom ax) {
		try {
			// ax.getClassExpressions() is a set!
			ClassExpression[] disjointProperties = new ClassExpression[ax.getClassExpressions().size()];
			int i = 0;
			for (OWLClassExpression ce : ax.getClassExpressions()) {
				ClassExpression c = getSubclassExpression(ce);
				disjointProperties[i++] = c;
			}
			dl_onto.addDisjointClassesAxiom(disjointProperties);		
		}
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}
	
	/**
	 * (4) 
	 * 
	 * ClassAssertion := 'ClassAssertion' '(' axiomAnnotations Class Individual ')'
	 */
	
	@Override
	public void visit(OWLClassAssertionAxiom ax) {
		try {
			ClassAssertion a = helper.translate(ax);
			if (a != null)
				dl_onto.addClassAssertion(a);
		}
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		}		
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		} 
	}


	/**
	 * (5)
	 * 
	 * SubObjectPropertyOf := 'SubObjectPropertyOf' '(' axiomAnnotations 
	 * 												ObjectPropertyExpression ObjectPropertyExpression ')'
	 * ObjectPropertyExpression := ObjectProperty | InverseObjectProperty
	 * InverseObjectProperty := 'ObjectInverseOf' '(' ObjectProperty ')'
	 */
	
	@Override
	public void visit(OWLSubObjectPropertyOfAxiom ax) {		
		try {
			ObjectPropertyExpression ope1 = helper.getPropertyExpression(ax.getSubProperty());
			ObjectPropertyExpression ope2 = helper.getPropertyExpression(ax.getSuperProperty());

			dl_onto.addSubPropertyOfAxiom(ope1, ope2);	
		}		
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		} 
	}

	/**
	 * (6)
	 * 
	 * EquivalentObjectProperties := 'EquivalentObjectProperties' '(' axiomAnnotations 
	 * 								ObjectPropertyExpression ObjectPropertyExpression { ObjectPropertyExpression } ')'
	 * 
	 * replaced by SubObjectPropertyOfAxiom (rule [R1])
	 */

	@Override
	public void visit(OWLEquivalentObjectPropertiesAxiom ax) {
		try {
			Iterator<OWLObjectPropertyExpression> it = ax.getProperties().iterator();
			ObjectPropertyExpression first = helper.getPropertyExpression(it.next());
			ObjectPropertyExpression previous = first;
			while (it.hasNext()) {
				ObjectPropertyExpression current = helper.getPropertyExpression(it.next());
				dl_onto.addSubPropertyOfAxiom(previous, current);
				previous = current;
			}
			dl_onto.addSubPropertyOfAxiom(previous, first);
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}
	
	/**
	 * (7)
	 * 
	 * DisjointObjectProperties := 'DisjointObjectProperties' '(' axiomAnnotations 
	 * 			ObjectPropertyExpression ObjectPropertyExpression { ObjectPropertyExpression } ')'
	 * 
	 */
	
	@Override
	public void visit(OWLDisjointObjectPropertiesAxiom ax) {
		try {
			// ax.Properties() is a set!
			ObjectPropertyExpression[] opes = new ObjectPropertyExpression[ax.getProperties().size()];
			int i = 0;
			for (OWLObjectPropertyExpression prop : ax.getProperties()) {
				ObjectPropertyExpression ope = helper.getPropertyExpression(prop);
				opes[i++] = ope;
			}
			dl_onto.addDisjointObjectPropertiesAxiom(opes);		
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}
	
	/**
	 * (8)
	 * 
	 * InverseObjectProperties := 'InverseObjectProperties' '(' axiomAnnotations 
	 * 									ObjectPropertyExpression ObjectPropertyExpression ')'
	 * 
	 * replaced by SubObjectPropertyOfAxiom (rule [R1])
	 */
	
	@Override
	public void visit(OWLInverseObjectPropertiesAxiom ax) {		
		try {
			ObjectPropertyExpression ope1 = helper.getPropertyExpression(ax.getFirstProperty());
			ObjectPropertyExpression ope2 = helper.getPropertyExpression(ax.getSecondProperty());

			dl_onto.addSubPropertyOfAxiom(ope1, ope2.getInverse());
			dl_onto.addSubPropertyOfAxiom(ope2, ope1.getInverse());		
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}
	
	/**
	 * (9)
	 * 
	 * ObjectPropertyDomain := 'ObjectPropertyDomain' '(' axiomAnnotations ObjectPropertyExpression superClassExpression ')'
	 * 
	 * replaced by SubClassOfAxiom (rule [R2])
	 */
	
	@Override
	public void visit(OWLObjectPropertyDomainAxiom ax) {
		try {
			ObjectPropertyExpression ope = helper.getPropertyExpression(ax.getProperty());
			addSubClassAxioms(ope.getDomain(), ax.getDomain());
		} 
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}
	
	/**
	 * (10)
	 * 
	 * ObjectPropertyRange := 'ObjectPropertyRange' '(' axiomAnnotations ObjectPropertyExpression superClassExpression ')'
	 * 
	 * replaced by SubClassOfAxiom (rule [R2])
	 */
	
	@Override
	public void visit(OWLObjectPropertyRangeAxiom ax) {
		try {
			ObjectPropertyExpression ope = helper.getPropertyExpression(ax.getProperty());
			addSubClassAxioms(ope.getRange(), ax.getRange());
		} 
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	/**
	 * (11)
	 * 
	 * ReflexiveObjectProperty := 'ReflexiveObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'
	 * 
	 */
	
	@Override
	public void visit(OWLReflexiveObjectPropertyAxiom ax) {
		try {
			ObjectPropertyExpression ope = helper.getPropertyExpression(ax.getProperty());
			dl_onto.addReflexiveObjectPropertyAxiom(ope);
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	/**
	 * (12)
	 * 
	 * IrreflexiveObjectProperty := 'IrreflexiveObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'
	 * 
	 */
	
	@Override
	public void visit(OWLIrreflexiveObjectPropertyAxiom ax) {
		try {
			ObjectPropertyExpression ope = helper.getPropertyExpression(ax.getProperty());
			dl_onto.addIrreflexiveObjectPropertyAxiom(ope);
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	
	/**
	 * (13)
	 * 
	 * SymmetricObjectProperty := 'SymmetricObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'
	 * 
	 * replaced by SubObjectPropertyOfAxiom (rule [R3])
	 */

	@Override
	public void visit(OWLSymmetricObjectPropertyAxiom ax) {
		try {
			ObjectPropertyExpression ope = helper.getPropertyExpression(ax.getProperty());
			dl_onto.addSubPropertyOfAxiom(ope, ope.getInverse());
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	/**
	 * (14)
	 * 
	 * AsymmetricObjectProperty :='AsymmetricObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'
	 * 
	 * replaced by DisjointObjectPropertiesAxiom (rule [R3])
	 */
	
	@Override
	public void visit(OWLAsymmetricObjectPropertyAxiom ax) {
		try {
			ObjectPropertyExpression ope = helper.getPropertyExpression(ax.getProperty());
			dl_onto.addDisjointObjectPropertiesAxiom(ope, ope.getInverse());
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	/**
	 * (15)
	 * 
	 * ObjectPropertyAssertion := 'ObjectPropertyAssertion' '(' axiomAnnotations ObjectPropertyExpression 
	 * 				sourceIndividual targetIndividual ')'
	 */
	
	@Override
	public void visit(OWLObjectPropertyAssertionAxiom ax) {
		try {
			ObjectPropertyAssertion a = helper.translate(ax);
			if (a != null)
				dl_onto.addObjectPropertyAssertion(a);
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}						
	}


	/**
	 * (16) 
	 * 
	 * SubDataPropertyOf := 'SubDataPropertyOf' '(' axiomAnnotations 
	 * 							subDataPropertyExpression superDataPropertyExpression ')'
	 * subDataPropertyExpression := DataPropertyExpression
	 * superDataPropertyExpression := DataPropertyExpression
	 * 
	 */
	
	@Override
	public void visit(OWLSubDataPropertyOfAxiom ax) {
		try {
			DataPropertyExpression dpe1 = helper.getPropertyExpression(ax.getSubProperty());
			DataPropertyExpression dpe2 = helper.getPropertyExpression(ax.getSuperProperty());

			dl_onto.addSubPropertyOfAxiom(dpe1, dpe2);	
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}						
	}
	
	
	/**
	 * (17)
	 * 
	 * EquivalentDataProperties := 'EquivalentDataProperties' '(' axiomAnnotations 
	 * 								DataPropertyExpression DataPropertyExpression { DataPropertyExpression } ')'
	 * 
	 * replaced by SubDataPropertyOfAxiom (rule [R1])	
	 */
	
	@Override
	public void visit(OWLEquivalentDataPropertiesAxiom ax) {	
		try {
			Iterator<OWLDataPropertyExpression> it = ax.getProperties().iterator();
			DataPropertyExpression first = helper.getPropertyExpression(it.next());
			DataPropertyExpression previous = first;
			while (it.hasNext()) {
				DataPropertyExpression current = helper.getPropertyExpression(it.next());
				dl_onto.addSubPropertyOfAxiom(previous, current);
				previous = current;
			}
			dl_onto.addSubPropertyOfAxiom(previous, first);
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}						
	}
	
	/**
	 * (18)
	 * 
	 * DisjointDataProperties := 'DisjointDataProperties' '(' axiomAnnotations 
	 * 					DataPropertyExpression DataPropertyExpression { DataPropertyExpression } ')'
	 * 
	 */
	
	@Override
	public void visit(OWLDisjointDataPropertiesAxiom ax) {
		try {
			// ax.Properties() is a set!
			DataPropertyExpression[] dpes = new DataPropertyExpression[ax.getProperties().size()];
			int i = 0;
			for (OWLDataPropertyExpression prop : ax.getProperties()) {
				DataPropertyExpression dpe = helper.getPropertyExpression(prop);
				dpes[i++] = dpe;
			}
			dl_onto.addDisjointDataPropertiesAxiom(dpes);		
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	/**
	 * (19)
	 * 
	 * DataPropertyDomain := 'DataPropertyDomain' '(' axiomAnnotations DataPropertyExpression superClassExpression ')'
	 * 
	 * replaced by SubClassOfAxiom (rule [R2])
	 */
	
	@Override
	public void visit(OWLDataPropertyDomainAxiom ax) {
		try {
			DataPropertyExpression role = helper.getPropertyExpression(ax.getProperty());
			addSubClassAxioms(role.getDomainRestriction(DatatypeImpl.rdfsLiteral), ax.getDomain());		
		} 
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	
	/**
	 * (20)
	 * 
	 * DataPropertyRange := 'DataPropertyRange' '(' axiomAnnotations DataPropertyExpression DataRange ')'
	 * 
	 * replaced by data property inclusion with bottomDataProperty if DataRange is empty (rule [DT1.1])
	 * 
	 */
	
	@Override
	public void visit(OWLDataPropertyRangeAxiom ax) {

		DataPropertyExpression dpe = helper.getPropertyExpression(ax.getProperty());
		try {
			OWL2Datatype owlDatatype = getCanonicalDatatype(ax.getRange());
			if (owlDatatype == null) {
				// range is empty (rule [DT1.1])
				dl_onto.addSubPropertyOfAxiom(dpe, DataPropertyExpressionImpl.owlBottomDataProperty);
			}
			else {
				//Predicate.COL_TYPE columnType = OWLTypeMapper.getType(owlDatatype);
				//Datatype datatype = ofac.createDataType(columnType);
				Datatype datatype = dl_onto.getVocabulary().getDatatype(owlDatatype.getIRI().toString());
				dl_onto.addDataPropertyRangeAxiom(dpe.getRange(), datatype);		
			}		
		} 
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}
	
	/**
	 * (21)
	 * 
	 * DataPropertyAssertion := 'DataPropertyAssertion' '(' axiomAnnotations 
	 * 			DataPropertyExpression sourceIndividual targetValue ')'
	 */

	@Override
	public void visit(OWLDataPropertyAssertionAxiom ax) {
		try {
			DataPropertyAssertion a = helper.translate(ax);
			if (a != null)
				dl_onto.addDataPropertyAssertion(a);
		} 
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
		catch (TranslationException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * (22)
	 * 
	 * DifferentIndividuals := 'DifferentIndividuals' '(' axiomAnnotations Individual Individual { Individual } ')'
	 * 
	 * (simply ignored because it has no effect on reasoning)
	 */
	
	@Override
	public void visit(OWLDifferentIndividualsAxiom ax) {
		// NO-OP: DifferentInfividualsAxioms have no effect in OWL 2 QL
	}
	

	/**
	 * (23)
	 * 
	 * DatatypeDefinition := 'DatatypeDefinition' '(' axiomAnnotations Datatype DataRange ')'
	 * DataRange := Datatype | DataIntersectionOf
	 * DataIntersectionOf := 'DataIntersectionOf' '(' DataRange DataRange { DataRange } ')'
	 *
	 */
	
	@Override
	public void visit(OWLDatatypeDefinitionAxiom ax) {
		// NO-OP: datatype definitions are handled when they are used in data property restrictions 
	}
	
	private OWL2Datatype getCanonicalDatatype(OWLDataRange dr) throws TranslationException {
		return getCanonicalDatatype(OWL2Datatype.RDFS_LITERAL, dr);
	}
	
	private OWL2Datatype getCanonicalDatatype(OWL2Datatype dt0, OWLDataRange dr) throws TranslationException {
		switch (dr.getDataRangeType()) {
		case DATATYPE:
			OWLDatatype dtype = dr.asOWLDatatype();
			if (dtype.isBuiltIn()) {
				OWL2Datatype dt1 = dtype.getBuiltInDatatype();
				if (!DATATYPE_GROUP.containsKey(dt1)) 
					throw new TranslationException("unsupported datatype: " + dt1);
				
				return getIntersection(dt0, dt1);
			}
			else {
				Set<OWLDatatypeDefinitionAxiom> defs = currentOWLOntology.getDatatypeDefinitions(dtype);
				if (defs == null)
					throw new TranslationException("undeclared datatype: " + dtype);
				
				for (OWLDatatypeDefinitionAxiom def : defs) 
					dt0 = getCanonicalDatatype(dt0, def.getDataRange());
				
				return dt0;
			}
		case DATA_INTERSECTION_OF:
			for (OWLDataRange d : ((OWLDataIntersectionOf)dr).getOperands())
				dt0 = getCanonicalDatatype(dt0, d);
			return dt0;
		default:	
			throw new TranslationException("unsupported OWLDataRange construct: " + dr);
		}
	}

/*
 *  All datatypes supported in OWL 2 QL
 *   numbers specify the group: a group is formed by the second-level datatypes
 *      (the second-level datatypes are disjoint --  primitive XSD datatypes are marked *)
	 
    OWL2Datatype.RDFS_LITERAL, //		rdfs:Literal
	1. OWL2Datatype.RDF_XML_LITERAL, //	rdf:XMLLiteral
	2. OWL2Datatype.OWL_REAL, // 			owl:real
	   + OWL2Datatype.OWL_RATIONAL, // 		owl:rational		
	     * OWL2Datatype.XSD_DECIMAL, // 	xsd:decimal
	       - OWL2Datatype.XSD_INTEGER, // 	xsd:integer
	       - - OWL2Datatype.XSD_NON_NEGATIVE_INTEGER, // 	xsd:nonNegativeInteger
	3. OWL2Datatype.RDF_PLAIN_LITERAL, // 	rdf:PlainLiteral
	   * OWL2Datatype.XSD_STRING, // 	xsd:string
	     - OWL2Datatype.XSD_NORMALIZED_STRING, // 	xsd:normalizedString
	     - - OWL2Datatype.XSD_TOKEN, // 	xsd:token
	     - - - OWL2Datatype.XSD_NMTOKEN, // 	xsd:NMTOKEN (see 2.3 in http://www.w3.org/TR/xml11)
	     - - - - OWL2Datatype.XSD_NAME,  // 	xsd:Name
	     - - - - - OWL2Datatype.XSD_NCNAME, //	xsd:NCName
	4* OWL2Datatype.XSD_HEX_BINARY, // 	xsd:hexBinary
	5* OWL2Datatype.XSD_BASE_64_BINARY, // 	xsd:base64Binary
	6* OWL2Datatype.XSD_ANY_URI, // 	xsd:anyURI
	7* OWL2Datatype.XSD_DATE_TIME, // 	xsd:dateTime
	   - OWL2Datatype.XSD_DATE_TIME_STAMP // 	xsd:dateTimeStamp
	   
*/
	private static final Map<OWL2Datatype, Integer> DATATYPE_GROUP = ImmutableMap.<OWL2Datatype, Integer>builder()
			.put(OWL2Datatype.RDF_PLAIN_LITERAL, 3) // 	rdf:PlainLiteral
			.put(OWL2Datatype.RDF_XML_LITERAL, 1) //	rdf:XMLLiteral
			.put(OWL2Datatype.RDFS_LITERAL, 0) //		rdfs:Literal
			.put(OWL2Datatype.OWL_REAL, 2) // 			owl:real
			.put(OWL2Datatype.OWL_RATIONAL, 2) // 		owl:rational		
			.put(OWL2Datatype.XSD_DECIMAL, 2) // 	xsd:decimal
			.put(OWL2Datatype.XSD_INTEGER, 2) // 	xsd:integer
			.put(OWL2Datatype.XSD_NON_NEGATIVE_INTEGER, 2) // 	xsd:nonNegativeInteger
			.put(OWL2Datatype.XSD_STRING, 3) // 	xsd:string
			.put(OWL2Datatype.XSD_NORMALIZED_STRING, 3) // 	xsd:normalizedString
			.put(OWL2Datatype.XSD_TOKEN, 3) // 	xsd:token
			.put(OWL2Datatype.XSD_NAME,  3) // 	xsd:Name
			.put(OWL2Datatype.XSD_NCNAME, 3) //	xsd:NCName
			.put(OWL2Datatype.XSD_NMTOKEN, 3) // 	xsd:NMTOKEN
			.put(OWL2Datatype.XSD_HEX_BINARY, 4) // 	xsd:hexBinary
			.put(OWL2Datatype.XSD_BASE_64_BINARY, 5) // 	xsd:base64Binary
			.put(OWL2Datatype.XSD_ANY_URI, 6) // 	xsd:anyURI
			.put(OWL2Datatype.XSD_DATE_TIME, 7) // 	xsd:dateTime
			.put(OWL2Datatype.XSD_DATE_TIME_STAMP, 7) // 	xsd:dateTimeStamp
			.put(OWL2Datatype.XSD_INT, 8) // 	TEMPORARY FOR Q9 / FISHMARK
			.put(OWL2Datatype.XSD_LONG, 9) // 	TEMPORARY FOR OntologyTypesTest
			.build();
	
	// these three maps order specify linear order in each of the groups
	//    the super-type has a smaller index
	
	private static final Map<OWL2Datatype, Integer> DATATYPE_ORDER_IN_GROUP2 = ImmutableMap.of(
			OWL2Datatype.OWL_REAL, 0, // 			owl:real
			OWL2Datatype.OWL_RATIONAL, 1, // 		owl:rational		
			OWL2Datatype.XSD_DECIMAL, 2, // 	xsd:decimal
			OWL2Datatype.XSD_INTEGER, 3, // 	xsd:integer
			OWL2Datatype.XSD_NON_NEGATIVE_INTEGER, 4); // 	xsd:nonNegativeInteger

	private static final Map<OWL2Datatype, Integer> DATATYPE_ORDER_IN_GROUP3 = ImmutableMap.<OWL2Datatype, Integer>builder()
			.put(OWL2Datatype.RDF_PLAIN_LITERAL, 0) // 	rdf:PlainLiteral
			.put(OWL2Datatype.XSD_STRING, 1) // 	xsd:string
			.put(OWL2Datatype.XSD_NORMALIZED_STRING, 2) // 	xsd:normalizedString
			.put(OWL2Datatype.XSD_TOKEN, 3) // 	xsd:token
			.put(OWL2Datatype.XSD_NMTOKEN, 4) // 	xsd:NMTOKEN (see 2.3 in http://www.w3.org/TR/xml11)
			.put(OWL2Datatype.XSD_NAME,  5) // 	xsd:Name
			.put(OWL2Datatype.XSD_NCNAME, 6)//	xsd:NCName
			.build();
	
	private static final Map<OWL2Datatype, Integer> DATATYPE_ORDER_IN_GROUP7 = ImmutableMap.of(
			OWL2Datatype.XSD_DATE_TIME, 0, // 	xsd:dateTime
			OWL2Datatype.XSD_DATE_TIME_STAMP, 1); // 	xsd:dateTimeStamp
	
	/**
	 * computes intersection of dt0 and dt1 (null is the empty datatype)
	 *     (in OWL 2 QL, this will always coincide with one of the two)
	 *     
	 * @param dt0
	 * @param dt1
	 * @return
	 */

	private static OWL2Datatype getIntersection(OWL2Datatype dt0, OWL2Datatype dt1) {
		// intersection with the empty datatype is empty
		if (dt0 == null || dt1 == null)
			return null;

		int g0 = DATATYPE_GROUP.get(dt0);
		int g1 = DATATYPE_GROUP.get(dt1);

		// intersection with the top datatype is the other argument		
		if (g0 == 0)
			return dt1;
		
		if (g1 == 0)
			return dt0;
		
		// both arguments are neither empty nor the top datatype
		// if they belong to different groups, then the intersection is empty
		if (g0 != g1)
			return null;
		
		// groups but 2, 3 and 7 require special treatment
		// all other groups contain just one datatype
		Map<OWL2Datatype, Integer> linearOrder;
		if (g0 == 2)
			linearOrder = DATATYPE_ORDER_IN_GROUP2;
		else if (g0 == 3)
			linearOrder = DATATYPE_ORDER_IN_GROUP3;
		else if (g0 == 7) 
			linearOrder = DATATYPE_ORDER_IN_GROUP7;
		else
			return dt0;
		
		int o0 = linearOrder.get(dt0);
		int o1 = linearOrder.get(dt1);
		
		if (o0 > o1)
			return dt0;
		
		return dt1;
	}
	
	
	/**
	 * 
	 * SERVICE METHODS
	 * 
	 */
	
	
	/**
	 * (CL)
	 * 
	 * subClassExpression := Class | subObjectSomeValuesFrom | DataSomeValuesFrom
	 * 
	 * subObjectSomeValuesFrom := 'ObjectSomeValuesFrom' '(' ObjectPropertyExpression owl:Thing ')'
	 * DataSomeValuesFrom := 'DataSomeValuesFrom' '(' DataPropertyExpression DataRange ')'
	 * 
	 * @param owlCE
	 * @return
	 * @throws TranslationException
	 */
	
	private ClassExpression getSubclassExpression(OWLClassExpression owlCE) throws TranslationException {
		
		if (owlCE instanceof OWLClass) {
			return helper.getOClass((OWLClass)owlCE);
		} 
		else if (owlCE instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom someexp = (OWLObjectSomeValuesFrom)owlCE;
			if (!someexp.getFiller().isOWLThing()) 
				throw new TranslationException();
			
			return helper.getPropertyExpression(someexp.getProperty()).getDomain();		
		} 
		else if (owlCE instanceof OWLDataSomeValuesFrom) {
			OWLDataSomeValuesFrom someexp = (OWLDataSomeValuesFrom) owlCE;
			return getDataSomeValuesFrom(someexp.getProperty(), someexp.getFiller());
		}
		else if (minCardinalityClassExpressions && owlCE instanceof OWLObjectMinCardinality) {
			OWLObjectMinCardinality someexp = (OWLObjectMinCardinality) owlCE;
			if (someexp.getCardinality() != 1 || !someexp.getFiller().isOWLThing()) 
				throw new TranslationException();
				
			return helper.getPropertyExpression(someexp.getProperty()).getDomain();
		} 
		else if (minCardinalityClassExpressions && owlCE instanceof OWLDataMinCardinality) {
			OWLDataMinCardinality someexp = (OWLDataMinCardinality) owlCE;
			if (someexp.getCardinality() != 1) 
				throw new TranslationException();
			
			return getDataSomeValuesFrom(someexp.getProperty(), someexp.getFiller());
		} 
		else
			throw new TranslationException("unsupported construct " + owlCE);
	}

	
	/**
	 * (CR)
	 * 
	 * superClassExpression := Class | superObjectIntersectionOf | superObjectComplementOf | 
	 * 								superObjectSomeValuesFrom | DataSomeValuesFrom
	 * 
	 * superObjectIntersectionOf := 'ObjectIntersectionOf' '(' superClassExpression superClassExpression { superClassExpression } ')'
	 * superObjectComplementOf := 'ObjectComplementOf' '(' subClassExpression ')'
	 * superObjectSomeValuesFrom := 'ObjectSomeValuesFrom' '(' ObjectPropertyExpression Class ')'
	 * DataSomeValuesFrom := 'DataSomeValuesFrom' '(' DataPropertyExpression DataRange ')'
	 * 
	 * replaces ObjectIntersectionOf by a number of subClassOf axioms (rule [R4])
	 *          superObjectComplementOf by disjointness axioms (rule [R5])
	 *          
	 * @throws InconsistentOntologyException 
	 */
	
	private void addSubClassAxioms(ClassExpression ce1, OWLClassExpression owlCE2) throws TranslationException, InconsistentOntologyException {
		
		// .asConjunctSet() flattens out the intersections and the loop deals with [R4]
		for (OWLClassExpression superClass : owlCE2.asConjunctSet()) {
			if (superClass instanceof OWLClass) {				
				ClassExpression ce2 = helper.getOClass((OWLClass)superClass);
				dl_onto.addSubClassOfAxiom(ce1, ce2);
			} 
			else if (superClass instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom someexp = (OWLObjectSomeValuesFrom) superClass;
				addSubClassOfObjectSomeValuesFromAxiom(ce1, someexp.getProperty(), someexp.getFiller());
			} 
			else if (superClass instanceof OWLDataSomeValuesFrom) {
				OWLDataSomeValuesFrom someexp = (OWLDataSomeValuesFrom) superClass;
				
				ClassExpression ce2 = getDataSomeValuesFrom(someexp.getProperty(), someexp.getFiller());
				//if ((ce2 instanceof DataSomeValuesFrom) && 
				//		!((DataSomeValuesFrom)ce2).getDatatype().equals(DatatypeImpl.rdfsLiteral))
				//	System.err.println("CI WITH QDD: " + ce1 + " <= " + ce2);

				dl_onto.addSubClassOfAxiom(ce1, ce2);
			} 
			else if (superClass instanceof OWLObjectComplementOf) {
				OWLObjectComplementOf superC = (OWLObjectComplementOf)superClass;
				// [R5]
				ClassExpression ce2 = getSubclassExpression(superC.getOperand());
				dl_onto.addDisjointClassesAxiom(ce1, ce2);
			}
			else if (minCardinalityClassExpressions && superClass instanceof OWLObjectMinCardinality) {
				OWLObjectMinCardinality someexp = (OWLObjectMinCardinality) superClass;
				if (someexp.getCardinality() != 1) 
					throw new TranslationException();
				
				addSubClassOfObjectSomeValuesFromAxiom(ce1, someexp.getProperty(), someexp.getFiller());
			} 
			else if (minCardinalityClassExpressions && superClass instanceof OWLDataMinCardinality) {
				OWLDataMinCardinality someexp = (OWLDataMinCardinality) superClass;				
				if (someexp.getCardinality() != 1) 
					throw new TranslationException();
				
				ClassExpression ce2 = getDataSomeValuesFrom(someexp.getProperty(), someexp.getFiller());
				dl_onto.addSubClassOfAxiom(ce1, ce2);
			} 
			else
				throw new TranslationException("unsupported operation in " + superClass);			
		}
	}
	
	private void addSubClassOfObjectSomeValuesFromAxiom(ClassExpression ce1, OWLObjectPropertyExpression owlOPE, OWLClassExpression owlCE) throws TranslationException, InconsistentOntologyException {
		// rule [C0]
		if (owlOPE.isOWLBottomObjectProperty() || owlCE.isOWLNothing()) {
			dl_onto.addSubClassOfAxiom(ce1, ClassImpl.owlNothing);
		}
		else {
			if (owlCE.isOWLThing()) {
				if (!owlOPE.isOWLTopObjectProperty()) { // this check is not really needed
					ObjectPropertyExpression ope = helper.getPropertyExpression(owlOPE);
					dl_onto.addSubClassOfAxiom(ce1, ope.getDomain());		
				}
			}
			else {
				Map<OWLClassExpression, ObjectSomeValuesFrom> entry = auxiliaryClassProperties.get(owlOPE);
				if (entry == null) {
					entry = new HashMap<>();
					auxiliaryClassProperties.put(owlOPE, entry);
				}
				ObjectSomeValuesFrom existsSA = entry.get(owlCE);
				if (existsSA == null) {
					// no replacement found for this exists R.A, creating a new one						
					ObjectPropertyExpression R = helper.getPropertyExpression(owlOPE);
					ObjectPropertyExpression SA = dl_onto.createAuxiliaryObjectProperty();
					if (R.isInverse())
						SA = SA.getInverse();

					existsSA = SA.getDomain();
					entry.put(owlCE, existsSA);
					//System.err.println("AUX:" + owlOPE + "." + owlCE);

					if (owlCE instanceof OWLClass) {
						ClassExpression A = getSubclassExpression(owlCE);
						dl_onto.addSubClassOfAxiom(SA.getRange(), A);
					}
					else if (nestedQualifiedExistentials) {
						addSubClassAxioms(SA.getRange(), owlCE);
					}
					else
						throw new TranslationException("Complex expression in the superclass filler"); 
					
					dl_onto.addSubPropertyOfAxiom(SA, R);
				}
				dl_onto.addSubClassOfAxiom(ce1, existsSA);			
			}
		}
	}
	
	private ClassExpression getDataSomeValuesFrom(OWLDataPropertyExpression owlDPE, OWLDataRange owlDR) throws TranslationException {

		OWL2Datatype owlDatatype = getCanonicalDatatype(owlDR);
		if (owlDatatype == null) {
			// rule [DT1.2]
			return ClassImpl.owlNothing;
		}
		else {
			Datatype datatype = dl_onto.getVocabulary().getDatatype(owlDatatype.getIRI().toString());
			//if (!datatype.equals(DatatypeImpl.rdfsLiteral))
			//	System.err.println("QDD: " + owlDPE + "." + owlDR);
			
			DataPropertyExpression dpe = helper.getPropertyExpression(owlDPE);			
			return dpe.getDomainRestriction(datatype);
		}				
	}
	
	@Override
	public void visit(OWLDisjointUnionAxiom ax) {
		log.warn(NOT_SUPPORTED, ax);
	}
	
	@Override
	public void visit(OWLFunctionalObjectPropertyAxiom ax) {
		// TEMPORARY FIX
		if (functionalityAxioms) {
			ObjectPropertyExpression ope = helper.getPropertyExpression(ax.getProperty());
			dl_onto.addFunctionalObjectPropertyAxiom(ope);
		}
		else
			log.warn(NOT_SUPPORTED, ax);
	}
	
	@Override
	public void visit(OWLInverseFunctionalObjectPropertyAxiom ax) {
		// TEMPORARY FIX
		if (functionalityAxioms) {
			ObjectPropertyExpression ope = helper.getPropertyExpression(ax.getProperty());
			dl_onto.addFunctionalObjectPropertyAxiom(ope.getInverse());
		}
		else
			log.warn(NOT_SUPPORTED, ax);
	}
	
	@Override
	public void visit(OWLTransitiveObjectPropertyAxiom ax) {
		log.warn(NOT_SUPPORTED, ax);
	}

	@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom ax) {
		log.warn(NOT_SUPPORTED, ax);
	}

	@Override
	public void visit(OWLFunctionalDataPropertyAxiom ax) {
		// TEMPORARY FIX
		if (functionalityAxioms) {
			DataPropertyExpression dpe = helper.getPropertyExpression(ax.getProperty());
			dl_onto.addFunctionalDataPropertyAxiom(dpe);		
		}
		else
			log.warn(NOT_SUPPORTED, ax);
	}

	@Override
	public void visit(OWLNegativeDataPropertyAssertionAxiom ax) {
		log.warn(NOT_SUPPORTED, ax);
	}
	
	@Override
	public void visit(OWLSameIndividualAxiom ax) {
		log.warn(NOT_SUPPORTED, ax);
	}

	@Override
	public void visit(OWLSubPropertyChainOfAxiom ax) {
		log.warn(NOT_SUPPORTED, ax);
	}

	@Override
	public void visit(OWLHasKeyAxiom ax) {
		log.warn(NOT_SUPPORTED, ax);
	}

	@Override
	public void visit(SWRLRule ax) {
		log.warn(NOT_SUPPORTED, ax);
	}

	
	
	
	@Override
	public void visit(OWLAnnotationAssertionAxiom ax) {
		try {
			AnnotationAssertion a = helper.translate(ax);
			if (a != null)
				dl_onto.addAnnotationAssertion(a);
		}
		catch (TranslationException e) {
			log.warn(NOT_SUPPORTED_EXT, ax, e.getMessage());
		}
		catch (InconsistentOntologyException e) {
			log.warn(INCONSISTENT_ONTOLOGY, ax);
			throw new RuntimeException(INCONSISTENT_ONTOLOGY_EXCEPTION_MESSAGE + ax);
		}
	}

	@Override
	public void visit(OWLSubAnnotationPropertyOfAxiom arg0) {
		// NO-OP: AnnotationAxioms have no effect
	}

	@Override
	public void visit(OWLAnnotationPropertyDomainAxiom arg0) {
		// NO-OP: AnnotationAxioms have no effect
	}

	@Override
	public void visit(OWLAnnotationPropertyRangeAxiom arg0) {
		// NO-OP: AnnotationAxioms have no effect
	}

	@Override
	public void visit(OWLDeclarationAxiom ax) {
		// NO-OP: DeclarationAxioms are handled in prepare()
	}

	
	
	private OWLOntology currentOWLOntology; // required to retrieve datatype definitions 
	
	private final Set<String> objectproperties = new HashSet<>();
	private final Set<String> dataproperties = new HashSet<>();
	private final Set<String> annotationproperties = new HashSet<>();
	private final Set<String> punnedPredicates = new HashSet<>();
	
	
	public void setCurrentOWLOntology(OWLOntology owl) {
		currentOWLOntology = owl;
	}
	
	private final Ontology createOntology(Collection<OWLOntology> owls) {
		OntologyVocabulary vb = OntologyFactoryImpl.getInstance().createVocabulary();
		
		// add all definitions for classes and properties		
		for (OWLOntology owl : owls) {
			for (OWLClass entity : owl.getClassesInSignature())  {
				String uri = entity.getIRI().toString();
				vb.createClass(uri);			
			}

			for (OWLObjectProperty prop : owl.getObjectPropertiesInSignature()) {
				String uri = prop.getIRI().toString();
				if (dataproperties.contains(uri))  {
					punnedPredicates.add(uri); 
					log.warn("Quest can become unstable with properties declared as both data and object. Offending property: " + uri);
				}
				else {
					objectproperties.add(uri);
					vb.createObjectProperty(uri);
				}
			}
			
			for (OWLDataProperty prop : owl.getDataPropertiesInSignature())  {
				String uri = prop.getIRI().toString();
				if (objectproperties.contains(uri)) {
					punnedPredicates.add(uri);
					log.warn("Quest can become unstable with properties declared as both data and object. Offending property: " + uri);
				}
				else {
					dataproperties.add(uri);
					vb.createDataProperty(uri);
				}
			}

			for (OWLAnnotationProperty prop : owl.getAnnotationPropertiesInSignature())  {
				String uri = prop.getIRI().toString();
					annotationproperties.add(uri);
					vb.createAnnotationProperty(uri);
			}

		}
		return ofac.createOntology(vb);		
	}
	
	
}
