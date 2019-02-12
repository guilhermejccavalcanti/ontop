package it.unibz.krdb.odba;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import it.unibz.krdb.obda.utils.SQLScriptRunner;
import org.junit.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/***
 * A simple test that check if the system is able to handle Mappings for
 * classes/roles and attributes even if there are no URI templates. i.e., the
 * database stores URI's directly.
 * 
 * We are going to create an H2 DB, the .sql file is fixed. We will map directly
 * there and then query on top.
 */
public class ReverseURITestH2 {

    // TODO We need to extend this test to import the contents of the mappings
    // into OWL and repeat everything taking form OWL
    private static OBDADataFactory fac;

    private static QuestOWLConnection conn;

    static Logger log = LoggerFactory.getLogger(ReverseURITestH2.class);

    private static OBDAModel obdaModel;

    private static OWLOntology ontology;

    static final String owlfile = "src/test/resources/reverse-uri-test.owl";

    static final String obdafile = "src/test/resources/reverse-uri-test.obda";

    private static QuestOWL reasoner;

    private static Connection sqlConnection;

    private static void runUpdateOnSQLDB(String sqlscript, Connection sqlConnection) throws Exception {
        Statement st = sqlConnection.createStatement();
        FileReader reader = new FileReader(sqlscript);
        BufferedReader in = new BufferedReader(reader);
        StringBuilder bf = new StringBuilder();
        String line = in.readLine();
        while (line != null) {
            bf.append(line);
            bf.append("\n");
            line = in.readLine();
        }
        in.close();
        System.out.println(bf.toString());
        st.executeUpdate(bf.toString());
        if (!sqlConnection.getAutoCommit()) {
            sqlConnection.commit();
        }
    }

    @Before
    public void init() {
    }

    @After
    public void after() {
    }

    @BeforeClass
    public static void setUp() throws Exception {
        String url = "jdbc:h2:mem:questrepository;";
        String username = "fish";
        String password = "fish";
        System.out.println("Test");
        fac = OBDADataFactoryImpl.getInstance();
        try {
            sqlConnection = DriverManager.getConnection(url, username, password);
            FileReader reader = new FileReader("src/test/resources/reverse-uri-test.sql");
            BufferedReader in = new BufferedReader(reader);
            SQLScriptRunner runner = new SQLScriptRunner(sqlConnection, true, false);
            runner.runScript(in);
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
        } catch (Exception e) {
            System.err.println(e.getMessage());
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileReader reader = new FileReader("src/test/resources/reverse-uri-test.sql.drop");
        BufferedReader in = new BufferedReader(reader);
        SQLScriptRunner runner = new SQLScriptRunner(sqlConnection, true, false);
        runner.runScript(in);
        conn.close();
        reasoner.dispose();
        if (!sqlConnection.isClosed()) {
            java.sql.Statement s = sqlConnection.createStatement();
            try {
                s.execute("DROP ALL OBJECTS DELETE FILES");
            } catch (SQLException sqle) {
                System.out.println("Table not found, not dropping");
            } finally {
                s.close();
                sqlConnection.close();
            }
        }
    }

    private void runTests(String query, int numberOfResults) throws Exception {
        QuestOWLStatement st = conn.createStatement();
        try {
            QuestOWLResultSet rs = st.executeTuple(query);
            int count = 0;
            while (rs.nextRow()) {
                OWLObject ind1 = rs.getOWLObject("x");
                System.out.println("Result " + ind1.toString());
                count += 1;
            }
            org.junit.Assert.assertTrue(count == numberOfResults);
        } catch (Exception e) {
            throw e;
        } finally {
            try {
            } catch (Exception e) {
                st.close();
                org.junit.Assert.assertTrue(false);
            }
            conn.close();
            reasoner.dispose();
        }
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testSingleColum2() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-Cote%20D%22ivore> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testSingleColum() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-John%20Smith> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testTwoColum2() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-Cote%20D%22ivore-Cote%20D%22ivore> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testTwoColum2Value() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-John%20Smith-John%20Smith%202> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testTwoColum22Vaule() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-Cote%20D%22ivore-Cote%20D%22ivore%202> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testTwoColum() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-John%20Smith-John%20Smith> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testThreeColum2() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-Cote%20D%22ivore-Cote%20D%22ivore-Cote%20D%22ivore> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testThreeColum() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-John%20Smith-John%20Smith-John%20Smith> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testThreeColum23Value() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-Cote%20D%22ivore-Cote%20D%22ivore%202-Cote%20D%22ivore%203> a ?x}";
        runTests(query, 1);
    }

    /**
	 * Test use of two aliases to same table
	 * 
	 * @throws Exception
	 */
    @Test
    public void testThreeColum3Value() throws Exception {
        String query = "PREFIX : <http://www.ontop.org/> SELECT ?x WHERE {<http://www.ontop.org/test-John%20Smith-John%20Smith%202-John%20Smith%203> a ?x}";
        runTests(query, 1);
    }
}
