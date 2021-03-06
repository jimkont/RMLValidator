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
/***************************************************************************
 *
 * R2RML Model : Standard GraphtMap Class
 *
 * Any subject map or predicate-object map may have one
 * or more associated graph maps. Graph maps are 
 * themselves term maps. When RDF triples are generated,
 * the set of target graphs is determined by taking into
 * account any graph maps associated with the subject map
 * or predicate-object map.
 * 
 * modified by mielvandersande, andimou
 *
 ****************************************************************************/
package be.ugent.mmlab.rml.model.std;

import be.ugent.mmlab.rml.exceptions.InvalidRMLStructureException;
import be.ugent.mmlab.rml.exceptions.RMLDataError;
import be.ugent.mmlab.rml.model.AbstractTermMap;
import be.ugent.mmlab.rml.model.GraphMap;
import be.ugent.mmlab.rml.model.TermType;
import be.ugent.mmlab.rml.model.reference.ReferenceIdentifier;
import net.antidot.semantic.rdf.model.tools.RDFDataValidator;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

public class StdGraphMap extends AbstractTermMap implements GraphMap {


	public StdGraphMap(Value constantValue,
			String stringTemplate, String inverseExpression,
			ReferenceIdentifier referenceValue, URI termType){ 
		// No Literal term type
		// ==> No datatype
		// ==> No specified language tag
		// Only termType possible : IRI => by default
		super(constantValue, null, null, stringTemplate,
				termType, inverseExpression, referenceValue);
		
	}


        @Override
	protected void checkSpecificTermType(TermType tt)
			throws InvalidRMLStructureException {
		// If the term map is a predicate map: rr:IRI
		if (tt != TermType.IRI) {
			throw new InvalidRMLStructureException(
					"[StdGraphMap:checkSpecificTermType] If the term map is a "
							+ "graph map: only rr:IRI  is required");
		}
	}

        @Override
	protected void checkConstantValue(Value constantValue)
			throws RMLDataError {
		// If the constant-valued term map is a graph map then its constant
				// value must be an IRI.
		if (!RDFDataValidator.isValidURI(constantValue.stringValue())) {
                                throw new RMLDataError(
                                                "[StdGraphMap:checkConstantValue] Not a valid URI : "
                                                + constantValue);
                            }
	}
	
}
