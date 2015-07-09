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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.internal.jaxb.gco.GO_Measure;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.util.ArgumentChecks;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;

// Branch-specific imports
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Axis properties.
 *
 * <p><b>Limitations:</b></p>
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
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_Dimension_Type", propOrder = {
    "dimensionName",
    "dimensionSize",
    "resolution",
/// "dimensionTitle",
/// "dimensionDescription"
})
@XmlRootElement(name = "MD_Dimension")
public class DefaultDimension extends ISOMetadata implements Dimension {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1104542984724265236L;

    /**
     * Name of the axis.
     */
    private DimensionNameType dimensionName;

    /**
     * Number of elements along the axis.
     */
    private Integer dimensionSize;

    /**
     * Degree of detail in the grid dataset.
     */
    private Double resolution;

    /**
     * Enhancement/ modifier of the dimension name.
     * Example: dimensionName = "column",
     *          dimensionTitle = "longitude"
     */
    private InternationalString dimensionTitle;

    /**
     * Description of the axis.
     */
    private InternationalString dimensionDescription;

    /**
     * Constructs an initially empty dimension.
     */
    public DefaultDimension() {
    }

    /**
     * Creates a dimension initialized to the given type and size.
     *
     * @param  dimensionName The name of the axis, or {@code null} if none, or {@code null} if none.
     * @param  dimensionSize The number of elements along the axis, or {@code null} if none.
     * @throws IllegalArgumentException if {@code dimensionSize} is negative.
     */
    public DefaultDimension(final DimensionNameType dimensionName, final int dimensionSize) {
        ArgumentChecks.ensurePositive("dimensionSize", dimensionSize);
        this.dimensionName = dimensionName;
        this.dimensionSize = dimensionSize;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * <div class="note"><b>Note on properties validation:</b>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     * </div>
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Dimension)
     */
    public DefaultDimension(final Dimension object) {
        super(object);
        if (object != null) {
            dimensionName        = object.getDimensionName();
            dimensionSize        = object.getDimensionSize();
            resolution           = object.getResolution();
            if (object instanceof DefaultDimension) {
                dimensionTitle       = ((DefaultDimension) object).getDimensionTitle();
                dimensionDescription = ((DefaultDimension) object).getDimensionDescription();
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
     *       {@code DefaultDimension}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDimension} instance is created using the
     *       {@linkplain #DefaultDimension(Dimension) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDimension castOrCopy(final Dimension object) {
        if (object == null || object instanceof DefaultDimension) {
            return (DefaultDimension) object;
        }
        return new DefaultDimension(object);
    }

    /**
     * Returns the name of the axis.
     *
     * @return Name of the axis, or {@code null}.
     */
    @Override
    @XmlElement(name = "dimensionName", required = true)
    public DimensionNameType getDimensionName() {
        return dimensionName;
    }

    /**
     * Sets the name of the axis.
     *
     * @param newValue The new dimension name.
     */
    public void setDimensionName(final DimensionNameType newValue) {
        checkWritePermission();
        dimensionName = newValue;
    }

    /**
     * Returns the number of elements along the axis.
     *
     * @return Number of elements along the axis, or {@code null}.
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "dimensionSize", required = true)
    public Integer getDimensionSize() {
        return dimensionSize;
    }

    /**
     * Sets the number of elements along the axis.
     *
     * @param newValue The new dimension size, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setDimensionSize(final Integer newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultDimension.class, "dimensionSize", false, newValue)) {
            dimensionSize = newValue;
        }
    }

    /**
     * Returns the degree of detail in the grid dataset.
     *
     * @return Degree of detail in the grid dataset, or {@code null}.
     */
    @Override
    @ValueRange(minimum=0, isMinIncluded=false)
    @XmlJavaTypeAdapter(GO_Measure.class)
    @XmlElement(name = "resolution")
    public Double getResolution() {
        return resolution;
    }

    /**
     * Sets the degree of detail in the grid dataset.
     *
     * @param newValue The new resolution, or {@code null}.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     */
    public void setResolution(final Double newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultDimension.class, "dimensionSize", true, newValue)) {
            resolution = newValue;
        }
    }

    /**
     * Returns the enhancement/ modifier of the dimension name.
     *
     * <div class="note"><b>Example:</b>
     * dimensionName = "column", dimensionTitle = "longitude"</div>
     *
     * @return The enhancement/ modifier of the dimension name.
     *
     * @since 0.5
     */
/// @XmlElement(name = "dimensionTitle")
    @UML(identifier="dimensionTitle", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getDimensionTitle() {
        return dimensionTitle;
    }

    /**
     * Sets the enhancement/ modifier of the dimension name.
     *
     * @param newValue The new enhancement/ modifier of the dimension name.
     *
     * @since 0.5
     */
    public void setDimensionTitle(final InternationalString newValue) {
        checkWritePermission();
        dimensionTitle = newValue;
    }

    /**
     * Return the axis dimension description.
     *
     * @return The axis dimension description.
     *
     * @since 0.5
     */
/// @XmlElement(name = "dimensionDescription")
    @UML(identifier="dimensionDescription", obligation=OPTIONAL, specification=ISO_19115)
    public InternationalString getDimensionDescription() {
        return dimensionDescription;
    }

    /**
     * Sets the axis dimension description.
     *
     * @param newValue The new axis dimension description.
     *
     * @since 0.5
     */
    public void setDimensionDescription(final InternationalString newValue) {
        checkWritePermission();
        dimensionDescription = newValue;
    }
}
