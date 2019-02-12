package it.unibz.krdb.obda.owlrefplatform.sql;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import static org.junit.Assert.assertFalse;

/**
 * Tests that the generated SQL contains no blank lines
 * This leads to problems if the SQL is copied to the oracle command line client
 * @author Dag Hovland
 *
 */

public class TestSQLBlankLines {


	private OBDADataFactory fac;
	private QuestOWLConnection conn;

	Logger log = LoggerFactory.getLogger(this.getClass());
	private OBDAModel obdaModel;
	private OWLOntology ontology;
	private QuestOWLFactory factory;


	private QuestOWL reasoner;


	final String owlfile = "src/test/resources/sqlgenerator/blanklines.owl";
	final String obdafile = "src/test/resources/sqlgenerator/blanklines.obda";

	private Connection sqlConnection;




	@After
	public void tearDown() throws Exception{
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


	@Before
	public void setUp() throws Exception {
		try {
			sqlConnection= DriverManager.getConnection("jdbc:h2:mem:countries","sa", "");
			java.sql.Statement s = sqlConnection.createStatement();

			try {
				String text = new Scanner( new File("src/test/resources/sqlgenerator/create-h2.sql") ).useDelimiter("\\A").next();
				s.execute(text);
				//Server.startWebServer(sqlConnection);

			} catch(SQLException sqle) {
				System.out.println("Exception in creating db from script");
			}

			s.close();
			// Loading the OWL file
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			ontology = manager.loadOntologyFromOntologyDocument((new File(owlfile)));

			// Loading the OBDA data
			fac = OBDADataFactoryImpl.getInstance();
			obdaModel = fac.getOBDAModel();

			ModelIOManager ioManager = new ModelIOManager(obdaModel);
			ioManager.load(obdafile);

			QuestPreferences p = new QuestPreferences();
			p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
			p.setCurrentValueOf(QuestPreferences.OBTAIN_FULL_METADATA, QuestConstants.FALSE);
			// Creating a new instance of the reasoner
			this.factory = new QuestOWLFactory();
	        QuestOWLConfiguration config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
	        reasoner = factory.createReasoner(ontology, config);

			
		} catch (Exception exc) {
			try {
				tearDown();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}	
	}


	@Test
	public void testNoSQLBlankLines() throws Exception {

		// Now we are ready for querying
		conn = reasoner.getConnection();
		
		String query = "PREFIX : <http://www.semanticweb.org/ontologies/2013/7/untitled-ontology-150#> SELECT * WHERE {?x a :Class1}";
		QuestOWLStatement st = conn.createStatement();


		String sql = st.getUnfolding(query);
		boolean m = sql.matches("(?ms)(.*)\\n\\n(.*)");
		assertFalse(m);
	}

}
