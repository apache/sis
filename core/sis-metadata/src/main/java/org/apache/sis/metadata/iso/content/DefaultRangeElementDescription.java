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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.Record;
import org.opengis.util.InternationalString;
import org.opengis.metadata.content.RangeElementDescription;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.Namespaces;


/**
 * Description of specific range elements.
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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "MI_RangeElementDescription_Type", propOrder = {
    "name",
    "definition"/*,
    "rangeElements"*/ // TODO: not yet supported.
})
@XmlRootElement(name = "MI_RangeElementDescription", namespace = Namespaces.GMI)
public class DefaultRangeElementDescription extends ISOMetadata implements RangeElementDescription {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8891149098619355114L;

    /**
     * Designation associated with a set of range elements.
     */
    private InternationalString name;

    /**
     * Description of a set of specific range elements.
     */
    private InternationalString definition;

    /**
     * Specific range elements, i.e. range elements associated with a name and their definition.
     */
    private Collection<Record> rangeElements;

    /**
     * Constructs an initially empty range element description.
     */
    public DefaultRangeElementDescription() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(RangeElementDescription)
     */
    public DefaultRangeElementDescription(final RangeElementDescription object) {
        super(object);
        if (object != null) {
            name          = object.getName();
            definition    = object.getDefinition();
            rangeElements = copyCollection(object.getRangeElements(), Record.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultRangeElementDescription}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRangeElementDescription} instance is created using the
     *       {@linkplain #DefaultRangeElementDescription(RangeElementDescription) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultRangeElementDescription castOrCopy(final RangeElementDescription object) {
        if (object == null || object instanceof DefaultRangeElementDescription) {
            return (DefaultRangeElementDescription) object;
        }
        return new DefaultRangeElementDescription(object);
    }

    /**
     * Returns the designation associated with a set of range elements.
     *
     * @return Designation associated with a set of range elements, or {@code null}.
     */
    @Override
    @XmlElement(name = "name", namespace = Namespaces.GMI, required = true)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the designation associated with a set of range elements.
     *
     * @param newValue The new name value.
     */
    public void setName(final InternationalString newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns the description of a set of specific range elements.
     *
     * @return Description of a set of specific range elements, or {@code null}.
     */
    @Override
    @XmlElement(name = "definition", namespace = Namespaces.GMI, required = true)
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Sets the description of a set of specific range elements.
     *
     * @param newValue The new definition value.
     */
    public void setDefinition(final InternationalString newValue) {
        checkWritePermission();
        definition = newValue;
    }

    /**
     * Returns the specific range elements, i.e. range elements associated with a name
     * and their definition.
     *
     * @return Specific range elements.
     *
     * @todo implements {@link Record} in order to use the annotation.
     */
    @Override
    //@XmlElement(name = "rangeElement", namespace = Namespaces.GMI, required = true)
    public Collection<Record> getRangeElements() {
        return rangeElements = nonNullCollection(rangeElements, Record.class);
    }

    /**
     * Sets the specific range elements, i.e. range elements associated with a name and
     * their definition.
     *
     * @param newValues The new range element values.
     */
    public void setRangeElements(final Collection<? extends Record> newValues) {
        rangeElements = writeCollection(newValues, rangeElements, Record.class);
    }
}
