package it.unibz.krdb.obda.quest.dag;

import it.unibz.krdb.obda.ontology.ClassExpression;
import it.unibz.krdb.obda.ontology.DataPropertyExpression;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.Equivalences;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.EquivalencesDAG;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Set;

public class S_HierarchyTestNewDAG extends TestCase {

    ArrayList<String> input = new ArrayList<String>();

    ArrayList<String> output = new ArrayList<String>();

    Logger log = LoggerFactory.getLogger(S_HierarchyTestNewDAG.class);

    public S_HierarchyTestNewDAG(String name) {
        super(name);
    }

    public void setUp() {
        input.add("src/test/resources/test/dag/test-role-hierarchy.owl");
        input.add("src/test/resources/test/dag/role-equivalence.owl");
        input.add("src/test/resources/test/dag/test-class-hierarchy.owl");
        input.add("src/test/resources/test/newDag/ancestor1.owl");
        input.add("src/test/resources/test/newDag/ancestors2.owl");
        input.add("src/test/resources/test/newDag/ancestors3.owl");
        input.add("src/test/resources/test/newDag/ancestors4.owl");
        input.add("src/test/resources/test/newDag/inverseAncestor1.owl");
        input.add("src/test/resources/test/newDag/inverseAncestor2.owl");
        input.add("src/test/resources/test/newDag/inverseAncestor3.owl");
        input.add("src/test/resources/test/newDag/inverseAncestor4.owl");
    }

    public void testReachability() throws Exception {
        for (int i = 0; i < input.size(); i++) {
            String fileInput = input.get(i);
            TBoxReasoner reasoner = TBoxReasonerImpl.create(OWLAPITranslatorUtility.loadOntologyFromFile(fileInput));
            TestTBoxReasonerImpl_OnNamedDAG dag2 = new TestTBoxReasonerImpl_OnNamedDAG(reasoner);
            TestTBoxReasonerImpl_Named dag1 = new TestTBoxReasonerImpl_Named(reasoner);
            log.debug("Input number {}", i + 1);
            log.info("First dag {}", dag1);
            log.info("Second dag {}", dag2);
            assertTrue(testDescendants(dag1.getClassDAG(), dag2.getClassDAG()));
            assertTrue(testDescendants(dag1.getObjectPropertyDAG(), dag2.getObjectPropertyDAG()));
            assertTrue(testAncestors(dag1.getClassDAG(), dag2.getClassDAG()));
            assertTrue(testAncestors(dag1.getObjectPropertyDAG(), dag2.getObjectPropertyDAG()));
            assertTrue(checkforNamedVertexesOnly(dag2, reasoner));
            assertTrue(testDescendants(dag2.getClassDAG(), dag1.getClassDAG()));
            assertTrue(testDescendants(dag2.getObjectPropertyDAG(), dag1.getObjectPropertyDAG()));
            assertTrue(testAncestors(dag2.getClassDAG(), dag1.getClassDAG()));
            assertTrue(testAncestors(dag2.getObjectPropertyDAG(), dag1.getObjectPropertyDAG()));
        }
    }

    private <T extends java.lang.Object> boolean testDescendants(EquivalencesDAG<T> d1, EquivalencesDAG<T> d2) {
        for (Equivalences<T> node : d1) {
            Set<Equivalences<T>> setd1 = d1.getSub(node);
            Set<Equivalences<T>> setd2 = d2.getSub(node);
            if (!setd1.equals(setd2)) {
                return false;
            }
        }
        return true;
    }

    private <T extends java.lang.Object> boolean testAncestors(EquivalencesDAG<T> d1, EquivalencesDAG<T> d2) {
        for (Equivalences<T> node : d1) {
            Set<Equivalences<T>> setd1 = d1.getSuper(node);
            Set<Equivalences<T>> setd2 = d2.getSuper(node);
            if (!setd1.equals(setd2)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkforNamedVertexesOnly(TestTBoxReasonerImpl_OnNamedDAG dag, TBoxReasoner reasoner) {
        for (Equivalences<ObjectPropertyExpression> node : dag.getObjectPropertyDAG()) {
            ObjectPropertyExpression vertex = node.getRepresentative();
            if (!reasoner.getObjectPropertyDAG().getVertex(vertex).isIndexed()) {
                return false;
            }
        }
        for (Equivalences<DataPropertyExpression> node : dag.getDataPropertyDAG()) {
            DataPropertyExpression vertex = node.getRepresentative();
            if (!reasoner.getDataPropertyDAG().getVertex(vertex).isIndexed()) {
                return false;
            }
        }
        for (Equivalences<ClassExpression> node : dag.getClassDAG()) {
            ClassExpression vertex = node.getRepresentative();
            if (!reasoner.getClassDAG().getVertex(vertex).isIndexed()) {
                return false;
            }
        }
        return true;
    }
}
