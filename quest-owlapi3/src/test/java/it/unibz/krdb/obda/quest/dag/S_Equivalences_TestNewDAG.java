package it.unibz.krdb.obda.quest.dag;

import it.unibz.krdb.obda.ontology.ClassExpression;
import it.unibz.krdb.obda.ontology.DataPropertyExpression;
import it.unibz.krdb.obda.ontology.Description;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.Equivalences;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.EquivalencesDAG;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class S_Equivalences_TestNewDAG extends TestCase {

    ArrayList<String> input = new ArrayList<String>();

    ArrayList<String> output = new ArrayList<String>();

    Logger log = LoggerFactory.getLogger(S_HierarchyTestNewDAG.class);

    public S_Equivalences_TestNewDAG(String name) {
        super(name);
    }

    public void setUp() {
        input.add("src/test/resources/test/dag/test-role-hierarchy.owl");
        input.add("src/test/resources/test/stockexchange-unittest.owl");
        input.add("src/test/resources/test/dag/role-equivalence.owl");
        input.add("src/test/resources/test/dag/test-equivalence-classes.owl");
        input.add("src/test/resources/test/dag/test-equivalence-roles-inverse.owl");
        input.add("src/test/resources/test/newDag/equivalents1.owl");
        input.add("src/test/resources/test/newDag/equivalents2.owl");
        input.add("src/test/resources/test/newDag/equivalents3.owl");
        input.add("src/test/resources/test/newDag/equivalents4.owl");
        input.add("src/test/resources/test/newDag/equivalents5.owl");
        input.add("src/test/resources/test/newDag/equivalents6.owl");
        input.add("src/test/resources/test/newDag/equivalents7.owl");
        input.add("src/test/resources/test/newDag/equivalents8.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents1.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents2.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents3.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents4.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents5.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents6.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents7.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents8.owl");
    }

    public void testEquivalences() throws Exception {
        for (int i = 0; i < input.size(); i++) {
            String fileInput = input.get(i);
            TBoxReasonerImpl reasoner = (TBoxReasonerImpl) TBoxReasonerImpl.create(OWLAPITranslatorUtility.loadOntologyFromFile(fileInput));
            TestTBoxReasonerImpl_OnGraph graphReasoner = new TestTBoxReasonerImpl_OnGraph(reasoner);
            log.debug("Input number {}", i + 1);
            log.info("First graph {}", reasoner.getClassGraph());
            log.info("First graph {}", reasoner.getObjectPropertyGraph());
            log.info("Second dag {}", reasoner);
            assertTrue(testDescendants(graphReasoner.getClassDAG(), reasoner.getClassDAG()));
            assertTrue(testDescendants(graphReasoner.getObjectPropertyDAG(), reasoner.getObjectPropertyDAG()));
            assertTrue(testDescendants(reasoner.getClassDAG(), graphReasoner.getClassDAG()));
            assertTrue(testDescendants(reasoner.getObjectPropertyDAG(), graphReasoner.getObjectPropertyDAG()));
            assertTrue(testChildren(graphReasoner.getClassDAG(), reasoner.getClassDAG()));
            assertTrue(testChildren(graphReasoner.getObjectPropertyDAG(), reasoner.getObjectPropertyDAG()));
            assertTrue(testChildren(reasoner.getClassDAG(), graphReasoner.getClassDAG()));
            assertTrue(testChildren(reasoner.getObjectPropertyDAG(), graphReasoner.getObjectPropertyDAG()));
            assertTrue(testAncestors(graphReasoner.getClassDAG(), reasoner.getClassDAG()));
            assertTrue(testAncestors(graphReasoner.getObjectPropertyDAG(), reasoner.getObjectPropertyDAG()));
            assertTrue(testAncestors(reasoner.getClassDAG(), graphReasoner.getClassDAG()));
            assertTrue(testAncestors(reasoner.getObjectPropertyDAG(), graphReasoner.getObjectPropertyDAG()));
            assertTrue(testParents(graphReasoner.getClassDAG(), reasoner.getClassDAG()));
            assertTrue(testParents(graphReasoner.getObjectPropertyDAG(), reasoner.getObjectPropertyDAG()));
            assertTrue(testParents(reasoner.getClassDAG(), graphReasoner.getClassDAG()));
            assertTrue(testParents(reasoner.getObjectPropertyDAG(), graphReasoner.getObjectPropertyDAG()));
            assertTrue(checkVertexReduction(graphReasoner, reasoner));
            assertTrue(checkEdgeReduction(graphReasoner, reasoner));
        }
    }

    private static <T extends java.lang.Object> boolean coincide(Set<Equivalences<T>> setd1, Set<Equivalences<T>> setd2) {
        Set<T> set2 = new HashSet<T>();
        Iterator<Equivalences<T>> it1 = setd2.iterator();
        while (it1.hasNext()) {
            set2.addAll(it1.next().getMembers());
        }
        Set<T> set1 = new HashSet<T>();
        Iterator<Equivalences<T>> it2 = setd1.iterator();
        while (it2.hasNext()) {
            set1.addAll(it2.next().getMembers());
        }
        return set2.equals(set1);
    }

    private <T extends java.lang.Object> boolean testDescendants(EquivalencesDAG<T> d1, EquivalencesDAG<T> d2) {
        for (Equivalences<T> vertex : d1) {
            Set<Equivalences<T>> setd1 = d1.getSub(vertex);
            log.info("vertex {}", vertex);
            log.debug("descendants {} ", setd1);
            Set<Equivalences<T>> setd2 = d2.getSub(vertex);
            log.debug("descendants {} ", setd2);
            if (!coincide(setd1, setd2)) {
                return false;
            }
        }
        return true;
    }

    private <T extends java.lang.Object> boolean testChildren(EquivalencesDAG<T> d1, EquivalencesDAG<T> d2) {
        for (Equivalences<T> vertex : d1) {
            Set<Equivalences<T>> setd1 = d1.getDirectSub(vertex);
            log.info("vertex {}", vertex);
            log.debug("children {} ", setd1);
            Set<Equivalences<T>> setd2 = d2.getDirectSub(vertex);
            log.debug("children {} ", setd2);
            if (!coincide(setd1, setd2)) {
                return false;
            }
        }
        return true;
    }

    private <T extends java.lang.Object> boolean testAncestors(EquivalencesDAG<T> d1, EquivalencesDAG<T> d2) {
        for (Equivalences<T> vertex : d1) {
            Set<Equivalences<T>> setd1 = d1.getSuper(vertex);
            log.info("vertex {}", vertex);
            log.debug("ancestors {} ", setd1);
            Set<Equivalences<T>> setd2 = d2.getSuper(vertex);
            log.debug("ancestors {} ", setd2);
            if (!coincide(setd1, setd2)) {
                return false;
            }
        }
        return true;
    }

    private <T extends java.lang.Object> boolean testParents(EquivalencesDAG<T> d1, EquivalencesDAG<T> d2) {
        for (Equivalences<T> vertex : d1) {
            Set<Equivalences<T>> setd1 = d1.getDirectSuper(vertex);
            log.info("vertex {}", vertex);
            log.debug("children {} ", setd1);
            Set<Equivalences<T>> setd2 = d2.getDirectSuper(vertex);
            log.debug("children {} ", setd2);
            if (!coincide(setd1, setd2)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkVertexReduction(TestTBoxReasonerImpl_OnGraph reasonerd1, TBoxReasonerImpl d2) {
        int numberVertexesD1 = reasonerd1.vertexSetSize();
        int numberVertexesD2 = d2.vertexSetSize();
        int numberEquivalents = 0;
        Set<Description> set2 = new HashSet<Description>();
        for (Equivalences<ObjectPropertyExpression> equivalents : d2.getObjectPropertyDAG()) {
            numberEquivalents += equivalents.size() - 1;
            set2.addAll(equivalents.getMembers());
        }
        for (Equivalences<DataPropertyExpression> equivalents : d2.getDataPropertyDAG()) {
            numberEquivalents += equivalents.size() - 1;
            set2.addAll(equivalents.getMembers());
        }
        for (Equivalences<ClassExpression> equivalents : d2.getClassDAG()) {
            numberEquivalents += equivalents.size() - 1;
            set2.addAll(equivalents.getMembers());
        }
        log.info("vertex dag {}", numberVertexesD2);
        log.info("equivalents {} ", numberEquivalents);
        return numberVertexesD1 == set2.size() & numberEquivalents == (numberVertexesD1 - numberVertexesD2);
    }

    private boolean checkEdgeReduction(TestTBoxReasonerImpl_OnGraph reasonerd1, TBoxReasonerImpl d2) {
        int numberEdgesD1 = reasonerd1.edgeSetSize();
        int numberEdgesD2 = d2.edgeSetSize();
        int numberEquivalents = 0;
        for (Equivalences<ObjectPropertyExpression> equivalents : d2.getObjectPropertyDAG()) {
            if (equivalents.size() >= 2) {
                numberEquivalents += equivalents.size();
            }
        }
        for (Equivalences<ClassExpression> equivalents : d2.getClassDAG()) {
            if (equivalents.size() >= 2) {
                numberEquivalents += equivalents.size();
            }
        }
        log.info("edges graph {}", numberEdgesD1);
        log.info("edges dag {}", numberEdgesD2);
        log.info("equivalents {} ", numberEquivalents);
        return numberEdgesD1 >= (numberEquivalents + numberEdgesD2);
    }
}
