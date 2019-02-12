package it.unibz.krdb.obda.reformulation.tests;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.ontology.Assertion;
import it.unibz.krdb.obda.ontology.ClassAssertion;
import it.unibz.krdb.obda.ontology.ObjectPropertyAssertion;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.core.abox.QuestMaterializer;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

public class QuestOWLMaterializerTest extends TestCase {

    private Connection jdbcconn = null;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static QuestPreferences prefs;

    static {
        prefs = new QuestPreferences();
        prefs.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        prefs.setCurrentValueOf(QuestPreferences.REWRITE, QuestConstants.TRUE);
    }

    static {
        prefs = new QuestPreferences();
        prefs.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        prefs.setCurrentValueOf(QuestPreferences.REWRITE, QuestConstants.TRUE);
    }

    @Override
    public void setUp() throws Exception {
        createTables();
    }

    private String readSQLFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
        StringBuilder bf = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            bf.append(line + "\n");
            line = reader.readLine();
        }
        return bf.toString();
    }

    private void createTables() throws IOException, SQLException, URISyntaxException {
        String createDDL = readSQLFile("src/test/resources/test/materializer/createMaterializeTest.sql");
        String url = "jdbc:h2:mem:questjunitdb";
        String username = "sa";
        String password = "";
        jdbcconn = DriverManager.getConnection(url, username, password);
        Statement st = jdbcconn.createStatement();
        st.executeUpdate(createDDL);
        jdbcconn.commit();
    }

    @Override
    public void tearDown() throws Exception {
        dropTables();
        jdbcconn.close();
    }

    private void dropTables() throws SQLException, IOException {
        String dropDDL = readSQLFile("src/test/resources/test/materializer/dropMaterializeTest.sql");
        Statement st = jdbcconn.createStatement();
        st.executeUpdate(dropDDL);
        st.close();
        jdbcconn.commit();
    }

    public void testDataWithModel() throws Exception {
        File f = new File("src/test/resources/test/materializer/MaterializeTest.obda");
        OBDAModel model = OBDADataFactoryImpl.getInstance().getOBDAModel();
        ModelIOManager man = new ModelIOManager(model);
        man.load(f);
        QuestMaterializer mat = new QuestMaterializer(model, prefs, false);
        Iterator<Assertion> iterator = mat.getAssertionIterator();
        int classAss = 0;
        int propAss = 0;
        int objAss = 0;
        while (iterator.hasNext()) {
            Assertion assertion = iterator.next();
            if (assertion instanceof ClassAssertion) {
                classAss++;
            } else {
                if (assertion instanceof ObjectPropertyAssertion) {
                    objAss++;
                } else {
                    propAss++;
                }
            }
        }
        Assert.assertEquals(3, classAss);
        Assert.assertEquals(21, propAss);
        Assert.assertEquals(3, objAss);
    }

    public void testDataWithModelAndOnto() throws Exception {
        File f = new File("src/test/resources/test/materializer/MaterializeTest.obda");
        OBDAModel model = OBDADataFactoryImpl.getInstance().getOBDAModel();
        ModelIOManager man = new ModelIOManager(model);
        man.load(f);
        Ontology onto = OWLAPITranslatorUtility.loadOntologyFromFile("src/test/resources/test/materializer/MaterializeTest.owl");
        System.out.println(onto.getSubClassAxioms());
        System.out.println(onto.getSubObjectPropertyAxioms());
        System.out.println(onto.getSubDataPropertyAxioms());
        QuestMaterializer mat = new QuestMaterializer(model, onto, prefs, false);
        Iterator<Assertion> iterator = mat.getAssertionIterator();
        int classAss = 0;
        int propAss = 0;
        int objAss = 0;
        while (iterator.hasNext()) {
            Assertion assertion = iterator.next();
            if (assertion instanceof ClassAssertion) {
                classAss++;
            } else {
                if (assertion instanceof ObjectPropertyAssertion) {
                    objAss++;
                } else {
                    propAss++;
                }
            }
        }
        Assert.assertEquals(6, classAss);
        Assert.assertEquals(42, propAss);
        Assert.assertEquals(3, objAss);
    }
}
