package org.obda.owlrefplatform.core.abox;

import it.unibz.krdb.obda.model.Atom;
import it.unibz.krdb.obda.model.DataSource;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAMappingAxiom;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.Query;
import it.unibz.krdb.obda.model.Term;
import it.unibz.krdb.obda.model.impl.AtomImpl;
import it.unibz.krdb.obda.model.impl.CQIEImpl;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.RDBMSMappingAxiomImpl;
import it.unibz.krdb.obda.model.impl.SQLQueryImpl;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectMappingGenerator {

	private final OBDADataFactory	predicateFactory	= OBDADataFactoryImpl.getInstance();
	private final OBDADataFactory	termFactory			= OBDADataFactoryImpl.getInstance();
	private int						mappingcounter		= 1;

	private final Logger			log					= LoggerFactory.getLogger(ABoxToDBDumper.class);

	public Set<OBDAMappingAxiom> getMappings(DataSource datasource) throws Exception {
		throw new Exception("Not yet implemented");
	}

	public Set<OBDAMappingAxiom> getMappings(Set<OWLOntology> ontologies, Map<URIIdentyfier, String> tableMap) {

		Iterator<OWLOntology> ontologyIterator = ontologies.iterator();
		Set<OBDAMappingAxiom> mappings = new HashSet<OBDAMappingAxiom>();

		while (ontologyIterator.hasNext()) {

			/*
			 * For each ontology
			 */
			OWLOntology onto = ontologyIterator.next();
			log.debug("Materializing ABox for ontology: {}", onto.getURI().toString());

			Set<OWLEntity> entities = onto.getSignature();
			Iterator<OWLEntity> entityIterator = entities.iterator();

			while (entityIterator.hasNext()) {
				/* For each entity */
				OWLEntity entity = entityIterator.next();

				if (entity instanceof OWLClass) {
					OWLClass clazz = (OWLClass) entity;
					if (!clazz.isOWLThing()) {// if class equal to owl thing
												// just skip it
						/* Creating the mapping */
						URI name = clazz.getURI();
						URIIdentyfier id = new URIIdentyfier(name, URIType.CONCEPT);
						String tablename = tableMap.get(id);
						Term qt = termFactory.getVariable("x");
						List<Term> terms = new Vector<Term>();
						terms.add(qt);
						Predicate predicate = predicateFactory.getPredicate(name, terms.size());
						Atom bodyAtom = predicateFactory.getAtom(predicate, terms);
//						List<Atom> body = new Vector<Atom>();
//						body.add(bodyAtom); // the body
						predicate = predicateFactory.getPredicate(URI.create("q"), terms.size());
						Atom head = predicateFactory.getAtom(predicate, terms); // the head
						Query cq = predicateFactory.getCQIE(head, bodyAtom);
						String sql = "SELECT term0 as x FROM " + tablename;
						OBDAMappingAxiom ax = predicateFactory.getRDBMSMappingAxiom("id" + mappingcounter++,cq,predicateFactory.getSQLQuery(sql));
						mappings.add(ax);

						log.debug("Mapping created: {}", ax.toString());
					}
				} else if (entity instanceof OWLObjectProperty) {
					OWLObjectProperty objprop = (OWLObjectProperty) entity;
					URIIdentyfier id = new URIIdentyfier(objprop.getURI(), URIType.OBJECTPROPERTY);
					String tablename = tableMap.get(id);
					Term qt1 = termFactory.getVariable("x");
					Term qt2 = termFactory.getVariable("y");
					List<Term> terms = new Vector<Term>();
					terms.add(qt1);
					terms.add(qt2);
					Predicate predicate = predicateFactory.getPredicate(objprop.getURI(), terms.size());
					Atom bodyAtom = predicateFactory.getAtom(predicate, terms);
//					List<Atom> body = new Vector<Atom>();
//					body.add(bodyAtom); // the body
					predicate = predicateFactory.getPredicate(URI.create("q"), terms.size());
					Atom head = predicateFactory.getAtom(predicate, terms); // the head
					Query cq = predicateFactory.getCQIE(head, bodyAtom);
					String sql = "SELECT term0 as x, term1 as y FROM " + tablename;
					OBDAMappingAxiom ax = predicateFactory.getRDBMSMappingAxiom("id" + mappingcounter++,cq,predicateFactory.getSQLQuery(sql));
					log.debug("Mapping created: {}", ax.toString());
					mappings.add(ax);

				} else if (entity instanceof OWLDataProperty) {

					OWLDataProperty dataProp = (OWLDataProperty) entity;
					URIIdentyfier id = new URIIdentyfier(dataProp.getURI(), URIType.DATAPROPERTY);
					Term qt1 = termFactory.getVariable("x");
					String tablename = tableMap.get(id);
					Term qt2 = termFactory.getVariable("y");
					List<Term> terms = new Vector<Term>();
					terms.add(qt1);
					terms.add(qt2);
					Predicate predicate = predicateFactory.getPredicate(dataProp.getURI(), terms.size());
					Atom bodyAtom = predicateFactory.getAtom(predicate, terms);
//					List<Atom> body = new Vector<Atom>();
//					body.add(bodyAtom); // the body
					predicate = predicateFactory.getPredicate(URI.create("q"), terms.size());
					Atom head = predicateFactory.getAtom(predicate, terms); // the head
					Query cq = predicateFactory.getCQIE(head, bodyAtom);
					String sql = "SELECT term0 as x, term1 as y FROM " + tablename;
					OBDAMappingAxiom ax = predicateFactory.getRDBMSMappingAxiom("id" + mappingcounter++,cq,predicateFactory.getSQLQuery(sql));
					mappings.add(ax);
				}
			}
		}
		return mappings;
	}
}
