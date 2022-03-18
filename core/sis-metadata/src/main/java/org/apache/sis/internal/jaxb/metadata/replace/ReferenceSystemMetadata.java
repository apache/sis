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

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.internal.simple.SimpleIdentifiedObject;
import org.apache.sis.internal.jaxb.FilterByVersion;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.xml.Namespaces;


/**
 * An implementation of {@link ReferenceSystem} marshalled as specified in ISO 19115.
 * This is different than the {@code ReferenceSystem} implementation provided in the
 * referencing module, since the latter marshals the CRS as specified in GML (close
 * to ISO 19111 model). This class contains only CRS identification as below:
 *
 * {@preformat text
 *   mrs:MD_ReferenceSystem
 *   ├─mrs:referenceSystemIdentifier  :  mcc:MD_Identifier
 *   └─mrs:referenceSystemType        :  mrs:MD_ReferenceSystemTypeCode
 * }
 *
 * The {@code referenceSystemType} attribute is currently missing.
 * See <a href="https://issues.apache.org/jira/browse/SIS-470">SIS-470</a>.
 *
 * <p>Note that this implementation is very simple and serves no other purpose than being
 * a container for XML parsing or formatting. For real referencing service, consider using
 * {@link org.apache.sis.referencing.AbstractReferenceSystem} subclasses instead.</p>
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 *
 * @see org.apache.sis.referencing.AbstractReferenceSystem
 * @see <a href="https://issues.apache.org/jira/browse/SIS-431">SIS-431</a>
 *
 * @since 0.3
 * @module
 */
@XmlType(name = "MD_ReferenceSystem_Type", namespace = Namespaces.MRS)
@XmlRootElement(name = "MD_ReferenceSystem", namespace = Namespaces.MRS)
public class ReferenceSystemMetadata extends SimpleIdentifiedObject implements ReferenceSystem {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2810145397032096087L;

    /**
     * {@code true} if marshalling ISO 19115:2003 model, or {@code false} if marshalling ISO 19115:2014 model.
     */
    private boolean isLegacyMetadata;

    /**
     * Creates a reference system without identifier.
     * This constructor is mainly for JAXB.
     */
    public ReferenceSystemMetadata() {
    }

    /**
     * Creates a new reference system from the given one.
     *
     * @param  crs  the reference system to partially copy.
     */
    public ReferenceSystemMetadata(final ReferenceSystem crs) {
        super(crs);
    }

    /**
     * Creates a new reference system from the given identifier.
     *
     * @param  name  the primary name by which this object is identified.
     */
    public ReferenceSystemMetadata(final Identifier name) {
        super(name);
    }

    /**
     * Invoked by JAXB {@link javax.xml.bind.Marshaller} before this object is marshalled to XML.
     */
    private void beforeMarshal(final Marshaller marshaller) {
        isLegacyMetadata = !FilterByVersion.CURRENT_METADATA.accept();
    }

    /**
     * Returns the primary name by which this object is identified.
     * This method can be invoked during ISO 19115-3 marshalling.
     *
     * @return the identifier given at construction time.
     */
    @Override
    @XmlElement(name = "referenceSystemIdentifier")
    public final Identifier getName() {
        Identifier name = super.getName();
        if (isLegacyMetadata) {
            name = RS_Identifier.wrap(name);
        }
        return name;
    }

    /**
     * Sets the primary name by which this object is identified.
     *
     * @param  name  the new primary name.
     */
    public final void setName(final Identifier name) {
        this.name = name;
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param  object  the object to compare with this reference system.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode) && (object instanceof ReferenceSystem)) {
            final ReferenceSystem that = (ReferenceSystem) object;
            if (mode.isIgnoringMetadata()) {
                // Compare the name because it was ignored by super.equals(…) in "ignore metadata" mode.
                return Utilities.deepEquals(getName(), that.getName(), mode);
            }
            return that.getDomainOfValidity() == null && that.getScope() == null;
        }
        return false;
    }
}
