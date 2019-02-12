package it.unibz.krdb.obda.quest.dag;

import it.unibz.krdb.obda.ontology.*;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.SemanticIndexBuilder;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.SemanticIndexRange;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import junit.framework.TestCase;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Map.Entry;

public class S_Indexes_Compare extends TestCase {

    ArrayList<String> input = new ArrayList<String>();

    Logger log = LoggerFactory.getLogger(S_HierarchyTestNewDAG.class);

    public S_Indexes_Compare(String name) {
        super(name);
    }

    public void setUp() {
        input.add("src/test/resources/test/equivalence/test_404.owl");
    }

    public void testIndexes() throws Exception {
        for (int i = 0; i < input.size(); i++) {
            String fileInput = input.get(i);
            TBoxReasoner dag = TBoxReasonerImpl.create(OWLAPITranslatorUtility.loadOntologyFromFile(fileInput));
            SemanticIndexBuilder engine = new SemanticIndexBuilder(dag);
            log.debug("Input number {}", i + 1);
            testIndexes(engine, dag);
            Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile(fileInput);
            DAG dag2 = DAGConstructor.getISADAG(onto);
            dag2.clean();
            DAGOperations.buildDescendants(dag2);
            DAGOperations.buildAncestors(dag2);
            DAG pureIsa = DAGConstructor.filterPureISA(dag2, onto.getVocabulary());
            pureIsa.clean();
            pureIsa.index();
            DAGOperations.buildDescendants(pureIsa);
            DAGOperations.buildAncestors(pureIsa);
            testOldIndexes(pureIsa, engine);
        }
    }

    private void testOldIndexes(DAG d1, SemanticIndexBuilder d2) {
        for (DAGNode d : d1.getClasses()) {
            System.out.println(d + "\n " + d.getEquivalents());
            System.out.println(d1.equi_mappings.values());
        }
        for (DAGNode d : d1.getRoles()) {
            System.out.println(d);
            for (DAGNode dd : d.getEquivalents()) {
                System.out.println(d1.getRoleNode(((ObjectPropertyExpression) dd.getDescription())));
                ;
            }
            ;
        }
    }

    private boolean testIndexes(SemanticIndexBuilder engine, TBoxReasoner reasoner) {
        boolean result = false;
        SimpleDirectedGraph<ObjectPropertyExpression, DefaultEdge> namedOP = SemanticIndexBuilder.getNamedDAG(reasoner.getObjectPropertyDAG());
        for (Entry<ObjectPropertyExpression, SemanticIndexRange> vertex : engine.getIndexedObjectProperties()) {
            int index = vertex.getValue().getIndex();
            log.info("vertex {} index {}", vertex, index);
            for (ObjectPropertyExpression parent : Graphs.successorListOf(namedOP, vertex.getKey())) {
                result = engine.getRange(parent).contained(new SemanticIndexRange(index));
                if (result) {
                    return result;
                }
            }
        }
        SimpleDirectedGraph<DataPropertyExpression, DefaultEdge> namedDP = SemanticIndexBuilder.getNamedDAG(reasoner.getDataPropertyDAG());
        for (Entry<DataPropertyExpression, SemanticIndexRange> vertex : engine.getIndexedDataProperties()) {
            int index = vertex.getValue().getIndex();
            log.info("vertex {} index {}", vertex, index);
            for (DataPropertyExpression parent : Graphs.successorListOf(namedDP, vertex.getKey())) {
                result = engine.getRange(parent).contained(new SemanticIndexRange(index));
                if (result) {
                    return result;
                }
            }
        }
        SimpleDirectedGraph<ClassExpression, DefaultEdge> namedCL = SemanticIndexBuilder.getNamedDAG(reasoner.getClassDAG());
        for (Entry<ClassExpression, SemanticIndexRange> vertex : engine.getIndexedClasses()) {
            int index = vertex.getValue().getIndex();
            log.info("vertex {} index {}", vertex, index);
            for (ClassExpression parent : Graphs.successorListOf(namedCL, vertex.getKey())) {
                result = engine.getRange((OClass) parent).contained(new SemanticIndexRange(index));
                if (result) {
                    return result;
                }
            }
        }
        return result;
    }
}
