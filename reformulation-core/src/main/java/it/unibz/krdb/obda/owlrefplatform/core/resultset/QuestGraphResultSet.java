package it.unibz.krdb.obda.owlrefplatform.core.resultset;

import it.unibz.krdb.obda.model.*;
import it.unibz.krdb.obda.model.ValueConstant;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.OBDAVocabulary;
import it.unibz.krdb.obda.ontology.Assertion;
import it.unibz.krdb.obda.ontology.AssertionFactory;
import it.unibz.krdb.obda.ontology.InconsistentOntologyException;
import it.unibz.krdb.obda.ontology.impl.AssertionFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.translator.SesameConstructTemplate;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.algebra.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestGraphResultSet implements GraphResultSet {

    private List<List<Assertion>> results = new ArrayList<>();

    private TupleResultSet tupleResultSet;

    private SesameConstructTemplate sesameTemplate;

    List<ExtensionElem> extList = null;

    Map<String, ValueExpr> extMap = null;

    //store results in case of describe queries
    private boolean storeResults = false;

    private OBDADataFactory dfac = OBDADataFactoryImpl.getInstance();

    private AssertionFactory ofac = AssertionFactoryImpl.getInstance();

    public QuestGraphResultSet(TupleResultSet results, SesameConstructTemplate template, boolean storeResult) throws OBDAException {
        this.tupleResultSet = results;
        this.sesameTemplate = template;
        this.storeResults = storeResult;
        processResultSet(tupleResultSet, sesameTemplate);
    }

    private void processResultSet(TupleResultSet resSet, SesameConstructTemplate template) throws OBDAException {
        if (storeResults) {
            while (resSet.nextRow()) {
                this.results.add(processResults(resSet, template));
            }
        }
    }

    @Override
    public void addNewResultSet(List<Assertion> result) {
        results.add(result);
    }

    //	@Override
    //	public Template getTemplate() {
    //		return template;
    //	}
    /**
	 * The method to actually process the current result set Row.
	 * Construct a list of assertions from the current result set row.
	 * In case of describe it is called to process and store all 
	 * the results from a resultset.
	 * In case of construct it is called upon next, to process
	 * the only current result set.
	 */
    private List<Assertion> processResults(TupleResultSet result, SesameConstructTemplate template) throws OBDAException {
        List<Assertion> tripleAssertions = new ArrayList<>();
        List<ProjectionElemList> peLists = template.getProjectionElemList();
        Extension ex = template.getExtension();
        if (ex != null) {
            extList = ex.getElements();
            Map<String, ValueExpr> newExtMap = new HashMap<>();
            for (ExtensionElem anExtList : extList) {
                newExtMap.put(anExtList.getName(), anExtList.getExpr());
            }
            extMap = newExtMap;
        }
        for (ProjectionElemList peList : peLists) {
            int size = peList.getElements().size();
            for (int i = 0; i < size / 3; i++) {
                ObjectConstant subjectConstant = (ObjectConstant) getConstant(peList.getElements().get(i * 3), result);
                Constant predicateConstant = getConstant(peList.getElements().get(i * 3 + 1), result);
                Constant objectConstant = getConstant(peList.getElements().get(i * 3 + 2), result);
                String predicateName = predicateConstant.getValue();
                Assertion assertion;
                try {
                    if (predicateName.equals(OBDAVocabulary.RDF_TYPE)) {
                        assertion = ofac.createClassAssertion(objectConstant.getValue(), subjectConstant);
                    } else {
                        if ((objectConstant instanceof URIConstant) || (objectConstant instanceof BNode)) {
                            assertion = ofac.createObjectPropertyAssertion(predicateName, subjectConstant, (ObjectConstant) objectConstant);
                        } else {
                            assertion = ofac.createDataPropertyAssertion(predicateName, subjectConstant, (ValueConstant) objectConstant);
                        }
                    }
                    if (assertion != null) {
                        tripleAssertions.add(assertion);
                    }
                } catch (InconsistentOntologyException e) {
                    throw new RuntimeException("InconsistentOntologyException: " + predicateName + " " + subjectConstant + " " + objectConstant);
                }
            }
        }
        return tripleAssertions;
    }

    @Override
    public boolean hasNext() throws OBDAException {
        if (storeResults) {
            return results.size() != 0;
        } else {
            return tupleResultSet.nextRow();
        }
    }

    @Override
    public List<Assertion> next() throws OBDAException {
        if (results.size() > 0) {
            return results.remove(0);
        } else {
            return processResults(tupleResultSet, sesameTemplate);
        }
    }

    private Constant getConstant(ProjectionElem node, TupleResultSet resSet) throws OBDAException {
        Constant constant = null;
        String node_name = node.getSourceName();
        ValueExpr ve = null;
        if (extMap != null) {
            ve = extMap.get(node_name);
            if (ve != null && ve instanceof Var) {
                throw new RuntimeException("Invalid query. Found unbound variable: " + ve);
            }
        }
        if (node_name.charAt(0) == '-') {
            org.openrdf.query.algebra.ValueConstant vc = (org.openrdf.query.algebra.ValueConstant) ve;
            if (vc.getValue() instanceof URIImpl) {
                constant = dfac.getConstantURI(vc.getValue().stringValue());
            } else {
                if (vc.getValue() instanceof LiteralImpl) {
                    constant = dfac.getConstantLiteral(vc.getValue().stringValue());
                } else {
                    constant = dfac.getConstantBNode(vc.getValue().stringValue());
                }
            }
        } else {
            constant = resSet.getConstant(node_name);
        }
        return constant;
    }

    @Override
    public void close() throws OBDAException {
        tupleResultSet.close();
    }
}
