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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.SecurityConstraints;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;


/**
 * Restrictions on the access and use of a resource or metadata.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Constraints_Type")
@XmlRootElement(name = "MD_Constraints")
@XmlSeeAlso({
    DefaultLegalConstraints.class,
    DefaultSecurityConstraints.class
})
public class DefaultConstraints extends ISOMetadata implements Constraints {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1771854790746022204L;

    /**
     * Limitation affecting the fitness for use of the resource.
     * Example: "<cite>not to be used for navigation</cite>".
     */
    private Collection<InternationalString> useLimitations;

    /**
     * Constructs an initially empty constraints.
     */
    public DefaultConstraints() {
    }

    /**
     * Constructs a new constraints with the given {@linkplain #getUseLimitations() use limitation}.
     *
     * @param useLimitation The use limitation, or {@code null} if none.
     */
    public DefaultConstraints(final CharSequence useLimitation) {
        useLimitations = singleton(Types.toInternationalString(useLimitation), InternationalString.class);
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * <p>This method checks for the {@link LegalConstraints} and {@link SecurityConstraints}
     * sub-interfaces. If one of those interfaces is found, then this method delegates to the
     * corresponding {@code castOrCopy} static method. If the given object implements more
     * than one of the above-cited interfaces, then the {@code castOrCopy} method to be used
     * is unspecified.</p>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultConstraints castOrCopy(final Constraints object) {
        if (object instanceof LegalConstraints) {
            return DefaultLegalConstraints.castOrCopy((LegalConstraints) object);
        }
        if (object instanceof SecurityConstraints) {
            return DefaultSecurityConstraints.castOrCopy((SecurityConstraints) object);
        }
        if (object == null || object instanceof DefaultConstraints) {
            return (DefaultConstraints) object;
        }
        final DefaultConstraints copy = new DefaultConstraints();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the limitation affecting the fitness for use of the resource.
     * Example: "<cite>not to be used for navigation</cite>".
     */
    @Override
    @XmlElement(name = "useLimitation")
    public synchronized Collection<InternationalString> getUseLimitations() {
        return useLimitations = nonNullCollection(useLimitations, InternationalString.class);
    }

    /**
     * Sets the limitation affecting the fitness for use of the resource.
     * Example: "<cite>not to be used for navigation</cite>".
     *
     * @param newValues The new use limitations.
     */
    public synchronized void setUseLimitations(final Collection<? extends InternationalString> newValues) {
        useLimitations = writeCollection(newValues, useLimitations, InternationalString.class);
    }
}
