package it.unibz.krdb.obda.quest.dag;

import it.unibz.krdb.obda.ontology.ClassExpression;
import it.unibz.krdb.obda.ontology.DataPropertyExpression;
import it.unibz.krdb.obda.ontology.OClass;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;
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

public class S_Indexes_TestNewDAG extends TestCase {

    ArrayList<String> input = new ArrayList<String>();

    Logger log = LoggerFactory.getLogger(S_HierarchyTestNewDAG.class);

    public S_Indexes_TestNewDAG(String name) {
        super(name);
    }

    public void setUp() {
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
        input.add("src/test/resources/test/newDag/inverseEquivalents6b.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents7.owl");
        input.add("src/test/resources/test/newDag/inverseEquivalents8.owl");
    }

    public void testIndexes() throws Exception {
        for (int i = 0; i < input.size(); i++) {
            String fileInput = input.get(i);
            TBoxReasoner dag = TBoxReasonerImpl.create(OWLAPITranslatorUtility.loadOntologyFromFile(fileInput));
            SemanticIndexBuilder engine = new SemanticIndexBuilder(dag);
            log.debug("Input number {}", i + 1);
            log.info("named graph {}", engine);
            assertTrue(testIndexes(engine, dag));
        }
    }

    private boolean testIndexes(SemanticIndexBuilder engine, TBoxReasoner reasoner) {
        boolean result = true;
        SimpleDirectedGraph<ObjectPropertyExpression, DefaultEdge> namedOP = SemanticIndexBuilder.getNamedDAG(reasoner.getObjectPropertyDAG());
        for (Entry<ObjectPropertyExpression, SemanticIndexRange> vertex : engine.getIndexedObjectProperties()) {
            int index = vertex.getValue().getIndex();
            log.info("vertex {} index {}", vertex, index);
            for (ObjectPropertyExpression parent : Graphs.successorListOf(namedOP, vertex.getKey())) {
                result = engine.getRange(parent).contained(new SemanticIndexRange(index));
                if (!result) {
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
                if (!result) {
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
                if (!result) {
                    return result;
                }
            }
        }
        return result;
    }
}
