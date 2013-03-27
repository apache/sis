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
package org.apache.sis.metadata.iso.constraint;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.metadata.constraint.LegalConstraints;


/**
 * Restrictions and legal prerequisites for accessing and using the resource.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_LegalConstraints_Type", propOrder = {
    "accessConstraints",
    "useConstraints",
    "otherConstraints"
})
@XmlRootElement(name = "MD_LegalConstraints")
public class DefaultLegalConstraints extends DefaultConstraints implements LegalConstraints {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2891061818279024901L;

    /**
     * Access constraints applied to assure the protection of privacy or intellectual property,
     * and any special restrictions or limitations on obtaining the resource.
     */
    private Collection<Restriction> accessConstraints;

    /**
     * Constraints applied to assure the protection of privacy or intellectual property,
     * and any special restrictions or limitations or warnings on using the resource.
     */
    private Collection<Restriction> useConstraints;

    /**
     * Other restrictions and legal prerequisites for accessing and using the resource.
     * Should be a non-empty value only if {@linkplain #getAccessConstraints() access constraints}
     * or {@linkplain #getUseConstraints() use constraints} declares
     * {@linkplain Restriction#OTHER_RESTRICTIONS other restrictions}.
     */
    private Collection<InternationalString> otherConstraints;

    /**
     * Constructs an initially empty constraints.
     */
    public DefaultLegalConstraints() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultLegalConstraints castOrCopy(final LegalConstraints object) {
        if (object == null || object instanceof DefaultLegalConstraints) {
            return (DefaultLegalConstraints) object;
        }
        final DefaultLegalConstraints copy = new DefaultLegalConstraints();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the access constraints applied to assure the protection of privacy or intellectual property,
     * and any special restrictions or limitations on obtaining the resource.
     */
    @Override
    @XmlElement(name = "accessConstraints")
    public synchronized Collection<Restriction> getAccessConstraints() {
        return accessConstraints = nonNullCollection(accessConstraints, Restriction.class);
    }

    /**
     * Sets the access constraints applied to assure the protection of privacy or intellectual property,
     * and any special restrictions or limitations on obtaining the resource.
     *
     * @param newValues The new access constraints.
     */
    public synchronized void setAccessConstraints(final Collection<? extends Restriction> newValues) {
        accessConstraints = writeCollection(newValues, accessConstraints, Restriction.class);
    }

    /**
     * Returns the constraints applied to assure the protection of privacy or intellectual property,
     * and any special restrictions or limitations or warnings on using the resource.
     */
    @Override
    @XmlElement(name = "useConstraints")
    public synchronized Collection<Restriction> getUseConstraints() {
        return useConstraints = nonNullCollection(useConstraints, Restriction.class);
    }

    /**
     * Sets the constraints applied to assure the protection of privacy or intellectual property,
     * and any special restrictions or limitations or warnings on using the resource.
     *
     * @param newValues The new use constraints.
     */
    public synchronized void setUseConstraints(final Collection<? extends Restriction> newValues) {
        useConstraints = writeCollection(newValues, useConstraints, Restriction.class);
    }

    /**
     * Returns the other restrictions and legal prerequisites for accessing and using the resource.
     * Should be a non-empty value only if {@linkplain #getAccessConstraints() access constraints}
     * or {@linkplain #getUseConstraints() use constraints} declares
     * {@linkplain Restriction#OTHER_RESTRICTIONS other restrictions}.
     */
    @Override
    @XmlElement(name = "otherConstraints")
    public synchronized Collection<InternationalString> getOtherConstraints() {
        return otherConstraints = nonNullCollection(otherConstraints, InternationalString.class);
    }

    /**
     * Sets the other restrictions and legal prerequisites for accessing and using the resource.
     *
     * @param newValues Other constraints.
     */
    public synchronized void setOtherConstraints(final Collection<? extends InternationalString> newValues) {
        otherConstraints = writeCollection(newValues, otherConstraints, InternationalString.class);
    }
}
