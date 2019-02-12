package it.unibz.krdb.obda.reformulation.tests;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import junit.framework.TestCase;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class TMappingConstantPositionsTest extends TestCase {

    private OBDADataFactory fac;

    private Connection conn;

    private OBDAModel obdaModel;

    private OWLOntology ontology;

    final String owlfile = "src/test/resources/test/tmapping-positions.owl";

    final String obdafile = "src/test/resources/test/tmapping-positions.obda";

    @Override
    public void setUp() throws Exception {
        String url = "jdbc:h2:mem:questjunitdb";
        String username = "sa";
        String password = "";
        fac = OBDADataFactoryImpl.getInstance();
        conn = DriverManager.getConnection(url, username, password);
        Statement st = conn.createStatement();
        FileReader reader = new FileReader("src/test/resources/test/tmapping-positions-create-h2.sql");
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
        FileReader reader = new FileReader("src/test/resources/test/tmapping-positions-drop-h2.sql");
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
        String query = "PREFIX : <http://it.unibz.krdb/obda/test/simple#> SELECT * WHERE { ?x a :A. }";
        try {
            QuestOWLResultSet rs = st.executeTuple(query);
            assertTrue(rs.nextRow());
            assertTrue(rs.nextRow());
            assertTrue(rs.nextRow());
            assertFalse(rs.nextRow());
        } catch (Exception e) {
            throw e;
        } finally {
            st.close();
        }
    }

    @Test
    public void testViEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        runTests(p);
    }

    @Test
    public void testClassicEqSig() throws Exception {
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
        runTests(p);
    }
}
