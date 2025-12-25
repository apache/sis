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
package org.apache.sis.metadata.iso.extent;

import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.internal.Resources;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;


/**
 * Thrown when an envelope cannot be transformed to a geographic, vertical or temporal extent.
 * This exception occurs when the envelope Coordinate Reference System (CRS) has no spatial or temporal component.
 * For example, it may be an engineering CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class NotSpatioTemporalException extends TransformException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6964320493942299039L;

    /**
     * Constructs a new exception with no detail message.
     */
    public NotSpatioTemporalException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param  message  the details message, or {@code null} if none.
     */
    public NotSpatioTemporalException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param  message  the details message, or {@code null} if none.
     * @param  cause    the cause, or {@code null} if none.
     */
    public NotSpatioTemporalException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a message for a spatial, vertical or temporal dimension not found.
     *
     * @param type      0=horizontal, 1=vertical, 2=temporal, 3=spatial or temporal.
     * @param envelope  the envelope where the dimension was not found.
     */
    NotSpatioTemporalException(final int type, final Envelope envelope) {
        super(message(type, envelope.getCoordinateReferenceSystem()));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="7", fixed="25")
    private static String message(final int type, final CoordinateReferenceSystem crs) {
        return (crs != null) ? Resources.format(Resources.Keys.MissingDimension_2, type, crs.getName())
                             : Errors.format(Errors.Keys.UnspecifiedCRS);
    }
}
