package it.unibz.krdb.obda.reformulation.tests;

import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class NPDTest {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @Test
    public void test_load_NPD() throws Exception {
        File ontDir = new File("src/test/resources/npd-v2");
        String path = ontDir.getAbsolutePath() + "/";
        String prfx = "file://" + path;
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-bfo"), IRI.create(prfx + "npd-bfo.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-db"), IRI.create(prfx + "npd-db")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-facility"), IRI.create(prfx + "npd-facility.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-geology"), IRI.create(prfx + "npd-geology.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-geometry"), IRI.create(prfx + "npd-geometry.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-org"), IRI.create(prfx + "npd-org.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-prod"), IRI.create(prfx + "npd-prod.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-seismic"), IRI.create(prfx + "npd-seismic.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-well"), IRI.create(prfx + "npd-well.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd"), IRI.create(prfx + "npd.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/sql"), IRI.create(prfx + "sql.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://sws.ifi.uio.no/vocab/version/20130919/npd-isc-2012"), IRI.create(prfx + "npd-isc-2012.owl")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://www.opengis.net/ont/geosparql"), IRI.create(prfx + "geosparql_vocab_all.xml")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://www.opengis.net/ont/gml"), IRI.create(prfx + "gml_32_geometries.xml")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://www.opengis.net/ont/sf"), IRI.create(prfx + "gml_32_geometries.xml")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://purl.org/dc/elements/1.1/"), IRI.create(prfx + "dc.xml")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://www.w3.org/2004/02/skos/core"), IRI.create(prfx + "skos.xml")));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create("http://www.ifomis.org/bfo/owl"), IRI.create(prfx + "bfo-1.1.owl")));
        OWLOntology owlOnto = manager.loadOntologyFromOntologyDocument(new File(path + "npd-v2.owl"));
        OBDADataFactory fac = OBDADataFactoryImpl.getInstance();
        OBDAModel obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(path + "npd.obda");
        QuestPreferences pref = new QuestPreferences();
        pref.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        pref.setCurrentValueOf(QuestPreferences.REFORMULATION_TECHNIQUE, QuestConstants.TW);
        pref.setCurrentValueOf(QuestPreferences.REWRITE, QuestConstants.TRUE);
        pref.setCurrentValueOf(QuestPreferences.PRINT_KEYS, QuestConstants.FALSE);
        setupDatabase();
        QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(pref).build();
        QuestOWL reasoner = factory.createReasoner(owlOnto, config);
        String q12 = "PREFIX : <http://sws.ifi.uio.no/vocab/npd-v2#>" + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + "PREFIX npd: <http://sws.ifi.uio.no/data/npd-v2/>" + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" + "PREFIX npdv: <http://sws.ifi.uio.no/vocab/npd-v2#>" + "SELECT DISTINCT ?unit ?well " + "WHERE { " + "[] npdv:wellboreStratumTopDepth     ?stratTop ; " + " npdv:wellboreStratumBottomDepth  ?stratBottom ; " + " npdv:stratumForWellbore          ?wellboreURI ; " + " npdv:inLithostratigraphicUnit [ npdv:name ?unit ] . " + "?wellboreURI npdv:name ?well . " + " " + "?core " + "npdv:coreForWellbore ?wellboreURI ." + "" + "{ ?core npdv:coreIntervalUOM \"m\"^^xsd:string ; " + "\t  npdv:coreIntervalTop     ?coreTopM ;" + " \t  npdv:coreIntervalBottom  ?coreBottomM ;" + "BIND(?coreTopM    AS ?coreTop) " + "BIND(?coreBottomM AS ?coreBottom) " + "}" + "UNION " + "{ ?core npdv:coreIntervalUOM \"ft\"^^xsd:string ; " + "\t  npdv:coreIntervalTop     ?coreTopFT ; " + " \t  npdv:coreIntervalBottom  ?coreBottomFT ; " + "BIND((?coreTopFT    * 0.3048) AS ?coreTop) " + "BIND((?coreBottomFT * 0.3048) AS ?coreBottom) " + "} " + "" + "} ORDER BY ?unit ?well";
        QuestOWLConnection qconn = reasoner.getConnection();
        QuestOWLStatement st = qconn.createStatement();
        String unfolding = st.getUnfolding(q12);
        st.close();
    }

    public void setupDatabase() throws SQLException, IOException {
        String url = "jdbc:h2:mem:npdv";
        String username = "sa";
        String password = "";
        Connection conn = DriverManager.getConnection(url, username, password);
        Statement st = conn.createStatement();
        int i = 0;
        FileReader reader = new FileReader("src/test/resources/npd-v2/npd-schema.sql");
        StringBuilder bf = new StringBuilder();
        try (BufferedReader in = new BufferedReader(reader)) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                bf.append(line + "\n");
                if (line.startsWith("--")) {
                    st.executeUpdate(bf.toString());
                    conn.commit();
                    bf = new StringBuilder();
                }
            }
        }
        DatabaseMetaData md = conn.getMetaData();
        try (ResultSet rsTables = md.getTables(null, null, null, new String[] { "TABLE", "VIEW" })) {
            int tbl = 0;
            while (rsTables.next()) {
                final String tblName = rsTables.getString("TABLE_NAME");
                tbl++;
            }
            assertEquals(tbl, 70);
        }
        List<String> pk = new LinkedList<String>();
        try (ResultSet rsPrimaryKeys = md.getPrimaryKeys(null, null, "FIELD_DESCRIPTION")) {
            while (rsPrimaryKeys.next()) {
                String colName = rsPrimaryKeys.getString("COLUMN_NAME");
                String pkName = rsPrimaryKeys.getString("PK_NAME");
                if (pkName != null) {
                    pk.add(colName);
                }
            }
        }
        try (ResultSet rsIndexes = md.getIndexInfo(null, null, "WELLBORE_CORE", true, true)) {
            while (rsIndexes.next()) {
                String colName = rsIndexes.getString("COLUMN_NAME");
                String indName = rsIndexes.getString("INDEX_NAME");
                boolean nonUnique = rsIndexes.getBoolean("NON_UNIQUE");
            }
        }
        System.out.println("Database schema created successfully");
    }
}
