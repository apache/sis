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

import java.io.Serializable;
import java.util.function.Predicate;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Optional information about the context in which a requested coordinate operation will be used.
 * The context can provide information such as:
 *
 * <ul>
 *   <li>The geographic area where the transformation will be used.</li>
 *   <li>The desired accuracy. A coarser accuracy may allow SIS to choose a faster transformation method.</li>
 * </ul>
 *
 * While optional, those information can help {@link DefaultCoordinateOperationFactory}
 * to choose the most suitable coordinate transformation between two CRS.
 *
 * <div class="note"><b>Example:</b>
 * if a transformation from NAD27 to WGS84 is requested without providing context, then Apache SIS will return the
 * transformation applicable to the widest North American surface. But if the user provides a context saying that
 * he wants to transform coordinates in Texas, then Apache SIS may return another coordinate transformation with
 * different {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters} more suitable
 * to Texas, but not suitable to the rest of North-America.
 * </div>
 *
 * {@code CoordinateOperationContext} is part of the API used by SIS for implementing the <cite>late binding</cite>
 * model. See {@linkplain org.apache.sis.referencing.operation package javadoc} for a note on early binding versus
 * late binding implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 *
 * @todo Should also take the country of a {@link java.util.Locale}. The EPSG database contains ISO2 and ISO3
 *       identifiers that we can use.
 */
public class CoordinateOperationContext implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6944460471653277973L;

    /**
     * The spatio-temporal area of interest, or {@code null} if none.
     */
    private Extent areaOfInterest;

    /**
     * The desired accuracy in metres, or 0 for the best accuracy available.
     * See {@link #getDesiredAccuracy()} for more details about what we mean by <cite>"best accuracy"</cite>.
     */
    private double desiredAccuracy;

    /**
     * Creates a new context with no area of interest and the best accuracy available.
     */
    public CoordinateOperationContext() {
    }

    /**
     * Creates a new context with the given area of interest and desired accuracy.
     *
     * @param area      the area of interest, or {@code null} if none.
     * @param accuracy  the desired accuracy in metres, or 0 for the best accuracy available.
     * See {@link #getDesiredAccuracy()} for more details about what we mean by <cite>"best accuracy"</cite>.
     */
    public CoordinateOperationContext(final Extent area, final double accuracy) {
        ArgumentChecks.ensurePositive("accuracy", accuracy);
        areaOfInterest  = area;
        desiredAccuracy = accuracy;
    }

    /**
     * Creates an operation context for the given area of interest, which may be null.
     * This is a convenience method for a frequently-used operation.
     *
     * @param  areaOfInterest  the area of interest, or {@code null} if none.
     * @return the operation context, or {@code null} if the given bounding box was null.
     *
     * @since 1.0
     */
    public static CoordinateOperationContext fromBoundingBox(final GeographicBoundingBox areaOfInterest) {
        if (areaOfInterest != null) {
            if (areaOfInterest instanceof DefaultGeographicBoundingBox && ((DefaultGeographicBoundingBox) areaOfInterest).isEmpty()) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "areaOfInterest"));
            }
            final CoordinateOperationContext context = new CoordinateOperationContext();
            context.setAreaOfInterest(areaOfInterest);
            return context;
        }
        return null;
    }

    /**
     * Returns the spatio-temporal area of interest, or {@code null} if none.
     *
     * @return the spatio-temporal area of interest, or {@code null} if none.
     *
     * @see Extents#getGeographicBoundingBox(Extent)
     */
    public Extent getAreaOfInterest() {
        return areaOfInterest;
    }

    /**
     * Sets the spatio-temporal area of interest, or {@code null} if none.
     *
     * @param  area  the spatio-temporal area of interest, or {@code null} if none.
     */
    public void setAreaOfInterest(Extent area) {
        if (area != null) {
            area = new DefaultExtent(area);
        }
        areaOfInterest = area;
    }

    /**
     * Sets the geographic component of the area of interest, or {@code null} if none.
     * This convenience method set the bounding box into the spatio-temporal {@link Extent}.
     *
     * <p>The reverse operation can be done with <code>{@linkplain Extents#getGeographicBoundingBox(Extent)
     * Extents.getGeographicBoundingBox}({@linkplain #getAreaOfInterest()})</code>.</p>
     *
     * @param  area  the geographic area of interest, or {@code null} if none.
     */
    public void setAreaOfInterest(final GeographicBoundingBox area) {
        areaOfInterest = setGeographicBoundingBox(areaOfInterest, area);
    }

    /**
     * Sets the given geographic bounding box in the given extent.
     */
    static Extent setGeographicBoundingBox(Extent areaOfInterest, final GeographicBoundingBox bbox) {
        if (areaOfInterest != null) {
            final DefaultExtent ex = DefaultExtent.castOrCopy(areaOfInterest);
            ex.setGeographicElements(CollectionsExt.singletonOrEmpty(bbox));
            areaOfInterest = ex;
        } else if (bbox != null) {
            areaOfInterest = new DefaultExtent(null, bbox, null, null);
        }
        return areaOfInterest;
    }

    /**
     * Returns the desired accuracy in metres.
     * A value of 0 means to search for the most accurate operation.
     *
     * <p>When searching for the most accurate operation, SIS considers only the operations specified by the authority.
     * For example the <cite>Molodensky</cite> method is a better datum shift approximation than <cite>Abridged Molodensky</cite>.
     * But if all coordinate operations defined by the authority use the Abridged Molodensky method, then SIS will ignore
     * the Molodensky one.</p>
     *
     * @return the desired accuracy in metres.
     */
    public double getDesiredAccuracy() {
        return desiredAccuracy;
    }

    /**
     * Sets the desired accuracy in metres.
     * A value of 0 means to search for the most accurate operation.
     * See {@link #getDesiredAccuracy()} for more details about what we mean by <cite>"most accurate"</cite>.
     *
     * @param  accuracy  the desired accuracy in metres.
     */
    public void setDesiredAccuracy(final double accuracy) {
        ArgumentChecks.ensurePositive("accuracy", accuracy);
        desiredAccuracy = accuracy;
    }

    /**
     * Returns a filter that can be used for applying additional restrictions on the coordinate operation.
     *
     * @todo Not yet implemented.
     */
    Predicate<CoordinateOperation> getOperationFilter() {
        return null;
    }
}
