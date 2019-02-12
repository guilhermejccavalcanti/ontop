package inf.unibz.ontop.sesame.tests.general;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConfiguration;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLFactory;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import java.io.File;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;

public class InconsistencyCheckingVirtualTest {

    private String owlfile = "src/test/resources/example/BooksNoAxioms.owl";

    private String obdafile = "src/test/resources/example/exampleBooks.obda";

    private QuestOWL reasoner;

    private OWLOntology ontology;

    private OWLOntologyManager manager;

    private OBDAModel obdaModel;

    QuestPreferences p;

    String prefix = "http://meraka/moss/exampleBooks.owl#";

    OWLClass c1 = Class(IRI.create(prefix + "AudioBook"));

    OWLClass c2 = Class(IRI.create(prefix + "Book"));

    OWLObjectProperty r1 = ObjectProperty(IRI.create(prefix + "hasMother"));

    OWLObjectProperty r2 = ObjectProperty(IRI.create(prefix + "hasFather"));

    OWLDataProperty d1 = DataProperty(IRI.create(prefix + "title"));

    OWLDataProperty d2 = DataProperty(IRI.create(prefix + "name"));

    OWLNamedIndividual a = NamedIndividual(IRI.create(prefix + "a"));

    OWLNamedIndividual b = NamedIndividual(IRI.create(prefix + "b"));

    OWLNamedIndividual c = NamedIndividual(IRI.create(prefix + "c"));

    OWLNamedIndividual book = NamedIndividual(IRI.create(prefix + "book/23/"));

    @Before
    public void setUp() throws Exception {
        p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, QuestConstants.TRUE);
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, QuestConstants.TRUE);
        manager = OWLManager.createOWLOntologyManager();
        try {
            ontology = manager.loadOntologyFromOntologyDocument(new File(owlfile));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInitialConsistency() {
        startReasoner();
        assertTrue(reasoner.isQuestConsistent());
    }

    private void startReasoner() {
        obdaModel = OBDADataFactoryImpl.getInstance().getOBDAModel();
        ModelIOManager mng = new ModelIOManager(obdaModel);
        try {
            mng.load(new File(obdafile));
            QuestOWLFactory factory = new QuestOWLFactory();
            QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
            reasoner = factory.createReasoner(ontology, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDisjointClassInconsistency() throws OWLOntologyCreationException {
        manager.addAxiom(ontology, DisjointClasses(c1, c2));
        startReasoner();
        boolean consistent = reasoner.isQuestConsistent();
        assertFalse(consistent);
        manager.removeAxiom(ontology, DisjointClasses(c1, c2));
    }

    @Test
    public void testDisjointObjectPropInconsistency() throws OWLOntologyCreationException {
        manager.addAxiom(ontology, ObjectPropertyAssertion(r1, a, b));
        manager.addAxiom(ontology, ObjectPropertyAssertion(r2, a, b));
        manager.addAxiom(ontology, DisjointObjectProperties(r1, r2));
        startReasoner();
        boolean consistent = reasoner.isQuestConsistent();
        assertFalse(consistent);
        manager.removeAxiom(ontology, ObjectPropertyAssertion(r1, a, b));
        manager.removeAxiom(ontology, ObjectPropertyAssertion(r2, a, b));
        manager.removeAxiom(ontology, DisjointObjectProperties(r1, r2));
    }

    //@Test
    public void testDisjointDataPropInconsistency() throws OWLOntologyCreationException {
        manager.addAxiom(ontology, DataPropertyAssertion(d1, a, Literal(21)));
        manager.addAxiom(ontology, DataPropertyAssertion(d2, a, Literal(21)));
        manager.addAxiom(ontology, DisjointDataProperties(d1, d2));
        startReasoner();
        boolean consistent = reasoner.isQuestConsistent();
        assertFalse(consistent);
        manager.removeAxiom(ontology, DataPropertyAssertion(d1, a, Literal(21)));
        manager.removeAxiom(ontology, DataPropertyAssertion(d2, a, Literal(21)));
        manager.removeAxiom(ontology, DisjointDataProperties(d1, d2));
    }

    @Test
    public void testFunctionalObjPropInconsistency() throws OWLOntologyCreationException {
        manager.addAxiom(ontology, ObjectPropertyAssertion(r1, a, b));
        manager.addAxiom(ontology, ObjectPropertyAssertion(r1, a, c));
        manager.addAxiom(ontology, FunctionalObjectProperty(r1));
        startReasoner();
        boolean consistent = reasoner.isQuestConsistent();
        assertFalse(consistent);
        manager.removeAxiom(ontology, ObjectPropertyAssertion(r1, a, b));
        manager.removeAxiom(ontology, ObjectPropertyAssertion(r1, a, c));
        manager.removeAxiom(ontology, FunctionalObjectProperty(r1));
    }

    //@Test
    public void testFunctionalDataPropInconsistency() throws OWLOntologyCreationException {
        manager.addAxiom(ontology, DataPropertyAssertion(d2, book, Literal("Jules Verne")));
        manager.addAxiom(ontology, FunctionalDataProperty(d2));
        startReasoner();
        boolean consistent = reasoner.isQuestConsistent();
        assertFalse(consistent);
        manager.removeAxiom(ontology, DataPropertyAssertion(d1, a, Literal(18)));
        manager.removeAxiom(ontology, DataPropertyAssertion(d1, a, Literal(21)));
        manager.removeAxiom(ontology, FunctionalDataProperty(d1));
    }
}
