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
package org.apache.sis.metadata.iso.spatial;

import java.util.List;
import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.spatial.GCP;
import org.opengis.metadata.spatial.Georectified;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.geometry.primitive.Point;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.xml.Namespaces;


/**
 * Grid whose cells are regularly spaced in a geographic or projected coordinate reference system.
 * Any cell in the grid can be geolocated given its grid coordinate and the grid origin, cell spacing,
 * and orientation indication of whether or not geographic.
 *
 * <div class="section">Relationship between properties</div>
 * Providing the {@linkplain #getCheckPointDescription() check point description} implies that
 * {@linkplain #isCheckPointAvailable() check point availability} is {@code true}. The setter
 * methods will ensure that this condition is not violated.
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Georectified_Type", propOrder = {
    "checkPointAvailable",
    "checkPointDescription",
    "cornerPoints",
    "centerPoint",
    "pointInPixel",
    "transformationDimensionDescription",
    "transformationDimensionMapping",
    "checkPoints"
})
@XmlRootElement(name = "MD_Georectified")
@XmlSeeAlso(org.apache.sis.internal.jaxb.gmi.MI_Georectified.class)
public class DefaultGeorectified extends DefaultGridSpatialRepresentation implements Georectified {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2924562334097446037L;

    /**
     * Mask for the {@code checkPointAvailable} boolean value.
     *
     * @see #booleans
     */
    private static final byte CHECK_POINT_MASK = TRANSFORMATION_MASK << 1;

    /**
     * Description of geographic position points used to test the accuracy of the
     * georeferenced grid data.
     */
    private InternationalString checkPointDescription;

    /**
     * Earth location in the coordinate system defined by the Spatial Reference System
     * and the grid coordinate of the cells at opposite ends of grid coverage along two
     * diagonals in the grid spatial dimensions. There are four corner points in a
     * georectified grid; at least two corner points along one diagonal are required.
     */
    private List<Point> cornerPoints;

    /**
     * Earth location in the coordinate system defined by the Spatial Reference System
     * and the grid coordinate of the cell halfway between opposite ends of the grid in the
     * spatial dimensions.
     */
    private Point centerPoint;

    /**
     * Point in a pixel corresponding to the Earth location of the pixel.
     */
    private PixelOrientation pointInPixel;

    /**
     * Description of the information about which grid dimensions are the spatial dimensions.
     */
    private InternationalString transformationDimensionDescription;

    /**
     * Information about which grid dimensions are the spatial dimensions.
     */
    private Collection<InternationalString> transformationDimensionMapping;

    /**
     * Geographic references used to validate georectification of the data.
     */
    private Collection<GCP> checkPoints;

    /**
     * Constructs an initially empty georectified object.
     */
    public DefaultGeorectified() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Georectified)
     */
    public DefaultGeorectified(final Georectified object) {
        super(object);
        if (object != null) {
            checkPointDescription              = object.getCheckPointDescription();
            cornerPoints                       = copyList(object.getCornerPoints(), Point.class);
            centerPoint                        = object.getCenterPoint();
            pointInPixel                       = object.getPointInPixel();
            transformationDimensionDescription = object.getTransformationDimensionDescription();
            transformationDimensionMapping     = copyCollection(object.getTransformationDimensionMapping(), InternationalString.class);
            checkPoints                        = copyCollection(object.getCheckPoints(), GCP.class);

            // checkPointAvailability is required to be 'true' if there is a description.
            if (checkPointDescription != null || object.isCheckPointAvailable()) {
                booleans |= CHECK_POINT_MASK;
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultGeorectified}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultGeorectified} instance is created using the
     *       {@linkplain #DefaultGeorectified(Georectified) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGeorectified castOrCopy(final Georectified object) {
        if (object == null || object instanceof DefaultGeorectified) {
            return (DefaultGeorectified) object;
        }
        return new DefaultGeorectified(object);
    }

    /**
     * Returns an indication of whether or not geographic position points are available to test the
     * accuracy of the georeferenced grid data.
     *
     * @return Whether or not geographic position points are available to test accuracy.
     */
    @Override
    @XmlElement(name = "checkPointAvailability", required = true)
    public boolean isCheckPointAvailable() {
        return (booleans & CHECK_POINT_MASK) != 0;
    }

    /**
     * Sets an indication of whether or not geographic position points are available to test the
     * accuracy of the georeferenced grid data.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the given {@code newValue} is {@code false}, then this method automatically hides
     * the {@linkplain #setCheckPointDescription check point description} property. The description can
     * be shown again by reverting {@code checkPointAvailability} to {@code true}.
     *
     * @param newValue {@code true} if check points are available.
     */
    public void setCheckPointAvailable(final boolean newValue) {
        checkWritePermission();
        if (newValue) {
            booleans |= CHECK_POINT_MASK;
        } else {
            if (checkPointDescription != null && (booleans & CHECK_POINT_MASK) != 0) {
                Context.warningOccured(Context.current(), DefaultGeorectified.class, "setCheckPointAvailable",
                        Messages.class, Messages.Keys.PropertyHiddenBy_2, "checkPointDescription", "checkPointAvailability");
            }
            booleans &= ~CHECK_POINT_MASK;
        }
    }

    /**
     * Returns a description of geographic position points used to test the accuracy of the
     * georeferenced grid data. This value is non-null only if {@link #isCheckPointAvailable()}
     * returns {@code true}.
     *
     * @return Description of geographic position points used to test accuracy, or {@code null}.
     */
    @Override
    @XmlElement(name = "checkPointDescription")
    public InternationalString getCheckPointDescription() {
        return (booleans & CHECK_POINT_MASK) != 0 ? checkPointDescription : null;
    }

    /**
     * Sets the description of geographic position points used to test the accuracy of the
     * georeferenced grid data.
     *
     * <div class="section">Effect on other properties</div>
     * If and only if the given {@code newValue} is non-null, then this method automatically sets
     * the {@linkplain #setCheckPointAvailable check point availability} property to {@code true}.
     *
     * @param newValue The new check point description.
     */
    public void setCheckPointDescription(final InternationalString newValue) {
        checkWritePermission();
        checkPointDescription = newValue;
        if (newValue != null) {
            booleans |= CHECK_POINT_MASK;
        }
    }

    /**
     * Returns the Earth location in the coordinate system defined by the Spatial Reference System
     * and the grid coordinate of the cells at opposite ends of grid coverage along two diagonals.
     *
     * @return The corner points.
     */
    @Override
    @XmlElement(name = "cornerPoints", required = true)
    public List<Point> getCornerPoints() {
        return cornerPoints = nonNullList(cornerPoints, Point.class);
    }

    /**
     * Sets the corner points.
     *
     * The {@linkplain List#size() list size} should be 2 or 4.
     * The list should contain at least two corner points along one diagonal.
     * or may contains the 4 corner points of the georectified grid.
     *
     * <p>The first corner point shall correspond to the origin of the grid.</p>
     *
     * @param newValues The new corner points.
     */
    public void setCornerPoints(final List<? extends Point> newValues) {
        cornerPoints = writeList(newValues, cornerPoints, Point.class);
    }

    /**
     * Returns the Earth location in the coordinate system defined by the Spatial Reference System
     * and the grid coordinate of the cell halfway between opposite ends of the grid in the
     * spatial dimensions.
     *
     * @return The center point, or {@code null}.
     */
    @Override
    @XmlElement(name = "centerPoint")
    public Point getCenterPoint() {
        return centerPoint;
    }

    /**
     * Sets the center point.
     *
     * @param newValue The new center point.
     */
    public void setCenterPoint(final Point newValue) {
        checkWritePermission();
        centerPoint = newValue;
    }

    /**
     * Returns the point in a pixel corresponding to the Earth location of the pixel.
     *
     * @return Earth location of the pixel, or {@code null}.
     */
    @Override
    @XmlElement(name = "pointInPixel", required = true)
    public PixelOrientation getPointInPixel() {
        return pointInPixel;
    }

    /**
     * Sets the point in a pixel corresponding to the Earth location of the pixel.
     *
     * @param newValue The new point in a pixel.
     */
    public void setPointInPixel(final PixelOrientation newValue) {
        checkWritePermission();
        pointInPixel = newValue;
    }

    /**
     * Returns a general description of the transformation.
     *
     * @return General description of the transformation, or {@code null}.
     */
    @Override
    @XmlElement(name = "transformationDimensionDescription")
    public InternationalString getTransformationDimensionDescription() {
        return transformationDimensionDescription;
    }

    /**
     * Sets a general description of the transformation.
     *
     * @param newValue The new general description.
     */
    public void setTransformationDimensionDescription(final InternationalString newValue) {
        checkWritePermission();
        transformationDimensionDescription = newValue;
    }

    /**
     * Returns information about which grid dimensions are the spatial dimensions.
     *
     * @return Information about which grid dimensions are the spatial dimensions, or {@code null}.
     */
    @Override
    @XmlElement(name = "transformationDimensionMapping")
    public Collection<InternationalString> getTransformationDimensionMapping() {
        return transformationDimensionMapping = nonNullCollection(transformationDimensionMapping, InternationalString.class);
    }

    /**
     * Sets information about which grid dimensions are the spatial dimensions.
     * The given list should contain at most 2 elements.
     *
     * @param newValues The new transformation mapping.
     */
    public void setTransformationDimensionMapping(final Collection<? extends InternationalString> newValues) {
        transformationDimensionMapping = writeCollection(newValues, transformationDimensionMapping, InternationalString.class);
    }

    /**
     * Returns the geographic references used to validate georectification of the data.
     *
     * @return Geographic references used to validate georectification.
     */
    @Override
    @XmlElement(name = "checkPoint", namespace = Namespaces.GMI)
    public Collection<GCP> getCheckPoints() {
        return checkPoints = nonNullCollection(checkPoints, GCP.class);
    }

    /**
     * Sets the geographic references used to validate georectification of the data.
     *
     * @param newValues The new check points values.
     */
    public void setCheckPoints(final Collection<? extends GCP> newValues) {
        checkPoints = writeCollection(newValues, checkPoints, GCP.class);
    }
}
