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
package org.apache.sis.metadata.iso.content;

import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.opengis.metadata.content.Band;
import org.apache.sis.measure.ValueRange;

// Branch-specific imports
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


// Leading </pre> is a workaround for a javadoc 6 bug on classes having @UML annotation.
/**
 * </pre>
 * The characteristic of each dimension (layer) included in the resource.
 *
 * <div class="warning"><b>Note on International Standard versions</b><br>
 * This class is derived from a new type defined in the ISO 19115 international standard published in 2014,
 * while GeoAPI 3.0 is based on the version published in 2003. Consequently this implementation class does
 * not yet implement a GeoAPI interface, but is expected to do so after the next GeoAPI releases.
 * When the interface will become available, all references to this implementation class in Apache SIS will
 * be replaced be references to the corresponding interface.
 * </div>
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
 * @author  Remi Marechal (geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
@XmlType(name = "MD_SampleDimension_Type", propOrder = {
    "maxValue",
    "minValue",
    "units",
/// "scaleFactor",
/// "offset",
/// "meanValue",
/// "numberOfValues",
/// "standardDeviation",
/// "otherPropertyType",
/// "otherProperty",
/// "bitsPerValue"
})
@XmlRootElement(name = "MD_SampleDimension")
@XmlSeeAlso({DefaultBand.class, DefaultRangeDimension.class})
@UML(identifier="MD_SampleDimension", specification=ISO_19115)
public class DefaultSampleDimension extends DefaultRangeDimension {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4517148689016920767L;

    /**
     * Maximum value of data values in each dimension included in the resource.
     */
    private Double maxValue;

    /**
     * Minimum value of data values in each dimension included in the resource.
     */
    private Double minValue;

    /**
     * Units of data in each dimension included in the resource.
     */
    private Unit<?> units;

    /**
     * Scale factor which has been applied to the cell value.
     */
    private Double scaleFactor;

    /**
     * Physical value corresponding to a cell value of zero.
     */
    private Double offset;

    /**
     * Mean value of data values in each dimension included in the resource.
     */
    private Double meanValue;

    /**
     * Number of values used in a thematicClassification resource.
     */
    private Integer numberOfValues;

    /**
     * Standard deviation of data values in each dimension included in the resource.
     */
    private Double standardDeviation;

    /**
     * Type of other attribute description.
     */
    private RecordType otherPropertyType;

    /**
     * Instance of other/attributeType that defines attributes not explicitly
     * included in {@link CoverageContentType}.
     */
    private Record otherProperty;

    /**
     * Maximum number of significant bits in the uncompressed representation
     * for the value in each band of each pixel.
     */
    private Integer bitsPerValue;

    /**
     * Constructs an initially empty sample dimension.
     */
    public DefaultSampleDimension() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(SampleDimension)
     */
    public DefaultSampleDimension(final DefaultSampleDimension object) {
        super(object);
        if (object != null) {
            init(object);
        }
    }

    /**
     * Initializes this sample dimension to the values of the given object.
     */
    private void init(final DefaultSampleDimension object) {
        maxValue          = object.getMaxValue();
        minValue          = object.getMinValue();
        units             = object.getUnits();
        scaleFactor       = object.getScaleFactor();
        offset            = object.getOffset();
        meanValue         = object.getMeanValue();
        numberOfValues    = object.getNumberOfValues();
        standardDeviation = object.getStandardDeviation();
        otherPropertyType = object.getOtherPropertyType();
        otherProperty     = object.getOtherProperty();
        bitsPerValue      = object.getBitsPerValue();
    }

    /**
     * Bridge constructor for {@link DefaultBand#DefaultBand(Band)}.
     */
    DefaultSampleDimension(final Band object) {
        super(object);
        if (object != null) {
            if (object instanceof DefaultSampleDimension) {
                init((DefaultSampleDimension) object);
            } else {
                maxValue     = object.getMaxValue();
                minValue     = object.getMinValue();
                units        = object.getUnits();
                scaleFactor  = object.getScaleFactor();
                offset       = object.getOffset();
                bitsPerValue = object.getBitsPerValue();
            }
        }
    }

    /**
     * Returns the minimum value of data values in each dimension included in the resource.
     *
     * @return Minimum value of data values in each dimension included in the resource, or {@code null} if unspecified.
     */
    @XmlElement(name = "minValue")
    @UML(identifier="minValue", obligation=OPTIONAL, specification=ISO_19115)
    public Double getMinValue() {
        return minValue;
    }

    /**
     * Sets the minimum value of data values in each dimension included in the resource.
     *
     * @param newValue The new new minimum value.
     */
    public void setMinValue(final Double newValue) {
        checkWritePermission();
        minValue = newValue;
    }

    /**
     * Returns the maximum value of data values in each dimension included in the resource.
     *
     * @return Maximum value of data values in each dimension included in the resource, or {@code null} if unspecified.
     */
    @XmlElement(name = "maxValue")
    @UML(identifier="maxValue", obligation=OPTIONAL, specification=ISO_19115)
    public Double getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the maximum value of data values in each dimension included in the resource.
     *
     * @param newValue The new new maximum value.
     */
    public void setMaxValue(final Double newValue) {
        checkWritePermission();
        maxValue = newValue;
    }

    /**
     * Returns the units of data in the dimension.
     *
     * @return The units of data in the dimension, or {@code null} if unspecified.
     */
    @XmlElement(name = "units")
    @UML(identifier="units", obligation=CONDITIONAL, specification=ISO_19115)
    public Unit<?> getUnits() {
        return units;
    }

    /**
     * Sets the units of data in the dimension.
     *
     * @param newValue The new units of data in the dimension.
     */
    public void setUnits(final Unit<?> newValue) {
        checkWritePermission();
        units = newValue;
    }

    /**
     * Returns the scale factor which has been applied to the cell value.
     *
     * @return Scale factor which has been applied to the cell value, or {@code null} if none.
     */
/// @XmlElement(name = "scaleFactor")
    @UML(identifier="scaleFactor", obligation=OPTIONAL, specification=ISO_19115)
    public Double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Sets the scale factor which has been applied to the cell value.
     *
     * @param newValue The new scale factor which has been applied to the cell value.
     */
    public void setScaleFactor(final Double newValue) {
        checkWritePermission();
        scaleFactor = newValue;
    }

    /**
     * Returns the physical value corresponding to a cell value of zero.
     *
     * @return The physical value corresponding to a cell value of zero, or {@code null} if none.
     */
/// @XmlElement(name = "offset")
    @UML(identifier="offset", obligation=OPTIONAL, specification=ISO_19115)
    public Double getOffset() {
        return offset;
    }

    /**
     * Sets the physical value corresponding to a cell value of zero.
     *
     * @param newValue The new physical value corresponding to a cell value of zero, or {@code null} if none..
     */
    public void setOffset(final Double newValue) {
        checkWritePermission();
        offset = newValue;
    }

    /**
     * Returns the mean value of data values in each dimension included in the resource.
     *
     * @return The mean value of data values in each dimension included in the resource, or {@code null} if none.
     */
/// @XmlElement(name = "meanValue")
    @UML(identifier="meanValue", obligation=OPTIONAL, specification=ISO_19115)
    public Double getMeanValue() {
        return meanValue;
    }

    /**
     * Sets the mean value of data values in each dimension included in the resource.
     *
     * @param newValue The new mean value of data values in each dimension included in the resource.
     */
    public void setMeanValue(final Double newValue) {
        checkWritePermission();
        meanValue = newValue;
    }

    /**
     * Returns the number of values used in a thematic classification resource.
     *
     * @return The number of values used in a thematic classification resource, or {@code null} if none.
     */
/// @XmlElement(name = "numberOfValues")
    @UML(identifier="numberOfValues", obligation=OPTIONAL, specification=ISO_19115)
    public Integer getNumberOfValues() {
        return numberOfValues;
    }

    /**
     * Sets the number of values used in a thematic classification resource.
     *
     * @param newValues The new number of values used in a thematic classification resource.
     */
    public void setNumberOfValues(final Integer newValues) {
        checkWritePermission();
        numberOfValues = newValues;
    }

    /**
     * Returns the standard deviation of data values in each dimension included in the resource.
     *
     * @return Standard deviation of data values in each dimension included in the resource, or {@code null} if none.
     */
/// @XmlElement(name = "standardDeviation")
    @UML(identifier="standardDeviation", obligation=OPTIONAL, specification=ISO_19115)
    public Double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * Sets the standard deviation of data values in each dimension included in the resource.
     *
     * @param newValue The new standard deviation of data values in each dimension included in the resource.
     */
    public void setStandardDeviation(final Double newValue) {
        checkWritePermission();
        standardDeviation = newValue;
    }

    /**
     * Returns type of other attribute description.
     *
     * @return Type of other attribute description, or {@code null} if none.
     */
/// @XmlElement(name = "otherPropertyType")
    @UML(identifier="otherPropertyType", obligation=OPTIONAL, specification=ISO_19115)
    public RecordType getOtherPropertyType() {
        return otherPropertyType;
    }

    /**
     * Sets a new type of other attribute description.
     *
     * @param newValue The new type of other attribute description.
     */
    public void setOtherPropertyType(final RecordType newValue) {
        checkWritePermission();
        otherPropertyType = newValue;
    }

    /**
     * Returns instance of other/attributeType that defines attributes not explicitly
     * included in {@link CoverageContentType}, or {@code null} if none.
     *
     * @return instance of other/attributeType that defines attributes, or {@code null} if none.
     */
/// @XmlElement(name = "otherProperty")
    @UML(identifier="otherProperty", obligation=OPTIONAL, specification=ISO_19115)
    public Record getOtherProperty() {
        return otherProperty;
    }

    /**
     * Sets a new instance of other/attributeType that defines attributes not explicitly
     * included in {@link CoverageContentType}.
     *
     * @param newValue The new instance of other/attributeType.
     */
    public void setOtherProperty(final Record newValue) {
        checkWritePermission();
        otherProperty = newValue;
    }

    /**
     * Returns the maximum number of significant bits in the uncompressed representation
     * for the value in each band of each pixel.
     *
     * @return Maximum number of significant bits in the uncompressed representation
     *         for the value in each band of each pixel, or {@code null} if none.
     */
    @ValueRange(minimum = 1)
/// @XmlElement(name = "bitsPerValues")
    @UML(identifier="bitsPerValue", obligation=OPTIONAL, specification=ISO_19115)
    public Integer getBitsPerValue() {
        return bitsPerValue;
    }

    /**
     * Sets the maximum number of significant bits in the uncompressed representation
     * for the value in each band of each pixel.
     *
     * @param newValue The new maximum number of significant bits.
     */
    public void setBitsPerValue(final Integer newValue) {
        checkWritePermission();
        bitsPerValue = newValue;
    }
}
