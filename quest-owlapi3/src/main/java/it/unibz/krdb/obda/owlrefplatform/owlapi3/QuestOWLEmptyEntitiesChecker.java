package it.unibz.krdb.obda.owlrefplatform.owlapi3;

import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.ontology.DataPropertyExpression;
import it.unibz.krdb.obda.ontology.OClass;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;
import it.unibz.krdb.obda.ontology.Ontology;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorUtility;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Return empty concepts and roles, based on the mappings. Given an ontology,
 * which is connected to a database via mappings, generate a suitable set of
 * queries that test if there are empty concepts, concepts that are no populated
 * to anything.
 */
public class QuestOWLEmptyEntitiesChecker {

    private Ontology onto;

    private QuestOWLConnection conn;

    Logger log = LoggerFactory.getLogger(QuestOWLEmptyEntitiesChecker.class);

    private List<Predicate> emptyConcepts = new ArrayList<>();

    private List<Predicate> emptyRoles = new ArrayList<>();

    public QuestOWLEmptyEntitiesChecker(OWLOntology onto, QuestOWLConnection conn) throws Exception {
        this.onto = OWLAPITranslatorUtility.translate(onto);
        this.conn = conn;
        runQueries();
    }

    public QuestOWLEmptyEntitiesChecker(Ontology translatedOntologyMerge, QuestOWLConnection conn) throws Exception {
        this.onto = translatedOntologyMerge;
        this.conn = conn;
        runQueries();
    }

    /**
	 * Returns the empty concepts.
	 * 
	 * @return The empty concepts.
	 */
    public List<Predicate> getEmptyConcepts() {
        return emptyConcepts;
    }

    /**
	 * Returns the empty roles.
	 * 
	 * @return The empty roles.
	 */
    public List<Predicate> getEmptyRoles() {
        return emptyRoles;
    }

    /**
	 * Gets the total number of empty entities
	 * 
	 * @return The total number of empty entities.
	 * @throws Exception
	 */
    public int getNumberEmptyEntities() throws Exception {
        return emptyConcepts.size() + emptyRoles.size();
    }

    @Override
    public String toString() {
        String str = new String();
        int countC = emptyConcepts.size();
        str += String.format("- %s Empty %s ", countC, (countC == 1) ? "concept" : "concepts");
        int countR = emptyRoles.size();
        str += String.format("- %s Empty %s\n", countR, (countR == 1) ? "role" : "roles");
        return str;
    }

    private void runQueries() throws Exception {
        for (OClass cl : onto.getVocabulary().getClasses()) {
            if (!cl.isTop() && !cl.isBottom()) {
                Predicate concept = cl.getPredicate();
                if (!runSPARQLConceptsQuery("<" + concept.getName() + ">")) {
                    emptyConcepts.add(concept);
                }
            }
        }
        log.debug(emptyConcepts.size() + " Empty concept/s: " + emptyConcepts);
        for (ObjectPropertyExpression prop : onto.getVocabulary().getObjectProperties()) {
            if (!prop.isBottom() && !prop.isTop()) {
                Predicate role = prop.getPredicate();
                if (!runSPARQLRolesQuery("<" + role.getName() + ">")) {
                    emptyRoles.add(role);
                }
            }
        }
        log.debug(emptyRoles.size() + " Empty role/s: " + emptyRoles);
        for (DataPropertyExpression prop : onto.getVocabulary().getDataProperties()) {
            if (!prop.isBottom() && !prop.isTop()) {
                Predicate role = prop.getPredicate();
                if (!runSPARQLRolesQuery("<" + role.getName() + ">")) {
                    emptyRoles.add(role);
                }
            }
        }
        log.debug(emptyRoles.size() + " Empty role/s: " + emptyRoles);
    }

    private boolean runSPARQLConceptsQuery(String description) throws Exception {
        String query = "SELECT ?x WHERE {?x a " + description + ".}";
        QuestOWLStatement st = conn.createStatement();
        try {
            QuestOWLResultSet rs = st.executeTuple(query);
            return (rs.nextRow());
        } catch (Exception e) {
            throw e;
        } finally {
            st.close();
        }
    }

    private boolean runSPARQLRolesQuery(String description) throws Exception {
        String query = "SELECT * WHERE {?x " + description + " ?y.}";
        QuestOWLStatement st = conn.createStatement();
        try {
            QuestOWLResultSet rs = st.executeTuple(query);
            return (rs.nextRow());
        } catch (Exception e) {
            throw e;
        } finally {
            st.close();
        }
    }
}
