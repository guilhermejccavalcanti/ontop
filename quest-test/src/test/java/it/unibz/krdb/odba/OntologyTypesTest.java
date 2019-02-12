package it.unibz.krdb.odba;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import it.unibz.krdb.obda.r2rml.R2RMLReader;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.net.URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OntologyTypesTest {

    private OBDADataFactory fac;

    Logger log = LoggerFactory.getLogger(this.getClass());

    private OBDAModel obdaModel;

    private OWLOntology ontology;

    final String owlFile = "src/test/resources/ontologyType/dataPropertiesOntologyType.owl";

    final String obdaFile = "src/test/resources/ontologyType/dataPropertiesOntologyType.obda";

    final String r2rmlFile = "src/test/resources/ontologyType/dataPropertiesPrettyType.ttl";

    final String obdaErroredFile = "src/test/resources/ontologyType/erroredOntologyType.obda";

    @Before
    public void setUp() throws Exception {
        fac = OBDADataFactoryImpl.getInstance();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument((new File(owlFile)));
    }

    private void runTests(QuestPreferences p, String query, int numberResults) throws Exception {
        // Creating a new instance of the reasoner
        QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
        QuestOWL reasoner = factory.createReasoner(ontology, config);
        // Now we are ready for querying
        QuestOWLConnection conn = reasoner.getConnection();
        QuestOWLStatement st = conn.createStatement();
        try {
            executeQueryAssertResults(query, st, numberResults);
        } catch (Exception e) {
            st.close();
            e.printStackTrace();
            assertTrue(false);
        } finally {
            conn.close();
            reasoner.dispose();
        }
    }

    private void executeQueryAssertResults(String query, QuestOWLStatement st, int expectedRows) throws Exception {
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

    @Test
    public void testOntologyType() throws Exception {
        obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(obdaFile);
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        String query1 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :number ?y. FILTER(datatype(?y) = xsd:integer)}";
        runTests(p, query1, 0);
        String query1b = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :number ?y. FILTER(datatype(?y) = xsd:long)}";
        runTests(p, query1b, 3);
        String query2 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :assayName ?y. FILTER(datatype(?y) = xsd:string)}";
        runTests(p, query2, 3);
        String query3 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :hasDepartment ?y. FILTER(datatype(?y) = rdfs:Literal)}";
        runTests(p, query3, 3);
        String query4 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :AssayID ?y. FILTER(datatype(?y) = xsd:decimal)}";
        runTests(p, query4, 3);
        String query5 = "PREFIX franz: <http://www.franz.com/>" + "select * {?x  franz:solrDocid ?y. FILTER(datatype(?y) = xsd:long)}";
        runTests(p, query5, 3);
        String query6 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :hasSection ?y. FILTER(datatype(?y) = xsd:positiveInteger)}";
        runTests(p, query6, 3);
    }

    @Test
    public void testOntologyTypeR2rml() throws Exception {
        String jdbcurl = "jdbc:oracle:thin:@//10.7.20.91:1521/xe";
        String username = "system";
        String password = "obdaps83";
        String driverclass = "oracle.jdbc.driver.OracleDriver";
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        OBDADataFactory f = OBDADataFactoryImpl.getInstance();
        URI obdaURI = new File(r2rmlFile).toURI();
        String sourceUrl = obdaURI.toString();
        OBDADataSource dataSource = f.getJDBCDataSource(sourceUrl, jdbcurl, username, password, driverclass);
        log.info("Loading r2rml file");
        R2RMLReader reader = new R2RMLReader(r2rmlFile);
        obdaModel = reader.readModel(dataSource);
        String query1 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :number ?y. FILTER(datatype(?y) = xsd:integer)}";
        runTests(p, query1, 0);
        String query1b = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :number ?y. FILTER(datatype(?y) = xsd:long)}";
        runTests(p, query1b, 3);
        String query2 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :assayName ?y. FILTER(datatype(?y) = xsd:string)}";
        runTests(p, query2, 3);
        String query3 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :hasDepartment ?y. FILTER(datatype(?y) = rdfs:Literal)}";
        runTests(p, query3, 3);
        String query4 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :AssayID ?y. FILTER(datatype(?y) = xsd:decimal)}";
        runTests(p, query4, 3);
        String query5 = "PREFIX franz: <http://www.franz.com/>" + "select * {?x  franz:solrDocid ?y. FILTER(datatype(?y) = xsd:long)}";
        runTests(p, query5, 3);
        String query6 = "PREFIX : <http://www.company.com/ARES#>" + "select * {?x :hasSection ?y. FILTER(datatype(?y) = xsd:positiveInteger)}";
        runTests(p, query6, 3);
    }

    @Test
    public void failedMapping() throws Exception {
        obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(obdaErroredFile);
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        try {
            QuestOWLFactory factory = new QuestOWLFactory();
            QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
            QuestOWL reasoner = factory.createReasoner(ontology, config);
        } catch (Exception e) {
            assertEquals(e.getCause().getClass().getCanonicalName(), "it.unibz.krdb.obda.model.OBDAException");
        }
    }
}
