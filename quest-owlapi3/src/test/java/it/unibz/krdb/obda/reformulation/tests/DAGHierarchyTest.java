package it.unibz.krdb.obda.reformulation.tests;

import com.google.common.collect.ImmutableSet;
import it.unibz.krdb.obda.ontology.ClassExpression;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.Equivalences;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.EquivalencesDAG;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import it.unibz.krdb.obda.quest.dag.TestTBoxReasonerImpl_OnNamedDAG;
import junit.framework.TestCase;
import java.util.Set;

public class DAGHierarchyTest extends TestCase {

    /**
	 * A -> B, B -> {E, F}, {C, D} -> {E, F} with A, B, C, D, E, F are atomic
	 * concepts.
	 */
    private final String inputFile1 = "src/test/resources/test/dag/test-class-hierarchy.owl";

    /**
	 * P -> Q, Q -> {T, U}, {R, S} -> {T, U} with P, Q, R, S, T, U are atomic
	 * roles.
	 */
    private final String inputFile2 = "src/test/resources/test/dag/test-role-hierarchy.owl";

    private static <T extends java.lang.Object> int sizeOf(Set<Equivalences<T>> set) {
        int size = 0;
        for (Equivalences<T> d : set) {
            size += d.size();
        }
        return size;
    }

    /**
	 * List all the descendants of a class in the TBox with having equivalent
	 * classes into account.
	 */
    public void testDescendantClasses() throws Exception {
        final String ontoURI = "http://obda.inf.unibz.it/ontologies/test-class-hierarchy.owl#";
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile(inputFile1);
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        TestTBoxReasonerImpl_OnNamedDAG namedReasoner = new TestTBoxReasonerImpl_OnNamedDAG(dag);
        EquivalencesDAG<ClassExpression> classes = namedReasoner.getClassDAG();
        ClassExpression A = onto.getVocabulary().getClass(ontoURI + "A");
        ClassExpression B = onto.getVocabulary().getClass(ontoURI + "B");
        ClassExpression C = onto.getVocabulary().getClass(ontoURI + "C");
        ClassExpression D = onto.getVocabulary().getClass(ontoURI + "D");
        ClassExpression E = onto.getVocabulary().getClass(ontoURI + "E");
        ClassExpression F = onto.getVocabulary().getClass(ontoURI + "F");
        Equivalences<ClassExpression> initialNode = classes.getVertex(A);
        Set<Equivalences<ClassExpression>> descendants = classes.getSub(initialNode);
        assertEquals(descendants.size(), 1);
        initialNode = classes.getVertex(B);
        descendants = classes.getSub(initialNode);
        assertEquals(descendants.size(), 2);
        assertTrue(descendants.contains(classes.getVertex(A)));
        initialNode = classes.getVertex(D);
        descendants = classes.getSub(initialNode);
        assertEquals(descendants.size(), 1);
        assertTrue(descendants.contains(new Equivalences<ClassExpression>(ImmutableSet.of(C, D))));
        initialNode = classes.getVertex(F);
        descendants = classes.getSub(initialNode);
        assertEquals(sizeOf(descendants), 6);
        assertTrue(descendants.contains(classes.getVertex(A)));
        assertTrue(descendants.contains(classes.getVertex(B)));
        assertTrue(descendants.contains(classes.getVertex(C)));
        assertTrue(descendants.contains(classes.getVertex(D)));
        assertTrue(descendants.contains(new Equivalences<ClassExpression>(ImmutableSet.of(E, F))));
    }

    /**
	 * List all the ancestors of a class in the TBox with having equivalent
	 * classes into account.
	 */
    public void testAncestorClasses() throws Exception {
        final String ontoURI = "http://obda.inf.unibz.it/ontologies/test-class-hierarchy.owl#";
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile(inputFile1);
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        TestTBoxReasonerImpl_OnNamedDAG namedReasoner = new TestTBoxReasonerImpl_OnNamedDAG(dag);
        EquivalencesDAG<ClassExpression> classes = namedReasoner.getClassDAG();
        ClassExpression A = onto.getVocabulary().getClass(ontoURI + "A");
        ClassExpression B = onto.getVocabulary().getClass(ontoURI + "B");
        ClassExpression C = onto.getVocabulary().getClass(ontoURI + "C");
        ClassExpression D = onto.getVocabulary().getClass(ontoURI + "D");
        ClassExpression E = onto.getVocabulary().getClass(ontoURI + "E");
        ClassExpression F = onto.getVocabulary().getClass(ontoURI + "F");
        Equivalences<ClassExpression> initialNode = classes.getVertex(A);
        Set<Equivalences<ClassExpression>> ancestors = classes.getSuper(initialNode);
        assertEquals(sizeOf(ancestors), 4);
        assertTrue(ancestors.contains(classes.getVertex(B)));
        assertTrue(ancestors.contains(classes.getVertex(E)));
        assertTrue(ancestors.contains(classes.getVertex(F)));
        initialNode = classes.getVertex(B);
        ancestors = classes.getSuper(initialNode);
        assertEquals(sizeOf(ancestors), 3);
        assertTrue(ancestors.contains(classes.getVertex(F)));
        assertTrue(ancestors.contains(classes.getVertex(E)));
        initialNode = classes.getVertex(D);
        ancestors = classes.getSuper(initialNode);
        assertEquals(sizeOf(ancestors), 4);
        assertTrue(ancestors.contains(new Equivalences<ClassExpression>(ImmutableSet.of(C, D))));
        assertTrue(ancestors.contains(classes.getVertex(E)));
        assertTrue(ancestors.contains(classes.getVertex(F)));
        initialNode = classes.getVertex(F);
        ancestors = classes.getSuper(initialNode);
        assertEquals(ancestors.size(), 1);
        assertTrue(ancestors.contains(new Equivalences<ClassExpression>(ImmutableSet.of(E, F))));
    }

    /**
	 * List all the descendants of a role in the TBox with having equivalent
	 * roles into account.
	 */
    public void testDescendantRoles() throws Exception {
        final String ontoURI = "http://obda.inf.unibz.it/ontologies/test-role-hierarchy.owl#";
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile(inputFile2);
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        TestTBoxReasonerImpl_OnNamedDAG namedReasoner = new TestTBoxReasonerImpl_OnNamedDAG(dag);
        EquivalencesDAG<ObjectPropertyExpression> properties = namedReasoner.getObjectPropertyDAG();
        ObjectPropertyExpression P = onto.getVocabulary().getObjectProperty(ontoURI + "P");
        ObjectPropertyExpression S = onto.getVocabulary().getObjectProperty(ontoURI + "S");
        ObjectPropertyExpression R = onto.getVocabulary().getObjectProperty(ontoURI + "R");
        ObjectPropertyExpression Q = onto.getVocabulary().getObjectProperty(ontoURI + "Q");
        ObjectPropertyExpression T = onto.getVocabulary().getObjectProperty(ontoURI + "T");
        ObjectPropertyExpression U = onto.getVocabulary().getObjectProperty(ontoURI + "U");
        Equivalences<ObjectPropertyExpression> initialNode = properties.getVertex(P);
        Set<Equivalences<ObjectPropertyExpression>> descendants = properties.getSub(initialNode);
        assertEquals(descendants.size(), 1);
        initialNode = properties.getVertex(Q);
        descendants = properties.getSub(initialNode);
        assertEquals(descendants.size(), 2);
        assertTrue(descendants.contains(properties.getVertex(P)));
        initialNode = properties.getVertex(S);
        descendants = properties.getSub(initialNode);
        assertEquals(descendants.size(), 1);
        assertTrue(descendants.contains(new Equivalences<ObjectPropertyExpression>(ImmutableSet.of(R, S))));
        initialNode = properties.getVertex(U);
        descendants = properties.getSub(initialNode);
        assertEquals(sizeOf(descendants), 6);
        assertTrue(descendants.contains(properties.getVertex(P)));
        assertTrue(descendants.contains(properties.getVertex(Q)));
        assertTrue(descendants.contains(properties.getVertex(R)));
        assertTrue(descendants.contains(properties.getVertex(S)));
        assertTrue(descendants.contains(new Equivalences<ObjectPropertyExpression>(ImmutableSet.of(T, U))));
    }

    /**
	 * List all the ancestors of a role in the TBox with having equivalent roles
	 * into account.
	 */
    public void testAncestorRoles() throws Exception {
        final String ontoURI = "http://obda.inf.unibz.it/ontologies/test-role-hierarchy.owl#";
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile(inputFile2);
        TBoxReasoner dag = TBoxReasonerImpl.create(onto);
        TestTBoxReasonerImpl_OnNamedDAG namedReasoner = new TestTBoxReasonerImpl_OnNamedDAG(dag);
        EquivalencesDAG<ObjectPropertyExpression> properties = namedReasoner.getObjectPropertyDAG();
        ObjectPropertyExpression P = onto.getVocabulary().getObjectProperty(ontoURI + "P");
        ObjectPropertyExpression S = onto.getVocabulary().getObjectProperty(ontoURI + "S");
        ObjectPropertyExpression R = onto.getVocabulary().getObjectProperty(ontoURI + "R");
        ObjectPropertyExpression Q = onto.getVocabulary().getObjectProperty(ontoURI + "Q");
        ObjectPropertyExpression T = onto.getVocabulary().getObjectProperty(ontoURI + "T");
        ObjectPropertyExpression U = onto.getVocabulary().getObjectProperty(ontoURI + "U");
        Equivalences<ObjectPropertyExpression> initialNode = properties.getVertex(P);
        Set<Equivalences<ObjectPropertyExpression>> ancestors = properties.getSuper(initialNode);
        assertEquals(sizeOf(ancestors), 4);
        assertTrue(ancestors.contains(properties.getVertex(Q)));
        assertTrue(ancestors.contains(properties.getVertex(T)));
        assertTrue(ancestors.contains(properties.getVertex(U)));
        initialNode = properties.getVertex(Q);
        ancestors = properties.getSuper(initialNode);
        assertEquals(sizeOf(ancestors), 3);
        assertTrue(ancestors.contains(properties.getVertex(T)));
        assertTrue(ancestors.contains(properties.getVertex(U)));
        initialNode = properties.getVertex(S);
        ancestors = properties.getSuper(initialNode);
        assertEquals(sizeOf(ancestors), 4);
        assertTrue(ancestors.contains(new Equivalences<ObjectPropertyExpression>(ImmutableSet.of(R, S))));
        assertTrue(ancestors.contains(properties.getVertex(T)));
        assertTrue(ancestors.contains(properties.getVertex(U)));
        initialNode = properties.getVertex(U);
        ancestors = properties.getSuper(initialNode);
        assertEquals(ancestors.size(), 1);
        assertTrue(ancestors.contains(new Equivalences<ObjectPropertyExpression>(ImmutableSet.of(T, U))));
    }
}
