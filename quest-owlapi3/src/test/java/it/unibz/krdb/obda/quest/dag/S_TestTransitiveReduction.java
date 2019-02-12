package it.unibz.krdb.obda.quest.dag;

import com.google.common.collect.ImmutableSet;
import it.unibz.krdb.obda.ontology.ClassExpression;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.Equivalences;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.EquivalencesDAG;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import junit.framework.TestCase;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Set;

public class S_TestTransitiveReduction extends TestCase {

    ArrayList<String> input = new ArrayList<String>();

    ArrayList<String> output = new ArrayList<String>();

    Logger log = LoggerFactory.getLogger(S_TestTransitiveReduction.class);

    public S_TestTransitiveReduction(String name) {
        super(name);
    }

    public void setUp() {
        input.add("src/test/resources/test/dag/test-equivalence-roles-inverse.owl");
        input.add("src/test/resources/test/dag/test-role-hierarchy.owl");
        input.add("src/test/resources/test/stockexchange-unittest.owl");
        input.add("src/test/resources/test/dag/role-equivalence.owl");
        input.add("src/test/resources/test/newDag/transitive.owl");
        input.add("src/test/resources/test/newDag/transitive2.owl");
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

    public void testR() throws Exception {
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile("src/test/resources/test/newDag/transitive.owl");
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        ClassExpression A = onto.getVocabulary().getClass("http://www.kro.com/ontologies/A");
        ClassExpression B = onto.getVocabulary().getClass("http://www.kro.com/ontologies/B");
        ClassExpression C = onto.getVocabulary().getClass("http://www.kro.com/ontologies/C");
        EquivalencesDAG<ClassExpression> classes = dag.getClassDAG();
        Equivalences<ClassExpression> vA = classes.getVertex(A);
        Equivalences<ClassExpression> vB = classes.getVertex(B);
        Equivalences<ClassExpression> vC = classes.getVertex(C);
        assertEquals(ImmutableSet.of(vB), classes.getDirectSuper(vC));
        assertEquals(ImmutableSet.of(vA), classes.getDirectSuper(vB));
    }

    public void testR2() throws Exception {
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile("src/test/resources/test/newDag/transitive2.owl");
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        ClassExpression A = onto.getVocabulary().getClass("http://www.kro.com/ontologies/A");
        ClassExpression B = onto.getVocabulary().getClass("http://www.kro.com/ontologies/B");
        ClassExpression C = onto.getVocabulary().getClass("http://www.kro.com/ontologies/C");
        ClassExpression D = onto.getVocabulary().getClass("http://www.kro.com/ontologies/D");
        EquivalencesDAG<ClassExpression> classes = dag.getClassDAG();
        Equivalences<ClassExpression> vA = classes.getVertex(A);
        Equivalences<ClassExpression> vB = classes.getVertex(B);
        Equivalences<ClassExpression> vC = classes.getVertex(C);
        Equivalences<ClassExpression> vD = classes.getVertex(D);
        assertEquals(ImmutableSet.of(vB, vD), classes.getDirectSuper(vC));
        assertEquals(ImmutableSet.of(vA), classes.getDirectSuper(vB));
        assertEquals(ImmutableSet.of(vA), classes.getDirectSuper(vD));
    }

    public void testSimplification() throws Exception {
        for (int i = 0; i < input.size(); i++) {
            String fileInput = input.get(i);
            TBoxReasonerImpl dag2 = (TBoxReasonerImpl) TBoxReasonerImpl.create(OWLAPITranslatorUtility.loadOntologyFromFile(fileInput));
            TestTBoxReasonerImpl_OnGraph reasonerd1 = new TestTBoxReasonerImpl_OnGraph(dag2);
            log.debug("Input number {}", i + 1);
            log.info("First graph {}", dag2.getObjectPropertyGraph());
            log.info("First graph {}", dag2.getClassGraph());
            log.info("Second dag {}", dag2);
            assertTrue(testRedundantEdges(reasonerd1, dag2));
        }
    }

    private boolean testRedundantEdges(TestTBoxReasonerImpl_OnGraph reasonerd1, TBoxReasonerImpl d2) {
        int numberEdgesD1 = reasonerd1.edgeSetSize();
        int numberEdgesD2 = d2.edgeSetSize();
        int numberEquivalents = 0;
        int numberRedundants = 0;
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
        {
            DefaultDirectedGraph<ObjectPropertyExpression, DefaultEdge> g1 = reasonerd1.getObjectPropertyGraph();
            for (Equivalences<ObjectPropertyExpression> equivalents : reasonerd1.getObjectPropertyDAG()) {
                log.info("equivalents {} ", equivalents);
                for (ObjectPropertyExpression vertex : equivalents) {
                    if (g1.incomingEdgesOf(vertex).size() != g1.inDegreeOf(vertex)) {
                        numberRedundants += g1.inDegreeOf(vertex) - g1.incomingEdgesOf(vertex).size();
                    }
                    Set<Equivalences<ObjectPropertyExpression>> descendants = d2.getObjectPropertyDAG().getSub(equivalents);
                    Set<Equivalences<ObjectPropertyExpression>> children = d2.getObjectPropertyDAG().getDirectSub(equivalents);
                    log.info("descendants{} ", descendants);
                    log.info("children {} ", children);
                    for (DefaultEdge edge : g1.incomingEdgesOf(vertex)) {
                        ObjectPropertyExpression source = g1.getEdgeSource(edge);
                        for (Equivalences<ObjectPropertyExpression> descendant : descendants) {
                            if (!children.contains(descendant) & !equivalents.contains(descendant.iterator().next()) & descendant.contains(source)) {
                                numberRedundants += 1;
                            }
                        }
                    }
                }
            }
        }
        {
            DefaultDirectedGraph<ClassExpression, DefaultEdge> g1 = reasonerd1.getClassGraph();
            for (Equivalences<ClassExpression> equivalents : reasonerd1.getClassDAG()) {
                log.info("equivalents {} ", equivalents);
                for (ClassExpression vertex : equivalents) {
                    if (g1.incomingEdgesOf(vertex).size() != g1.inDegreeOf(vertex)) {
                        numberRedundants += g1.inDegreeOf(vertex) - g1.incomingEdgesOf(vertex).size();
                    }
                    Set<Equivalences<ClassExpression>> descendants = d2.getClassDAG().getSub(equivalents);
                    Set<Equivalences<ClassExpression>> children = d2.getClassDAG().getDirectSub(equivalents);
                    log.info("descendants{} ", descendants);
                    log.info("children {} ", children);
                    for (DefaultEdge edge : g1.incomingEdgesOf(vertex)) {
                        ClassExpression source = g1.getEdgeSource(edge);
                        for (Equivalences<ClassExpression> descendant : descendants) {
                            if (!children.contains(descendant) & !equivalents.contains(descendant.iterator().next()) & descendant.contains(source)) {
                                numberRedundants += 1;
                            }
                        }
                    }
                }
            }
        }
        log.info("edges graph {}", numberEdgesD1);
        log.info("edges dag {}", numberEdgesD2);
        log.info("equivalents {} ", numberEquivalents);
        log.info("redundants {} ", numberRedundants);
        return numberEdgesD1 >= (numberRedundants + numberEquivalents + numberEdgesD2);
    }
}
