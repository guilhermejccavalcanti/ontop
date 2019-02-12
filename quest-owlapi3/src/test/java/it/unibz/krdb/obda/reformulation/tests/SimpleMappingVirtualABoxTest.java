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
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.OWLObject;
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
import java.util.Properties;

/***
 * A simple test that check if the system is able to handle Mappings for
 * classes/roles and attributes even if there are no URI templates. i.e., the
 * database stores URI's directly.
 * 
 * We are going to create an H2 DB, the .sql file is fixed. We will map directly
 * there and then query on top.
 */
public class SimpleMappingVirtualABoxTest extends TestCase {

    // TODO We need to extend this test to import the contents of the mappings
    // into OWL and repeat everything taking form OWL
    private OBDADataFactory fac;

    private Connection conn;

    Logger log = LoggerFactory.getLogger(this.getClass());

    private OBDAModel obdaModel;

    private OWLOntology ontology;

    final String owlfile = "src/test/resources/test/simplemapping.owl";

    final String obdafile = "src/test/resources/test/simplemapping.obda";

    @Override
    public void setUp() throws Exception {
        String url = "jdbc:h2:mem:questjunitdb";
        String username = "sa";
        String password = "";
        fac = OBDADataFactoryImpl.getInstance();
        conn = DriverManager.getConnection(url, username, password);
        Statement st = conn.createStatement();
        FileReader reader = new FileReader("src/test/resources/test/simplemapping-create-h2.sql");
        BufferedReader in = new BufferedReader(reader);
        StringBuilder bf = new StringBuilder();
        String line = in.readLine();
        while (line != null) {
            bf.append(line);
            line = in.readLine();
        }
        in.close();
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
        FileReader reader = new FileReader("src/test/resources/test/simplemapping-drop-h2.sql");
        BufferedReader in = new BufferedReader(reader);
        StringBuilder bf = new StringBuilder();
        String line = in.readLine();
        while (line != null) {
            bf.append(line);
            line = in.readLine();
        }
        in.close();
        st.executeUpdate(bf.toString());
        st.close();
        conn.commit();
    }

    private void runTests(Properties p) throws Exception {
        QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).build();
        QuestOWL reasoner = factory.createReasoner(ontology, config);
        QuestOWLConnection conn = reasoner.getConnection();
        QuestOWLStatement st = conn.createStatement();
        String query = "PREFIX : <http://it.unibz.krdb/obda/test/simple#> SELECT * WHERE { ?x a :A; :P ?y; :U ?z; :P ?y; :U ?z; :P ?y ; :U ?z; :P ?y; :U ?z; :P ?y ; :U ?z; :P ?y; :U ?z; :P ?y ; :U ?z; :P ?y; :U ?z; :P ?y ; :U ?z; :P ?y; :U ?z; :P ?y ; :U ?z; :P ?y; :U ?z; :P ?y ; :U ?z; :P ?y; :U ?z; :P ?y ; :U ?z }";
        try {
            QuestOWLResultSet rs = st.executeTuple(query);
            assertTrue(rs.nextRow());
            OWLObject ind1 = rs.getOWLObject("x");
            OWLObject ind2 = rs.getOWLObject("y");
            OWLObject val = rs.getOWLObject("z");
            assertEquals("<uri1>", ToStringRenderer.getInstance().getRendering(ind1));
            assertEquals("<uri1>", ToStringRenderer.getInstance().getRendering(ind2));
            assertEquals("\"value1\"", ToStringRenderer.getInstance().getRendering(val));
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                st.close();
            } catch (Exception e) {
                throw e;
            } finally {
                conn.close();
                reasoner.dispose();
            }
        }
    }

    public void testViEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        runTests(p);
    }

    public void testClassicEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        runTests(p);
    }
}
