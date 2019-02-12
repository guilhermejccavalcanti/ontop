package it.unibz.krdb.obda.owlapi3;

import it.unibz.krdb.obda.ontology.Assertion;
import it.unibz.krdb.obda.ontology.ClassAssertion;
import it.unibz.krdb.obda.ontology.DataPropertyAssertion;
import it.unibz.krdb.obda.ontology.ObjectPropertyAssertion;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import java.util.Iterator;

public class QuestOWLIndividualAxiomIterator implements Iterator<OWLIndividualAxiom> {

    private final OWLAPIIndividualTranslator translator = new OWLAPIIndividualTranslator();

    private final Iterator<Assertion> assertions;

    public QuestOWLIndividualAxiomIterator(Iterator<Assertion> assertions) {
        this.assertions = assertions;
    }

    @Override
    public boolean hasNext() {
        return assertions.hasNext();
    }

    @Override
    public OWLIndividualAxiom next() {
        Assertion assertion = assertions.next();
        OWLIndividualAxiom individual = translate(assertion);
        return individual;
    }

    private OWLIndividualAxiom translate(Assertion a) {
        if (a instanceof ClassAssertion) {
            return translator.translate((ClassAssertion) a);
        } else {
            if (a instanceof ObjectPropertyAssertion) {
                return translator.translate((ObjectPropertyAssertion) a);
            } else {
                return translator.translate((DataPropertyAssertion) a);
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This iterator is read-only");
    }
}
