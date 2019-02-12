package it.unibz.krdb.obda.owlapi3;

import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.Predicate.COL_TYPE;
import it.unibz.krdb.obda.owlapi3.OWLAPITranslatorOWL2QL.TranslationException;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import java.util.HashMap;
import java.util.Map;

public class OWLTypeMapper {

    private static final Map<OWL2Datatype, COL_TYPE> OWLtoCOLTYPE = new HashMap<>();

    private static final Map<COL_TYPE, OWL2Datatype> COLTYPEtoOWL = new HashMap<>();

    static {
        registerType(OWL2Datatype.RDF_PLAIN_LITERAL, COL_TYPE.LITERAL);
        // 1
        registerType(OWL2Datatype.XSD_DECIMAL, COL_TYPE.DECIMAL);
        // 2
        registerType(OWL2Datatype.XSD_STRING, COL_TYPE.STRING);
        // 3
        registerType(OWL2Datatype.XSD_INTEGER, COL_TYPE.INTEGER);
        registerType(// 4
        OWL2Datatype.XSD_NON_NEGATIVE_INTEGER, // 4
        COL_TYPE.NON_NEGATIVE_INTEGER);
        registerType(// 5
        OWL2Datatype.XSD_DATE_TIME, // 5
        COL_TYPE.DATETIME);
        // 15
        registerType(OWL2Datatype.XSD_DATE_TIME_STAMP, COL_TYPE.DATETIME_STAMP);
        // not OWL 2 QL types
        registerType(OWL2Datatype.XSD_INT, // 6
        COL_TYPE.INT);
        registerType(OWL2Datatype.XSD_POSITIVE_INTEGER, // 7
        COL_TYPE.POSITIVE_INTEGER);
        registerType(OWL2Datatype.XSD_NEGATIVE_INTEGER, // 8
        COL_TYPE.NEGATIVE_INTEGER);
        registerType(OWL2Datatype.XSD_NON_POSITIVE_INTEGER, // 9
        COL_TYPE.NON_POSITIVE_INTEGER);
        registerType(// 10
        OWL2Datatype.XSD_UNSIGNED_INT, // 10
        COL_TYPE.UNSIGNED_INT);
        registerType(// 11
        OWL2Datatype.XSD_DOUBLE, // 11
        COL_TYPE.DOUBLE);
        // registerType(OWL2Datatype.XSD_FLOAT, COL_TYPE.DOUBLE); // 12 // TEMPORARY!!
        // 13 
        registerType(OWL2Datatype.XSD_LONG, COL_TYPE.LONG);
        registerType(// 14
        OWL2Datatype.XSD_BOOLEAN, // 14
        COL_TYPE.BOOLEAN);
        // irregularities
        // 12 // TEMPORARY!!
        OWLtoCOLTYPE.put(OWL2Datatype.XSD_FLOAT, COL_TYPE.DOUBLE);
        OWLtoCOLTYPE.put(OWL2Datatype.RDFS_LITERAL, COL_TYPE.LITERAL);
        // not XSD_DATE;
        COLTYPEtoOWL.put(COL_TYPE.DATE, OWL2Datatype.RDF_PLAIN_LITERAL);
        COLTYPEtoOWL.put(COL_TYPE.TIME, OWL2Datatype.RDF_PLAIN_LITERAL);
        COLTYPEtoOWL.put(COL_TYPE.YEAR, OWL2Datatype.RDF_PLAIN_LITERAL);
    }

    static {
        registerType(OWL2Datatype.RDF_PLAIN_LITERAL, COL_TYPE.LITERAL);
        // 1
        registerType(OWL2Datatype.XSD_DECIMAL, COL_TYPE.DECIMAL);
        // 2
        registerType(OWL2Datatype.XSD_STRING, COL_TYPE.STRING);
        // 3
        registerType(OWL2Datatype.XSD_INTEGER, COL_TYPE.INTEGER);
        registerType(// 4
        OWL2Datatype.XSD_NON_NEGATIVE_INTEGER, // 4
        COL_TYPE.NON_NEGATIVE_INTEGER);
        registerType(// 5
        OWL2Datatype.XSD_DATE_TIME, // 5
        COL_TYPE.DATETIME);
        // 15
        registerType(OWL2Datatype.XSD_DATE_TIME_STAMP, COL_TYPE.DATETIME_STAMP);
        // not OWL 2 QL types
        registerType(OWL2Datatype.XSD_INT, // 6
        COL_TYPE.INT);
        registerType(OWL2Datatype.XSD_POSITIVE_INTEGER, // 7
        COL_TYPE.POSITIVE_INTEGER);
        registerType(OWL2Datatype.XSD_NEGATIVE_INTEGER, // 8
        COL_TYPE.NEGATIVE_INTEGER);
        registerType(OWL2Datatype.XSD_NON_POSITIVE_INTEGER, // 9
        COL_TYPE.NON_POSITIVE_INTEGER);
        registerType(// 10
        OWL2Datatype.XSD_UNSIGNED_INT, // 10
        COL_TYPE.UNSIGNED_INT);
        registerType(// 11
        OWL2Datatype.XSD_DOUBLE, // 11
        COL_TYPE.DOUBLE);
        // registerType(OWL2Datatype.XSD_FLOAT, COL_TYPE.DOUBLE); // 12 // TEMPORARY!!
        // 13 
        registerType(OWL2Datatype.XSD_LONG, COL_TYPE.LONG);
        registerType(// 14
        OWL2Datatype.XSD_BOOLEAN, // 14
        COL_TYPE.BOOLEAN);
        // irregularities
        // 12 // TEMPORARY!!
        OWLtoCOLTYPE.put(OWL2Datatype.XSD_FLOAT, COL_TYPE.DOUBLE);
        OWLtoCOLTYPE.put(OWL2Datatype.RDFS_LITERAL, COL_TYPE.LITERAL);
        // not XSD_DATE;
        COLTYPEtoOWL.put(COL_TYPE.DATE, OWL2Datatype.RDF_PLAIN_LITERAL);
        COLTYPEtoOWL.put(COL_TYPE.TIME, OWL2Datatype.RDF_PLAIN_LITERAL);
        COLTYPEtoOWL.put(COL_TYPE.YEAR, OWL2Datatype.RDF_PLAIN_LITERAL);
    }

    private static void registerType(OWL2Datatype owlType, Predicate.COL_TYPE type) {
        OWLtoCOLTYPE.put(owlType, type);
        COLTYPEtoOWL.put(type, owlType);
    }

    // OWLAPI3TranslatorDLLiteA only
    public static Predicate.COL_TYPE getType(OWL2Datatype owlDatatype) throws TranslationException {
        if (owlDatatype == null) {
            return COL_TYPE.LITERAL;
        }
        COL_TYPE type = OWLtoCOLTYPE.get(owlDatatype);
        if (type == null) {
            throw new TranslationException("Unsupported data range: " + owlDatatype);
        }
        return type;
    }

    // OWLAPI3TranslatorDLLiteA only
    @Deprecated
    public static Predicate.COL_TYPE getType(OWLDatatype owlDatatype) throws TranslationException {
        if (owlDatatype == null) {
            return COL_TYPE.LITERAL;
        }
        return getType(owlDatatype.getBuiltInDatatype());
    }

    // OWLAPI3IndividualTranslator only
    public static OWL2Datatype getOWLType(COL_TYPE type) {
        return COLTYPEtoOWL.get(type);
    }
}
