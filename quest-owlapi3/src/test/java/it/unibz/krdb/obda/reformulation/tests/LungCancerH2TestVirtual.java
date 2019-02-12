package it.unibz.krdb.obda.reformulation.tests;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import junit.framework.TestCase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

/***
 * A simple test that check if the system is able to handle Mappings for
 * classes/roles and attributes even if there are no URI templates. i.e., the
 * database stores URI's directly.
 * 
 * We are going to create an H2 DB, the .sql file is fixed. We will map directly
 * there and then query on top.
 */
public class LungCancerH2TestVirtual extends TestCase {

    // TODO We need to extend this test to import the contents of the mappings
    // into OWL and repeat everything taking form OWL
    private OBDADataFactory fac;

    private Connection conn;

    Logger log = LoggerFactory.getLogger(this.getClass());

    private OBDAModel obdaModel;

    private OWLOntology ontology;

    final String owlfile = "src/test/resources/test/lung-cancer3.owl";

    final String obdafile = "src/test/resources/test/lung-cancer3.obda";

    @Override
    public void setUp() throws Exception {
        String url = "jdbc:h2:mem:questjunitdb";
        String username = "sa";
        String password = "";
        fac = OBDADataFactoryImpl.getInstance();
        conn = DriverManager.getConnection(url, username, password);
        Statement st = conn.createStatement();
        FileReader reader = new FileReader("src/test/resources/test/lung-cancer3-create-h2.sql");
        BufferedReader in = new BufferedReader(reader);
        StringBuilder bf = new StringBuilder();
        String line = in.readLine();
        while (line != null) {
            bf.append(line + "\n");
            line = in.readLine();
        }
        st.executeUpdate(bf.toString());
        conn.commit();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument((new File(owlfile)));
        obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(obdafile);
    }

    @Override
    public void tearDown() throws Exception {
        dropTables();
        conn.close();
    }

    private void dropTables() throws SQLException, IOException {
        Statement st = conn.createStatement();
        FileReader reader = new FileReader("src/test/resources/test/lung-cancer3-drop-h2.sql");
        BufferedReader in = new BufferedReader(reader);
        StringBuilder bf = new StringBuilder();
        String line = in.readLine();
        while (line != null) {
            bf.append(line);
            line = in.readLine();
        }
        st.executeUpdate(bf.toString());
        st.close();
        conn.commit();
    }

    private void runTests(QuestPreferences p) throws Exception {
        QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
        QuestOWL reasoner = factory.createReasoner(ontology, config);
        // Now we are ready for querying
        QuestOWLConnection conn = reasoner.getConnection();
        QuestOWLStatement st = conn.createStatement();
        String query1 = "PREFIX : <http://example.org/> SELECT * WHERE { ?x :hasNeoplasm <http://example.org/db1/neoplasm/1> }";
        String query2 = "PREFIX : <http://example.org/> SELECT * WHERE { <http://example.org/db1/1> :hasNeoplasm ?y }";
        String query3 = "PREFIX : <http://example.org/> SELECT * WHERE { ?y :hasStage <http://example.org/stages/II> }";
        String query4 = "PREFIX : <http://example.org/> SELECT * WHERE { ?y :hasStage <http://example.org/stages/limited> }";
        String query5 = "PREFIX : <http://example.org/db2/neoplasm/> DESCRIBE :1";
        String query6 = "PREFIX : <http://example.org/> DESCRIBE ?y WHERE { ?x :hasCondition ?y . ?y a :Cancer . } ";
        String query7 = "PREFIX : <http://example.org/> DESCRIBE <http://example.org/db1/1>";
        String query8 = "PREFIX : <http://example.org/> DESCRIBE ?x WHERE { ?x a <http://example.org/Person> . } ";
        String query9 = "PREFIX : <http://example.org/> CONSTRUCT { ?x ?y ?z } WHERE {  { ?x :name \"Mary\" .   ?x ?y ?z  } UNION {  ?x2 :name \"Mary\" . ?x2 :hasCondition ?x .   ?x ?y ?z } } ";
        String query = "PREFIX : <http://example.org/> SELECT * WHERE { ?y :hasStage <http://example.org/stages/limi> }";
        try {
            executeQueryAssertResults(query, st, 1);
        //			executeQueryAssertResults(query3, st, 1);
        //			executeQueryAssertResults(query4, st, 2);
        //			executeQueryAssertResults(query1, st, 2);
        //			executeQueryAssertResults(query2, st, 1);
        //			
        //			
        //			executeGraphQueryAssertResults(query5, st, 8);
        //			executeGraphQueryAssertResults(query6, st, 34);
        //			executeGraphQueryAssertResults(query7, st, 4);
        //			executeGraphQueryAssertResults(query8, st, 16);
        //			executeGraphQueryAssertResults(query9, st, 9);
        // NOTE CHECK THE CONTENT OF THIS QUERY, it seems to return the correct number of results but incorrect content, compare to the SELECT version which is correct
        } catch (Exception e) {
            throw e;
        } finally {
            try {
            } catch (Exception e) {
                st.close();
                throw e;
            }
            conn.close();
            reasoner.dispose();
        }
    }

    public void executeQueryAssertResults(String query, QuestOWLStatement st, int expectedRows) throws Exception {
        QuestOWLResultSet rs = st.executeTuple(query);
        int count = 0;
        while (rs.nextRow()) {
            count++;
            for (int i = 1; i <= rs.getColumnCount(); i++) {
                System.out.print(rs.getSignature().get(i - 1));
                System.out.print("=" + rs.getOWLObject(i));
                System.out.print(" ");
            }
            System.out.println();
        }
        rs.close();
        assertEquals(expectedRows, count);
    }

    public void executeGraphQueryAssertResults(String query, QuestOWLStatement st, int expectedRows) throws Exception {
        List<OWLAxiom> rs = st.executeGraph(query);
        int count = 0;
        Iterator<OWLAxiom> axit = rs.iterator();
        while (axit.hasNext()) {
            System.out.println(axit.next());
            count++;
        }
        assertEquals(expectedRows, count);
    }

    public void testViEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        runTests(p);
    }
}
