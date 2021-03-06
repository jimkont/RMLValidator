/* 
 * Copyright 2011 Antidot opensource@antidot.net
 * https://github.com/antidot/db2triples
 * 
 * DB2Triples is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * DB2Triples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * *************************************************************************
 *
 * R2RML Model : Abstract Term Map
 *
 * Partial implementation of a term map.
 *
 * modified by mielvandersande, andimou
 *
 ***************************************************************************
 */
package be.ugent.mmlab.rml.model;

import be.ugent.mmlab.rml.model.std.StdObjectMap;
import be.ugent.mmlab.rml.model.reference.ReferenceIdentifier;
import be.ugent.mmlab.rml.model.reference.ReferenceIdentifierImpl;
import be.ugent.mmlab.rml.tools.CustomRDFDataValidator;
import be.ugent.mmlab.rml.exceptions.*;

import java.util.HashSet;
import java.util.Set;

import net.antidot.semantic.rdf.model.tools.RDFDataValidator;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.tools.R2RMLToolkit;
import net.antidot.semantic.xmls.xsd.XSDLexicalTransformation;
import net.antidot.semantic.xmls.xsd.XSDType;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

public abstract class AbstractTermMap implements TermMap {

        // Log
        private static Logger log = LogManager.getLogger(AbstractTermMap.class);
        
        private Value constantValue;
        private URI dataType; //MVS: Changed to resource, since XSD is to limited
        private TermType termType;
        private URI implicitDataType; //MVS: Changed to resource, since XSD is to limited
        private String languageTag;
        private String stringTemplate;
        private ReferenceIdentifier referenceValue;
        private String inverseExpression;
        protected TriplesMap ownTriplesMap;

        protected AbstractTermMap(Value constantValue, URI dataType,
                String languageTag, String stringTemplate, URI termType,
                String inverseExpression, ReferenceIdentifier referenceValue){
            try {
                setConstantValue(constantValue);
                setReferenceValue(referenceValue);
                setLanguageTag(languageTag);
                setStringTemplate(stringTemplate);
                setTermType(termType, dataType);
                setDataType(dataType);
                setInversionExpression(inverseExpression);
                //TODO: make it call consistency only when validation is requested
                //checkGlobalConsistency();
                setOwnTriplesMap(ownTriplesMap);
            } catch (RMLDataError ex) {
                Logger.getLogger(AbstractTermMap.class.getName()).log(Level.FATAL, null, ex);
            } catch (InvalidRMLStructureException ex) {
                Logger.getLogger(AbstractTermMap.class.getName()).log(Level.FATAL, null, ex);
            } catch (InvalidRMLSyntaxException ex) {
                Logger.getLogger(AbstractTermMap.class.getName()).log(Level.FATAL, null, ex);
            } 
        }

        /**
         * Check if the global structure of this TermMap is consistent and valid
         * according to R2RML standard.
         *
         * @throws InvalidRMLStructureException
         */
        private void checkGlobalConsistency() throws InvalidRMLStructureException {
            // A term map must be exactly one term map type
            if (getTermMapType() == null) // In db2triples and contrary to the R2RML norm, we accepts
            // auto-assignments of blank nodes.
            {
                if (getTermType() != TermType.BLANK_NODE) {
                    log.error(
                            Thread.currentThread().getStackTrace()[1].getMethodName() + ": "
                            + "RML Syntax error: "
                            + "A constant RDF Term, a reference or a string template must be specified.");
                }
            }

        }

        private void setInversionExpression(String inverseExpression)
                throws InvalidRMLSyntaxException, InvalidRMLStructureException {
                // An inverse expression is associated with
                // a column-valued term map or template-value term map
                if (inverseExpression != null && getTermMapType() != null
                        && getTermMapType() == TermMapType.CONSTANT_VALUED) {
                        throw new InvalidRMLStructureException(
                                "[AbstractTermMap:setInversionExpression] An inverseExpression "
                                + "can not be associated with a constant-value term map.");
                }
                // This property is optional
                if (inverseExpression != null) {
                        checkInverseExpression(inverseExpression);
                }
                this.inverseExpression = inverseExpression;
        }

        private void checkInverseExpression(String inverseExpression)
                throws InvalidRMLSyntaxException {
                // An inverse expression must satisfy a lot of conditions
                if (!R2RMLToolkit.checkInverseExpression(inverseExpression)) {
                        throw new InvalidRMLSyntaxException(
                                "[AbstractTermMap:checkInverseExpression] Not a valid inverse "
                                + "expression : " + stringTemplate);
                }

        }

        private void setReferenceValue(ReferenceIdentifier referenceValue)
                throws InvalidRMLSyntaxException, InvalidRMLStructureException {
                // The value of the rml:reference property MUST be a valid reference for this queryLanguage.
//		if (columnValue != null)
//			checkColumnValue(columnValue);
                this.referenceValue = referenceValue;
        }

//	private void checkColumnValue(String columnValue)
//			throws InvalidR2RMLSyntaxException {
//		if (!SQLDataValidator.isValidSQLIdentifier(columnValue))
//			throw new InvalidR2RMLSyntaxException(
//					"[AbstractTermMap:checkColumnValue] Not a valid column "
//							+ "value : " + termType);
//	}
        protected void setTermType(URI termType, URI dataType)
                throws InvalidRMLSyntaxException, RMLDataError,
                InvalidRMLStructureException {
                if (termType == null) {
                        // If the term map does not have a rr:termType property :
                        // rr:Literal by default, if it is an object map and at
                        // least one of the following conditions is true
                        if ((this instanceof StdObjectMap)
                                && (getReferenceValue() != null || dataType != null
                                || getLanguageTag() != null || constantValue instanceof Literal)) {
                                this.termType = TermType.LITERAL;
                                log.debug("[AbstractTermMap:setTermType] No term type specified : use Literal by default.");
                        } else {
                                // otherwise its term type is IRI
                                this.termType = TermType.IRI;
                                log.debug("[AbstractTermMap:setTermType] No term type specified : use IRI by default." + getReferenceValue());
                        }

                } else 
                    this.termType = checkTermType(termType);
        }

        private TermType checkTermType(URI termType)
                throws InvalidRMLSyntaxException, InvalidRMLStructureException,
                RMLDataError {
                // Its value MUST be an IRI
                if (!RDFDataValidator.isValidURI(termType.stringValue())) {
                        throw new RMLDataError(
                                "[AbstractTermMap:checkTermType] Not a valid URI : "
                                + termType);
                }
                // (IRIs, blank nodes or literals)
                TermType tt = TermType.toTermType(termType.stringValue());
                if (tt == null) {
                        throw new InvalidRMLSyntaxException(
                                "[AbstractTermMap:checkTermType] Not a valid term type : "
                                + termType);
                }
                // Check rules in function of term map nature (subject, predicate ...)
                checkSpecificTermType(tt);
                return tt;
        }

        protected abstract void checkSpecificTermType(TermType tt)
                throws InvalidRMLStructureException;

        private void setStringTemplate(String stringTemplate)
                throws InvalidRMLSyntaxException, InvalidRMLStructureException {
                // he value of the rr:template property MUST be a
                // valid string template.
                if (stringTemplate != null) {
                        checkStringTemplate(stringTemplate);
                }

                this.stringTemplate = stringTemplate;
        }

        /**
         * A string template is a format string that can be used to build
         * strings from multiple components. It can reference column names by
         * enclosing them in curly braces.
         *
         * @throws RMLDataError
         */
        private void checkStringTemplate(String stringTemplate)
                throws InvalidRMLSyntaxException {
                // Its value MUST be an IRI
                if (!R2RMLToolkit.checkStringTemplate(stringTemplate)) {
                        throw new InvalidRMLSyntaxException(
                                "[AbstractTermMap:checkStringTemplate] Not a valid string "
                                + "template : " + stringTemplate);
                }
        }

        private void setLanguageTag(String languageTag) throws RMLDataError {
                // its value MUST be a valid language tag
                if (languageTag != null) {
                        checkLanguageTag(languageTag);
                }
                this.languageTag = languageTag;
        }

        /**
         * Check if language tag is valid, as defined by [RFC-3066]
         *
         * @throws RMLDataError
         */
        private void checkLanguageTag(String languageTag) throws RMLDataError {
                // Its value MUST be an IRI
                if (!RDFDataValidator.isValidLanguageTag(languageTag)) {
                        throw new RMLDataError(
                                "[AbstractTermMap:checkLanguageTag] Not a valid language tag : "
                                + languageTag);
                }
        }

        /**
         * Check if constant value is correctly defined. Constant value is an
         * IRI or literal in function of this term map type.
         */
        protected abstract void checkConstantValue(Value constantValue)
                throws RMLDataError;

        public void setConstantValue(Value constantValue) throws RMLDataError,
                InvalidRMLStructureException {
                // Check if constant value is valid
                if (constantValue != null) {
                        checkConstantValue(constantValue);
                }
                this.constantValue = constantValue;
        }

        /**
         * Check if datatype is correctly defined.
         *
         * @throws RMLDataError
         */
        public void checkDataType(URI dataType) throws RMLDataError {
                // Its value MUST be an IRI
                //MVS: class below prevents datatypes other than XSD
                //if (!RDFDataValidator.isValidDatatype(dataType.stringValue())) {
                if (!CustomRDFDataValidator.isValidDatatype(dataType.stringValue())) {
                        throw new RMLDataError(
                                "[AbstractTermMap:checkDataType] Not a valid URI : "
                                + dataType);
                }
        }

        public void setDataType(URI dataType) throws RMLDataError,
                InvalidRMLStructureException {
                if (!isTypeable() && dataType != null) {
                        throw new InvalidRMLStructureException(
                                "[AbstractTermMap:setDataType] A term map that is not "
                                + "a typeable term map MUST NOT have an rr:datatype"
                                + " property.");
                }
                if (dataType != null) {
                        // Check if datatype is valid
                        checkDataType(dataType);
                        //this.dataType = new dataType.stringValue();
                        this.dataType = dataType;
                }
        }
        
        public void setOwnTriplesMap(TriplesMap ownTriplesMap)
			throws InvalidRMLStructureException {               
		this.ownTriplesMap = ownTriplesMap;
	}

        @Override
        public Value getConstantValue() {
                return constantValue;
        }

        @Override
        public URI getDataType() {
                return dataType;
        }

        @Override
        public URI getImplicitDataType() {
                return implicitDataType;
        }

        @Override
        public XSDLexicalTransformation.Transformation getImplicitTransformation() {
                if (implicitDataType == null) {
                        return null;
                } else {
                        return XSDLexicalTransformation
                                .getLexicalTransformation(XSDType.toXSDType(implicitDataType.stringValue()));
                }
        }

        @Override
        public String getInverseExpression() {
                return inverseExpression;
        }

        @Override
        public String getLanguageTag() {
                return languageTag;
        }

        @Override
        public Set<ReferenceIdentifier> getReferencedSelectors() {
                Set<ReferenceIdentifier> references = new HashSet<ReferenceIdentifier>();
                switch (getTermMapType()) {
                        case CONSTANT_VALUED:
                                // The referenced columns of a constant-valued term map is the
                                // empty set.
                                break;

                        case REFERENCE_VALUED:
                                // The referenced columns of a column-valued term map is
                                // the singleton set containing the value of rr:column.
                                // referencedColumns.add(R2RMLToolkit.deleteBackSlash(columnValue));
                                references.add(referenceValue);
                                break;

                        case TEMPLATE_VALUED:
                                // The referenced columns of a template-valued term map is
                                // the set of column names enclosed in unescaped curly braces
                                // in the template string.
                                for (String colName : R2RMLToolkit
                                        .extractColumnNamesFromStringTemplate(stringTemplate)) {
                                        references.add(ReferenceIdentifierImpl.buildFromR2RMLConfigFile(colName));
                                }
                                break;

                        default:
                                break;
                }
                log.debug("[AbstractTermMap:getReferencedSelectors] References are now : "
                        + references);
                return references;
        }

        @Override
        public String getStringTemplate() {
                return stringTemplate;
        }

        @Override
        public TermMapType getTermMapType() {
                if (constantValue != null) {
                        return TermMapType.CONSTANT_VALUED;
                } else if (referenceValue != null) {
                        return TermMapType.REFERENCE_VALUED;
                } else if (stringTemplate != null) {
                        return TermMapType.TEMPLATE_VALUED;
                } else if (termType == TermType.BLANK_NODE) {
                        return TermMapType.NO_VALUE_FOR_BNODE;
                }
                return null;
        }

        @Override
        public TermType getTermType() {
                return termType;
        }

        @Override
        public ReferenceIdentifier getReferenceValue() {
                return referenceValue;
        }

        @Override
        public boolean isOveridden() {
                if (implicitDataType == null) {
                        // No implicit datatype extracted for yet
                        // Return false by default
                        return false;
                }
                return dataType != implicitDataType;
        }

        @Override
        public boolean isTypeable() {
                return (termType == TermType.LITERAL) && (languageTag == null);
        }

        public void setImplicitDataType(URI implicitDataType) {
                this.implicitDataType = implicitDataType;
        }
        
        @Override
        public TriplesMap getOwnTriplesMap(){
            return this.ownTriplesMap;
        }

//	public String getValue(Map<ColumnIdentifier, byte[]> dbValues,
//			ResultSetMetaData dbTypes) throws R2RMLDataError, SQLException,
//			UnsupportedEncodingException {
//
//		log.debug("[AbstractTermMap:getValue] Extract value of termType with termMapType : "
//				+ getTermMapType());
//		switch (getTermMapType()) {
//		case CONSTANT_VALUED:
//			return constantValue.stringValue();
//
//		case REFERENCE_VALUED:
//			if (dbValues.keySet().isEmpty())
//				throw new IllegalStateException(
//						"[AbstractTermMap:getValue] impossible to extract from an empty database value set.");
//			byte[] bytesResult = dbValues.get(referenceValue);
//			/* Extract the SQLType in dbValues from the key which is
//			 * equals to "columnValue" */
//			SQLType sqlType = null;			
//			for(ColumnIdentifier colId : dbValues.keySet()) {
//			    if(colId.equals(referenceValue)) {
//				sqlType = colId.getSqlType();
//				break;
//			    }
//			}			    
//			// Apply cast to string to the SQL data value
//			String result;
//			if (sqlType != null) {
//				XSDType xsdType = SQLToXMLS.getEquivalentType(sqlType);
//				result = XSDLexicalTransformation.extractNaturalRDFFormFrom(
//						xsdType, bytesResult);
//			}
//			else
//			{
//			    result = new String(bytesResult, "UTF-8");
//			}
//			return result;
//
//		case TEMPLATE_VALUED:
//			if (dbValues.keySet().isEmpty())
//				throw new IllegalStateException(
//						"[AbstractTermMap:getValue] impossible to extract from an empty database value set.");
//			result = R2RMLToolkit.extractColumnValueFromStringTemplate(
//					stringTemplate, dbValues, dbTypes);
//			return result;
//
//		default:
//			return null;
//		}
//}
}
