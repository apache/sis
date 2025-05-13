/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.filter.sqlmm;

import java.util.Objects;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InvalidObjectException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.feature.privy.FeatureProjectionBuilder;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Literal;
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;


/**
 * A function where the last argument is the identifier of a Coordinate Reference System.
 * The first argument may be a geometry or data (WKT, WKT, GML…) for creating a geometry.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
abstract class FunctionWithSRID<R> extends SpatialFunction<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6870024245928121613L;

    /**
     * The expression giving the spatial reference system identifier, or {@code null} if none.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    final Expression<R,?> srid;

    /**
     * Identifier of the coordinate reference system in which to represent the geometry.
     * This identifier is specified by the second expression and is stored in order to
     * avoid computing {@link #targetCRS} many times when the SRID does not change.
     */
    private transient Object lastSRID;

    /**
     * The coordinate reference system in which to represent the geometry, or {@code null}
     * if not yet determined. This field is recomputed when the {@link #lastSRID} changed.
     * If {@link #literalCRS} is {@code true}, then {@code targetCRS} shall be effectively final.
     */
    private transient CoordinateReferenceSystem targetCRS;

    /**
     * Whether the {@link #getTargetCRS(Object)} value is defined by a literal.
     */
    final boolean literalCRS;

    /**
     * Whether the SRID is present, absent, or may be present or absent depending on the value.
     * If {@code ABSENT} then the {@link #srid} field will be null. In all other cases that field
     * will be non-null.
     */
    static final int PRESENT = 1, ABSENT = 0, MAYBE = 2;

    /**
     * Creates a new function for a geometry represented by the given parameter.
     *
     * @param  operation   identification of the SQLMM operation.
     * @param  parameters  sub-expressions that will be evaluated to provide the parameters to the function.
     * @param  hasSRID     whether the SRID is expected as one of {@link #PRESENT}, {@link #ABSENT} or {@link #MAYBE}.
     *
     * @todo The {@code MAYBE} flag could be removed if we know the type of value evaluated by the expression.
     *       For now it exists mostly because the last parameter given to {@code ST_Point} can be of various types.
     */
    FunctionWithSRID(final SQLMM operation, final Expression<R,?>[] parameters, int hasSRID) {
        super(operation, parameters);
        if (hasSRID == MAYBE && parameters.length < operation.maxParamCount) {
            hasSRID = ABSENT;
        }
        if (hasSRID == ABSENT) {
            literalCRS = true;
            srid = null;
            return;
        }
        srid = parameters[operation.maxParamCount - 1];
        if (srid instanceof Literal<?,?>) {
            final Object value = ((Literal<?,?>) srid).getValue();
            if (value == null) {
                literalCRS = true;
            } else {
                literalCRS = (hasSRID == PRESENT || isCRS(value.getClass()));
                if (literalCRS) try {
                    setTargetCRS(value);
                } catch (FactoryException e) {
                    throw new InvalidFilterValueException(e);
                }
            }
        } else {
            literalCRS = false;
        }
    }

    /**
     * Invoked on deserialization for restoring the {@link #targetCRS} field.
     *
     * @param  in  the input stream from which to deserialize an attribute.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the module path.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (literalCRS && srid != null) try {
            setTargetCRS(((Literal<?,?>) srid).getValue());
        } catch (FactoryException e) {
            throw (IOException) new InvalidObjectException(e.getLocalizedMessage()).initCause(e);
        }
    }

    /**
     * Returns whether the given type is a CRS or may be a SRID.
     */
    private static boolean isCRS(final Class<?> type) {
        return type == Integer.class || type == String.class || CoordinateReferenceSystem.class.isAssignableFrom(type);
    }

    /**
     * Sets {@link #targetCRS} to a coordinate reference system inferred from the given value.
     * The CRS argument shall be the result of {@code parameters.get(1).apply(object)}.
     *
     * @param  crs  the object evaluated by the {@link #srid} expression.
     * @throws FactoryException if no CRS can be created from the given object.
     */
    private void setTargetCRS(final Object crs) throws FactoryException {
search: if (crs instanceof CoordinateReferenceSystem) {
            targetCRS = (CoordinateReferenceSystem) crs;
        } else {
            final String code;
            if (crs instanceof String) {
                code = (String) crs;
            } else if (crs instanceof Integer) {
                if (((Integer) crs) == 0) {
                    targetCRS = null;
                    break search;
                }
                // TODO: should be a reference in the "spatial_ref_sys" table instead.
                code = Constants.EPSG + ':' + crs;
            } else {
                throw new InvalidGeodeticParameterException(crs == null
                        ? Errors.format(Errors.Keys.UnspecifiedCRS)
                        : Errors.format(Errors.Keys.IllegalCRSType_1, crs.getClass()));
            }
            targetCRS = CRS.forCode(code);
        }
        lastSRID = crs;
    }

    /**
     * Gets the coordinate reference system for the {@code input} resources.
     * The {@code input} argument is used only if the SRID is not a literal (which is rare).
     * If the {@link #srid} parameter is optional, then it is caller's responsibility to verify that it is non-null.
     *
     * @param  input  the resource for which to get the CRS. This is often ignored.
     * @return the CRS for the given resource.
     * @throws FactoryException if the CRS can be created.
     */
    final CoordinateReferenceSystem getTargetCRS(final R input) throws FactoryException {
        if (literalCRS) {
            return targetCRS;                   // No need to synchronize because effectively final.
        } else {
            final Object value = srid.apply(input);
            if (value == null) {
                return null;
            }
            synchronized (this) {
                if (!Objects.equals(value, lastSRID)) {
                    setTargetCRS(value);
                }
                return targetCRS;               // Must be inside synchronized block.
            }
        }
    }

    /**
     * Returns the class of resources expected by this expression.
     * Subclasses should override this method.
     */
    @Override
    public Class<? super R> getResourceClass() {
        return (srid != null) ? srid.getResourceClass() : Object.class;
    }

    /**
     * Provides the type of values produced by this expression.
     * This is the value computed by the parent class except for the <abbr>SRID</abbr>.
     *
     * @param  addTo  where to add the type of properties evaluated by this expression.
     * @return handler of type resulting from expression evaluation (never null).
     */
    @Override
    public FeatureProjectionBuilder.Item expectedType(final FeatureProjectionBuilder addTo) {
        // We must unconditionally overwrite the CRS set by the parent class.
        return super.expectedType(addTo).setCRS(literalCRS ? targetCRS : null);
    }
}
