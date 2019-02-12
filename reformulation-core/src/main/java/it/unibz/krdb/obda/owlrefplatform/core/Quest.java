package it.unibz.krdb.obda.owlrefplatform.core;

import com.google.common.collect.Lists;
import it.unibz.krdb.obda.model.*;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.RDBMSourceParameterConstants;
import it.unibz.krdb.obda.ontology.ImmutableOntologyVocabulary;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlrefplatform.core.abox.RDBMSSIRepositoryManager;
import it.unibz.krdb.obda.owlrefplatform.core.abox.RepositoryChangedListener;
import it.unibz.krdb.obda.owlrefplatform.core.abox.SemanticIndexURIMap;
import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.LinearInclusionDependencies;
import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.VocabularyValidator;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import it.unibz.krdb.obda.owlrefplatform.core.mappingprocessing.TMappingExclusionConfig;
import it.unibz.krdb.obda.owlrefplatform.core.queryevaluation.SQLAdapterFactory;
import it.unibz.krdb.obda.owlrefplatform.core.queryevaluation.SQLDialectAdapter;
import it.unibz.krdb.obda.owlrefplatform.core.reformulation.DummyReformulator;
import it.unibz.krdb.obda.owlrefplatform.core.reformulation.QueryRewriter;
import it.unibz.krdb.obda.owlrefplatform.core.reformulation.TreeWitnessRewriter;
import it.unibz.krdb.obda.owlrefplatform.core.sql.SQLGenerator;
import it.unibz.krdb.obda.owlrefplatform.core.srcquerygeneration.SQLQueryGenerator;
import it.unibz.krdb.obda.owlrefplatform.core.translator.MappingVocabularyRepair;
import it.unibz.krdb.obda.utils.MappingParser;
import it.unibz.krdb.sql.*;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Statement;
import java.io.Serializable;
import java.net.URI;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

enum ConnClasses {

    MYSQL("com.mysql.jdbc.JDBC4Connection"), POSTGRES("org.postgresql.jdbc4.Jdbc4Connection"), DB2("com.ibm.db2.jcc.DB2Connection", "com.ibm.db2.jcc.t4.b");

    private final List<String> connClasses;

    ConnClasses(String... connClasses) {
        this.connClasses = Lists.newArrayList(connClasses);
    }

    public static ConnClasses fromString(String connectionClassName) {
        ConnClasses result = null;
        if (connectionClassName != null) {
            for (ConnClasses b : ConnClasses.values()) {
                if (b.connClasses.indexOf(connectionClassName) != -1) {
                    result = b;
                }
            //	        if (connectionClassName.equals(b.connClass)) {
            //	          result = b;
            //	        }
            }
        } else
            throw new IllegalArgumentException("No constant with text " + connectionClassName + " found");
        return result;
    }

    @Override
    public String toString() {
        return this.connClasses.toString();
    }
}

public class Quest implements Serializable {

    private static final long serialVersionUID = -6074403119825754295L;

    private PoolProperties poolProperties = null;

    private DataSource tomcatPool = null;

    // Tomcat pool default properties
    // These can be changed in the properties file
    protected int maxPoolSize = 20;

    protected int startPoolSize = 2;

    protected boolean removeAbandoned = true;

    protected boolean logAbandoned = false;

    protected int abandonedTimeout = 60;

    // 60 seconds
    protected boolean keepAlive = true;

    // Whether to print primary and foreign keys to stdout.
    private boolean printKeys;

    /***
	 * Internal components
	 */
    /* The active ABox repository (is null if there is no Semantic Index, i.e., in Virtual Mode) */
    private RDBMSSIRepositoryManager dataRepository = null;

    /* The active connection used to get metadata from the DBMS */
    private transient Connection localConnection = null;

    /* The active query evaluation engine */
    /* The merge and translation of all loaded ontologies */
    private final Ontology inputOntology;

    /* The input OBDA model */
    private OBDAModel inputOBDAModel = null;

    private QuestQueryProcessor engine;

    /**
	 * This represents user-supplied constraints, i.e. primary
	 * and foreign keys not present in the database metadata
	 */
    private ImplicitDBConstraintsReader userConstraints = null;

    /*
	 * Whether to apply the user-supplied database constraints given above
	 * userConstraints must be initialized and non-null whenever this is true
	 */
    private boolean applyUserConstraints;

    /** Davide> Exclude specific predicates from T-Mapping approach **/
    private TMappingExclusionConfig excludeFromTMappings = TMappingExclusionConfig.empty();

    /** Davide> Whether to exclude the user-supplied predicates from the
	 *          TMapping procedure (that is, the mapping assertions for 
	 *          those predicates should not be extended according to the 
	 *          TBox hierarchies
	 */
    //private boolean applyExcludeFromTMappings;
    /***
	 * General flags and fields
	 */
    private final Logger log = LoggerFactory.getLogger(Quest.class);

    /***
	 * Configuration
	 */
    public boolean reformulate = false;

    private String reformulationTechnique = QuestConstants.UCQBASED;

    private boolean bOptimizeEquivalences = true;

    private boolean bObtainFromOntology = true;

    private boolean bObtainFromMappings = true;

    private boolean obtainFullMetadata = false;

    private boolean sqlGenerateReplace = true;

    private boolean distinctResultSet = false;

    private String aboxMode = QuestConstants.CLASSIC;

    private String aboxSchemaType = QuestConstants.SEMANTIC_INDEX;

    private OBDADataSource obdaSource;

    private Properties preferences;

    private boolean inmemory;

    private String aboxJdbcURL;

    private String aboxJdbcUser;

    private String aboxJdbcPassword;

    private String aboxJdbcDriver;

    private DBMetadata metadata;

    // TODO Remove this
    private static boolean timeoutSet = false;

    public Quest(Ontology tbox, Properties config) {
        this(tbox, null, null, config);
    }

    public Quest(Ontology tbox, OBDAModel mappings, Properties config) {
        this(tbox, mappings, null, config);
    }

    public Quest(Ontology tbox, OBDAModel obdaModel, DBMetadata metadata, Properties config) {
        if (tbox == null) {
            throw new InvalidParameterException("TBox cannot be null");
        }
        inputOntology = tbox;
        this.metadata = metadata;
        setPreferences(config);
        if (obdaModel == null && !aboxMode.equals(QuestConstants.CLASSIC)) {
            throw new IllegalArgumentException("When working without mappings, you must set the ABox mode to \"" + QuestConstants.CLASSIC + "\". If you want to work with no mappings in virtual ABox mode you must at least provide an empty but not null OBDAModel");
        }
        if (obdaModel != null && !aboxMode.equals(QuestConstants.VIRTUAL)) {
            throw new IllegalArgumentException("When working with mappings, you must set the ABox mode to \"" + QuestConstants.VIRTUAL + "\". If you want to work in \"classic abox\" mode, that is, as a triple store, you may not provide mappings (quest will take care of setting up the mappings and the database), set them to null.");
        }
        loadOBDAModel(obdaModel);
    }

    /** Davide> Exclude specific predicates from T-Mapping approach **/
    public void setExcludeFromTMappings(TMappingExclusionConfig excludeFromTMappings) {
        assert (excludeFromTMappings != null);
        this.excludeFromTMappings = excludeFromTMappings;
    }

    /**
	 * Supply user constraints: that is primary and foreign keys not in the database
	 * Can be useful for eliminating self-joins
	 *
	 * @param userConstraints User supplied primary and foreign keys (only useful if these are not in the metadata)
	 * 						May be used by ontop to eliminate self-joins
	 */
    public void setImplicitDBConstraints(ImplicitDBConstraintsReader userConstraints) {
        assert (userConstraints != null);
        this.userConstraints = userConstraints;
        this.applyUserConstraints = true;
    }

    // TEST ONLY
    public List<CQIE> getUnfolderRules() {
        return engine.unfolder.ufp;
    }

    private void loadOBDAModel(OBDAModel model) {
        if (model == null) {
            model = OBDADataFactoryImpl.getInstance().getOBDAModel();
        }
        inputOBDAModel = (OBDAModel) model.clone();
    }

    public OBDAModel getOBDAModel() {
        return inputOBDAModel;
    }

    public void dispose() {
        try {
            if (localConnection != null && !localConnection.isClosed()) {
                disconnect();
            }
        } catch (Exception e) {
            log.debug("Error during disconnect: " + e.getMessage());
        }
    }

    public Properties getPreferences() {
        return preferences;
    }

    private void setPreferences(Properties preferences) {
        this.preferences = preferences;
        keepAlive = Boolean.valueOf((String) preferences.get(QuestPreferences.KEEP_ALIVE));
        removeAbandoned = Boolean.valueOf((String) preferences.get(QuestPreferences.REMOVE_ABANDONED));
        abandonedTimeout = Integer.valueOf((String) preferences.get(QuestPreferences.ABANDONED_TIMEOUT));
        startPoolSize = Integer.valueOf((String) preferences.get(QuestPreferences.INIT_POOL_SIZE));
        maxPoolSize = Integer.valueOf((String) preferences.get(QuestPreferences.MAX_POOL_SIZE));
        reformulate = Boolean.valueOf((String) preferences.get(QuestPreferences.REWRITE));
        reformulationTechnique = (String) preferences.get(QuestPreferences.REFORMULATION_TECHNIQUE);
        bOptimizeEquivalences = Boolean.valueOf((String) preferences.get(QuestPreferences.OPTIMIZE_EQUIVALENCES));
        bObtainFromOntology = Boolean.valueOf((String) preferences.get(QuestPreferences.OBTAIN_FROM_ONTOLOGY));
        bObtainFromMappings = Boolean.valueOf((String) preferences.get(QuestPreferences.OBTAIN_FROM_MAPPINGS));
        aboxMode = (String) preferences.get(QuestPreferences.ABOX_MODE);
        aboxSchemaType = (String) preferences.get(QuestPreferences.DBTYPE);
        inmemory = preferences.getProperty(QuestPreferences.STORAGE_LOCATION).equals(QuestConstants.INMEMORY);
        obtainFullMetadata = Boolean.valueOf((String) preferences.get(QuestPreferences.OBTAIN_FULL_METADATA));
        printKeys = Boolean.valueOf((String) preferences.get(QuestPreferences.PRINT_KEYS));
        distinctResultSet = Boolean.valueOf((String) preferences.get(QuestPreferences.DISTINCT_RESULTSET));
        sqlGenerateReplace = Boolean.valueOf((String) preferences.get(QuestPreferences.SQL_GENERATE_REPLACE));
        if (!inmemory) {
            aboxJdbcURL = preferences.getProperty(QuestPreferences.JDBC_URL);
            aboxJdbcUser = preferences.getProperty(QuestPreferences.DBUSER);
            aboxJdbcPassword = preferences.getProperty(QuestPreferences.DBPASSWORD);
            aboxJdbcDriver = preferences.getProperty(QuestPreferences.JDBC_DRIVER);
        }
        log.debug("Quest configuration:");
        log.debug("Extensional query rewriting enabled: {}", reformulate);
        if (reformulate) {
            log.debug("Extensional query rewriting technique: {}", reformulationTechnique);
        }
        log.debug("Optimize TBox using class/property equivalences: {}", bOptimizeEquivalences);
        log.debug("ABox mode: {}", aboxMode);
        if (!aboxMode.equals("virtual")) {
            log.debug("Use in-memory database: {}", inmemory);
            log.debug("Schema configuration: {}", aboxSchemaType);
            log.debug("Get ABox assertions from OBDA models: {}", bObtainFromMappings);
            log.debug("Get ABox assertions from ontology: {}", bObtainFromOntology);
        }
    }

    /***
	 * Starts the local connection that Quest maintains to the DBMS. This
	 * connection belongs only to Quest and is used to get information from the
	 * DBMS. At the moment this connection is mainly used during initialization,
	 * to get metadata about the DBMS or to create repositories in classic mode.
	 * 
	 * @return
	 * @throws SQLException
	 */
    private boolean connect() throws SQLException {
        if (localConnection != null && !localConnection.isClosed()) {
            return true;
        }
        String url = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_URL);
        String username = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_USERNAME);
        String password = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD);
        String driver = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER);
        localConnection = DriverManager.getConnection(url, username, password);
        if (localConnection != null) {
            return true;
        }
        return false;
    }

    public void disconnect() throws SQLException {
        try {
            localConnection.close();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    /***
	 * Method that starts all components of a Quest instance. Call this after
	 * creating the instance.
	 * 
	 * @throws Exception
	 */
    public void setupRepository() throws Exception {
        OBDADataFactory fac = OBDADataFactoryImpl.getInstance();
        log.debug("Initializing Quest...");
        if (aboxMode.equals(QuestConstants.VIRTUAL) && inputOBDAModel == null) {
            throw new Exception("ERROR: Working in virtual mode but no OBDA model has been defined.");
        }
        if (inputOBDAModel != null && !inputOntology.getVocabulary().isEmpty()) {
            MappingVocabularyRepair.fixOBDAModel(inputOBDAModel, inputOntology.getVocabulary());
        }
        final TBoxReasoner reformulationReasoner = TBoxReasonerImpl.create(inputOntology, bOptimizeEquivalences);
        try {
            Collection<OBDAMappingAxiom> mappings = null;
            if (aboxMode.equals(QuestConstants.CLASSIC)) {
                if (!aboxSchemaType.equals(QuestConstants.SEMANTIC_INDEX)) {
                    throw new Exception(aboxSchemaType + " is unknown or not yet supported Data Base type. Currently only the direct db type is supported");
                }
                if (inmemory) {
                    String url = "jdbc:h2:mem:questrepository:" + System.currentTimeMillis() + ";LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0";
                    obdaSource = fac.getDataSource(URI.create("http://www.obda.org/ABOXDUMP" + System.currentTimeMillis()));
                    obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_DRIVER, "org.h2.Driver");
                    obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD, "");
                    obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_URL, url);
                    obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_USERNAME, "sa");
                    obdaSource.setParameter(RDBMSourceParameterConstants.IS_IN_MEMORY, "true");
                    obdaSource.setParameter(RDBMSourceParameterConstants.USE_DATASOURCE_FOR_ABOXDUMP, "true");
                } else {
                    if (aboxJdbcURL.trim().equals("")) {
                        throw new OBDAException("Found empty JDBC_URL parametery. Quest in CLASSIC/JDBC mode requires a JDBC_URL value.");
                    }
                    if (aboxJdbcDriver.trim().equals("")) {
                        throw new OBDAException("Found empty JDBC_DRIVER parametery. Quest in CLASSIC/JDBC mode requires a JDBC_DRIVER value.");
                    }
                    obdaSource = fac.getDataSource(URI.create("http://www.obda.org/ABOXDUMP" + System.currentTimeMillis()));
                    obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_DRIVER, aboxJdbcDriver.trim());
                    obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD, aboxJdbcPassword);
                    obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_URL, aboxJdbcURL.trim());
                    obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_USERNAME, aboxJdbcUser.trim());
                    obdaSource.setParameter(RDBMSourceParameterConstants.IS_IN_MEMORY, "false");
                    obdaSource.setParameter(RDBMSourceParameterConstants.USE_DATASOURCE_FOR_ABOXDUMP, "true");
                }
                connect();
                setupConnectionPool();
                dataRepository = new RDBMSSIRepositoryManager(reformulationReasoner, inputOntology.getVocabulary());
                if (inmemory) {
                    dataRepository.generateMetadata();
                    dataRepository.createDBSchemaAndInsertMetadata(localConnection);
                } else {
                    dataRepository.loadMetadata(localConnection);
                }
                mappings = dataRepository.getMappings();
            } else {
                if (aboxMode.equals(QuestConstants.VIRTUAL)) {
                    Collection<OBDADataSource> sources = this.inputOBDAModel.getSources();
                    if (sources == null || sources.size() == 0) {
                        throw new Exception("No datasource has been defined. Virtual ABox mode requires exactly 1 data source in your OBDA model.");
                    }
                    if (sources.size() > 1) {
                        throw new Exception("Quest in virtual ABox mode only supports OBDA models with 1 single data source. Your OBDA model contains " + sources.size() + " data sources. Please remove the aditional sources.");
                    }
                    obdaSource = sources.iterator().next();
                    log.debug("Testing DB connection...");
                    connect();
                    setupConnectionPool();
                    mappings = inputOBDAModel.getMappings(obdaSource.getSourceID());
                }
            }
            if (metadata == null) {
                metadata = DBMetadataExtractor.createMetadata(localConnection);
                if (obtainFullMetadata) {
                    DBMetadataExtractor.loadMetadata(metadata, localConnection, null);
                } else {
                    try {
                        Set<RelationID> realTables = MappingParser.getRealTables(metadata.getQuotedIDFactory(), mappings);
                        if (applyUserConstraints) {
                            Set<RelationID> referredTables = userConstraints.getReferredTables(metadata.getQuotedIDFactory());
                            realTables.addAll(referredTables);
                        }
                        DBMetadataExtractor.loadMetadata(metadata, localConnection, realTables);
                    } catch (JSQLParserException e) {
                        System.out.println("Error obtaining the tables" + e);
                    } catch (SQLException e) {
                        System.out.println("Error obtaining the metadata " + e);
                    }
                }
            }
            if (applyUserConstraints) {
                userConstraints.insertUniqueConstraints(metadata);
                userConstraints.insertForeignKeyConstraints(metadata);
            }
            if (printKeys) {
                Collection<DatabaseRelationDefinition> table_list = metadata.getDatabaseRelations();
                System.out.println("\n====== Unique constraints ==========");
                for (DatabaseRelationDefinition dd : table_list) {
                    System.out.println(dd + ";");
                    for (UniqueConstraint uc : dd.getUniqueConstraints()) {
                        System.out.println(uc + ";");
                    }
                    System.out.println("");
                }
                System.out.println("====== Foreign key constraints ==========");
                for (DatabaseRelationDefinition dd : table_list) {
                    for (ForeignKeyConstraint fk : dd.getForeignKeys()) {
                        System.out.println(fk + ";");
                    }
                }
            } else {
                log.debug("DB Metadata: \n{}", metadata);
            }
            SQLDialectAdapter sqladapter = SQLAdapterFactory.getSQLDialectAdapter(obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER), metadata.getDbmsVersion());
            SQLQueryGenerator datasourceQueryGenerator = new SQLGenerator(metadata, sqladapter, sqlGenerateReplace, distinctResultSet, getUriMap());
            VocabularyValidator vocabularyValidator = new VocabularyValidator(reformulationReasoner, inputOntology.getVocabulary());
            final QuestUnfolder unfolder = new QuestUnfolder(metadata);
            if (aboxMode.equals(QuestConstants.VIRTUAL)) {
                unfolder.setupInVirtualMode(mappings, localConnection, vocabularyValidator, reformulationReasoner, inputOntology, excludeFromTMappings);
            } else {
                unfolder.setupInSemanticIndexMode(mappings, reformulationReasoner);
            }
            if (dataRepository != null) {
                dataRepository.addRepositoryChangedListener(new RepositoryChangedListener() {

                    @Override
                    public void repositoryChanged() {
                        engine.clearSQLCache();
                        try {
                            unfolder.setupInSemanticIndexMode(dataRepository.getMappings(), reformulationReasoner);
                            log.debug("Mappings and unfolder have been updated after inserts to the semantic index DB");
                        } catch (Exception e) {
                            log.error("Error updating Semantic Index mappings", e);
                        }
                    }
                });
            }
            LinearInclusionDependencies sigma = LinearInclusionDependencies.getABoxDependencies(reformulationReasoner, true);
            QueryRewriter rewriter;
            if (reformulate == false) {
                rewriter = new DummyReformulator();
            } else {
                if (QuestConstants.TW.equals(reformulationTechnique)) {
                    rewriter = new TreeWitnessRewriter();
                } else {
                    throw new IllegalArgumentException("Invalid value for argument: " + QuestPreferences.REFORMULATION_TECHNIQUE);
                }
            }
            rewriter.setTBox(reformulationReasoner, inputOntology.getVocabulary(), sigma);
            engine = new QuestQueryProcessor(rewriter, sigma, unfolder, vocabularyValidator, getUriMap(), datasourceQueryGenerator);
            log.debug("... Quest has been initialized.");
        } catch (Exception e) {
            OBDAException ex = new OBDAException(e);
            if (e instanceof SQLException) {
                SQLException sqle = (SQLException) e;
                SQLException e1 = sqle.getNextException();
                while (e1 != null) {
                    log.error("NEXT EXCEPTION");
                    log.error(e1.getMessage());
                    e1 = e1.getNextException();
                }
            }
            throw ex;
        } finally {
            if (!(aboxMode.equals(QuestConstants.CLASSIC) && (inmemory))) {
                disconnect();
            }
        }
    }

    public ImmutableOntologyVocabulary getVocabulary() {
        return inputOntology.getVocabulary();
    }

    // Davide> TODO: Test
    public void setQueryTimeout(Statement st) throws SQLException {
        int timeout = 1200;
        //int timeout = 30;
        ConnClasses connClass = ConnClasses.fromString(localConnection.getClass().getName());
        if (connClass == null) {
            //			st.setQueryTimeout(timeout);
            return;
        }
        switch(connClass) {
            case DB2:
            case MYSQL:
                st.setQueryTimeout(timeout);
                break;
            case POSTGRES:
                {
                    if (!timeoutSet) {
                        // 1000ms = one second
                        String query = String.format("SET statement_timeout TO %d", timeout * 1000);
                        st.execute(query);
                        timeoutSet = true;
                    }
                    break;
                }
            default:
                st.setQueryTimeout(timeout);
                break;
        }
    }

    public void resetTimeouts(Statement st) throws SQLException {
        ConnClasses connClass = ConnClasses.fromString(localConnection.getClass().toString());
        if (connClass == null) {
            // TODO: check
            return;
        }
        switch(connClass) {
            case MYSQL:
            case DB2:
                // Do nothing
                break;
            case POSTGRES:
                {
                    String query = "RESET statement_timeout;";
                    st.execute(query);
                    break;
                }
        }
    }

    private void setupConnectionPool() {
        String url = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_URL);
        String username = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_USERNAME);
        String password = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD);
        String driver = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER);
        poolProperties = new PoolProperties();
        poolProperties.setUrl(url);
        poolProperties.setDriverClassName(driver);
        poolProperties.setUsername(username);
        poolProperties.setPassword(password);
        poolProperties.setJmxEnabled(true);
        poolProperties.setTestOnBorrow(keepAlive);
        if (keepAlive) {
            if (driver.contains("oracle")) {
                poolProperties.setValidationQuery("select 1 from dual");
            } else {
                if (driver.contains("db2")) {
                    poolProperties.setValidationQuery("select 1 from sysibm.sysdummy1");
                } else {
                    poolProperties.setValidationQuery("select 1");
                }
            }
        }
        poolProperties.setTestOnReturn(false);
        poolProperties.setMaxActive(maxPoolSize);
        poolProperties.setMaxIdle(maxPoolSize);
        poolProperties.setInitialSize(startPoolSize);
        poolProperties.setMaxWait(30000);
        poolProperties.setRemoveAbandonedTimeout(abandonedTimeout);
        poolProperties.setMinEvictableIdleTimeMillis(30000);
        poolProperties.setLogAbandoned(logAbandoned);
        poolProperties.setRemoveAbandoned(removeAbandoned);
        poolProperties.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;" + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        tomcatPool = new DataSource();
        tomcatPool.setPoolProperties(poolProperties);
        log.debug("Connection Pool Properties:");
        log.debug("Start size: " + startPoolSize);
        log.debug("Max size: " + maxPoolSize);
        log.debug("Remove abandoned connections: " + removeAbandoned);
    }

    public void close() {
        tomcatPool.close();
    }

    public void releaseSQLPoolConnection(Connection co) {
        try {
            co.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized Connection getSQLPoolConnection() throws OBDAException {
        Connection conn = null;
        try {
            conn = tomcatPool.getConnection();
        } catch (SQLException e) {
            throw new OBDAException(e);
        }
        return conn;
    }

    /***
	 * Establishes a new connection to the data source. This is a normal JDBC
	 * connection. Used only internally to get metadata at the moment.
	 * 
	 * @return
	 * @throws OBDAException
	 */
    private Connection getSQLConnection() throws OBDAException {
        Connection conn;
        String url = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_URL);
        String username = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_USERNAME);
        String password = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD);
        String driver = obdaSource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER);
        try {
            conn = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new OBDAException(e.getMessage());
        } catch (Exception e) {
            throw new OBDAException(e.getMessage());
        }
        return conn;
    }

    // get a real (non pool) connection - used for protege plugin
    public QuestConnection getNonPoolConnection() throws OBDAException {
        return new QuestConnection(this, getSQLConnection());
    }

    /***
	 * Returns a QuestConnection, the main object that a client should use to
	 * access the query answering services of Quest. With the QuestConnection
	 * you can get a QuestStatement to execute queries.
	 * 
	 * <p>
	 * Note, the QuestConnection is not a normal JDBC connection. It is a
	 * wrapper of one of the N JDBC connections that quest's connection pool
	 * starts on initialization. Calling .close() will not actually close the
	 * connection, with will just release it back to the pool.
	 * <p>
	 * to close all connections you must call Quest.close().
	 * 
	 * @return
	 * @throws OBDAException
	 */
    public QuestConnection getConnection() throws OBDAException {
        return new QuestConnection(this, getSQLPoolConnection());
    }

    public DBMetadata getMetaData() {
        return metadata;
    }

    public SemanticIndexURIMap getUriMap() {
        if (dataRepository != null) {
            return dataRepository.getUriMap();
        } else {
            return null;
        }
    }

    public RDBMSSIRepositoryManager getSemanticIndexRepository() {
        return dataRepository;
    }

    public boolean hasDistinctResultSet() {
        return distinctResultSet;
    }

    public QuestQueryProcessor getEngine() {
        return engine;
    }
}
