package sesameWrapper;

import it.unibz.krdb.obda.ontology.ImmutableOntologyVocabulary;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlapi3.OWLAPIABoxIterator;
import it.unibz.krdb.obda.owlrefplatform.core.abox.RDBMSSIRepositoryManager;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasonerImpl;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.krdb.obda.sesame.SesameRDFIterator;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.turtle.TurtleParser;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/***
 * An utility to setup and maintain a semantic index repository independently
 * from a Quest instance.
 * 
 * @author mariano
 * 
 */
public class SemanticIndexManager {

    private final Connection conn;

    private final TBoxReasoner reasoner;

    private final ImmutableOntologyVocabulary voc;

    private final RDBMSSIRepositoryManager dataRepository;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public SemanticIndexManager(OWLOntology tbox, Connection connection) throws Exception {
        conn = connection;
        Ontology ontologyClosure = QuestOWL.loadOntologies(tbox);
        voc = ontologyClosure.getVocabulary();
        reasoner = TBoxReasonerImpl.create(ontologyClosure, true);
        dataRepository = new RDBMSSIRepositoryManager(reasoner, ontologyClosure.getVocabulary());
        dataRepository.generateMetadata();
        log.debug("TBox has been processed. Ready to ");
    }

    public void restoreRepository() throws SQLException {
        dataRepository.loadMetadata(conn);
        log.debug("Semantic Index metadata was found and restored from the DB");
    }

    public void setupRepository(boolean drop) throws SQLException {
        if (drop) {
            log.debug("Droping existing tables");
            try {
                dataRepository.dropDBSchema(conn);
            } catch (SQLException e) {
                log.debug(e.getMessage(), e);
            }
        }
        dataRepository.createDBSchemaAndInsertMetadata(conn);
        log.debug("Semantic Index repository has been setup.");
    }

    public void dropRepository() throws SQLException {
        dataRepository.dropDBSchema(conn);
    }

    public void updateMetadata() throws SQLException {
        dataRepository.insertMetadata(conn);
        log.debug("Updated metadata in the repository");
    }

    public int insertData(OWLOntology ontology, int commitInterval, int batchSize) throws SQLException {
        OWLAPIABoxIterator aBoxIter = new OWLAPIABoxIterator(ontology.getOWLOntologyManager().getImportsClosure(ontology), voc);
        int result = dataRepository.insertData(conn, aBoxIter, commitInterval, batchSize);
        log.info("Loaded {} items into the DB.", result);
        return result;
    }

    public int insertDataNTriple(final String ntripleFile, final String baseURI, final int commitInterval, final int batchSize) throws SQLException, RDFParseException, RDFHandlerException, FileNotFoundException, IOException {
        final TurtleParser parser = new TurtleParser();
        final SesameRDFIterator aBoxIter = new SesameRDFIterator();
        parser.setRDFHandler(aBoxIter);
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    parser.parse(new BufferedReader(new FileReader(ntripleFile)), baseURI);
                } catch (RDFParseException e) {
                    e.printStackTrace();
                } catch (RDFHandlerException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        final int[] val = new int[1];
        Thread t2 = new Thread() {

            @Override
            public void run() {
                try {
                    val[0] = dataRepository.insertData(conn, aBoxIter, commitInterval, batchSize);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                log.info("Loaded {} items into the DB.", val[0]);
            }
        };
        t.start();
        t2.start();
        try {
            t.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return val[0];
    }
}
