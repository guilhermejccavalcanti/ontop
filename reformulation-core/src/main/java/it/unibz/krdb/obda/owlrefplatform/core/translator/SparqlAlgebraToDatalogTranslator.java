package it.unibz.krdb.obda.owlrefplatform.core.translator;

import com.google.common.collect.ImmutableMap;
import it.unibz.krdb.obda.model.*;
import it.unibz.krdb.obda.model.OBDAQueryModifiers.OrderCondition;
import it.unibz.krdb.obda.model.Predicate.COL_TYPE;
import it.unibz.krdb.obda.model.ValueConstant;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.OBDAVocabulary;
import it.unibz.krdb.obda.model.impl.TermUtils;
import it.unibz.krdb.obda.owlrefplatform.core.abox.SemanticIndexURIMap;
import it.unibz.krdb.obda.owlrefplatform.core.basicoperations.UriTemplateMatcher;
import it.unibz.krdb.obda.parser.EncodeForURI;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.*;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/***
 * Translate a SPARQL algebra expression into a Datalog program that has the
 * same semantics. We use the built-in predicates Join and Left join. The rules
 * in the program have always 1 or 2 operator atoms, plus (in)equality atoms
 * (due to filters).
 * 
 * This programs needs to be flattened by another procedure later.
 * 
 * @author mariano
 */
public class SparqlAlgebraToDatalogTranslator {

    private final OBDADataFactory ofac = OBDADataFactoryImpl.getInstance();

    private final DatatypeFactory dtfac = OBDADataFactoryImpl.getInstance().getDatatypeFactory();

    private final UriTemplateMatcher uriTemplateMatcher;

    private final SemanticIndexURIMap uriRef;

    private final Set<Predicate> dataPropertiesAndClassesSameAs;

    private final Set<Predicate> objectPropertiesSameAs;

    private int bnode;

    //count for bnode created in sameAsmap
    private static final Logger log = LoggerFactory.getLogger(SparqlAlgebraToDatalogTranslator.class);

    public SparqlAlgebraToDatalogTranslator(UriTemplateMatcher uriTemplateMatcher, SemanticIndexURIMap uriRef, Set<Predicate> dataPropertiesAndClassesSameAs, Set<Predicate> objectPropertiesSameAs) {
        this.uriTemplateMatcher = uriTemplateMatcher;
        this.uriRef = uriRef;
        this.dataPropertiesAndClassesSameAs = dataPropertiesAndClassesSameAs;
        this.objectPropertiesSameAs = objectPropertiesSameAs;
        bnode = 0;
    }

    /**
	 * 
	 * @param templateMatcher
	 * @param uriRef is used only in the Semantic Index mode
	 */
    /**
	 * Translate a given SPARQL query object to datalog program.
	 * 
	 *
	 *            The Query object.
	 * @return Datalog program that represents the construction of the SPARQL
	 *         query.
	 */
    public DatalogProgram translate(ParsedQuery pq) {
        TupleExpr te = pq.getTupleExpr();
        log.debug("SPARQL algebra: \n{}", te);
        List<Term> answerVariables;
        if (pq instanceof ParsedTupleQuery || pq instanceof ParsedGraphQuery) {
            Set<String> signature = te.getBindingNames();
            answerVariables = new ArrayList<>(signature.size());
            for (String vs : signature) {
                answerVariables.add(ofac.getVariable(vs));
            }
        } else {
            answerVariables = Collections.emptyList();
        }
        DatalogProgram result = ofac.getDatalogProgram();
        Function bodyAtom = translateTupleExpr(te, result, OBDAVocabulary.QUEST_QUERY + "0");
        createRule(result, OBDAVocabulary.QUEST_QUERY, answerVariables, bodyAtom);
        return result;
    }

    /**
	 * main translation method -- a recursive switch over all possible types of subexpressions
	 * 
	 * @param te
	 * @param pr
	 * @param newHeadName
	 */
    private Function translateTupleExpr(TupleExpr te, DatalogProgram pr, String newHeadName) {
        if (te instanceof Slice) {
            Slice slice = (Slice) te;
            pr.getQueryModifiers().setOffset(slice.getOffset());
            pr.getQueryModifiers().setLimit(slice.getLimit());
            return translateTupleExpr(slice.getArg(), pr, newHeadName);
        } else {
            if (te instanceof Distinct) {
                Distinct distinct = (Distinct) te;
                pr.getQueryModifiers().setDistinct();
                return translateTupleExpr(distinct.getArg(), pr, newHeadName);
            } else {
                if (te instanceof Order) {
                    Order order = (Order) te;
                    for (OrderElem c : order.getElements()) {
                        ValueExpr expression = c.getExpr();
                        if (!(expression instanceof Var)) {
                            throw new IllegalArgumentException("Error translating ORDER BY. " + "The current implementation can only sort by variables, this query has a more complex expression \'" + expression + "\'");
                        }
                        Var v = (Var) expression;
                        Variable var = ofac.getVariable(v.getName());
                        int direction = c.isAscending() ? OrderCondition.ORDER_ASCENDING : OrderCondition.ORDER_DESCENDING;
                        pr.getQueryModifiers().addOrderCondition(var, direction);
                    }
                    return translateTupleExpr(order.getArg(), pr, newHeadName);
                } else {
                    if (te instanceof Projection) {
                        return translate((Projection) te, pr, newHeadName);
                    } else {
                        if (te instanceof Filter) {
                            return translate((Filter) te, pr, newHeadName);
                        } else {
                            if (te instanceof StatementPattern) {
                                return addSameAs(translate((StatementPattern) te), pr, newHeadName);
                            } else {
                                if (te instanceof Join) {
                                    return translate((Join) te, pr, newHeadName);
                                } else {
                                    if (te instanceof Union) {
                                        return translate((Union) te, pr, newHeadName);
                                    } else {
                                        if (te instanceof LeftJoin) {
                                            return translate((LeftJoin) te, pr, newHeadName);
                                        } else {
                                            if (te instanceof Reduced) {
                                                Reduced reduced = (Reduced) te;
                                                return translateTupleExpr(reduced.getArg(), pr, newHeadName);
                                            } else {
                                                if (te instanceof Extension) {
                                                    return translate((Extension) te, pr, newHeadName);
                                                } else {
                                                    if (te instanceof BindingSetAssignment) {
                                                        return createFilterValuesAtom((BindingSetAssignment) te);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        try {
            throw new QueryEvaluationException("Operation not supported: " + te);
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Set<Variable> getVariables(Function atom) {
        Set<Variable> set = new HashSet<>();
        for (Term t : atom.getTerms()) {
            if (t instanceof Variable) {
                set.add((Variable) t);
            }
        }
        return set;
    }

    private static List<Term> getUnion(Set<Variable> s1, Set<Variable> s2) {
        Set<Term> vars = new HashSet<>();
        vars.addAll(s1);
        vars.addAll(s2);
        List<Term> varList = new ArrayList<>(vars);
        return varList;
    }

    private CQIE createRule(DatalogProgram pr, String headName, List<Term> headParameters, Function... body) {
        Predicate pred = ofac.getPredicate(headName, headParameters.size());
        Function head = ofac.getFunction(pred, headParameters);
        CQIE rule = ofac.getCQIE(head, body);
        pr.appendRule(rule);
        return rule;
    }

    /**
	 * EXTEND { (T_j AS V_j) } EXPR
	 * 
	 * where the T_j are built from the variables X of EXPR,
	 *   
	 * adds the following rule:
	 * 
	 *   ans_i(X * T) :- ans_{i.0}(X)
	 * 
	 * @param extend
	 * @param pr
	 * @param newHeadName
	 * @return 
	 */
    private Function translate(Extension extend, DatalogProgram pr, String newHeadName) {
        Function subAtom = translateTupleExpr(extend.getArg(), pr, newHeadName + "0");
        Set<Variable> subVarSet = getVariables(subAtom);
        int sz = subVarSet.size() + extend.getElements().size();
        List<Term> varList = new ArrayList<>(sz);
        varList.addAll(subVarSet);
        List<Term> termList = new ArrayList<>(sz);
        termList.addAll(varList);
        for (ExtensionElem el : extend.getElements()) {
            Variable var = ofac.getVariable(el.getName());
            varList.add(var);
            Term term = getExpression(el.getExpr());
            termList.add(term);
        }
        CQIE rule = createRule(pr, newHeadName, termList, subAtom);
        Function newHeadAtom = ofac.getFunction(rule.getHead().getFunctionSymbol(), varList);
        return newHeadAtom;
    }

    /**
	 * EXPR_1 UNION EXPR_2
	 * 
	 * adds the following two rules
	 * 
	 * ans_i(X * X_1 * NULL_2) :- ans_{i.0}(X * X_1)
	 * ans_i(X * NULL_1 * X_2) :- ans_{i.1}(X * X_2)
	 * 
	 * where NULL_i is the padding of X_i with NULLs
	 * 
	 * @param union
	 * @param pr
	 * @param newHeadName
	 * @return 
	 */
    private Function translate(Union union, DatalogProgram pr, String newHeadName) {
        Function leftAtom = translateTupleExpr(union.getLeftArg(), pr, newHeadName + "0");
        Set<Variable> leftVars = getVariables(leftAtom);
        Function rightAtom = translateTupleExpr(union.getRightArg(), pr, newHeadName + "1");
        Set<Variable> rightVars = getVariables(rightAtom);
        List<Term> varList = getUnion(leftVars, rightVars);
        List<Term> leftTermList = new ArrayList<>(varList.size());
        for (Term t : varList) {
            Term lt = (leftVars.contains(t)) ? t : OBDAVocabulary.NULL;
            leftTermList.add(lt);
        }
        CQIE leftRule = createRule(pr, newHeadName, leftTermList, leftAtom);
        List<Term> rightTermList = new ArrayList<>(varList.size());
        for (Term t : varList) {
            Term lt = (rightVars.contains(t)) ? t : OBDAVocabulary.NULL;
            rightTermList.add(lt);
        }
        CQIE rightRule = createRule(pr, newHeadName, rightTermList, rightAtom);
        Function atom = ofac.getFunction(rightRule.getHead().getFunctionSymbol(), varList);
        return atom;
    }

    /**
	 * EXPR_1 JOIN EXPR_2
	 * 
	 * adds the following rule 
	 * 
	 * ans_i(X_1 U X_2) :- ans_{i.0}(X_1), ans_{i.1}(X_2)
	 * 
	 * @param join
	 * @param pr
	 * @param newHeadName
	 * @return 
	 */
    private Function translate(Join join, DatalogProgram pr, String newHeadName) {
        Function leftAtom = translateTupleExpr(join.getLeftArg(), pr, newHeadName + "0");
        Function rightAtom = translateTupleExpr(join.getRightArg(), pr, newHeadName + "1");
        List<Term> varList = getUnion(getVariables(leftAtom), getVariables(rightAtom));
        CQIE rule = createRule(pr, newHeadName, varList, leftAtom, rightAtom);
        return rule.getHead();
    }

    /**
	 * EXPR_1 OPT EXPR_2 FILTER F
	 * 
	 * ans_i(X_1 U X_2) :- LEFTJOIN(ans_{i.0}(X_1), ans_{i.1}(X_2), F(X_1 U X_2))
	 * 
	 * @param leftjoin
	 * @param pr
	 * @param newHeadName
	 * @return 
	 */
    private Function translate(LeftJoin leftjoin, DatalogProgram pr, String newHeadName) {
        Function leftAtom = translateTupleExpr(leftjoin.getLeftArg(), pr, newHeadName + "0");
        Function rightAtom = translateTupleExpr(leftjoin.getRightArg(), pr, newHeadName + "1");
        Function joinAtom = ofac.getSPARQLLeftJoin(leftAtom, rightAtom);
        ValueExpr filter = leftjoin.getCondition();
        if (filter != null) {
            List<Term> joinTerms = joinAtom.getTerms();
            joinTerms.add((Function) getExpression(filter));
        }
        List<Term> varList = getUnion(getVariables(leftAtom), getVariables(rightAtom));
        CQIE rule = createRule(pr, newHeadName, varList, joinAtom);
        return rule.getHead();
    }

    /**
	 * PROJECT { V_j } EXPR
	 * 
	 * adds the following rule
	 * 
	 * ans_i(V) :- ans_{i.0}(X)
	 * 
	 * @param project
	 * @param pr
	 * @param newHeadName
	 * @return 
	 */
    private Function translate(Projection project, DatalogProgram pr, String newHeadName) {
        Function atom = translateTupleExpr(project.getArg(), pr, newHeadName + "0");
        List<ProjectionElem> projectionElements = project.getProjectionElemList().getElements();
        List<Term> varList = new ArrayList<>(projectionElements.size());
        for (ProjectionElem var : projectionElements) {
            if (!var.getSourceName().equals(var.getTargetName())) {
                boolean found = false;
                for (Term a : atom.getTerms()) {
                    if ((a instanceof Variable) && ((Variable) a).getName().equals(var.getSourceName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException("Projection target of " + var + " not found in " + project.getArg());
                }
            }
            varList.add(ofac.getVariable(var.getTargetName()));
        }
        CQIE rule = createRule(pr, newHeadName, varList, atom);
        return rule.getHead();
    }

    /**
	 * FILTER EXPR F
	 * 
	 * adds the following rule
	 * 
	 * ans_i(X U X') :- ans_{i.0}(X), F(X')
	 * 
	 * @param filter
	 * @param pr
	 * @param newHeadName
	 * @return
	 */
    private Function translate(Filter filter, DatalogProgram pr, String newHeadName) {
        Function atom = translateTupleExpr(filter.getArg(), pr, newHeadName + "0");
        Set<Variable> atomVars = getVariables(atom);
        ValueExpr condition = filter.getCondition();
        Function filterAtom;
        if (condition instanceof Var) {
            filterAtom = ofac.getFunctionIsTrue(getOntopTerm((Var) condition));
        } else {
            filterAtom = (Function) getExpression(condition);
        }
        Set<Variable> filterVars = new HashSet<>();
        TermUtils.addReferencedVariablesTo(filterVars, filterAtom);
        List<Term> vars = getUnion(atomVars, filterVars);
        CQIE rule = createRule(pr, newHeadName, vars, atom, filterAtom);
        return rule.getHead();
    }

    /***
	 * This translates a single triple. 
	 * 
	 * @param triple
	 * @return
	 */
    private Function translate(StatementPattern triple) {
        Var pred = triple.getPredicateVar();
        Value p = pred.getValue();
        if (!(p instanceof URI || (p == null))) {
            throw new RuntimeException("Unsupported query syntax");
        }
        Var obj = triple.getObjectVar();
        Var subj = triple.getSubjectVar();
        Term sTerm = getOntopTerm(subj);
        if ((p != null) && p.toString().equals(RDF.TYPE.stringValue())) {
            Value o = obj.getValue();
            if (o == null) {
                Function rdfTypeConstant = ofac.getUriTemplate(ofac.getConstantLiteral(OBDAVocabulary.RDF_TYPE));
                return ofac.getTripleAtom(sTerm, rdfTypeConstant, ofac.getVariable(obj.getName()));
            } else {
                if (o instanceof URI) {
                    URI objectUri = (URI) o;
                    Predicate.COL_TYPE type = dtfac.getDatatype(objectUri);
                    Predicate predicate;
                    if (type != null) {
                        predicate = dtfac.getTypePredicate(type);
                    } else {
                        predicate = ofac.getClassPredicate(objectUri.stringValue());
                    }
                    return ofac.getFunction(predicate, sTerm);
                } else {
                    throw new RuntimeException("Unsupported query syntax");
                }
            }
        } else {
            Term oTerm = getOntopTerm(obj);
            if (p != null) {
                COL_TYPE subjectType = null;
                COL_TYPE objectType = null;
                Predicate predicate = ofac.getPredicate(p.stringValue(), new COL_TYPE[] { subjectType, objectType });
                return ofac.getFunction(predicate, sTerm, oTerm);
            } else {
                return ofac.getTripleAtom(sTerm, ofac.getVariable(pred.getName()), oTerm);
            }
        }
    }

    private Term getOntopTerm(Var subj) {
        Value s = subj.getValue();
        Term result = null;
        if (s == null) {
            result = ofac.getVariable(subj.getName());
        } else {
            if (s instanceof Literal) {
                Literal object = (Literal) s;
                URI type = object.getDatatype();
                String value = object.getLabel();
                if (type != null) {
                    boolean valid = XMLDatatypeUtil.isValidValue(value, type);
                    if (!valid) {
                        throw new RuntimeException("Invalid lexical form for datatype. Found: " + value);
                    }
                }
                COL_TYPE objectType;
                if (type == null) {
                    objectType = COL_TYPE.LITERAL;
                } else {
                    objectType = dtfac.getDatatype(type);
                    if (objectType == null) {
                        throw new RuntimeException("Unsupported datatype: " + type.stringValue());
                    }
                }
                if ((objectType == COL_TYPE.DECIMAL) && !value.contains(".")) {
                    objectType = COL_TYPE.INTEGER;
                }
                ValueConstant constant = ofac.getConstantLiteral(value, objectType);
                if (objectType == COL_TYPE.LITERAL) {
                    String lang = object.getLanguage();
                    if (lang != null && !lang.equals("")) {
                        result = ofac.getTypedTerm(constant, lang.toLowerCase());
                    } else {
                        result = ofac.getTypedTerm(constant, objectType);
                    }
                } else {
                    result = ofac.getTypedTerm(constant, objectType);
                }
            } else {
                if (s instanceof URI) {
                    if (uriRef != null) {
                        int id = uriRef.getId(s.stringValue());
                        result = ofac.getUriTemplate(ofac.getConstantLiteral(String.valueOf(id), COL_TYPE.INTEGER));
                    } else {
                        String subject_URI = EncodeForURI.decodeURIEscapeCodes(s.stringValue());
                        result = uriTemplateMatcher.generateURIFunction(subject_URI);
                    }
                }
            }
        }
        return result;
    }

    /**
	 * Creates a "FILTER" atom out of VALUES bindings.
	 */
    private Function createFilterValuesAtom(BindingSetAssignment expression) {
        Map<String, Variable> variableIndex = createVariableIndex(expression.getBindingNames());
        /**
		 * Example of a composite term corresponding to a binding: AND(EQ(X,1), EQ(Y,2))
		 */
        List<Function> bindingCompositeTerms = new ArrayList<>();
        for (BindingSet bindingSet : expression.getBindingSets()) {
            bindingCompositeTerms.add(createBindingCompositeTerm(variableIndex, bindingSet));
        }
        if (bindingCompositeTerms.isEmpty()) {
            // TODO: find a better exception
            throw new RuntimeException("Unsupported SPARQL query: VALUES entry without any binding!");
        }
        Function orAtom = buildBooleanTree(bindingCompositeTerms, ExpressionOperation.OR);
        return orAtom;
    }

    private Map<String, Variable> createVariableIndex(Set<String> variableNames) {
        Map<String, Variable> variableIndex = new HashMap<>();
        for (String varName : variableNames) {
            variableIndex.put(varName, ofac.getVariable(varName));
        }
        return variableIndex;
    }

    /**
	 * Used for VALUES bindings
	 */
    private Function createBindingCompositeTerm(Map<String, Variable> variableIndex, BindingSet bindingSet) {
        List<Function> bindingEqualityTerms = new ArrayList<>();
        for (Binding binding : bindingSet) {
            Variable variable = variableIndex.get(binding.getName());
            if (variable == null) {
                //TODO: find a better exception
                throw new RuntimeException("Unknown variable " + binding.getName() + " used in the VALUES clause.");
            }
            Term valueTerm = getConstantExpression(binding.getValue());
            Function equalityTerm = ofac.getFunction(ExpressionOperation.EQ, variable, valueTerm);
            bindingEqualityTerms.add(equalityTerm);
        }
        if (bindingEqualityTerms.isEmpty()) {
            //TODO: find a better exception
            throw new RuntimeException("Empty binding sets are not accepted.");
        }
        return buildBooleanTree(bindingEqualityTerms, ExpressionOperation.AND);
    }

    /**
	 * Builds a boolean tree (e.g. AND or OR-tree) out of boolean expressions.
	 *
	 * This approach is necessary because AND(..) and OR(..) have a 2-arity.
	 *
	 */
    private Function buildBooleanTree(List<Function> booleanFctTerms, ExpressionOperation booleanFunctionSymbol) {
        Function topFunction = null;
        int termNb = booleanFctTerms.size();
        for (int i = 0; i < termNb; i += 2) {
            Function newFunction;
            if ((termNb - i) >= 2) {
                newFunction = ofac.getFunction(booleanFunctionSymbol, booleanFctTerms.get(i), booleanFctTerms.get(i + 1));
            } else {
                newFunction = booleanFctTerms.get(i);
            }
            if (topFunction == null)
                topFunction = newFunction;
            else
                topFunction = ofac.getFunction(booleanFunctionSymbol, topFunction, newFunction);
        }
        return topFunction;
    }

    private Term getExpression(ValueExpr expr) {
        if (expr instanceof Var) {
            return getOntopTerm((Var) expr);
        } else {
            if (expr instanceof org.openrdf.query.algebra.ValueConstant) {
                return getConstantExpression(((org.openrdf.query.algebra.ValueConstant) expr).getValue());
            } else {
                if (expr instanceof UnaryValueOperator) {
                    return getUnaryExpression((UnaryValueOperator) expr);
                } else {
                    if (expr instanceof BinaryValueOperator) {
                        return getBinaryExpression((BinaryValueOperator) expr);
                    } else {
                        if (expr instanceof Bound) {
                            return ofac.getFunctionIsNotNull(getOntopTerm(((Bound) expr).getArg()));
                        } else {
                            if (expr instanceof FunctionCall) {
                                return getFunctionCallTerm((FunctionCall) expr);
                            }
                        }
                    }
                }
            }
        }
        throw new RuntimeException("The expression " + expr + " is not supported yet!");
    }

    private Term getConcat(List<ValueExpr> values) {
        Iterator<ValueExpr> iterator = values.iterator();
        Term topConcat = getExpression(iterator.next());
        if (!iterator.hasNext()) {
            throw new UnsupportedOperationException("Wrong number of arguments (found " + values.size() + ", at least 1) of SQL function CONCAT");
        }
        while (iterator.hasNext()) {
            Term second_string = getExpression(iterator.next());
            topConcat = ofac.getFunctionConcat(topConcat, second_string);
        }
        return topConcat;
    }

    private Term getSubstring(List<ValueExpr> args) {
        if (args.size() == 2) {
            Term str = getExpression(args.get(0));
            Term st = getExpression(args.get(1));
            return ofac.getFunctionSubstring(str, st);
        } else {
            if (args.size() == 3) {
                Term str = getExpression(args.get(0));
                Term st = getExpression(args.get(1));
                Term en = getExpression(args.get(2));
                return ofac.getFunctionSubstring(str, st, en);
            } else {
                throw new UnsupportedOperationException("Wrong number of arguments (found " + args.size() + ", only 2 or 3 supported) for SQL SUBSTRING function");
            }
        }
    }

    private Term getReplace(List<ValueExpr> args) {
        if (args.size() == 2) {
            Term t1 = getExpression(args.get(0));
            Term out_string = getExpression(args.get(1));
            Term in_string = ofac.getConstantLiteral("");
            return ofac.getFunctionReplace(t1, out_string, in_string);
        } else {
            if (args.size() == 3) {
                Term t1 = getExpression(args.get(0));
                Term out_string = getExpression(args.get(1));
                Term in_string = getExpression(args.get(2));
                return ofac.getFunctionReplace(t1, out_string, in_string);
            } else {
                throw new UnsupportedOperationException("Wrong number of arguments (found " + args.size() + ", only 2 or 3 supported) to sql function REPLACE");
            }
        }
    }

    // XPath 1.0 functions (XPath 1.1 has variants with more arguments)
    private static final ImmutableMap<String, OperationPredicate> XPathFunctions = new ImmutableMap.Builder<String, OperationPredicate>().put("http://www.w3.org/2005/xpath-functions#upper-case", ExpressionOperation.UCASE).put("http://www.w3.org/2005/xpath-functions#lower-case", ExpressionOperation.LCASE).put("http://www.w3.org/2005/xpath-functions#string-length", ExpressionOperation.STRLEN).put("http://www.w3.org/2005/xpath-functions#substring-before", ExpressionOperation.STRBEFORE).put("http://www.w3.org/2005/xpath-functions#substring-after", ExpressionOperation.STRAFTER).put("http://www.w3.org/2005/xpath-functions#starts-with", ExpressionOperation.STR_STARTS).put("http://www.w3.org/2005/xpath-functions#ends-with", ExpressionOperation.STR_ENDS).put("http://www.w3.org/2005/xpath-functions#encode-for-uri", ExpressionOperation.ENCODE_FOR_URI).put("http://www.w3.org/2005/xpath-functions#contains", ExpressionOperation.CONTAINS).put("UUID", ExpressionOperation.UUID).put("STRUUID", ExpressionOperation.STRUUID).put("http://www.w3.org/2005/xpath-functions#numeric-abs", ExpressionOperation.ABS).put("http://www.w3.org/2005/xpath-functions#numeric-ceil", ExpressionOperation.CEIL).put("http://www.w3.org/2005/xpath-functions#numeric-floor", ExpressionOperation.FLOOR).put("http://www.w3.org/2005/xpath-functions#numeric-round", ExpressionOperation.ROUND).put("RAND", ExpressionOperation.RAND).put("http://www.w3.org/2005/xpath-functions#year-from-dateTime", ExpressionOperation.YEAR).put("http://www.w3.org/2005/xpath-functions#day-from-dateTime", ExpressionOperation.DAY).put("http://www.w3.org/2005/xpath-functions#month-from-dateTime", ExpressionOperation.MONTH).put("http://www.w3.org/2005/xpath-functions#hours-from-dateTime", ExpressionOperation.HOURS).put("http://www.w3.org/2005/xpath-functions#minutes-from-dateTime", ExpressionOperation.MINUTES).put("http://www.w3.org/2005/xpath-functions#seconds-from-dateTime", ExpressionOperation.SECONDS).put("NOW", ExpressionOperation.NOW).put("TZ", ExpressionOperation.TZ).put("MD5", ExpressionOperation.MD5).put("SHA1", ExpressionOperation.SHA1).put("SHA256", ExpressionOperation.SHA256).put("SHA512", ExpressionOperation.SHA512).build();

    /** Return the Functions supported at the moment only
     * concat and replace
     * @param expr
     * @return
     */
    private Term getFunctionCallTerm(FunctionCall expr) {
        OperationPredicate p = XPathFunctions.get(expr.getURI());
        if (p != null) {
            List<ValueExpr> args = expr.getArgs();
            if (args.size() != p.getArity()) {
                throw new UnsupportedOperationException("Wrong number of arguments (found " + args.size() + ", only " + p.getArity() + "supported) for SPARQL " + expr.getURI() + "function");
            }
            List<Term> terms = new ArrayList<>(args.size());
            for (ValueExpr a : args) {
                terms.add(getExpression(a));
            }
            Term fun = ofac.getFunction(p, terms);
            return fun;
        }
        switch(expr.getURI()) {
            case "http://www.w3.org/2005/xpath-functions#concat":
                return getConcat(expr.getArgs());
            case "http://www.w3.org/2005/xpath-functions#replace":
                return getReplace(expr.getArgs());
            case "http://www.w3.org/2005/xpath-functions#substring":
                return getSubstring(expr.getArgs());
            default:
                throw new RuntimeException("The builtin function " + expr.getURI() + " is not supported yet!");
        }
    }

    private Term getConstantExpression(Value v) {
        if (v instanceof Literal) {
            Literal lit = (Literal) v;
            URI type = lit.getDatatype();
            COL_TYPE tp;
            if (type == null) {
                tp = COL_TYPE.LITERAL;
            } else {
                tp = dtfac.getDatatype(type);
                if (tp == null) {
                    return ofac.getUriTemplateForDatatype(type.stringValue());
                }
            }
            String constantString;
            switch(tp) {
                case INTEGER:
                case NEGATIVE_INTEGER:
                case NON_POSITIVE_INTEGER:
                case POSITIVE_INTEGER:
                case NON_NEGATIVE_INTEGER:
                    constantString = lit.integerValue().toString();
                    break;
                case LONG:
                    constantString = lit.longValue() + "";
                    break;
                case DECIMAL:
                    constantString = lit.decimalValue().toString();
                    break;
                case FLOAT:
                    constantString = lit.floatValue() + "";
                    break;
                case DOUBLE:
                    constantString = lit.doubleValue() + "";
                    break;
                case INT:
                case UNSIGNED_INT:
                    constantString = lit.intValue() + "";
                    break;
                case DATETIME_STAMP:
                case DATETIME:
                case YEAR:
                case DATE:
                case TIME:
                    constantString = lit.calendarValue().toString();
                    break;
                case BOOLEAN:
                    constantString = lit.booleanValue() + "";
                    break;
                case STRING:
                case LITERAL:
                    constantString = lit.stringValue() + "";
                    break;
                default:
                    throw new RuntimeException("Undefined datatype: " + tp);
            }
            ValueConstant constant = ofac.getConstantLiteral(constantString, tp);
            return ofac.getTypedTerm(constant, tp);
        } else {
            if (v instanceof URI) {
                Function constantFunction = uriTemplateMatcher.generateURIFunction(v.stringValue());
                if (constantFunction.getArity() == 1) {
                    constantFunction = ofac.getUriTemplateForDatatype(v.stringValue());
                }
                return constantFunction;
            }
        }
        throw new RuntimeException("The value " + v + " is not supported yet!");
    }

    private Term getUnaryExpression(UnaryValueOperator expr) {
        Term term = getExpression(expr.getArg());
        if (expr instanceof Not) {
            return ofac.getFunctionNOT(term);
        } else {
            if (expr instanceof IsLiteral) {
                return ofac.getFunction(ExpressionOperation.IS_LITERAL, term);
            } else {
                if (expr instanceof IsURI) {
                    return ofac.getFunction(ExpressionOperation.IS_IRI, term);
                } else {
                    if (expr instanceof Str) {
                        return ofac.getFunction(ExpressionOperation.SPARQL_STR, term);
                    } else {
                        if (expr instanceof Datatype) {
                            return ofac.getFunction(ExpressionOperation.SPARQL_DATATYPE, term);
                        } else {
                            if (expr instanceof IsBNode) {
                                return ofac.getFunction(ExpressionOperation.IS_BLANK, term);
                            } else {
                                if (expr instanceof Lang) {
                                    ValueExpr arg = expr.getArg();
                                    if (arg instanceof Var) {
                                        return ofac.getFunction(ExpressionOperation.SPARQL_LANG, term);
                                    } else {
                                        throw new RuntimeException("A variable or a value is expected in " + expr);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new RuntimeException("The expression " + expr + " is not supported yet!");
    }

    private static final ImmutableMap<Compare.CompareOp, ExpressionOperation> relationalOperations = new ImmutableMap.Builder<Compare.CompareOp, ExpressionOperation>().put(Compare.CompareOp.EQ, ExpressionOperation.EQ).put(Compare.CompareOp.GE, ExpressionOperation.GTE).put(Compare.CompareOp.GT, ExpressionOperation.GT).put(Compare.CompareOp.LE, ExpressionOperation.LTE).put(Compare.CompareOp.LT, ExpressionOperation.LT).put(Compare.CompareOp.NE, ExpressionOperation.NEQ).build();

    private static final ImmutableMap<MathExpr.MathOp, ExpressionOperation> numericalOperations = new ImmutableMap.Builder<MathExpr.MathOp, ExpressionOperation>().put(MathExpr.MathOp.PLUS, ExpressionOperation.ADD).put(MathExpr.MathOp.MINUS, ExpressionOperation.SUBTRACT).put(MathExpr.MathOp.MULTIPLY, ExpressionOperation.MULTIPLY).put(MathExpr.MathOp.DIVIDE, ExpressionOperation.DIVIDE).build();

    private Term getBinaryExpression(BinaryValueOperator expr) {
        Term term1 = getExpression(expr.getLeftArg());
        Term term2 = getExpression(expr.getRightArg());
        if (expr instanceof And) {
            return ofac.getFunctionAND(term1, term2);
        } else {
            if (expr instanceof Or) {
                return ofac.getFunctionOR(term1, term2);
            } else {
                if (expr instanceof SameTerm) {
                    return ofac.getFunctionEQ(term1, term2);
                } else {
                    if (expr instanceof Regex) {
                        Regex reg = (Regex) expr;
                        ValueExpr flags = reg.getFlagsArg();
                        Term term3 = (flags != null) ? getExpression(flags) : OBDAVocabulary.NULL;
                        return ofac.getFunction(ExpressionOperation.REGEX, term1, term2, term3);
                    } else {
                        if (expr instanceof Compare) {
                            ExpressionOperation p = relationalOperations.get(((Compare) expr).getOperator());
                            return ofac.getFunction(p, term1, term2);
                        } else {
                            if (expr instanceof MathExpr) {
                                ExpressionOperation p = numericalOperations.get(((MathExpr) expr).getOperator());
                                return ofac.getFunction(p, term1, term2);
                            } else {
                                if (expr instanceof LangMatches) {
                                    return ofac.getLANGMATCHESFunction(term1, toLowerCase(term2));
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new RuntimeException("The expression " + expr + " is not supported yet!");
    }

    private Term toLowerCase(Term term) {
        Term output = term;
        if (term instanceof Function) {
            Function f = (Function) term;
            if (f.isDataTypeFunction()) {
                Term functionTerm = f.getTerm(0);
                if (functionTerm instanceof Constant) {
                    Constant c = (Constant) functionTerm;
                    output = ofac.getFunction(f.getFunctionSymbol(), ofac.getConstantLiteral(c.getValue().toLowerCase(), c.getType()));
                }
            }
        }
        return output;
    }

    private Function addSameAs(Function leftAtom, DatalogProgram pr, String newHeadName) {
        //case of class and data properties
        if (dataPropertiesAndClassesSameAs.contains(leftAtom.getFunctionSymbol())) {
            Function rightAtomUnion = createJoinWithSameAsOnLeft(leftAtom, pr, newHeadName + "1");
            //create union between the first statement and join
            //between hasProperty(x,y) and owl:sameAs(x, anon-x) hasProperty (anon-x, y)
            Function atom = createUnion(leftAtom, rightAtomUnion, pr, newHeadName);
            return atom;
        } else if (objectPropertiesSameAs.contains(leftAtom.getFunctionSymbol())) {
            //create union between the first join on the left and join on the right
            Function union2 = createUnionObject(leftAtom, pr, newHeadName + "1");
            //create union between the first statement  and the union
            Function atom = createUnion(leftAtom, union2, pr, newHeadName);
            return atom;
        }
        return leftAtom;
    }

    private Function createUnionObject(Function leftAtom, DatalogProgram pr, String newHeadName) {
        Function union1 = createUnionUnbound(leftAtom, pr, newHeadName + "1");
        Function leftAtomUnion2 = createJoinWithSameAsOnRight(leftAtom, pr, newHeadName + "0");
        return createUnion(leftAtomUnion2, union1, pr, newHeadName);
    }

    private Function createUnionUnbound(Function leftAtom, DatalogProgram pr, String newHeadName) {
        Function rightAtomUnionDouble = createJoinWithSameAsOnLeftAndRight(leftAtom, pr, newHeadName + "1");
        Function leftAtomUnion1 = createJoinWithSameAsOnLeft(leftAtom, pr, newHeadName + "0");
        return createUnion(leftAtomUnion1, rightAtomUnionDouble, pr, newHeadName);
    }

    private Function createJoinWithSameAsOnLeftAndRight(Function leftAtom, DatalogProgram pr, String newHeadName) {
        //ON THE RIGHT
        //create right atom of the join between the data property and same as
        //given a data property as hasProperty (x, y)
        //create an unbound  hasProperty (anon-x1, anon-y1)
        Function unboundleftAtom = ofac.getFunction(leftAtom.getFunctionSymbol());
        unboundleftAtom.updateTerms(leftAtom.getTerms());
        unboundleftAtom.setTerm(0, ofac.getVariable("anon-" + bnode + leftAtom.getTerm(0)));
        unboundleftAtom.setTerm(1, ofac.getVariable("anon-" + bnode + leftAtom.getTerm(1)));
        //create statement pattern for same as create owl:sameAs(anon-y1, y)
        //it will be the right atom of the join
        Predicate sameAs = ofac.getPredicate("http://www.w3.org/2002/07/owl#sameAs", new COL_TYPE[] { COL_TYPE.OBJECT, COL_TYPE.OBJECT });
        Term sTerm2 = unboundleftAtom.getTerm(1);
        Term oTerm2 = leftAtom.getTerm(1);
        Function rightAtomJoin2 = ofac.getFunction(sameAs, sTerm2, oTerm2);
        //create join rule
        List<Term> varListJoin2 = getUnion(getVariables(unboundleftAtom), getVariables(rightAtomJoin2));
        CQIE joinRule2 = createRule(pr, newHeadName + "0", varListJoin2, unboundleftAtom, rightAtomJoin2);
        Function joinRight = joinRule2.getHead();
        //ON THE LEFT
        //given a data property ex hasProperty (x, y)
        //create statement pattern for same as create owl:sameAs( x, anon-x1)
        //it will be the left atom of the join
        Term sTerm = leftAtom.getTerm(0);
        Term oTerm = unboundleftAtom.getTerm(0);
        Function leftAtomJoin = ofac.getFunction(sameAs, sTerm, oTerm);
        //create join rule
        List<Term> varListJoin = getUnion(getVariables(leftAtomJoin), getVariables(joinRight));
        CQIE joinRule = createRule(pr, newHeadName, varListJoin, leftAtomJoin, joinRight);
        return joinRule.getHead();
    }

    private Function createUnion(Function leftAtom, Function rightAtom, DatalogProgram pr, String newHeadName) {
        Set<Variable> leftVars = getVariables(leftAtom);
        Set<Variable> rightVars = getVariables(rightAtom);
        List<Term> varListUnion = getUnion(leftVars, rightVars);
        // left atom rule
        List<Term> leftTermList = new ArrayList<>(varListUnion.size());
        for (Term t : varListUnion) {
            Term lt = (leftVars.contains(t)) ? t : OBDAVocabulary.NULL;
            leftTermList.add(lt);
        }
        CQIE leftRule = createRule(pr, newHeadName, leftTermList, leftAtom);
        // right atom rule
        List<Term> rightTermList = new ArrayList<>(varListUnion.size());
        for (Term t : varListUnion) {
            Term lt = (rightVars.contains(t)) ? t : OBDAVocabulary.NULL;
            rightTermList.add(lt);
        }
        CQIE rightRule = createRule(pr, newHeadName, rightTermList, rightAtom);
        return ofac.getFunction(rightRule.getHead().getFunctionSymbol(), varListUnion);
    }

    private Function createJoinWithSameAsOnLeft(Function leftAtom, DatalogProgram pr, String newHeadName) {
        //given a data property ex hasProperty (x, y)
        //create statement pattern for same as create owl:sameAs( anon-x, y)
        //it will be the left atom of the join
        Predicate predicate = ofac.getPredicate("http://www.w3.org/2002/07/owl#sameAs", new COL_TYPE[] { COL_TYPE.OBJECT, COL_TYPE.OBJECT });
        Term sTerm = leftAtom.getTerm(0);
        Term oTerm = ofac.getVariable("anon-" + bnode + leftAtom.getTerm(0));
        Function leftAtomJoin = ofac.getFunction(predicate, sTerm, oTerm);
        //create left atom of the join between the data property and same as
        //given a data property as hasProperty (x, y)
        //create the right atom hasProperty (anon-x, y)
        Function rightAtomJoin = ofac.getFunction(leftAtom.getFunctionSymbol());
        rightAtomJoin.updateTerms(leftAtom.getTerms());
        rightAtomJoin.setTerm(0, ofac.getVariable("anon-" + bnode + leftAtom.getTerm(0)));
        //create join rule
        List<Term> varListJoin = getUnion(getVariables(leftAtomJoin), getVariables(rightAtomJoin));
        CQIE joinRule = createRule(pr, newHeadName, varListJoin, leftAtomJoin, rightAtomJoin);
        bnode++;
        return joinRule.getHead();
    }

    private Function createJoinWithSameAsOnRight(Function leftAtom, DatalogProgram pr, String newHeadName) {
        //create right atom of the join between the data property and same as
        //given a data property as hasProperty (x, y)
        //create the left atom hasProperty (x, anon-y)
        Function leftAtomJoin2 = ofac.getFunction(leftAtom.getFunctionSymbol());
        leftAtomJoin2.updateTerms(leftAtom.getTerms());
        leftAtomJoin2.setTerm(1, ofac.getVariable("anon-" + bnode + leftAtom.getTerm(1)));
        //create statement pattern for same as create owl:sameAs(anon-y, y)
        //it will be the right atom of the join
        Predicate predicate = ofac.getPredicate("http://www.w3.org/2002/07/owl#sameAs", new COL_TYPE[] { COL_TYPE.OBJECT, COL_TYPE.OBJECT });
        Term sTerm2 = ofac.getVariable("anon-" + bnode + leftAtom.getTerm(1));
        Term oTerm2 = leftAtom.getTerm(1);
        Function rightAtomJoin2 = ofac.getFunction(predicate, sTerm2, oTerm2);
        //create join rule
        List<Term> varListJoin2 = getUnion(getVariables(leftAtomJoin2), getVariables(rightAtomJoin2));
        CQIE joinRule2 = createRule(pr, newHeadName, varListJoin2, leftAtomJoin2, rightAtomJoin2);
        bnode++;
        return joinRule2.getHead();
    }
}
