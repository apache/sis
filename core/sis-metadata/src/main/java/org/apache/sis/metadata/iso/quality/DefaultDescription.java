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
package org.apache.sis.metadata.iso.quality;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.identification.BrowseGraphic;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.Namespaces;


/**
 * Data quality measure description.
 * The following property is mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQM_Description}
 * {@code   └─textDescription……………} Text description.</div>
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
 * @author  Alexis Gaillard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
@XmlType(name = "DQM_Description_Type", namespace = Namespaces.DQM, propOrder = {
    "textDescription",
    "extendedDescription"
})
@XmlRootElement(name = "DQM_Description", namespace = Namespaces.DQM)
public class DefaultDescription extends ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4878784271547209576L;

    /**
     * Text description.
     */
    @SuppressWarnings("serial")
    private InternationalString textDescription;

    /**
     * Illustration.
     */
    @SuppressWarnings("serial")
    private BrowseGraphic extendedDescription;

    /**
     * Constructs an initially empty description.
     */
    public DefaultDescription() {
    }

    /**
     * Constructs a description initialized with the given text.
     *
     * @param  text  text description, or {@code null} if none.
     */
    public DefaultDescription(final CharSequence text) {
        textDescription = Types.toInternationalString(text);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     */
    public DefaultDescription(final DefaultDescription object) {
        super(object);
        if (object != null) {
            textDescription     = object.getTextDescription();
            extendedDescription = object.getExtendedDescription();
        }
    }

    /**
     * Returns the text description.
     *
     * @return text description.
     */
    @XmlElement(name = "textDescription", required = true)
    public InternationalString getTextDescription() {
        return textDescription;
    }

    /**
     * Sets the text description.
     *
     * @param  newValue  the new description text.
     */
    public void setTextDescription(final InternationalString newValue)  {
        checkWritePermission(textDescription);
        textDescription = newValue;
    }

    /**
     * Returns the illustration.
     *
     * @return description illustration, or {@code null} if none.
     */
    @XmlElement(name = "extendedDescription")
    public BrowseGraphic getExtendedDescription() {
        return extendedDescription;
    }

    /**
     * Sets the illustration.
     *
     * @param  newValue  the new description illustration.
     */
    public void setExtendedDescription(final BrowseGraphic newValue)  {
        checkWritePermission(extendedDescription);
        extendedDescription = newValue;
    }
}
