package it.unibz.krdb.obda.reformulation.tests;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import static org.junit.Assert.assertFalse;

/**
 * The following tests take the Stock exchange scenario and execute the queries
 * of the scenario to validate the results. The validation is simple, we only
 * count the number of distinct tuples returned by each query, which we know in
 * advance.
 * 
 * We execute the scenario in different modes, virtual, classic, with and
 * without optimizations.
 * 
 * The data is obtained from an inmemory database with the stock exchange
 * tuples. If the scenario is run in classic, this data gets imported
 * automatically by the reasoner.
 */
public class JoinElminationMappingTest {

    private OBDADataFactory fac;

    private Connection conn;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private OBDAModel obdaModel;

    private OWLOntology ontology;

    final String owlfile = "src/test/resources/test/ontologies/scenarios/join-elimination-test.owl";

    final String obdafile = "src/test/resources/test/ontologies/scenarios/join-elimination-test.obda";

    @Before
    public void setUp() throws Exception {
        String url = "jdbc:h2:mem:questjunitdb";
        String username = "sa";
        String password = "";
        fac = OBDADataFactoryImpl.getInstance();
        conn = DriverManager.getConnection(url, username, password);
        Statement st = conn.createStatement();
        String createStr = "CREATE TABLE address (" + "id integer NOT NULL," + "street character varying(100)," + "number integer," + "city character varying(100)," + "state character varying(100)," + "country character varying(100), PRIMARY KEY(id)" + ");";
        st.executeUpdate(createStr);
        conn.commit();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument((new File(owlfile)));
        obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(new File(obdafile));
    }

    @After
    public void tearDown() throws Exception {
        dropTables();
        conn.close();
    }

    private void dropTables() throws SQLException, IOException {
        Statement st = conn.createStatement();
        st.executeUpdate("DROP TABLE address;");
        st.close();
        conn.commit();
    }

    private void runTests(QuestPreferences p) throws Exception {
        QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
        QuestOWL reasoner = factory.createReasoner(ontology, config);
        reasoner.flush();
        // Now we are ready for querying
        QuestOWLStatement st = (QuestOWLStatement) reasoner.getStatement();
        boolean fail = false;
        String query = "PREFIX : <http://it.unibz.krdb/obda/ontologies/join-elimination-test.owl#> \n" + "SELECT ?x WHERE {?x :R ?y. ?y a :A}";
        try {
            System.out.println("\n\nSQL:\n" + st.getUnfolding(query));
            QuestOWLResultSet rs = st.executeTuple(query);
            rs.nextRow();
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            fail = true;
        }
        /* Closing resources */
        st.close();
        reasoner.dispose();
        /* Comparing and printing results */
        assertFalse(fail);
    }

    @Test(expected = IllegalConfigurationException.class)
    public void testSiEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
        runTests(p);
    }

    @Test(expected = IllegalConfigurationException.class)
    public void testSiEqNoSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
        runTests(p);
    }

    @Test(expected = IllegalConfigurationException.class)
    public void testSiNoEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "false");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
        runTests(p);
    }

    @Test(expected = IllegalConfigurationException.class)
    public void testSiNoEqNoSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "false");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
        runTests(p);
    }

    /*
	 * Direct Mapping
	 */
    public void disabletestDiEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.DIRECT);
        runTests(p);
    }

    public void disabletestDiEqNoSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.DIRECT);
        runTests(p);
    }

    /**
	 * This is a very slow test, disable it if you are doing routine checks.
	 * 
	 * @throws Exception
	 */
    public void disabletestDiNoEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "false");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.DIRECT);
        runTests(p);
    }

    /**
	 * This is a very slow test, disable it if you are doing routine checks.
	 * 
	 * @throws Exception
	 */
    public void disabletestDiNoEqNoSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "false");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.DIRECT);
        runTests(p);
    }

    @Test
    public void testViEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        runTests(p);
    }

    @Test
    public void testViEqNoSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        runTests(p);
    }

    /**
	 * This is a very slow test, disable it if you are doing routine checks.
	 */
    @Test
    public void testViNoEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "false");
        runTests(p);
    }

    /**
	 * This is a very slow test, disable it if you are doing routine checks.
	 */
    @Test
    public void testViNoEqNoSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "false");
        runTests(p);
    }
}
