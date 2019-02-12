package it.unibz.krdb.odba;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.ontology.*;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.Equivalences;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmptyEntitiesTest {

    private OBDADataFactory fac;

    private QuestOWLConnection conn;

    private Connection connection;

    Logger log = LoggerFactory.getLogger(this.getClass());

    private OBDAModel obdaModel;

    private OWLOntology ontology;

    //	final String owlfile = "src/test/resources/emptiesDatabase.owl";
    //	final String obdafile = "src/test/resources/emptiesDatabase.obda";
    final String owlfile = "src/main/resources/testcases-scenarios/virtual-mode/stockexchange/simplecq/stockexchange.owl";

    final String obdafile = "src/main/resources/testcases-scenarios/virtual-mode/stockexchange/simplecq/stockexchange-mysql.obda";

    private List<String> emptyConcepts = new ArrayList<String>();

    private List<String> emptyRoles = new ArrayList<String>();

    private Set<ClassExpression> emptyBasicConcepts = new HashSet<ClassExpression>();

    private Set<Description> emptyProperties = new HashSet<Description>();

    private QuestOWL reasoner;

    private Ontology onto;

    @Before
    public void setUp() throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument((new File(owlfile)));
        fac = OBDADataFactoryImpl.getInstance();
        obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(obdafile);
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FULL_METADATA, QuestConstants.FALSE);
        QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
        reasoner = factory.createReasoner(ontology, config);
        conn = reasoner.getConnection();
        onto = OWLAPITranslatorUtility.translate(ontology);
    }

    @After
    public void tearDown() throws Exception {
        reasoner.dispose();
    }

    //	private void dropTables() throws SQLException, IOException {
    //
    //		Statement st = connection.createStatement();
    //
    //		FileReader reader = new FileReader("src/test/resources/emptiesDatabase-drop-h2.sql");
    //		BufferedReader in = new BufferedReader(reader);
    //		StringBuilder bf = new StringBuilder();
    //		String line = in.readLine();
    //		while (line != null) {
    //			bf.append(line);
    //			line = in.readLine();
    //		}
    //
    //		st.executeUpdate(bf.toString());
    //		st.close();
    //		connection.commit();
    //	}
    private boolean runSPARQLConceptsQuery(String description) throws Exception {
        String query = "SELECT ?x WHERE {?x a " + description + ".}";
        QuestOWLStatement st = conn.createStatement();
        try {
            QuestOWLResultSet rs = st.executeTuple(query);
            return (rs.nextRow());
        } catch (Exception e) {
            throw e;
        } finally {
            try {
            } catch (Exception e) {
                st.close();
                throw e;
            }
            st.close();
        }
    }

    private boolean runSPARQLRolesQuery(String description) throws Exception {
        String query = "SELECT * WHERE {?x " + description + " ?y.}";
        QuestOWLStatement st = conn.createStatement();
        try {
            QuestOWLResultSet rs = st.executeTuple(query);
            return (rs.nextRow());
        } catch (Exception e) {
            throw e;
        } finally {
            try {
            } catch (Exception e) {
                st.close();
                throw e;
            }
            st.close();
        }
    }

    /**
	 * Test numbers of empty concepts
	 * 
	 * @throws Exception
	 */
    @Test
    public void testEmptyConcepts() throws Exception {
        int c = 0;
        for (OClass cl : onto.getVocabulary().getClasses()) {
            String concept = cl.getName();
            if (!runSPARQLConceptsQuery("<" + concept + ">")) {
                emptyConcepts.add(concept);
                c++;
            }
        }
        log.info(c + " Empty concept/s: " + emptyConcepts);
    }

    /**
	 * Test numbers of empty roles
	 * 
	 * @throws Exception
	 */
    @Test
    public void testEmptyRoles() throws Exception {
        int r = 0;
        for (ObjectPropertyExpression prop : onto.getVocabulary().getObjectProperties()) {
            String role = prop.getName();
            if (!runSPARQLRolesQuery("<" + role + ">")) {
                emptyRoles.add(role);
                r++;
            }
        }
        log.info(r + " Empty role/s: " + emptyRoles);
        r = 0;
        for (DataPropertyExpression prop : onto.getVocabulary().getDataProperties()) {
            String role = prop.getName();
            if (!runSPARQLRolesQuery("<" + role + ">")) {
                emptyRoles.add(role);
                r++;
            }
        }
        log.info(r + " Empty role/s: " + emptyRoles);
    }

    /**
	 * Test numbers of empty concepts and roles
	 * 
	 * @throws Exception
	 */
    @Test
    public void testEmpties() throws Exception {
        int c = 0;
        for (OClass cl : onto.getVocabulary().getClasses()) {
            String concept = cl.getName();
            if (!runSPARQLConceptsQuery("<" + concept + ">")) {
                emptyConcepts.add(concept);
                c++;
            }
        }
        log.info(c + " Empty concept/s: " + emptyConcepts);
        int r = 0;
        for (ObjectPropertyExpression prop : onto.getVocabulary().getObjectProperties()) {
            String role = prop.getName();
            if (!runSPARQLRolesQuery("<" + role + ">")) {
                emptyRoles.add(role);
                r++;
            }
        }
        log.info(r + " Empty role/s: " + emptyRoles);
        r = 0;
        for (DataPropertyExpression prop : onto.getVocabulary().getDataProperties()) {
            String role = prop.getName();
            if (!runSPARQLRolesQuery("<" + role + ">")) {
                emptyRoles.add(role);
                r++;
            }
        }
        log.info(r + " Empty role/s: " + emptyRoles);
    }

    /**
	 * Test numbers of empty concepts and roles considering existential and
	 * inverses
	 * Cannot work until inverses and existentials are considered  in the Abox
	 * @throws Exception
	 */
    // @Test
    public void testEmptiesWithInverses() throws Exception {
        TBoxReasoner tboxreasoner = TBoxReasonerImpl.create(onto);
        System.out.println();
        System.out.println(tboxreasoner.getObjectPropertyDAG());
        int c = 0;
        for (Equivalences<ClassExpression> concept : tboxreasoner.getClassDAG()) {
            ClassExpression representative = concept.getRepresentative();
            if ((!(representative instanceof Datatype)) && !runSPARQLConceptsQuery("<" + concept.getRepresentative().toString() + ">")) {
                emptyBasicConcepts.addAll(concept.getMembers());
                c += concept.size();
            }
        }
        log.info(c + " Empty concept/s: " + emptyConcepts);
        {
            int r = 0;
            for (Equivalences<ObjectPropertyExpression> properties : tboxreasoner.getObjectPropertyDAG()) {
                if (!runSPARQLRolesQuery("<" + properties.getRepresentative().toString() + ">")) {
                    emptyProperties.addAll(properties.getMembers());
                    r += properties.size();
                }
            }
            log.info(r + " Empty role/s: " + emptyRoles);
        }
        {
            int r = 0;
            for (Equivalences<DataPropertyExpression> properties : tboxreasoner.getDataPropertyDAG()) {
                if (!runSPARQLRolesQuery("<" + properties.getRepresentative().toString() + ">")) {
                    emptyProperties.addAll(properties.getMembers());
                    r += properties.size();
                }
            }
            log.info(r + " Empty role/s: " + emptyRoles);
        }
    }
}
