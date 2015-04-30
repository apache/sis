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
package org.apache.sis.referencing;

import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.AbstractDatum;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;


/**
 * Implementation of {@link AbstractIdentifiedObject} methods that require knowledge about subclasses.
 * Those methods are defined in a separated static class for avoiding class loading of all identified
 * object implementations before necessary.
 *
 * <p>This class currently provides implementation for the following methods:</p>
 * <ul>
 *   <li>{@link AbstractIdentifiedObject#castOrCopy(IdentifiedObject)}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
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
     * Returns a SIS implementation for the given object.
     *
     * @see AbstractIdentifiedObject#castOrCopy(IdentifiedObject)
     */
    static AbstractIdentifiedObject castOrCopy(final IdentifiedObject object) {
        if (object instanceof CoordinateReferenceSystem) {
            return AbstractCRS.castOrCopy((CoordinateReferenceSystem) object);
        }
        if (object instanceof CoordinateSystem) {
            return AbstractCS.castOrCopy((CoordinateSystem) object);
        }
        if (object instanceof CoordinateSystemAxis) {
            return DefaultCoordinateSystemAxis.castOrCopy((CoordinateSystemAxis) object);
        }
        if (object instanceof Datum) {
            return AbstractDatum.castOrCopy((Datum) object);
        }
        if (object instanceof Ellipsoid) {
            return DefaultEllipsoid.castOrCopy((Ellipsoid) object);
        }
        if (object instanceof PrimeMeridian) {
            return DefaultPrimeMeridian.castOrCopy((PrimeMeridian) object);
        }
        if (object instanceof CoordinateOperation) {
            return AbstractCoordinateOperation.castOrCopy((CoordinateOperation) object);
        }
        if (object instanceof OperationMethod) {
            return DefaultOperationMethod.castOrCopy((OperationMethod) object);
        }
        if (object instanceof ParameterDescriptor<?>) {
            return DefaultParameterDescriptor.castOrCopy((ParameterDescriptor<?>) object);
        }
        if (object instanceof ParameterDescriptorGroup) {
            return DefaultParameterDescriptorGroup.castOrCopy((ParameterDescriptorGroup) object);
        }
        /*
         * Intentionally check for AbstractIdentifiedObject after the interfaces because user may have defined his own
         * subclass implementing the interface. If we were checking for AbstractIdentifiedObject before the interfaces,
         * the returned instance could have been a user subclass without the JAXB annotations required for XML marshalling.
         */
        if (object == null || object instanceof AbstractIdentifiedObject) {
            return (AbstractIdentifiedObject) object;
        }
        return new AbstractIdentifiedObject(object);
    }
}
