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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.Georectified;
import org.opengis.metadata.spatial.Georeferenceable;
import org.opengis.metadata.spatial.GridSpatialRepresentation;
import org.apache.sis.xml.bind.gco.GO_Integer;
import org.apache.sis.measure.ValueRange;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ensurePositive;


/**
 * Method used to represent geographic information in the dataset.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_GridSpatialRepresentation}
 * {@code   ├─numberOfDimensions…………………………………………………} Number of independent spatial-temporal axes.
 * {@code   ├─axisDimensionProperties……………………………………} Information about spatial-temporal axis properties.
 * {@code   │   ├─dimensionName……………………………………………………} Name of the axis.
 * {@code   │   └─dimensionSize……………………………………………………} Number of elements along the axis.
 * {@code   ├─cellGeometry…………………………………………………………………} Identification of grid data as point or cell.
 * {@code   └─transformationParameterAvailability……} Indication of whether or not parameters for transformation exists.</div>
 *
 * <h2>Limitations</h2>
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
 * @version 1.5
 * @since   0.3
 */
@XmlType(name = "MD_GridSpatialRepresentation_Type", propOrder = {
    "dimensions",
    "axisDimensionProperties",
    "cellGeometry",
    "transformationParameterAvailable"
})
@XmlSeeAlso({
    DefaultGeorectified.class,
    DefaultGeoreferenceable.class
})
@XmlRootElement(name = "MD_GridSpatialRepresentation")
public class DefaultGridSpatialRepresentation extends AbstractSpatialRepresentation
        implements GridSpatialRepresentation
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1111392086980738831L;

    /**
     * Mask for the {@code transformationParameterAvailable} boolean value.
     *
     * @see #booleans
     */
    static final byte TRANSFORMATION_MASK = 1;

    // If more masks are added in a future version, then
    // all of them should be private except the last one.

    /**
     * Number of independent spatial-temporal axes.
     */
    private Integer numberOfDimensions;

    /**
     * Information about spatial-temporal axis properties.
     */
    @SuppressWarnings("serial")
    private List<Dimension> axisDimensionProperties;

    /**
     * Identification of grid data as point or cell.
     */
    private CellGeometry cellGeometry;

    /**
     * The set of booleans values. Bits are read and written using the {@code *_MASK} constants.
     *
     * @see #TRANSFORMATION_MASK
     * @see DefaultGeorectified#CHECK_POINT_MASK
     * @see DefaultGeoreferenceable#CONTROL_POINT_MASK
     * @see DefaultGeoreferenceable#OPERATION_MASK
     */
    byte booleans;

    /**
     * Constructs an initially empty grid spatial representation.
     */
    public DefaultGridSpatialRepresentation() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * <h4>Note on properties validation</h4>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(GridSpatialRepresentation)
     */
    public DefaultGridSpatialRepresentation(final GridSpatialRepresentation object) {
        super(object);
        if (object != null) {
            numberOfDimensions      = object.getNumberOfDimensions();
            axisDimensionProperties = copyList(object.getAxisDimensionProperties(), Dimension.class);
            cellGeometry            = object.getCellGeometry();
            if (object.isTransformationParameterAvailable()) {
                booleans = TRANSFORMATION_MASK;
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link Georectified} or
     *       {@link Georeferenceable}, then this method delegates to the {@code castOrCopy(…)}
     *       method of the corresponding SIS subclass. Note that if the given object implements
     *       more than one of the above-cited interfaces, then the {@code castOrCopy(…)} method
     *       to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultGridSpatialRepresentation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultGridSpatialRepresentation} instance is created using the
     *       {@linkplain #DefaultGridSpatialRepresentation(GridSpatialRepresentation) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGridSpatialRepresentation castOrCopy(final GridSpatialRepresentation object) {
        if (object instanceof Georectified) {
            return DefaultGeorectified.castOrCopy((Georectified) object);
        }
        if (object instanceof Georeferenceable) {
            return DefaultGeoreferenceable.castOrCopy((Georeferenceable) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof DefaultGridSpatialRepresentation) {
            return (DefaultGridSpatialRepresentation) object;
        }
        return new DefaultGridSpatialRepresentation(object);
    }

    /**
     * Returns the number of independent spatial-temporal axes.
     *
     * @return number of independent spatial-temporal axes, or {@code null}.
     */
    @Override
    @ValueRange(minimum = 0)
    public Integer getNumberOfDimensions() {
        return numberOfDimensions;
    }

    /**
     * Sets the number of independent spatial-temporal axes.
     *
     * @param  newValue  the new number of dimension, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setNumberOfDimensions(final Integer newValue) {
        checkWritePermission(numberOfDimensions);
        if (ensurePositive(DefaultGridSpatialRepresentation.class, "numberOfDimensions", false, newValue)) {
            numberOfDimensions = newValue;
        }
    }

    /**
     * Returns information about spatial-temporal axis properties.
     *
     * @return information about spatial-temporal axis properties.
     */
    @Override
    @XmlElement(name = "axisDimensionProperties")
    public List<Dimension> getAxisDimensionProperties() {
        return axisDimensionProperties = nonNullList(axisDimensionProperties, Dimension.class);
    }

    /**
     * Sets the information about spatial-temporal axis properties.
     *
     * @param  newValues  the new axis dimension properties.
     */
    public void setAxisDimensionProperties(final List<? extends Dimension> newValues) {
        axisDimensionProperties = writeList(newValues, axisDimensionProperties, Dimension.class);
    }

    /**
     * Returns the identification of grid data as point or cell.
     *
     * @return identification of grid data as point or cell, or {@code null}.
     */
    @Override
    @XmlElement(name = "cellGeometry", required = true)
    public CellGeometry getCellGeometry() {
        return cellGeometry;
    }

    /**
     * Sets identification of grid data as point or cell.
     *
     * @param  newValue  the new cell geometry.
     */
    public void setCellGeometry(final CellGeometry newValue) {
        checkWritePermission(cellGeometry);
        cellGeometry = newValue;
    }

    /**
     * Returns {@link Boolean#TRUE} if the given flag is set, or {@code null} otherwise.
     */
    final Boolean isDefined(final byte mask) {
        return (booleans & mask) != 0 ? Boolean.TRUE : null;
    }

    /**
     * Returns indication of whether or not parameters for transformation exists.
     *
     * @return whether or not parameters for transformation exists.
     */
    @Override
    @XmlElement(name = "transformationParameterAvailability", required = true)
    public boolean isTransformationParameterAvailable() {
        return (booleans & TRANSFORMATION_MASK) != 0;
    }

    /**
     * Sets indication of whether or not parameters for transformation exists.
     *
     * @param newValue {@code true} if the transformation parameters are available.
     */
    public void setTransformationParameterAvailable(final boolean newValue) {
        checkWritePermission(isDefined(TRANSFORMATION_MASK));
        if (newValue) {
            booleans |= TRANSFORMATION_MASK;
        } else {
            booleans &= ~TRANSFORMATION_MASK;
        }
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Invoked by JAXB for fetching the value to marshal.
     * This property is handled in a special way for allowing nil reason.
     *
     * @return the value to marshal.
     */
    @XmlElement(name = "numberOfDimensions", required = true)
    private GO_Integer getDimensions() {
        return new GO_Integer(this, "numberOfDimensions", getNumberOfDimensions(), true);
    }

    /**
     * Invoked by JAXB for setting the value.
     * This property is handled in a special way for allowing nil reason.
     *
     * @param  result  the value.
     */
    private void setDimensions(final GO_Integer result) {
        setNumberOfDimensions(result.getElement());
        result.getNilReason(this, "numberOfDimensions");
    }
}
