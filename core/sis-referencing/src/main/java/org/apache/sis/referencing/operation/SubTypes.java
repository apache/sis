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
package org.apache.sis.referencing.operation;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.AbstractIdentifiedObject;


/**
 * Implementation of {@link AbstractCoordinateOperation} methods that require knowledge about subclasses.
 * Those methods are defined in a separated static class for avoiding class loading of all implementations
 * before necessary.
 *
 * <p>This class currently provides implementation for the following methods:</p>
 * <ul>
 *   <li>{@link AbstractCoordinateOperation#castOrCopy(CoordinateOperation)}</li>
 *   <li>{@link DefaultConversion#castOrCopy(Conversion)}</li>
 *   <li>{@link DefaultConversion#specialize(Class, CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransformFactory)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
final class SubTypes {
    /**
     * Do not allow instantiation of this class.
     */
    private SubTypes() {
    }

    /**
     * Returns {@code true} if the given operation is a single operation but not a pass-through operation.
     * In an older ISO 19111 model, {@link PassThroughOperation} extended {@link SingleOperation}, which
     * was a problem for providing a value to the inherited {@link SingleOperation#getMethod()} method.
     * This has been fixed in newer ISO 19111 model, but for safety with objects following the older model
     * (e.g. GeoAPI 3.0) we are better to perform an explicit exclusion of {@link PassThroughOperation}.
     */
    static boolean isSingleOperation(final CoordinateOperation operation) {
        return (operation instanceof SingleOperation) && !(operation instanceof PassThroughOperation);
    }

    /**
     * Returns a SIS implementation for the given coordinate operation.
     *
     * @see AbstractCoordinateOperation#castOrCopy(CoordinateOperation)
     */
    static AbstractCoordinateOperation castOrCopy(final CoordinateOperation object) {
        if (object instanceof Transformation) {
            return DefaultTransformation.castOrCopy((Transformation) object);
        }
        if (object instanceof Conversion) {
            return forConversion((Conversion) object);
        }
        if (object instanceof PassThroughOperation) {
            return DefaultPassThroughOperation.castOrCopy((PassThroughOperation) object);
        }
        if (object instanceof ConcatenatedOperation) {
            return DefaultConcatenatedOperation.castOrCopy((ConcatenatedOperation) object);
        }
        if (isSingleOperation(object)) {
            return (object instanceof AbstractSingleOperation) ? (AbstractSingleOperation) object
                   : new AbstractSingleOperation((SingleOperation) object);
        }
        /*
         * Intentionally check for AbstractCoordinateOperation after the interfaces because user may have defined
         * his own subclass implementing the same interface.  If we were checking for AbstractCoordinateOperation
         * before the interfaces, the returned instance could have been a user subclass without the JAXB annotations
         * required for XML marshalling.
         */
        if (object == null || object instanceof AbstractCoordinateOperation) {
            return (AbstractCoordinateOperation) object;
        }
        return new AbstractCoordinateOperation(object);
    }

    /**
     * Returns a SIS implementation for the given conversion.
     *
     * @see DefaultConversion#castOrCopy(Conversion)
     */
    static DefaultConversion forConversion(final Conversion object) {
        if (object instanceof CylindricalProjection) {
            return (object instanceof DefaultCylindricalProjection) ? ((DefaultCylindricalProjection) object)
                   : new DefaultCylindricalProjection((CylindricalProjection) object);
        }
        if (object instanceof ConicProjection) {
            return (object instanceof DefaultConicProjection) ? ((DefaultConicProjection) object)
                   : new DefaultConicProjection((ConicProjection) object);
        }
        if (object instanceof PlanarProjection) {
            return (object instanceof DefaultPlanarProjection) ? ((DefaultPlanarProjection) object)
                   : new DefaultPlanarProjection((PlanarProjection) object);
        }
        if (object instanceof Projection) {
            return (object instanceof DefaultProjection) ? ((DefaultProjection) object)
                   : new DefaultProjection((Projection) object);
        }
        if (object == null || object instanceof DefaultConversion) {
            return (DefaultConversion) object;
        }
        return new DefaultConversion(object);
    }

    /**
     * Returns a conversion from the specified defining conversion.
     * The new conversion will be a more specific type like a {@linkplain PlanarProjection planar},
     * {@linkplain CylindricalProjection cylindrical} or {@linkplain ConicProjection conic projection}.
     * The returned conversion will implement at least the {@code baseType} interface, but may implement
     * a more specific GeoAPI interface if this method has been able to infer the type from the
     * {@code conversion} argument.
     *
     * @param  baseType   The base GeoAPI interface to be implemented by the conversion to return.
     * @param  definition The defining conversion.
     * @param  sourceCRS  The source CRS.
     * @param  targetCRS  The target CRS.
     * @param  factory    The factory to use for creating a transform from the parameters or for performing axis changes.
     * @return The conversion of the given type between the given CRS.
     * @throws ClassCastException if a contradiction is found between the given {@code baseType},
     *         the defining {@linkplain DefaultConversion#getInterface() conversion type} and
     *         the {@linkplain DefaultOperationMethod#getOperationType() method operation type}.
     */
    static <T extends Conversion> T create(final Class<T> baseType, Conversion definition,
            final CoordinateReferenceSystem sourceCRS, final CoordinateReferenceSystem targetCRS,
            final MathTransformFactory factory) throws FactoryException
    {
        Class<? extends T> type = baseType;
        if (definition instanceof AbstractIdentifiedObject) {
            final Class<?> c = ((AbstractIdentifiedObject) definition).getInterface();
            if (!c.isAssignableFrom(baseType)) {  // Do nothing if c is a parent type.
                type = c.asSubclass(type);
            }
        }
        final OperationMethod method = definition.getMethod();
        if (method instanceof DefaultOperationMethod) {
            final Class<? extends SingleOperation> c = ((DefaultOperationMethod) method).getOperationType();
            if (!c.isAssignableFrom(baseType)) {  // Do nothing if c is a parent type.
                type = c.asSubclass(type);
            }
        }
        Conversion conversion;
        if (type.isInstance(definition)
                && definition.getSourceCRS() == sourceCRS
                && definition.getTargetCRS() == targetCRS
                && definition.getMathTransform() != null)
        {
            conversion = definition;
        } else {
            final OperationMethod[] actual = new OperationMethod[1];
            boolean tryAgain;
            do {
                tryAgain = false;
                if (CylindricalProjection.class.isAssignableFrom(type)) {
                    conversion = new DefaultCylindricalProjection(definition, sourceCRS, targetCRS, factory, actual);
                } else if (ConicProjection.class.isAssignableFrom(type)) {
                    conversion = new DefaultConicProjection(definition, sourceCRS, targetCRS, factory, actual);
                } else if (PlanarProjection.class.isAssignableFrom(type)) {
                    conversion = new DefaultPlanarProjection(definition, sourceCRS, targetCRS, factory, actual);
                } else if (Projection.class.isAssignableFrom(type)) {
                    conversion = new DefaultProjection(definition, sourceCRS, targetCRS, factory, actual);
                } else {
                    conversion = new DefaultConversion(definition, sourceCRS, targetCRS, factory, actual);
                }
                /*
                 * The DefaultConversion constructor may have used by MathTransformFactory for creating the actual
                 * MathTransform object. In such case, we can use the knownledge that the factory has about the
                 * coordinate operation for refining again the type of the object to be returned.
                 */
                final OperationMethod m = actual[0];
                if (m instanceof DefaultOperationMethod) {
                    final Class<?> t = ((DefaultOperationMethod) m).getOperationType();
                    if (t != null && t != type && type.isAssignableFrom(t)) {
                        type = t.asSubclass(type);
                        definition = conversion;
                        tryAgain = true;
                    }
                }
            } while (tryAgain);
        }
        return type.cast(conversion);
    }
}
