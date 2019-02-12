package it.unibz.krdb.obda.owlrefplatform.core.mappingprocessing;

/*
 * #%L
 * ontop-reformulation-core
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

import it.unibz.krdb.obda.model.*;
import it.unibz.krdb.obda.model.impl.FunctionalTermImpl;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.ontology.DataPropertyRangeExpression;
import it.unibz.krdb.obda.ontology.DataRangeExpression;
import it.unibz.krdb.obda.ontology.Datatype;
import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.VocabularyValidator;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.Equivalences;
import it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.krdb.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MappingDataTypeRepair {

	private final DBMetadata metadata;
	private final boolean isDB2;
	private final Map<Predicate, Datatype> dataTypesMap;
	private final VocabularyValidator qvv;

  	private static final OBDADataFactory fac = OBDADataFactoryImpl.getInstance();
    private static final Logger log = LoggerFactory.getLogger(MappingDataTypeRepair.class);

    /**
     * Constructs a new mapping data type resolution. The object requires an
     * ontology for obtaining the user defined data-type.
     * 
     * If no datatype is defined than we use
     * database metadata for obtaining the table column definition as the
     * default data-type.
     * 
     * @param metadata The database metadata.
     * @throws OBDAException 
     */
    public MappingDataTypeRepair(DBMetadata metadata, TBoxReasoner reasoner, VocabularyValidator qvv) throws OBDAException {
        this.metadata = metadata;
        String databaseName = metadata.getDbmsProductName();
        String databaseDriver = metadata.getDriverName();
        this.isDB2 = (databaseName!= null && databaseName.contains("DB2"))
        			|| (databaseDriver != null && databaseDriver.contains("IBM"));
        
        this.qvv = qvv;
        try {
            dataTypesMap = getDataTypeFromOntology(reasoner);
        } 
        catch (PredicateRedefinitionException pe) {
            throw new OBDAException(pe);
        }
    }

    /**
     * Private method that gets the datatypes already present in the ontology and stores them in a map
     * It will be used later in insertDataTyping
     */
    private static Map<Predicate, Datatype> getDataTypeFromOntology(TBoxReasoner reasoner) {

    	final Map<Predicate, Datatype> dataTypesMap = new HashMap<>();
        
        // Traverse the graph searching for dataProperty
		for (Equivalences<DataRangeExpression> nodes : reasoner.getDataRangeDAG()) {
			DataRangeExpression node = nodes.getRepresentative();
			
			for (Equivalences<DataRangeExpression> descendants : reasoner.getDataRangeDAG().getSub(nodes)) {
				DataRangeExpression descendant = descendants.getRepresentative();
				if (descendant != node)
					onDataRangeInclusion(dataTypesMap, descendant, node);				
			}
			for (DataRangeExpression equivalent : nodes) {
				if (equivalent != node) {
					onDataRangeInclusion(dataTypesMap, node, equivalent);
					onDataRangeInclusion(dataTypesMap, equivalent, node);
				}
			}
		}	
    	
		return dataTypesMap;
    }

    private static void onDataRangeInclusion(Map<Predicate, Datatype> dataTypesMap, DataRangeExpression sub, DataRangeExpression sup) {
        //if sup is a datatype property  we store it in the map
        //it means that sub is of datatype sup
    	if (sup instanceof Datatype) {
    		Datatype supDataType = (Datatype)sup;
    		Predicate key;
    		if (sub instanceof Datatype) {
    			// datatype inclusion
    			key = ((Datatype)sub).getPredicate();
    		}
    		else if (sub instanceof DataPropertyRangeExpression) {
    			// range 
    			key = ((DataPropertyRangeExpression)sub).getProperty().getPredicate();
    		}
    		else
    			return;
    		
			if (dataTypesMap.containsKey(key))
                throw new PredicateRedefinitionException("Predicate " + key + " with " + dataTypesMap.get(key) + " is redefined as " + supDataType + " in the ontology");
			dataTypesMap.put(key, supDataType);
    	}
    }
   
    /**
     * This method wraps the variable that holds data property values with a
     * data type predicate. It will replace the variable with a new function
     * symbol and update the rule atom. However, if the users already defined
     * the data-type in the mapping, this method simply accepts the function
     * symbol.
     *
     * @param rule
     *            The set of mapping axioms.
     * @throws OBDAException
     */

    public void insertDataTyping(CQIE rule) throws OBDAException {
        Function atom = rule.getHead();
        Predicate predicate = atom.getFunctionSymbol();
        if (predicate.getArity() == 2) { // we check both for data and object property
            Term term = atom.getTerm(1); // the second argument only
            
    		Map<String, List<IndexedPosititon>> termOccurenceIndex = createIndex(rule.getBody());
            insertDataTyping(term, atom, 1, termOccurenceIndex);
        }
	}

    private void insertDataTyping(Term term, Function atom, int position, Map<String, List<IndexedPosititon>> termOccurenceIndex) throws OBDAException {
        Predicate predicate = atom.getFunctionSymbol();

        if (term instanceof Function) {
            Function function = (Function) term;
            Predicate functionSymbol = function.getFunctionSymbol();

            if (functionSymbol instanceof URITemplatePredicate || functionSymbol instanceof BNodePredicate) {
                // NO-OP for object properties
            }

            /** If it is a concat or replace function, can have a datatype assigned to its alias (function with datatype predicate)
             *  or if no information about the datatype is assigned we will assign the value from the ontology
             if present or the information from the database will be used.
             */

            else if (function.isOperation()) {

            	Function normal = qvv.getNormal(atom);
                Datatype dataType = dataTypesMap.get(normal.getFunctionSymbol());

                //Check if a datatype was already assigned in the ontology
                if (dataType != null) {
                    //assign the datatype of the ontology
                    if (!isBooleanDB2(dataType.getPredicate())) {
                        Predicate replacement = dataType.getPredicate();
                        Term newTerm = fac.getFunction(replacement, function);
                        atom.setTerm(position, newTerm);
                    }
                } 
                else {
                    for (int i = 0; i < function.getArity(); i++) 
                        insertDataTyping(function.getTerm(i), function, i, termOccurenceIndex);
                }
            } 
            else if (function.isDataTypeFunction()) {

                Function normal = qvv.getNormal(atom);
                Datatype dataType = dataTypesMap.get(normal.getFunctionSymbol());

                //if a datatype was already assigned in the ontology
                if (dataType != null) {

                    //check that no datatype mismatch is present
                    if (!functionSymbol.equals(dataType.getPredicate())) {
                        throw new OBDAException("Ontology datatype " + dataType + " for " + predicate + 
                        		"\ndoes not correspond to datatype " + functionSymbol + " in mappings");
                    }

                    if (isBooleanDB2(dataType.getPredicate())) {

                        Variable variable = (Variable) normal.getTerm(1);

                        //No Boolean datatype in DB2 database, the value in the database is used
                        Predicate.COL_TYPE type = getDataType(termOccurenceIndex, variable);
                        Term newTerm = fac.getTypedTerm(variable, type);
                        log.warn("Datatype for the value " +variable + "of the property "+ predicate+ " has been inferred from the database");
                        atom.setTerm(position, newTerm);
                    }
                }
            } 
           else 
               throw new OBDAException("Unknown data type predicate: " + functionSymbol.getName());
        } 
        else if (term instanceof Variable) {

            //check in the ontology if we have already information about the datatype

            Function normal = qvv.getNormal(atom);
            Datatype dataType = dataTypesMap.get(normal.getFunctionSymbol());

            // If the term has no data-type predicate then by default the
            // predicate is created following the database metadata of
            // column type.
            Variable variable = (Variable) term;

            Term newTerm;
            if (dataType == null || isBooleanDB2(dataType.getPredicate())) {
                Predicate.COL_TYPE type = getDataType(termOccurenceIndex, variable);
                newTerm = fac.getTypedTerm(variable, type);
                log.warn("Datatype for the value " +variable + "of the property "+ predicate+ " has been inferred from the database");
            } 
            else {
                Predicate replacement = dataType.getPredicate();
                newTerm = fac.getFunction(replacement, variable);
            }

            atom.setTerm(position, newTerm);
        } 
        else if (term instanceof ValueConstant) {
            Term newTerm = fac.getTypedTerm(term, Predicate.COL_TYPE.LITERAL);
            atom.setTerm(position, newTerm);
        }
    }

    /**
     * Private method, since DB2 does not support boolean value, we use the database metadata value
     * @param dataType
     * @return boolean to check if the database is DB2 and we assign  a boolean value
     */
    private boolean isBooleanDB2(Predicate dataType){

        if (isDB2){
            if (fac.getDatatypeFactory().isBoolean(dataType)) {
                log.warn("Boolean dataType do not exist in DB2 database, the value in the database metadata is used instead.");
                return true;
            }
        }
        return false;
    }
    
//	private boolean isDataProperty(Predicate predicate) {
//		return predicate.getArity() == 2 && predicate.getType(1) == Predicate.COL_TYPE.LITERAL;
//	}
    
    /**
     * returns COL_TYPE for one of the datatype ids
     * @param termOccurenceIndex
     * @param variable
     * @return
     * @throws OBDAException
     */
    
	private Predicate.COL_TYPE getDataType(Map<String, List<IndexedPosititon>> termOccurenceIndex, Variable variable) throws OBDAException {


		List<IndexedPosititon> list = termOccurenceIndex.get(variable.getName());
		if (list == null) 
			throw new OBDAException("Unknown term in head");
		
		// ROMAN (10 Oct 2015): this assumes the first occurrence is a database relation!
		//                      AND THAT THERE ARE NO CONSTANTS IN ARGUMENTS!
		IndexedPosititon ip = list.get(0);

		RelationID tableId = Relation2DatalogPredicate.createRelationFromPredicateName(metadata.getQuotedIDFactory(), ip.atom.getFunctionSymbol());
		RelationDefinition td = metadata.getRelation(tableId);
		Attribute attribute = td.getAttribute(ip.pos);

		Predicate.COL_TYPE type =  fac.getJdbcTypeMapper().getPredicate(attribute.getType());
		return type;
	}
	
	private static class IndexedPosititon {
		final Function atom;
		final int pos;
		
		IndexedPosititon(Function atom, int pos) {
			this.atom = atom;
			this.pos = pos;
		}
	}

	private static Map<String, List<IndexedPosititon>> createIndex(List<Function> body) {
		Map<String, List<IndexedPosititon>> termOccurenceIndex = new HashMap<>();
		for (Function a : body) {
			List<Term> terms = a.getTerms();
			int i = 1; // position index
			for (Term t : terms) {
				if (t instanceof Variable) {
					Variable var = (Variable) t;
					List<IndexedPosititon> aux = termOccurenceIndex.get(var.getName());
					if (aux == null) 
						aux = new LinkedList<>();	
					aux.add(new IndexedPosititon(a, i));
					termOccurenceIndex.put(var.getName(), aux);
					i++; // increase the position index for the next variable
				} 
				else if (t instanceof FunctionalTermImpl) {
					// NO-OP
				} 
				else if (t instanceof ValueConstant) {
					// NO-OP
				} 
				else if (t instanceof URIConstant) {
					// NO-OP
				}
			}
		}
		return termOccurenceIndex;
	}
}

