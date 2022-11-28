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
import javax.xml.bind.annotation.XmlRootElement;

// Branch-dependent imports
import org.opengis.annotation.UML;

import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Expected or tested uniformity of the results obtained for a data quality evaluation.
 * The following properties are mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_Homogeneity}
 * {@code   ├─result…………………………} Value obtained from applying a data quality measure.
 * {@code   └─derivedElement……} Derived element.</div>
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
@XmlType(name = "DQ_Homogeneity_Type")
@XmlRootElement(name = "DQ_Homogeneity")
@UML(identifier="DQ_Homogeneity", specification=UNSPECIFIED)
public class DefaultHomogeneity extends AbstractMetaquality {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8440822895642971849L;
    /**
     * Constructs an initially empty aggregation derivation.
     */
    public DefaultHomogeneity() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     */
    public DefaultHomogeneity(final DefaultHomogeneity object) {
        super(object);
    }
}
