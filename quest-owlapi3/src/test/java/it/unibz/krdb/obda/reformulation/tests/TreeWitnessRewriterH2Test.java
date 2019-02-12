package it.unibz.krdb.obda.reformulation.tests;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.io.QueryIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import it.unibz.krdb.obda.querymanager.QueryController;
import it.unibz.krdb.obda.querymanager.QueryControllerEntity;
import it.unibz.krdb.obda.querymanager.QueryControllerQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
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
import java.util.LinkedList;
import java.util.List;
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
// GUOHUI: 2016-01-16 SI+Mapping mode is disabled
@Ignore
public class TreeWitnessRewriterH2Test {

    // TODO We need to extend this test to import the contents of the mappings
    // into OWL and repeat everything taking form OWL
    private OBDADataFactory fac;

    private Connection conn;

    Logger log = LoggerFactory.getLogger(this.getClass());

    private OBDAModel obdaModel = null;

    private OWLOntology ontology;

    List<TestQuery> testQueries = new LinkedList<TestQuery>();

    final String testCase = "twr-predicate";

    final String owlfile = "src/test/resources/test/treewitness/" + testCase + ".owl";

    final String obdafile = "src/test/resources/test/treewitness/" + testCase + ".obda";

    final String qfile = "src/test/resources/test/treewitness/" + testCase + ".q";

    /* These are the distinct tuples that we know each query returns */
    final int[] tuples = { 7, 0, 4, 1, 1, 2, 2, 1, 4, 3, 3, 0, 1, 0, -1, 2, 0, 2, 2, 0, 2, 2, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 2, 2, 0, 2, 2, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, 1, 0, 0, 0, 0, 5, 5, -1, 0, 5, -1, -1, 5, 2, 5, 2, 5, 5, 7, 0, 2, 1, 3, 2, 3, 1, 2, 2, 0, 0, 2, 0, 2, 1, 3, 2, 3, 1, 2, 5, 5, 10, 1, 2, 1, 3, 2, 3, 3, 5, 5, 3, 7, 7, 7, 3, 10 };

    public class TestQuery {

        public String id = "";

        public String query = "";

        public int distinctTuples = -1;
    }

    public class Result {

        public String id = "";

        public String query = "";

        public int distinctTuples = -1;

        public long timeelapsed = -1;
    }

    @Before
    public void setUp() throws Exception {
        String url = "jdbc:h2:mem:questjunitdb";
        String username = "sa";
        String password = "";
        fac = OBDADataFactoryImpl.getInstance();
        conn = DriverManager.getConnection(url, username, password);
        Statement st = conn.createStatement();
        FileReader reader = new FileReader("src/test/resources/test/treewitness/bsbm-schema-create-mysql.sql");
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
        ioManager.load(new File(obdafile));
    }

    @After
    public void tearDown() throws Exception {
        dropTables();
        conn.close();
    }

    private void dropTables() throws SQLException, IOException {
        Statement st = conn.createStatement();
        FileReader reader = new FileReader("src/test/resources/test/treewitness/bsbm-schema-drop-mysql.sql");
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

    private void prepareTestQueries(int[] answer) throws Exception {
        QueryController qcontroller = new QueryController();
        QueryIOManager qman = new QueryIOManager(qcontroller);
        qman.load(new File(qfile));
        int counter = 0;
        for (QueryControllerEntity entity : qcontroller.getElements()) {
            if (!(entity instanceof QueryControllerQuery)) {
                continue;
            }
            QueryControllerQuery query = (QueryControllerQuery) entity;
            TestQuery tq = new TestQuery();
            tq.id = query.getID();
            tq.query = query.getQuery();
            tq.distinctTuples = answer[counter];
            testQueries.add(tq);
            counter += 1;
        }
    }

    private void runTests(QuestPreferences p) throws Exception {
        // Creating a new instance of the reasoner
        QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().preferences(p).obdaModel(obdaModel).build();
        QuestOWL reasoner = factory.createReasoner(ontology, config);
        // Now we are ready for querying
        QuestOWLStatement st = reasoner.getStatement();
        List<Result> summaries = new LinkedList<TreeWitnessRewriterH2Test.Result>();
        boolean fail = false;
        int qc = 0;
        for (TestQuery tq : testQueries) {
            log.debug("Executing query: {}", qc);
            String query = tq.query;
            log.debug("Query: {}", query);
            qc += 1;
            int count = 0;
            long start = System.currentTimeMillis();
            long end = 0;
            try {
                if (query.toLowerCase().contains("select")) {
                    QuestOWLResultSet rs = st.executeTuple(query);
                    end = System.currentTimeMillis();
                    while (rs.nextRow()) {
                        count += 1;
                    }
                } else {
                    List<OWLAxiom> list = st.executeGraph(query);
                    count += list.size();
                }
            } catch (Exception e) {
                fail = true;
                log.debug(e.getMessage(), e);
                end = System.currentTimeMillis();
                count = -1;
            }
            Result summary = new Result();
            summary.id = tq.id;
            summary.query = query;
            summary.timeelapsed = end - start;
            summary.distinctTuples = count;
            summaries.add(summary);
        }
        /* Closing resources */
        st.close();
        reasoner.dispose();
        // /* Comparing and printing results */
        //
        // int totaltime = 0;
        // for (int i = 0; i < testQueries.size(); i++) {
        // TestQuery tq = testQueries.get(i);
        // Result summary = summaries.get(i);
        // totaltime += summary.timeelapsed;
        // fail = fail | tq.distinctTuples != summary.distinctTuples;
        // String out =
        // "Query: %3d   Tup. Ex.: %6d Tup. ret.: %6d    Time elapsed: %6.3f s     ";
        // log.debug(String.format(out, i, tq.distinctTuples,
        // summary.distinctTuples, (double) summary.timeelapsed / (double) 1000)
        // + "   " + (tq.distinctTuples == summary.distinctTuples ? " " :
        // "ERROR"));
        // }
        // log.debug("==========================");
        //		log.debug(String.format("Total time elapsed: %6.3f s", (double) totaltime / (double) 1000));
        assertFalse(fail);
    }

    @Test
    public void testViEqSig() throws Exception {
        prepareTestQueries(tuples);
        QuestPreferences p = new QuestPreferences();
        p.setCurrentValueOf(QuestPreferences.REFORMULATION_TECHNIQUE, QuestConstants.TW);
        p.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
        p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
        p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
        p.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "true");
        p.setProperty("rewrite", "true");
        runTests(p);
    }
}
