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

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.content.RangeDimension;
import org.opengis.metadata.content.RangeElementDescription;
import org.opengis.util.RecordType;
import org.apache.sis.xml.Namespaces;


/**
 * Information about the content of a grid data cell.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_CoverageDescription_Type", propOrder = {
    "attributeDescription",
    "contentType",
    "dimensions",
    "rangeElementDescriptions"
})
@XmlRootElement(name = "MD_CoverageDescription")
@XmlSeeAlso({
    DefaultImageDescription.class,
    org.apache.sis.internal.jaxb.gmi.MI_CoverageDescription.class
})
public class DefaultCoverageDescription extends AbstractContentInformation implements CoverageDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5943716957630930520L;

    /**
     * Description of the attribute described by the measurement value.
     */
    private RecordType attributeDescription;

    /**
     * Type of information represented by the cell value.
     */
    private CoverageContentType contentType;

    /**
     * Information on the dimensions of the cell measurement value.
     */
    private Collection<RangeDimension> dimensions;

    /**
     * Provides the description of the specific range elements of a coverage.
     */
    private Collection<RangeElementDescription> rangeElementDescriptions;

    /**
     * Constructs an empty coverage description.
     */
    public DefaultCoverageDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(CoverageDescription)
     */
    public DefaultCoverageDescription(final CoverageDescription object) {
        super(object);
        attributeDescription     = object.getAttributeDescription();
        contentType              = object.getContentType();
        dimensions               = copyCollection(object.getDimensions(), RangeDimension.class);
        rangeElementDescriptions = copyCollection(object.getRangeElementDescriptions(), RangeElementDescription.class);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is is an instance of {@link ImageDescription}, then this
     *       method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultCoverageDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultCoverageDescription} instance is created using the
     *       {@linkplain #DefaultCoverageDescription(CoverageDescription) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCoverageDescription castOrCopy(final CoverageDescription object) {
        if (object instanceof ImageDescription) {
            return DefaultImageDescription.castOrCopy((ImageDescription) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof DefaultCoverageDescription) {
            return (DefaultCoverageDescription) object;
        }
        return new DefaultCoverageDescription(object);
    }

    /**
     * Returns the description of the attribute described by the measurement value.
     */
    @Override
    @XmlElement(name = "attributeDescription", required = true)
    public synchronized RecordType getAttributeDescription() {
        return attributeDescription;
    }

    /**
     * Sets the description of the attribute described by the measurement value.
     *
     * @param newValue The new attribute description.
     */
    public synchronized void setAttributeDescription(final RecordType newValue) {
        checkWritePermission();
        attributeDescription = newValue;
    }

    /**
     * Returns the type of information represented by the cell value.
     */
    @Override
    @XmlElement(name = "contentType", required = true)
    public synchronized CoverageContentType getContentType() {
        return contentType;
    }

    /**
     * Sets the type of information represented by the cell value.
     *
     * @param newValue The new content type.
     */
    public synchronized void setContentType(final CoverageContentType newValue) {
        checkWritePermission();
        contentType = newValue;
    }

    /**
     * Returns the information on the dimensions of the cell measurement value.
     */
    @Override
    @XmlElement(name = "dimension")
    public synchronized Collection<RangeDimension> getDimensions() {
        return dimensions = nonNullCollection(dimensions, RangeDimension.class);
    }

    /**
     * Sets the information on the dimensions of the cell measurement value.
     *
     * @param newValues The new dimensions.
     */
    public synchronized void setDimensions(final Collection<? extends RangeDimension> newValues) {
        dimensions = writeCollection(newValues, dimensions, RangeDimension.class);
    }

    /**
     * Provides the description of the specific range elements of a coverage.
     *
     * @return Description of the specific range elements of a coverage.
     */
    @Override
    @XmlElement(name = "rangeElementDescription", namespace = Namespaces.GMI)
    public synchronized Collection<RangeElementDescription> getRangeElementDescriptions() {
        return rangeElementDescriptions = nonNullCollection(rangeElementDescriptions, RangeElementDescription.class);
    }

    /**
     * Sets the description of the specific range elements of a coverage.
     *
     * @param newValues The new range element description.
     */
    public synchronized void setRangeElementDescriptions(final Collection<? extends RangeElementDescription> newValues) {
        rangeElementDescriptions = writeCollection(newValues, rangeElementDescriptions, RangeElementDescription.class);
    }
}
