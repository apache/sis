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
package org.apache.sis.internal.jaxb.metadata.replace;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.simple.SimpleIdentifiedObject;
import org.apache.sis.util.ComparisonMode;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An implementation of {@link ReferenceSystem} marshalled as specified in ISO 19115.
 * This is different than the {@code ReferenceSystem} implementation provided in the
 * referencing module, since the later marshall the CRS as specified in GML (close
 * to ISO 19111 model).
 *
 * <p>Note that this implementation is very simple and serves no other purpose than being
 * a container for XML parsing or formatting. For real referencing service, consider using
 * {@link org.apache.sis.referencing.AbstractReferenceSystem} subclasses instead.</p>
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see org.apache.sis.referencing.AbstractReferenceSystem
 */
@XmlRootElement(name = "MD_ReferenceSystem")
public class ReferenceSystemMetadata extends SimpleIdentifiedObject implements ReferenceSystem {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2810145397032096087L;

    /**
     * Creates a reference system without identifier.
     * This constructor is mainly for JAXB.
     */
    public ReferenceSystemMetadata() {
    }

    /**
     * Creates a new reference system from the given one.
     *
     * @param crs The reference system to partially copy.
     */
    public ReferenceSystemMetadata(final ReferenceSystem crs) {
        super(crs);
    }

    /**
     * Creates a new reference system from the given identifier.
     *
     * @param name The primary name by which this object is identified.
     */
    public ReferenceSystemMetadata(final ReferenceIdentifier name) {
        super(name);
    }

    /**
     * Returns the primary name by which this object is identified.
     *
     * @return The identifier given at construction time.
     */
    @Override
    @XmlElement(name = "referenceSystemIdentifier")
    public final ReferenceIdentifier getName() {
        return super.getName();
    }

    /**
     * Sets the primary name by which this object is identified.
     *
     * @param name The new primary name.
     */
    public final void setName(final ReferenceIdentifier name) {
        this.name = name;
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param  object The object to compare with this reference system.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode) && (object instanceof ReferenceSystem)) {
            final ReferenceSystem that = (ReferenceSystem) object;
            if (mode.isIgnoringMetadata()) {
                // Compare the name because it was ignored by super.equals(…) in "ignore metadata" mode.
                return Objects.equals(getName(), that.getName());
            }
            return that.getDomainOfValidity() == null && that.getScope() == null;
        }
        return false;
    }
}
