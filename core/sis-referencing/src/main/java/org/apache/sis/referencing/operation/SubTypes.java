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

import org.opengis.referencing.operation.*;


/**
 * Implementation of {@link AbstractCoordinateOperation} methods that require knowledge about subclasses.
 * Those methods are defined in a separated static class for avoiding class loading of all implementations
 * before necessary.
 *
 * <p>This class currently provides implementation for the following methods:</p>
 * <ul>
 *   <li>{@link AbstractCoordinateOperation#castOrCopy(CoordinateOperation)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class SubTypes {
    /**
     * Do not allow instantiation of this class.
     */
    private SubTypes() {
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
        if (object instanceof SingleOperation) {
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
}
