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
 *
 * This package contains documentation from OGC specifications.
 * Open Geospatial Consortium's work is fully acknowledged here.
 */
package org.apache.sis.metadata.iso.content;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.MemberName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.content.RangeDimension;
import org.opengis.metadata.content.Band;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information on the range of each dimension of a cell measurement value.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_RangeDimension_Type", propOrder = {
    "sequenceIdentifier",
    "descriptor"
})
@XmlRootElement(name = "MD_RangeDimension")
@XmlSeeAlso(DefaultBand.class)
public class DefaultRangeDimension extends ISOMetadata implements RangeDimension {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4365956866782010460L;

    /**
     * Number that uniquely identifies instances of bands of wavelengths on which a sensor operates.
     */
    private MemberName sequenceIdentifier;

    /**
     * Description of the range of a cell measurement value.
     */
    private InternationalString descriptor;

    /**
     * Constructs an initially empty range dimension.
     */
    public DefaultRangeDimension() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * <p>This method checks for the {@link Band} sub-interface. If that interface is found,
     * then this method delegates to the corresponding {@code castOrCopy} static method.</p>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultRangeDimension castOrCopy(final RangeDimension object) {
        if (object instanceof Band) {
            return DefaultBand.castOrCopy((Band) object);
        }
        if (object == null || object instanceof DefaultRangeDimension) {
            return (DefaultRangeDimension) object;
        }
        final DefaultRangeDimension copy = new DefaultRangeDimension();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the number that uniquely identifies instances of bands of wavelengths
     * on which a sensor operates.
     */
    @Override
    @XmlElement(name = "sequenceIdentifier")
    public synchronized MemberName getSequenceIdentifier() {
        return sequenceIdentifier;
    }

    /**
     * Sets the number that uniquely identifies instances of bands of wavelengths
     * on which a sensor operates.
     *
     * @param newValue The new sequence identifier.
     */
    public synchronized void setSequenceIdentifier(final MemberName newValue) {
        checkWritePermission();
        sequenceIdentifier = newValue;
    }

    /**
     * Returns the description of the range of a cell measurement value.
     */
    @Override
    @XmlElement(name = "descriptor")
    public synchronized InternationalString getDescriptor() {
        return descriptor;
    }

    /**
     * Sets the description of the range of a cell measurement value.
     *
     * @param newValue The new descriptor.
     */
    public synchronized void setDescriptor(final InternationalString newValue) {
        checkWritePermission();
        descriptor = newValue;
    }
}
