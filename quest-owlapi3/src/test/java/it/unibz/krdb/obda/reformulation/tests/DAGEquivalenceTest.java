package it.unibz.krdb.obda.reformulation.tests;

import it.unibz.krdb.obda.ontology.OClass;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.*;
import junit.framework.TestCase;
import java.util.List;

public class DAGEquivalenceTest extends TestCase {

    /**
	 * R1 = R2^- = R3, S1 = S2^- = S3, R1 ISA S1
	 */
    private final String testEquivalenceRoles = "src/test/resources/test/dag/role-equivalence.owl";

    /**
	 * A1 = A2^- = A3, B1 = B2^- = B3, C1 = C2^- = C3, C1 ISA B1 ISA A1
	 */
    private final String testEquivalenceRolesInverse = "src/test/resources/test/dag/test-equivalence-roles-inverse.owl";

    /**
	 * A1 = A2 = A3, B1 = B2 = B3, B1 ISA A1
	 */
    private final String testEquivalenceClasses = "src/test/resources/test/dag/test-equivalence-classes.owl";

    public void setUp() {
    }

    public void testIndexClasses() throws Exception {
        String testURI = "http://it.unibz.krdb/obda/ontologies/test.owl#";
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile(testEquivalenceClasses);
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        SemanticIndexBuilder engine = new SemanticIndexBuilder(dag);
        List<Interval> nodeInterval = engine.getRange((OClass) dag.getClassDAG().getVertex(onto.getVocabulary().getClass(testURI + "B1")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        Interval interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 2);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange((OClass) dag.getClassDAG().getVertex(onto.getVocabulary().getClass(testURI + "B2")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 2);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange((OClass) dag.getClassDAG().getVertex(onto.getVocabulary().getClass(testURI + "B3")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 2);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange((OClass) dag.getClassDAG().getVertex(onto.getVocabulary().getClass(testURI + "A1")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 1);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange((OClass) dag.getClassDAG().getVertex(onto.getVocabulary().getClass(testURI + "A2")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 1);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange((OClass) dag.getClassDAG().getVertex(onto.getVocabulary().getClass(testURI + "A3")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 1);
        assertEquals(interval.getEnd(), 2);
    }

    public void testIntervalsRoles() throws Exception {
        String testURI = "http://it.unibz.krdb/obda/ontologies/Ontology1314774461138.owl#";
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile(testEquivalenceRoles);
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        SemanticIndexBuilder engine = new SemanticIndexBuilder(dag);
        List<Interval> nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "R1")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        Interval interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 2);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "R2")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 2);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "R3")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 2);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "S1")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 1);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "S2")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 1);
        assertEquals(interval.getEnd(), 2);
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "S3")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(interval.getStart(), 1);
        assertEquals(interval.getEnd(), 2);
    }

    public void testIntervalsRolesWithInverse() throws Exception {
        String testURI = "http://obda.inf.unibz.it/ontologies/tests/dllitef/test.owl#";
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile(testEquivalenceRolesInverse);
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        SemanticIndexBuilder engine = new SemanticIndexBuilder(dag);
        List<Interval> nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "A1")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        Interval interval = nodeInterval.get(0);
        assertEquals(1, interval.getStart());
        assertEquals(3, interval.getEnd());
        EquivalencesDAG<ObjectPropertyExpression> properties = dag.getObjectPropertyDAG();
        ObjectPropertyExpression d = properties.getVertex(onto.getVocabulary().getObjectProperty(testURI + "A2")).getRepresentative();
        assertTrue(d.equals(onto.getVocabulary().getObjectProperty(testURI + "A1").getInverse()));
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "A3")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(1, interval.getStart());
        assertEquals(3, interval.getEnd());
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "C1")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(3, interval.getStart());
        assertEquals(3, interval.getEnd());
        d = properties.getVertex(onto.getVocabulary().getObjectProperty(testURI + "C2")).getRepresentative();
        assertTrue(d.equals(properties.getVertex(onto.getVocabulary().getObjectProperty(testURI + "C1").getInverse()).getRepresentative()));
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "C3")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(3, interval.getStart());
        assertEquals(3, interval.getEnd());
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "B1")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(2, interval.getStart());
        assertEquals(3, interval.getEnd());
        d = properties.getVertex(onto.getVocabulary().getObjectProperty(testURI + "B2")).getRepresentative();
        assertTrue(d.equals(properties.getVertex(onto.getVocabulary().getObjectProperty(testURI + "B3").getInverse()).getRepresentative()));
        nodeInterval = engine.getRange(dag.getObjectPropertyDAG().getVertex(onto.getVocabulary().getObjectProperty(testURI + "B3")).getRepresentative()).getIntervals();
        assertEquals(nodeInterval.size(), 1);
        interval = nodeInterval.get(0);
        assertEquals(2, interval.getStart());
        assertEquals(3, interval.getEnd());
    }
}
