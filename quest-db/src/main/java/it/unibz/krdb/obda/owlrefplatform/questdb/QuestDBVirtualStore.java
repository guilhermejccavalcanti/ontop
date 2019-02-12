package it.unibz.krdb.obda.owlrefplatform.questdb;

import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.RDBMSourceParameterConstants;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.ontology.OntologyVocabulary;
import it.unibz.krdb.obda.ontology.impl.OntologyFactoryImpl;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import it.unibz.krdb.obda.owlapi3.directmapping.DirectMappingEngine;
import it.unibz.krdb.obda.owlrefplatform.core.Quest;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConnection;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.core.abox.RDBMSSIRepositoryManager;
import it.unibz.krdb.obda.r2rml.R2RMLReader;
import it.unibz.krdb.sql.DBMetadata;
import it.unibz.krdb.sql.ImplicitDBConstraintsReader;
import org.openrdf.model.Model;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;

/***
 * A bean that holds all the data about a store, generates a store folder and
 * maintains this data.
 */
public class QuestDBVirtualStore extends QuestDBAbstractStore {

    private static final long serialVersionUID = 2495624993519521937L;

    private static Logger log = LoggerFactory.getLogger(QuestDBVirtualStore.class);

    private static OBDADataFactory fac = OBDADataFactoryImpl.getInstance();

    protected transient OWLOntologyManager man = OWLManager.createOWLOntologyManager();

    private QuestConnection questConn;

    private Quest questInstance;

    private boolean isinitalized = false;

    public QuestDBVirtualStore(String name, URI obdaURI) throws Exception {
        this(name, null, obdaURI, null);
    }

    public QuestDBVirtualStore(String name, URI obdaURI, QuestPreferences config) throws Exception {
        this(name, null, obdaURI, config);
    }

    public QuestDBVirtualStore(String name, URI tboxFile, URI obdaURI) throws Exception {
        this(name, tboxFile, obdaURI, null);
    }

    public QuestDBVirtualStore(String name, QuestPreferences pref) throws Exception {
        this(name, (URI) null, null, pref);
    }

    public QuestDBVirtualStore(String name, URI tboxFile, URI obdaUri, QuestPreferences config) throws Exception {
        this(name, tboxFile, obdaUri, config, null);
    }

    /**
	 * The method generates the OBDAModel from an
	 * obda or ttl (r2rml) file
	 * @param obdaURI - the file URI
	 * @return the generated OBDAModel
	 * @throws IOException
	 * @throws InvalidMappingException
	 */
    public OBDAModel getObdaModel(URI obdaURI) throws IOException, InvalidMappingException {
        OBDAModel obdaModel = fac.getOBDAModel();
        if (obdaURI.toString().endsWith(".obda")) {
            ModelIOManager modelIO = new ModelIOManager(obdaModel);
            modelIO.load(new File(obdaURI));
        } else {
            if (obdaURI.toString().endsWith(".ttl")) {
                R2RMLReader reader = null;
                try {
                    reader = new R2RMLReader(new File(obdaURI));
                    obdaModel = reader.readModel(obdaURI);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return obdaModel;
    }

    public QuestDBVirtualStore(String name, URI tboxFile, URI obdaUri, QuestPreferences config, ImplicitDBConstraintsReader userConstraints) throws Exception {
        super(name);
        OBDAModel obdaModel;
        if (obdaUri == null) {
            log.debug("No mappings where given, mappings will be automatically generated.");
            obdaModel = getOBDAModelDM();
        } else {
            obdaModel = getObdaModel(obdaUri);
        }
        if (config == null) {
            config = new QuestPreferences();
        }
        config.setProperty(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        Ontology tbox;
        if (tboxFile != null) {
            OWLOntology owlontology = getOntologyFromFile(tboxFile);
            tbox = OWLAPITranslatorUtility.translateImportsClosure(owlontology);
        } else {
            OntologyVocabulary voc = OntologyFactoryImpl.getInstance().createVocabulary();
            tbox = OntologyFactoryImpl.getInstance().createOntology(voc);
            if (obdaModel.getSources().size() == 0) {
                obdaModel.addSource(getMemOBDADataSource("MemH2"));
            }
        }
        obdaModel.getOntologyVocabulary().merge(tbox.getVocabulary());
        setupQuest(tbox, obdaModel, null, config);
    }

    public QuestDBVirtualStore(String name, OWLOntology tbox, Model mappings, DBMetadata metadata, QuestPreferences config) throws Exception {
        super(name);
        Ontology ontology = OWLAPITranslatorUtility.translateImportsClosure(tbox);
        OBDADataSource source = getDataSourceFromConfig(config);
        OBDAModel obdaModel;
        if (mappings != null) {
            R2RMLReader reader = new R2RMLReader(mappings);
            obdaModel = reader.readModel(source.getSourceID());
        } else {
            String baseIRI = Objects.requireNonNull(config.getProperty(QuestPreferences.BASE_IRI), "Base IRI is requires for direct mappings");
            DirectMappingEngine dm = new DirectMappingEngine(baseIRI, 0);
            obdaModel = dm.extractMappings(source);
        }
        obdaModel.addSource(source);
        obdaModel.getOntologyVocabulary().merge(ontology.getVocabulary());
        setupQuest(ontology, obdaModel, metadata, config);
    }

    private OBDADataSource getDataSourceFromConfig(QuestPreferences config) {
        String id = config.get(QuestPreferences.DBNAME).toString();
        String url = config.get(QuestPreferences.JDBC_URL).toString();
        String username = config.get(QuestPreferences.DBUSER).toString();
        String password = config.get(QuestPreferences.DBPASSWORD).toString();
        String driver = config.get(QuestPreferences.JDBC_DRIVER).toString();
        OBDADataSource source = OBDADataFactoryImpl.getInstance().getDataSource(URI.create(id));
        source.setParameter(RDBMSourceParameterConstants.DATABASE_URL, url);
        source.setParameter(RDBMSourceParameterConstants.DATABASE_USERNAME, username);
        source.setParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD, password);
        source.setParameter(RDBMSourceParameterConstants.DATABASE_DRIVER, driver);
        return source;
    }

    /**
	 * Given a URI of an owl file returns the 
	 * translated OWLOntology object
	 * @param tboxFile - the URI of the file
	 * @return the translated OWLOntology
	 * @throws Exception
	 */
    private OWLOntology getOntologyFromFile(URI tboxFile) throws Exception {
        OWLOntologyIRIMapper iriMapper = new AutoIRIMapper(new File(tboxFile).getParentFile(), false);
        man.addIRIMapper(iriMapper);
        OWLOntology owlontology = man.loadOntologyFromOntologyDocument(new File(tboxFile));
        return owlontology;
    }

    private void setupQuest(Ontology tbox, OBDAModel obdaModel, DBMetadata metadata, QuestPreferences pref) throws Exception {
        if (metadata == null) {
            questInstance = new Quest(tbox, obdaModel, pref);
        } else {
            questInstance = new Quest(tbox, obdaModel, metadata, pref);
        }
    }

    /**
	 * Sets the implicit db constraints, i.e. primary and foreign keys not in the database
	 * Must be called before the call to initialize
	 * 
	 * @param userConstraints
	 */
    public void setImplicitDBConstraints(ImplicitDBConstraintsReader userConstraints) {
        if (userConstraints == null) {
            throw new NullPointerException();
        }
        if (this.isinitalized) {
            throw new Error("Implicit DB Constraints must be given before the call to initialize to have effect. See https://github.com/ontop/ontop/wiki/Implicit-database-constraints and https://github.com/ontop/ontop/wiki/API-change-in-SesameVirtualRepo-and-QuestDBVirtualStore");
        }
        questInstance.setImplicitDBConstraints(userConstraints);
    }

    /**
	 * Create an in-memory H2 database data source
	 * @param name - the datasource name
	 * @return the created OBDADataSource
	 */
    private static OBDADataSource getMemOBDADataSource(String name) {
        OBDADataSource obdaSource = OBDADataFactoryImpl.getInstance().getDataSource(URI.create(name));
        String driver = "org.h2.Driver";
        String url = "jdbc:h2:mem:questrepository";
        String username = "sa";
        String password = "";
        obdaSource = fac.getDataSource(URI.create("http://www.obda.org/ABOXDUMP" + System.currentTimeMillis()));
        obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_DRIVER, driver);
        obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD, password);
        obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_URL, url);
        obdaSource.setParameter(RDBMSourceParameterConstants.DATABASE_USERNAME, username);
        obdaSource.setParameter(RDBMSourceParameterConstants.IS_IN_MEMORY, "true");
        obdaSource.setParameter(RDBMSourceParameterConstants.USE_DATASOURCE_FOR_ABOXDUMP, "true");
        return (obdaSource);
    }

    /**
	 * Generate an OBDAModel from Direct Mapping (Bootstrapping)
	 * @return the OBDAModel
	 */
    private OBDAModel getOBDAModelDM() {
        DirectMappingEngine dm = new DirectMappingEngine("http://example.org/base", 0);
        try {
            OBDAModel model = dm.extractMappings(getMemOBDADataSource("H2m"));
            return model;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Must be called once after the constructor call and before any queries are run, that is,
	 * before the call to getQuestConnection.
	 * 
	 * Calls {@link Quest.setupRepository()}
	 * @throws Exception
	 */
    public void initialize() throws Exception {
        if (this.isinitalized) {
            log.warn("Double initialization of QuestDBVirtualStore");
        } else {
            this.isinitalized = true;
            questInstance.setupRepository();
        }
    }

    /**
	 * Get a Quest connection from the Quest instance
	 * @return the QuestConnection
	 */
    public QuestConnection getQuestConnection() {
        if (!this.isinitalized) {
            throw new Error("The QuestDBVirtualStore must be initialized before getQuestConnection can be run. See https://github.com/ontop/ontop/wiki/API-change-in-SesameVirtualRepo-and-QuestDBVirtualStore");
        }
        try {
            questConn = questInstance.getConnection();
        } catch (OBDAException e) {
            e.printStackTrace();
        }
        return questConn;
    }

    /**
	 * Shut down Quest and its connections.
	 */
    public void close() {
        questInstance.close();
    }

    @Override
    public Properties getPreferences() {
        return questInstance.getPreferences();
    }

    @Override
    public RDBMSSIRepositoryManager getSemanticIndexRepository() {
        return questInstance.getSemanticIndexRepository();
    }
}
