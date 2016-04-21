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
package org.apache.sis.internal.jaxb.referencing;

import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.PassThroughOperation;
import org.opengis.referencing.operation.SingleOperation;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class CC_CoordinateOperation extends PropertyType<CC_CoordinateOperation, CoordinateOperation> {
    /**
     * Empty constructor for JAXB only.
     */
    public CC_CoordinateOperation() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code CoordinateOperation.class}
     */
    @Override
    protected Class<CoordinateOperation> getBoundType() {
        return CoordinateOperation.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private CC_CoordinateOperation(final CoordinateOperation conversion) {
        super(conversion);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given value
     * in a {@code <gml:CoordinateOperation>} XML element.
     *
     * @param  conversion The element to marshall.
     * @return A {@code PropertyType} wrapping the given the element.
     */
    @Override
    protected CC_CoordinateOperation wrap(final CoordinateOperation conversion) {
        return new CC_CoordinateOperation(conversion);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual element to write
     * inside the {@code <gml:CoordinateOperation>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The element to be marshalled.
     */
    @XmlElementRef
    public AbstractCoordinateOperation getElement() {
        return AbstractCoordinateOperation.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param operation The unmarshalled element.
     */
    public void setElement(final AbstractCoordinateOperation operation) {
        metadata = operation;
        /*
         * In an older ISO 19111 model, PassThroughOperation extended SingleOperation.
         * It was forcing us to provide a dummy value or null for the 'method' property.
         * This has been fixed in newer ISO 19111 model, but for safety with object following the older model
         * (e.g. when using GeoAPI 3.0) we are better to skip the check for the SingleOperation case when the
         * operation is a PassThroughOperation.
         */
        if (operation instanceof PassThroughOperation) {
            if (((PassThroughOperation) operation).getOperation() == null) {
                incomplete("coordOperation");
            }
        } else if ((operation instanceof SingleOperation) && ((SingleOperation) operation).getMethod() == null) {
            incomplete("method");
        }
    }
}
