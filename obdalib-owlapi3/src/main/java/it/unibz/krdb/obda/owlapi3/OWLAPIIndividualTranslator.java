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

import it.unibz.krdb.obda.model.BNode; 
import it.unibz.krdb.obda.model.ObjectConstant; 
import it.unibz.krdb.obda.model.Predicate.COL_TYPE; 
import it.unibz.krdb.obda.model.URIConstant; 
import it.unibz.krdb.obda.model.ValueConstant; 
import it.unibz.krdb.obda.ontology.ClassAssertion; 
import it.unibz.krdb.obda.ontology.DataPropertyAssertion; 
import it.unibz.krdb.obda.ontology.ObjectPropertyAssertion; 
import org.semanticweb.owlapi.model.*; 
import org.semanticweb.owlapi.vocab.OWL2Datatype; 
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl; 

/***
 * Translates a ontop ABox assertion into an OWLIndividualAxiom. Used in the
 * result sets.
 * 
 * @author Mariano Rodriguez Muro <mariano.muro@gmail.com>
 * 
 */
    OWLAPI3IndividualTranslator {
	

	


	

	


	 
	
	


	
	
	


	

	/***
	 * Translates from assertion objects into
	 * 
	 * @param constant
	 * @return
	 */
	


	
	
	



} 

/***
 * Translates a ontop ABox assertion into an OWLIndividualAxiom. Used in the
 * result sets.
 * 
 * @author Mariano Rodriguez Muro <mariano.muro@gmail.com>
 * 
 */
public  class  OWLAPIIndividualTranslator {
	

	private final OWLDataFactory dataFactory = new OWLDataFactoryImpl();
	

	public OWLIndividualAxiom translate(ClassAssertion ca) {
		IRI conceptIRI = IRI.create(ca.getConcept().getName());

		OWLClass description = dataFactory.getOWLClass(conceptIRI);
		OWLIndividual object = translate(ca.getIndividual());
		return dataFactory.getOWLClassAssertionAxiom(description, object);
	}
	 
	
	public OWLIndividualAxiom translate(ObjectPropertyAssertion opa) {
		IRI roleIRI = IRI.create(opa.getProperty().getName());

		OWLObjectProperty property = dataFactory.getOWLObjectProperty(roleIRI);
		OWLIndividual subject = translate(opa.getSubject());
		OWLIndividual object = translate(opa.getObject());
		return dataFactory.getOWLObjectPropertyAssertionAxiom(property, subject, object);				
	}
	
	
	public OWLIndividualAxiom translate(DataPropertyAssertion opa) {
		IRI roleIRI = IRI.create(opa.getProperty().getName());

		OWLDataProperty property = dataFactory.getOWLDataProperty(roleIRI);
		OWLIndividual subject = translate(opa.getSubject());
		OWLLiteral object = translate(opa.getValue());
		return dataFactory.getOWLDataPropertyAssertionAxiom(property, subject, object);			
	}
	

	/***
	 * Translates from assertion objects into
	 * 
	 * @param constant
	 * @return
	 */
	public OWLIndividual translate(ObjectConstant constant) {
		if (constant instanceof URIConstant)
			return dataFactory.getOWLNamedIndividual(IRI.create(((URIConstant)constant).getURI()));		

		else /*if (constant instanceof BNode)*/ 
			return dataFactory.getOWLAnonymousIndividual(((BNode) constant).getName());
	}
	
	
	public OWLLiteral translate(ValueConstant v) {
		if (v == null)
			return null;
		
		String value = v.getValue();
		if (value == null) {
			return null;
		} 
		else if (v.getType() == COL_TYPE.LITERAL_LANG) {
			return dataFactory.getOWLLiteral(value, v.getLanguage());
		} 
		else {
			OWL2Datatype datatype = OWLTypeMapper.getOWLType(v.getType());
			if (datatype != null)
				return dataFactory.getOWLLiteral(value, datatype);
			else 
				throw new IllegalArgumentException(v.getType().toString());
		}
	}

}
