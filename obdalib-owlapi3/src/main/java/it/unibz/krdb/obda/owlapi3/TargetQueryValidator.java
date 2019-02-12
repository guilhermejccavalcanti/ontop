package it.unibz.krdb.obda.owlapi3;

/*
 * #%L
 * ontop-obdalib-owlapi3
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

import it.unibz.krdb.obda.io.TargetQueryVocabularyValidator;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.Predicate.COL_TYPE;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.ontology.ImmutableOntologyVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

// TODO: move to a more appropriate package

public class TargetQueryValidator implements TargetQueryVocabularyValidator {
	
	// the ontology vocabulary of the OBDA model
	private final ImmutableOntologyVocabulary voc;

	/** Data factory **/
	private final OBDADataFactory dataFactory = OBDADataFactoryImpl.getInstance();

	/** List of invalid predicates */
	private List<String> invalidPredicates = new ArrayList<>();

    @SuppressWarnings("unused")
    Logger log = LoggerFactory.getLogger(this.getClass());

	public TargetQueryValidator(ImmutableOntologyVocabulary voc) {
		this.voc = voc;
	}
	
	@Override
	public boolean validate(List<Function> targetQuery) {
		// Reset the invalid list
		invalidPredicates.clear();

		// Get the predicates in the target query.
		for (Function atom : targetQuery) {
			Predicate p = atom.getFunctionSymbol();

			boolean isClass = isClass(p);
			boolean isObjectProp = isObjectProperty(p);
			boolean isDataProp = isDataProperty(p);
			boolean isAnnotProp = isAnnotationProperty(p);
			boolean isTriple = isTriple(p);


			// Check if the predicate contains in the ontology vocabulary as one
			// of these components (i.e., class, object property, data property).
			boolean isPredicateValid = isClass || isObjectProp || isDataProp || isAnnotProp || isTriple;

			String debugMsg = "The predicate: [" + p.getName() + "]";
			if (isPredicateValid) {
				Predicate predicate;
				if (isClass) {
					predicate = dataFactory.getClassPredicate(p.getName());
					debugMsg += " is a Class.";
				} else if (isObjectProp) {
					predicate = dataFactory.getObjectPropertyPredicate(p.getName());
					debugMsg += " is an Object property.";
				} else if (isDataProp) {
					predicate = dataFactory.getDataPropertyPredicate(p.getName(), COL_TYPE.LITERAL);
					debugMsg += " is a Data property.";
				}
                else if (isAnnotProp){
                    predicate =  dataFactory.getDataPropertyPredicate(p.getName(), COL_TYPE.LITERAL);
                    debugMsg += " is an Annotation property.";
                }
				else
					predicate = dataFactory.getPredicate(p.getName(), atom.getArity());
				atom.setPredicate(predicate); // TODO Fix the API!
//                log.debug(debugMsg);
			} else {
				invalidPredicates.add(p.getName());
			}
		}
		boolean isValid = true;
		if (!invalidPredicates.isEmpty()) {
			isValid = false; // if the list is not empty means the string is invalid!
		}
		return isValid;
	}

	@Override
	public List<String> getInvalidPredicates() {
		return invalidPredicates;
	}

	@Override
	public boolean isClass(Predicate predicate) {
		return voc.containsClass(predicate.getName());
	}
	
	@Override
	public boolean isObjectProperty(Predicate predicate) {
		return voc.containsObjectProperty(predicate.getName());
	}

	@Override
	public boolean isDataProperty(Predicate predicate) {
		return voc.containsDataProperty(predicate.getName());
	}

	@Override
	public boolean isAnnotationProperty(Predicate predicate) {
		return voc.containsAnnotationProperty(predicate.getName());
	}
	
	@Override
	public boolean isTriple(Predicate predicate){
		return predicate.isTriplePredicate();
	}
}
