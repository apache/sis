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

import javax.measure.Unit;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.content.Band;
import org.opengis.metadata.content.SampleDimension;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.internal.jaxb.gco.GO_Real;
import org.apache.sis.internal.jaxb.gco.GO_Integer;
import org.apache.sis.internal.jaxb.gco.GO_Record;
import org.apache.sis.internal.jaxb.gco.GO_RecordType;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;


/**
 * The characteristic of each dimension (layer) included in the resource.
 * The following property is conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_SampleDimension}
 * {@code   └─units………………………} Units of data in each dimension included in the resource.</div>
 *
 * <h2>Terminology</h2>
 * <cite>Data values</cite> should be physical values expressed in the unit of measurement
 * given by {@link #getUnits()}. <cite>Cell values</cite> are values stored in the device,
 * before conversion to data values by application of {@linkplain #getScaleFactor() scale
 * factor} and {@linkplain #getOffset() offset}.
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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.5
 * @module
 */
@XmlType(name = "MD_SampleDimension_Type", propOrder = {
    "maxValue",
    "minValue",
    "units",

    // New in ISO 19115-3
    "scaleFactor",
    "offset",
    "meanValue",
    "numberOfValues",
    "standardDeviation",
    "otherPropertyType",
    "otherProperty",
    "bitsPerValue"
})
@XmlRootElement(name = "MD_SampleDimension")
@XmlSeeAlso({DefaultBand.class, DefaultRangeDimension.class})
public class DefaultSampleDimension extends DefaultRangeDimension implements SampleDimension {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4517148689016920767L;

    /**
     * Number of values used in a thematic classification resource.
     * This value should be expressed in the unit of measurement given by {@link #units}.
     */
    private Integer numberOfValues;

    /**
     * Minimum value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #units}.
     */
    private Double minValue;

    /**
     * Maximum value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #units}.
     */
    private Double maxValue;

    /**
     * Mean value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #units}.
     */
    private Double meanValue;

    /**
     * Standard deviation of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #units}.
     */
    private Double standardDeviation;

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
     * Type of transfer function to be used when scaling a physical value for a given element.
     */
    private TransferFunctionType transferFunctionType;

    /**
     * Maximum number of significant bits in the uncompressed representation
     * for the value in each band of each pixel.
     */
    private Integer bitsPerValue;

    /**
     * Smallest distance between which separate points can be distinguished, as specified in
     * instrument design.
     */
    private Double nominalSpatialResolution;

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
     * Constructs an initially empty sample dimension.
     */
    public DefaultSampleDimension() {
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
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(SampleDimension)
     */
    public DefaultSampleDimension(final SampleDimension object) {
        super(object);
        if (object != null) {
            minValue                 = object.getMinValue();
            maxValue                 = object.getMaxValue();
            meanValue                = object.getMeanValue();
            numberOfValues           = object.getNumberOfValues();
            standardDeviation        = object.getStandardDeviation();
            units                    = object.getUnits();
            scaleFactor              = object.getScaleFactor();
            offset                   = object.getOffset();
            transferFunctionType     = object.getTransferFunctionType();
            bitsPerValue             = object.getBitsPerValue();
            nominalSpatialResolution = object.getNominalSpatialResolution();
            otherPropertyType        = object.getOtherPropertyType();
            otherProperty            = object.getOtherProperty();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is is an instance of {@link Band}, then this
     *       method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultSampleDimension}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultSampleDimension} instance is created using the
     *       {@linkplain #DefaultSampleDimension(SampleDimension) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultSampleDimension castOrCopy(final SampleDimension object) {
        if (object instanceof Band) {
            return DefaultBand.castOrCopy((Band) object);
        }
        //-- Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof DefaultSampleDimension) {
            return (DefaultSampleDimension) object;
        }
        return new DefaultSampleDimension(object);
    }

    /**
     * Returns the number of values used in a thematic classification resource.
     *
     * @return the number of values used in a thematic classification resource, or {@code null} if none.
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "numberOfValues")
    @XmlJavaTypeAdapter(GO_Integer.Since2014.class)
    public Integer getNumberOfValues() {
        return numberOfValues;
    }

    /**
     * Sets the number of values used in a thematic classification resource.
     *
     * @param  newValue  the new number of values used in a thematic classification resource.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setNumberOfValues(final Integer newValue) {
        checkWritePermission(numberOfValues);
        if (ensurePositive(DefaultSampleDimension.class, "numberOfValues", false, newValue)) {
            numberOfValues = newValue;
        }
    }

    /**
     * Returns the minimum value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #getUnits()}.
     *
     * @return minimum value of data values in each dimension included in the resource, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "minValue")
    public Double getMinValue() {
        return minValue;
    }

    /**
     * Sets the minimum value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #getUnits()}.
     *
     * @param  newValue  the new new minimum value.
     */
    public void setMinValue(final Double newValue) {
        checkWritePermission(minValue);
        minValue = newValue;
    }

    /**
     * Returns the maximum value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #getUnits()}.
     *
     * @return maximum value of data values in each dimension included in the resource, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "maxValue")
    public Double getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the maximum value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #getUnits()}.
     *
     * @param  newValue  the new new maximum value.
     */
    public void setMaxValue(final Double newValue) {
        checkWritePermission(maxValue);
        maxValue = newValue;
    }

    /**
     * Returns the mean value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #getUnits()}.
     *
     * @return the mean value of data values in each dimension included in the resource, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "meanValue")
    @XmlJavaTypeAdapter(GO_Real.Since2014.class)
    public Double getMeanValue() {
        return meanValue;
    }

    /**
     * Sets the mean value of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #getUnits()}.
     *
     * @param  newValue  the new mean value of data values in each dimension included in the resource.
     */
    public void setMeanValue(final Double newValue) {
        checkWritePermission(meanValue);
        meanValue = newValue;
    }

    /**
     * Returns the standard deviation of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #getUnits()}.
     *
     * @return standard deviation of data values in each dimension included in the resource, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "standardDeviation")
    @XmlJavaTypeAdapter(GO_Real.Since2014.class)
    public Double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * Sets the standard deviation of data values in each dimension included in the resource.
     * This value should be expressed in the unit of measurement given by {@link #getUnits()}.
     *
     * @param  newValue  the new standard deviation of data values in each dimension included in the resource.
     */
    public void setStandardDeviation(final Double newValue) {
        checkWritePermission(standardDeviation);
        standardDeviation = newValue;
    }

    /**
     * Returns the units of data in the dimension.
     *
     * @return the units of data in the dimension, or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "units")
    public Unit<?> getUnits() {
        return units;
    }

    /**
     * Sets the units of data in the dimension.
     *
     * @param  newValue  the new units of data in the dimension.
     */
    public void setUnits(final Unit<?> newValue) {
        checkWritePermission(units);
        units = newValue;
    }

    /**
     * Returns the scale factor which has been applied to the cell value.
     *
     * @return scale factor which has been applied to the cell value, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "scaleFactor")
    public Double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Sets the scale factor which has been applied to the cell value.
     *
     * @param  newValue  the new scale factor which has been applied to the cell value.
     */
    public void setScaleFactor(final Double newValue) {
        checkWritePermission(scaleFactor);
        scaleFactor = newValue;
    }

    /**
     * Returns the physical value corresponding to a cell value of zero.
     *
     * @return the physical value corresponding to a cell value of zero, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "offset")
    public Double getOffset() {
        return offset;
    }

    /**
     * Sets the physical value corresponding to a cell value of zero.
     *
     * @param  newValue  the new physical value corresponding to a cell value of zero.
     */
    public void setOffset(final Double newValue) {
        checkWritePermission(offset);
        offset = newValue;
    }

    /**
     * Returns type of transfer function to be used when scaling a physical value for a given element.
     *
     * <div class="note"><b>Note on XML marshalling:</b>
     * ISO 19115-2 defines this property in {@linkplain DefaultBand a subtype} for historical reasons.
     * Apache SIS moves this property up in the hierarchy since this property can apply to any sample dimension,
     * not only the measurements in the electromagnetic spectrum. However this property will not appear in XML
     * documents unless this {@code SampleDimension} is actually a {@code Band}.</div>
     *
     * @return type of transfer function, or {@code null}.
     */
    @Override
    public TransferFunctionType getTransferFunctionType() {
        return transferFunctionType;
    }

    /**
     * Sets the type of transfer function to be used when scaling a physical value for a given element.
     *
     * @param  newValue  the new transfer function value.
     */
    public void setTransferFunctionType(final TransferFunctionType newValue) {
        checkWritePermission(transferFunctionType);
        transferFunctionType = newValue;
    }

    /**
     * Returns the maximum number of significant bits in the uncompressed representation
     * for the value in each band of each pixel.
     *
     * @return maximum number of significant bits in the uncompressed representation
     *         for the value in each band of each pixel, or {@code null} if none.
     */
    @Override
    @ValueRange(minimum = 1)
    @XmlElement(name = "bitsPerValue")
    public Integer getBitsPerValue() {
        return bitsPerValue;
    }

    /**
     * Sets the maximum number of significant bits in the uncompressed representation
     * for the value in each band of each pixel.
     *
     * @param  newValue  the new maximum number of significant bits.
     * @throws IllegalArgumentException if the given value is zero or negative.
     */
    public void setBitsPerValue(final Integer newValue) {
        checkWritePermission(bitsPerValue);
        if (ensurePositive(DefaultSampleDimension.class, "bitsPerValue", true, newValue)) {
            bitsPerValue = newValue;
        }
    }

    /**
     * Returns the smallest distance between which separate points can be distinguished,
     * as specified in instrument design.
     *
     * <div class="note"><b>Note on XML marshalling:</b>
     * ISO 19115-2 defines this property in {@linkplain DefaultBand a subtype} for historical reasons.
     * Apache SIS moves this property up in the hierarchy since this property can apply to any sample dimension,
     * not only the measurements in the electromagnetic spectrum. However this property will not appear in XML
     * documents unless this {@code SampleDimension} is actually a {@code Band}.</div>
     *
     * @return smallest distance between which separate points can be distinguished, or {@code null}.
     */
    @Override
    @ValueRange(minimum = 0, isMinIncluded = false)
    public Double getNominalSpatialResolution() {
        return nominalSpatialResolution;
    }

    /**
     * Sets the smallest distance between which separate points can be distinguished,
     * as specified in instrument design.
     *
     * @param  newValue  the new nominal spatial resolution.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setNominalSpatialResolution(final Double newValue) {
        checkWritePermission(nominalSpatialResolution);
        if (ensurePositive(DefaultSampleDimension.class, "nominalSpatialResolution", true, newValue)) {
            nominalSpatialResolution = newValue;
        }
    }

    /**
     * Returns type of other attribute description.
     *
     * @return type of other attribute description, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "otherPropertyType")
    @XmlJavaTypeAdapter(GO_RecordType.Since2014.class)
    public RecordType getOtherPropertyType() {
        return otherPropertyType;
    }

    /**
     * Sets a new type of other attribute description.
     *
     * @param  newValue  the new type of other attribute description.
     */
    public void setOtherPropertyType(final RecordType newValue) {
        checkWritePermission(otherPropertyType);
        otherPropertyType = newValue;
    }

    /**
     * Returns instance of other/attributeType that defines attributes not explicitly
     * included in {@link CoverageContentType}, or {@code null} if none.
     *
     * @return instance of other/attributeType that defines attributes, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "otherProperty")
    @XmlJavaTypeAdapter(GO_Record.Since2014.class)
    public Record getOtherProperty() {
        return otherProperty;
    }

    /**
     * Sets a new instance of other/attributeType that defines attributes not explicitly
     * included in {@link CoverageContentType}.
     *
     * @param  newValue  the new instance of other/attributeType.
     */
    public void setOtherProperty(final Record newValue) {
        checkWritePermission(otherProperty);
        otherProperty = newValue;
    }
}
