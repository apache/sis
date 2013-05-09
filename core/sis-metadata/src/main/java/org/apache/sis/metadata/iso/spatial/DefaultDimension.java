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
import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.metadata.iso.ISOMetadata;
//import org.apache.sis.internal.jaxb.gco.GO_Measure;
import org.apache.sis.measure.ValueRange;


/**
 * Axis properties.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Dimension_Type", propOrder = {
    "dimensionName",
    "dimensionSize",
    "resolution"
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
     * Constructs an initially empty dimension.
     */
    public DefaultDimension() {
    }

    /**
     * Creates a dimension initialized to the given type and size.
     *
     * @param dimensionName The name of the axis, or {@code null} if none, or {@code null} if none.
     * @param dimensionSize The number of elements along the axis, or {@code null} if none.
     */
    public DefaultDimension(final DimensionNameType dimensionName, final int dimensionSize) {
        this.dimensionName = dimensionName;
        this.dimensionSize = dimensionSize;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Dimension)
     */
    public DefaultDimension(final Dimension object) {
        super(object);
        dimensionName = object.getDimensionName();
        dimensionSize = object.getDimensionSize();
        resolution    = object.getResolution();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     */
    @Override
    @ValueRange(minimum=0)
    @XmlElement(name = "dimensionSize", required = true)
    public Integer getDimensionSize() {
        return dimensionSize;
    }

    /**
     * Sets the number of elements along the axis.
     *
     * @param newValue The new dimension size.
     */
    public void setDimensionSize(final Integer newValue) {
        checkWritePermission();
        dimensionSize = newValue;
    }

    /**
     * Returns the degree of detail in the grid dataset.
     */
    @Override
    @ValueRange(minimum=0, isMinIncluded=false)
//  @XmlJavaTypeAdapter(GO_Measure.class) // TODO
    @XmlElement(name = "resolution")
    public Double getResolution() {
        return resolution;
    }

    /**
     * Sets the degree of detail in the grid dataset.
     *
     * @param newValue The new resolution.
     */
    public void setResolution(final Double newValue) {
        checkWritePermission();
        resolution = newValue;
    }
}
