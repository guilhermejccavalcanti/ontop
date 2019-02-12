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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ReasonerInternalException;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test is adapted from SimpleMappingVirtualABoxTest.
 *
 * A simple test that check if the system is able to handle Mappings for
 * classes/roles and attributes even if there are no URI templates. i.e., the
 * database stores URI's directly.
 *
 * We are going to create an H2 DB, the .sql file is fixed. We will map directly
 * there and then query on top.
 */
public class MetaMappingVirtualABoxMissingColumnTest {

    private OBDADataFactory fac;

    private Connection conn;

    Logger log = LoggerFactory.getLogger(this.getClass());

    private OBDAModel obdaModel;

    private OWLOntology ontology;

    final String owlfile = "src/test/resources/test/metamapping.owl";

    final String obdafile = "src/test/resources/test/metamapping_broken.obda";

    @Before
    public void setUp() throws Exception {
        String url = "jdbc:h2:mem:questjunitdb2_broken;DATABASE_TO_UPPER=FALSE";
        String username = "sa";
        String password = "";
        fac = OBDADataFactoryImpl.getInstance();
        conn = DriverManager.getConnection(url, username, password);
        Statement st = conn.createStatement();
        FileReader reader = new FileReader("src/test/resources/test/metamapping-create-h2.sql");
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

    @After
    public void tearDown() throws Exception {
        dropTables();
        conn.close();
    }

    private void dropTables() throws SQLException, IOException {
        Statement st = conn.createStatement();
        FileReader reader = new FileReader("src/test/resources/test/metamapping-drop-h2.sql");
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

    private void runTests(QuestPreferences p) throws Exception {
        QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
        String query1 = "PREFIX : <http://it.unibz.krdb/obda/test/simple#> SELECT * WHERE { ?x a :A_1 }";
        try (QuestOWL reasoner = factory.createReasoner(ontology, config);
            // Now we are ready for querying
            QuestOWLConnection conn = reasoner.getConnection();
            QuestOWLStatement st = conn.createStatement();
            QuestOWLResultSet rs1 = st.executeTuple(query1)) {
            assertTrue(rs1.nextRow());
            OWLIndividual ind = rs1.getOWLIndividual("x");
            //OWLIndividual ind2 = rs.getOWLIndividual("y");
            //OWLLiteral val = rs.getOWLLiteral("z");
            assertEquals("<uri1>", ind.toString());
        //assertEquals("<uri1>", ind2.toString());
        //assertEquals("\"value1\"", val.toString());
        }
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testViEqSig() throws Exception {
        expectedEx.expect(ReasonerInternalException.class);
        expectedEx.expectMessage("The placeholder \'code1\' in the target does not occur in the body of the mapping");
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        runTests(p);
    }
}
