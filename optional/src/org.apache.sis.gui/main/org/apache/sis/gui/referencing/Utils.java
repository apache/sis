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
package org.apache.sis.gui.referencing;

import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * Utility methods shared by classes in this package only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Utils {
    /**
     * Do not allow instantiation of this class.
     */
    private Utils() {
    }

    /**
     * Returns the default authority factory. Current implementation uses only the EPSG factory for avoiding
     * problems with "AUTO" factory (which requires parameters) and PROJ factory (which requires native code).
     * We lost the "CRS" factory, but it does not provide interesting new CRS compared to EPSG factory.
     * It provides CRS with different axis order such as "CRS:84", but widgets in this package ignore axis order.
     *
     * <p>If a future version uses more than one authority factory, note that it would have the side effect
     * of making authority namespaces visible in the {@link CRSChooser} "Code" column, requiring more space.
     * For example, "4326" would become "EPSG:4326". We may need to revisit the widget layout in such case.</p>
     */
    static CRSAuthorityFactory getDefaultFactory() throws FactoryException {
        return CRS.getAuthorityFactory(Constants.EPSG);
    }

    /**
     * Converts an arbitrary envelope to an envelope with (longitude, latitude) axis order in degrees.
     * The datum is unspecified. This is used for approximate comparisons of geographic area.
     */
    static ImmutableEnvelope toGeographic(final Class<?> caller, final String method, final Envelope areaOfInterest) {
        if (areaOfInterest != null) try {
            var bbox = new DefaultGeographicBoundingBox();
            bbox.setBounds(areaOfInterest);
            return new ImmutableEnvelope(bbox);
        } catch (TransformException e) {
            Logging.recoverableException(LOGGER, caller, method, e);
        }
        return null;
    }

    /**
     * Returns {@code true} if the specified domain of object intersects the area of interest.
     * If any information is missing, then this method conservatively returns {@code true}.
     * The reason for returning {@code true} is because it will usually result in no action from the caller,
     * while {@code false} results in warning emitted or CRS filtered out.
     */
    static boolean intersects(final ImmutableEnvelope areaOfInterest, final ReferenceSystem crs) {
        boolean conservative = true;
        if (areaOfInterest != null) {
            if (crs != null) {
                final GeographicBoundingBox bbox = Extents.getGeographicBoundingBox(crs.getDomainOfValidity());
                if (bbox != null) {
                    if (areaOfInterest.intersects(new ImmutableEnvelope(bbox))) {
                        return true;
                    }
                    conservative = false;
                }
            }
        }
        return conservative;
    }

    /**
     * Returns {@code true} if the given reference system should be ignored.
     */
    static boolean isIgnoreable(final ReferenceSystem system) {
        return (system instanceof CoordinateReferenceSystem) &&
                CommonCRS.Engineering.DISPLAY.datumUsedBy((CoordinateReferenceSystem) system);
    }
}
