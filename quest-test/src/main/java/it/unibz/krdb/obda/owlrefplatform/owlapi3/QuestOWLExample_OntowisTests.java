package it.unibz.krdb.obda.owlrefplatform.owlapi3;

/*
 * #%L
 * ontop-quest-owlapi3
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.exception.InvalidPredicateDeclarationException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.sql.ImplicitDBConstraintsReader;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;




public class QuestOWLExample_OntowisTests {

    class Constants {
        static final int NUM_FILTERS = 3;
        static final int NUM_SQL_JOINS = 4;

        static final int NUM_RUNS = 2;
        static final int NUM_WARM_UPS = 4;
    }

	interface ParamConst{
		// Postgres
		public static final String POSTGRESInt = "src/main/resources/example/postgres-NoViews-joins-int.obda";
		public static final String POSTGRESStr = "src/main/resources/example/postgres-NoViews-joins-str.obda";
		public static final String POSTGRESIntView = "src/main/resources/example/postgres-Views-joins-int.obda";
		public static final String POSTGRESStrView = "src/main/resources/example/postgres-Views-joins-str.obda";
		
		// MySQL
		public static final String MYSQLInt = "src/main/resources/example/mysql-NoViews-joins-int.obda";
		public static final String MYSQLStr = "src/main/resources/example/mysql-NoViews-joins-str.obda";
		public static final String MYSQLIntView = "src/main/resources/example/mysql-Views-joins-int.obda";
		public static final String MYSQLStrView = "src/main/resources/example/mysql-Views-joins-str.obda";
		
		// DB2
		public static final String DB2Int = "src/main/resources/example/db2-NoViews-joins-int.obda";
		public static final String DB2IntView = "src/main/resources/example/db2-Views-joins-int.obda";
		
	}
	
	enum DBType {
		MYSQL, POSTGRES, SMALL_POSTGRES, DB2
	}
	
	public static class Settings{
		static String obdaFile;
		static DBType type;
		static boolean mKeys = false;
	}

	// private static final String QuestOWLExample_OntowisTests = null;
	final String obdaFile;
	final DBType dbType;
	boolean mKeys = false;

	final String owlfile = "src/main/resources/example/ontowis-5joins-int.owl";
	final String usrConstrinFile = "src/main/resources/example/funcCons.txt";


	// Internal Modifiable State
	QuestOWL reasoner ;

	public QuestOWLExample_OntowisTests(String obdaFile, DBType type, boolean mKeys){
		this.obdaFile = obdaFile;
		this.dbType = type;
		this.mKeys = mKeys;
	}

	// Exclude from T-Mappings
	//final String tMappingsConfFile = "src/main/resources/example/tMappingsConf.conf";

	public void runQuery(String obdaFile) throws Exception {

		//	queries[30]="PREFIX :	<http://www.example.org/>  SELECT ?x   WHERE {?x a  :4Tab1 .   } LIMIT 100000  ";

		QuestOWLConnection conn =  createStuff(mKeys);

		// Results
//		String[] resultsOne = new String[31];
//		String[] resultsTwo = new String[31];
//		String[] resultsThree = new String[31];

		// Create Queries to be run
		QueryFactory queryFactory = new QueryFactory(dbType);

		// Run the tests on the queries


        List<List<Long>> resultsOne_list = new ArrayList<>(); // There is a list for each run.
        List<List<Long>> resultsTwo_list = new ArrayList<>();
        List<List<Long>> resultsThree_list = new ArrayList<>();


        runQueries(conn, queryFactory.warmUpQueries);


        for(int i = 0; i < Constants.NUM_RUNS; i++) {
            List<Long> resultsOne = runQueries(conn, queryFactory.queriesOneSPARQL);
            resultsOne_list.add(resultsOne);

            List<Long> resultsTwo = runQueries(conn, queryFactory.queriesTwoSPARQL);
            resultsTwo_list.add(resultsTwo);

            List<Long> resultsThree = runQueries(conn, queryFactory.queriesThreeSPARQL);
            resultsThree_list.add(resultsThree);
        }
		closeEverything(conn);

        List<Long> avg_resultsOne = average(resultsOne_list);
        List<Long> avg_resultsTwo = average(resultsTwo_list);
        List<Long> avg_resultsThree = average(resultsThree_list);

		generateFile(avg_resultsOne, avg_resultsTwo, avg_resultsThree);

	}

    public List<Long> average(List<List<Long>> lists ){

        int numList = lists.size();

        int size = lists.get(0).size();

        List<Long> results = new ArrayList<>();

        for(int i = 0 ; i < size; i++){
            long sum = 0;
            for (List<Long> list : lists) {
                sum += list.get(i);
            }
            results.add(sum/numList);
        }
        return results;
    }

	/**
	 * @param resultsOne
	 * @param obdaFile 
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException 
	 */
	private void generateFile( List<Long> resultsOne, List<Long> resultsTwo, List<Long> resultsThree) throws FileNotFoundException, UnsupportedEncodingException {
		/*
		 * Generate File !
		 */
		PrintWriter writer = new PrintWriter("src/main/resources/example/table.txt", "UTF-8");
		PrintWriter writerG = new PrintWriter("src/main/resources/example/graph.txt", "UTF-8");

		int sizeQueriesArray = Constants.NUM_FILTERS * Constants.NUM_SQL_JOINS;
		int nF = Constants.NUM_FILTERS;
		
		int j=0;
		
		while (j<sizeQueriesArray){
			writer.println(resultsOne.get(j) + " & " + resultsTwo.get(j) + " & " + resultsThree.get(j)); // table

			if (j<Constants.NUM_FILTERS){
                String gline = "(1," + resultsOne.get(j) + ")" + "(2," + resultsTwo.get(j) + ")"
                        + "(3," + resultsThree.get(j) + ")" + "(4," + resultsOne.get(j + nF*1) + ")"
                        + "(5," + resultsTwo.get(j + nF*1) + ")" + "(6," + resultsThree.get(j + nF*1) + ")"
                        + "(7," + resultsOne.get(j + nF*2) + ")" + "(8," + resultsTwo.get(j + nF*2) + ")"
                        + "(9," + resultsThree.get(j + nF*2) + ")" + "(10," + resultsOne.get(j + nF*3) + ")"
                        + "(11," + resultsTwo.get(j + nF*3) + ")" + "(12," + resultsThree.get(j + nF*3) + ")";
                writerG.println(gline);
			}
			j++;
		}
		writer.close();
		writerG.close();
	}

	/**
	 * @param conn
	 * @throws OWLException
	 */
	private void closeEverything(QuestOWLConnection conn) throws OWLException {
		/*
		 * Close connection and resources
		 */

		if (conn != null && !conn.isClosed()) {
			conn.close();
		}
		this.reasoner.dispose();
	}

	/**
	 * @throws OBDAException 
	 * @throws OWLOntologyCreationException 
	 * @throws InvalidMappingException 
	 * @throws InvalidPredicateDeclarationException 
	 * @throws IOException 
	 * @throws OWLException
	 */
	private QuestOWLConnection createStuff(boolean manualKeys) throws OBDAException, OWLOntologyCreationException, IOException, InvalidPredicateDeclarationException, InvalidMappingException{

		/*
		 * Load the ontology from an external .owl file.
		 */
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(owlfile));

		/*
		 * Load the OBDA model from an externa
		 * l .obda file
		 */
		OBDADataFactory fac = OBDADataFactoryImpl.getInstance();
		OBDAModel obdaModel = fac.getOBDAModel();
		ModelIOManager ioManager = new ModelIOManager(obdaModel);
		ioManager.load(obdaFile);
		
		/*
		 * Prepare the configuration for the Quest instance. The example below shows the setup for
		 * "Virtual ABox" mode
		 */
		QuestPreferences preference = new QuestPreferences();
		preference.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
		//		TEST preference.setCurrentValueOf(QuestPreferences.T_MAPPINGS, QuestConstants.FALSE); // Disable T_Mappings

		/*
		 * Create the instance of Quest OWL reasoner.
		 */
		QuestOWLFactory factory = new QuestOWLFactory();
//		factory.setOBDAController(obdaModel);
//		factory.setPreferenceHolder(preference);

		/*
		 * USR CONSTRAINTS !!!!
		 */
		QuestOWLConfiguration config;
		if (manualKeys){
			System.out.println();
			ImplicitDBConstraintsReader constr = new ImplicitDBConstraintsReader(new File(usrConstrinFile));
			//factory.setImplicitDBConstraints(constr);
			config = QuestOWLConfiguration.builder().obdaModel(obdaModel).dbConstraintsReader(constr).build();
		} else {
			config = QuestOWLConfiguration.builder().obdaModel(obdaModel).build();
		}
		/*
		 * T-Mappings Handling!!
		 */
		//TMappingsConfParser tMapParser = new TMappingsConfParser(tMappingsConfFile);
		//factory.setExcludeFromTMappingsPredicates(tMapParser.parsePredicates());

		QuestOWL reasoner = factory.createReasoner(ontology, config);

		
		
		this.reasoner = reasoner;
		/*
		 * Prepare the data connection for querying.
		 */
		QuestOWLConnection conn = reasoner.getConnection();

		return conn;

	}


	private List<Long> runQueries(QuestOWLConnection conn, String[] queries) throws OWLException {
		
		//int nWarmUps = Constants.NUM_WARM_UPS;
		//int nRuns = Constants.NUM_RUNS;

        List<Long> results = new ArrayList<>();
		
		int j=0;
		while (j < queries.length){
			String sparqlQuery = queries[j];
			QuestOWLStatement st = conn.createStatement();
			try {

				long time = 0;
				int count = 0;
				
				//for (int i=0; i<nRuns; ++i){
					long t1 = System.currentTimeMillis();
					QuestOWLResultSet rs = st.executeTuple(sparqlQuery);
					int columnSize = rs.getColumnCount();
					count = 0;
					while (rs.nextRow()) {
						count ++;
						for (int idx = 1; idx <= columnSize; idx++) {
							@SuppressWarnings("unused")
							OWLObject binding = rs.getOWLObject(idx);
							//System.out.print(binding.toString() + ", ");
						}
						//System.out.print("\n");
					}
					long t2 = System.currentTimeMillis();
					//time = time + (t2-t1);
                    time =  (t2-t1);
					System.out.println("partial time:" + time);
					rs.close();
				//}

				/*
				 * Print the query summary
				 */
				QuestOWLStatement qst = (QuestOWLStatement) st;
				String sqlQuery = qst.getUnfolding(sparqlQuery);

				System.out.println();
				System.out.println("The input SPARQL query:");
				System.out.println("=======================");
				System.out.println(sparqlQuery);
				System.out.println();

				System.out.println("The output SQL query:");
				System.out.println("=====================");
				System.out.println(sqlQuery);

				System.out.println("Query Execution Time:");
				System.out.println("=====================");
				//System.out.println((time/nRuns) + "ms");

				//results[j] = (time/nRuns)+"" ;
                results.add(j, time);

				System.out.println("The number of results:");
				System.out.println("=====================");
				System.out.println(count);

			} finally {
				if (st != null && !st.isClosed()) {
					st.close();
				}
			}
			j++;
		}

        return results;
	}

	/**
	 * Main client program
	 */
	public static void main(String[] args) {

		//QuestOWLExample_OntowisTests.Settings s = new Settings();
		Settings.mKeys = false;

		switch(args[0]){
		case "--help":{
			System.out.println(
					"Options:\n\n"
							+ "--mKeysON (default=off. SPECIFY AS FIRST OPTION!!)"
							+ "\n\n"
							+ "--POSTGRESInt; --POSTGRESIntView; --POSTGRESStr; --POSTGRESStrView"
							+ "--MYSQLInt; --MYSQLIntView; --MYSQLStr; --MYSQLStrView;"
							+ "--DB2Int; --DB2IntView;"
							+ "\n\n"
							+ "The concepts for which T-mappings should"
							+ "be disabled are defined the file tMappingsConf.conf");
			System.exit(0);
			break;
		}	
		case "--mKeysON":{
			Settings.mKeys = true;	
			defaults(args[1]);
			break;
		}
		default:
			defaults(args[0]);
			break;
		}			
		try {
			System.out.println(Settings.obdaFile);

			QuestOWLExample_OntowisTests example = new QuestOWLExample_OntowisTests(Settings.obdaFile, Settings.type, Settings.mKeys);
			example.runQuery(Settings.obdaFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void defaults(String string) {

		switch(string){
		case "--MYSQLInt":{
			Settings.obdaFile = ParamConst.MYSQLInt;
			Settings.type = DBType.MYSQL;
			break;
		}
		case "--MYSQLIntView":{
			Settings.obdaFile = ParamConst.MYSQLIntView;
			Settings.type = DBType.MYSQL;
			break;
		}
		case "--MYSQLStr":{
			Settings.obdaFile = ParamConst.MYSQLStr;
			Settings.type = DBType.MYSQL;
			break;
		}
		case "--MYSQLStrView":{
			Settings.obdaFile = ParamConst.MYSQLStrView;
			Settings.type = DBType.MYSQL;
			break;
		}
		case "--POSTGRESInt":{
			Settings.obdaFile = ParamConst.POSTGRESInt;
			Settings.type = DBType.POSTGRES;
			break;
		}

		case "--POSTGRESStr":{
			Settings.obdaFile = ParamConst.POSTGRESStr;
			Settings.type = DBType.POSTGRES;
			break;
		}
		case "--POSTGRESIntView":{
			Settings.obdaFile = ParamConst.POSTGRESIntView;
			Settings.type = DBType.POSTGRES;
			break;
		}
		case "--POSTGRESStrView":{
			Settings.obdaFile = ParamConst.POSTGRESStrView;
			Settings.type = DBType.POSTGRES;
			break;
		}
		case "--POSTGRESSmallInt":{
			Settings.obdaFile = ParamConst.POSTGRESInt;
			Settings.type = DBType.SMALL_POSTGRES;
			break;
		}

		case "--POSTGRESSmallStr":{
			Settings.obdaFile = ParamConst.POSTGRESStr;
			Settings.type = DBType.SMALL_POSTGRES;
			break;
		}
		case "--POSTGRESSmallIntView":{
			Settings.obdaFile = ParamConst.POSTGRESIntView;
			Settings.type = DBType.SMALL_POSTGRES;
			break;
		}
		case "--POSTGRESSmallStrView":{
			Settings.obdaFile = ParamConst.POSTGRESStrView;
			Settings.type = DBType.SMALL_POSTGRES;
			break;
		}
		case "--DB2Int":{
			Settings.obdaFile = ParamConst.DB2Int;
			Settings.type = DBType.DB2;
			break;
		}
		case "--DB2IntView":{
			Settings.obdaFile = ParamConst.DB2IntView;
			Settings.type = DBType.DB2;
			break;
		}
		default :{
			System.out.println(
					"Options:\n\n"
							+ "--mKeysON (default=off. SPECIFY AS FIRST OPTION!!)"
							+ "\n\n"
							+ "--POSTGRESInt; --POSTGRESIntView; --POSTGRESStr; --POSTGRESStrView"
							+ "--MYSQLInt; --MYSQLIntView; --MYSQLStr; --MYSQLStrView;"
							+ "--DB2Int; --DB2IntView;"
							+ "\n\n"
							+ "The concepts for which T-mappings should"
							+ "be disabled are defined the file tMappingsConf.conf");
			System.exit(0);
			break;
		}
		}
	}
	static class QueryTemplates{
		
		private final static String SPARQL_END = "}";
		
		static String oneSparqlJoinQuery(int nSqlJoins, int filter){
			String result = oneSparqlJoinTemplate(nSqlJoins) + filter(filter) + SPARQL_END;
			return result;
		}
		
		static String twoSparqlJoinQuery(int nSqlJoins, int filter){
			String result = twoSparqlJoinTemplate(nSqlJoins) + filter(filter) + SPARQL_END;
			return result;
		}
		
		static String threeSparqlJoinQuery(int nSqlJoins, int filter){
			String result = threeSparqlJoinTemplate(nSqlJoins) + filter(filter) + SPARQL_END;
			return result;
		}
		
		static private String filter(int filter){
			return "Filter( ?y < "+filter+" )";
		}
		
		static private String oneSparqlJoinTemplate(int nSqlJoins) {
			String templ = 
					"PREFIX :	<http://www.example.org/> "
							+ "SELECT ?x ?y  "
							+ "WHERE {"
							+ "?x a  :"+nSqlJoins+"Tab1 . "
							+ "?x :Tab"+(nSqlJoins+1)+"unique2Tab"+(nSqlJoins+1)+" ?y . ";
			return templ;
		}
		static private String twoSparqlJoinTemplate(int nSqlJoins){
			String templ =
					oneSparqlJoinTemplate(nSqlJoins) +
					"?x :hasString"+(nSqlJoins+1)+"j ?y1 . "; // Additional Sparql join 1
			return templ;
		}
		static private String threeSparqlJoinTemplate(int nSqlJoins){
			String templ = twoSparqlJoinTemplate(nSqlJoins) +
					"?x :hasString2"+(nSqlJoins+1)+"j ?y2 . "; // Additional Sparql Join 2
			return templ;
		}
	};
	
	class QueryFactory {
		
		private final static int sizeQueriesArray = Constants.NUM_FILTERS * Constants.NUM_SQL_JOINS;
		
		String[] queriesOneSPARQL = new String[sizeQueriesArray];
		String[] queriesTwoSPARQL = new String[sizeQueriesArray];
		String[] queriesThreeSPARQL = new String[sizeQueriesArray];

        String[] warmUpQueries = new String[Constants.NUM_WARM_UPS];

		int[] filters = new int[Constants.NUM_FILTERS];
		
		QueryFactory(DBType type){
			fillFilters(type);
			fillQueryArrays();
		}
		
		private void fillQueryArrays (){

            fillWarmUpQueries();

			// 1 SPARQL Join
			fillOneSparqlJoin();
			
			// 2 SPARQL Joins
			fillTwoSparqlJoins();
			
			// 3 SPARQL Joins
			fillThreeSparqlJoins();
		}

        private void fillWarmUpQueries() {
        	for(int i = 0; i < Constants.NUM_WARM_UPS; i++){
        		int limit = (i * 1000) + 1;
                warmUpQueries[i] = String.format("SELECT ?x WHERE { " +
                        "?x a <http://www.example.org/%dTab1> } LIMIT "+limit, i);
            }
        }


        private void fillFilters(DBType type) {
			switch(type){
			case MYSQL:	
				filters[0] = 1; // 0.001
				filters[1] = 100; // 0.1%
				filters[2] = 1000; //1%
				break;
			case POSTGRES:
				filters[0] = 100;   // 0.0001%
				filters[1] = 10000;  // 0.01%
				filters[2] = 100000; // 0.1%
				break;
			case SMALL_POSTGRES:
				filters[0] = 1; // 0.001%
				filters[1] = 100; // 0.005%
				filters[2] = 1000; // 0.01%
				break;
			case DB2:
				filters[0] = 100;
				filters[1] = 10000;
				filters[2] = 100000;
				break;
			}
		}
		
		private void fillOneSparqlJoin(){
			for( int i = 0; i < sizeQueriesArray; ++i ){
				if( i < Constants.NUM_FILTERS)	queriesOneSPARQL[i] = QueryTemplates.oneSparqlJoinQuery(1, filters[i % Constants.NUM_FILTERS]); // 1 SQL Join
				else if ( i < Constants.NUM_FILTERS * 2 ) queriesOneSPARQL[i] = QueryTemplates.oneSparqlJoinQuery(2, filters[i % Constants.NUM_FILTERS]); // 2 SQL Joins
				else if ( i < Constants.NUM_FILTERS * 3 ) queriesOneSPARQL[i] = QueryTemplates.oneSparqlJoinQuery(3, filters[i % Constants.NUM_FILTERS]); // 3 SQL Joins
				else if ( i < Constants.NUM_FILTERS * 4 ) queriesOneSPARQL[i] = QueryTemplates.oneSparqlJoinQuery(4, filters[i % Constants.NUM_FILTERS]); // 4 SQL Joins
			}
		}
		
		private void fillTwoSparqlJoins(){
			for( int i = 0; i < sizeQueriesArray; ++i ){
				if( i < Constants.NUM_FILTERS)	queriesTwoSPARQL[i] = QueryTemplates.twoSparqlJoinQuery(1, filters[i % Constants.NUM_FILTERS]); // 1 SQL Join
				else if ( i < Constants.NUM_FILTERS * 2 ) queriesTwoSPARQL[i] = QueryTemplates.twoSparqlJoinQuery(2, filters[i % Constants.NUM_FILTERS]); // 2 SQL Joins
				else if ( i < Constants.NUM_FILTERS * 3 ) queriesTwoSPARQL[i] = QueryTemplates.twoSparqlJoinQuery(3, filters[i % Constants.NUM_FILTERS]); // 3 SQL Joins
				else if ( i < Constants.NUM_FILTERS * 4 ) queriesTwoSPARQL[i] = QueryTemplates.twoSparqlJoinQuery(4, filters[i % Constants.NUM_FILTERS]); // 4 SQL Joins
			}
		}
		
		private void fillThreeSparqlJoins(){
			for( int i = 0; i < sizeQueriesArray; ++i ){
				if( i < Constants.NUM_FILTERS)	queriesThreeSPARQL[i] = QueryTemplates.threeSparqlJoinQuery(1, filters[i % Constants.NUM_FILTERS]); // 1 SQL Join
				else if ( i < Constants.NUM_FILTERS * 2 ) queriesThreeSPARQL[i] = QueryTemplates.threeSparqlJoinQuery(2, filters[i % Constants.NUM_FILTERS]); // 2 SQL Joins
				else if ( i < Constants.NUM_FILTERS * 3 ) queriesThreeSPARQL[i] = QueryTemplates.threeSparqlJoinQuery(3, filters[i % Constants.NUM_FILTERS]); // 3 SQL Joins
				else if ( i < Constants.NUM_FILTERS * 4 ) queriesThreeSPARQL[i] = QueryTemplates.threeSparqlJoinQuery(4, filters[i % Constants.NUM_FILTERS]); // 4 SQL Joins
			}
		}	
	};
}
