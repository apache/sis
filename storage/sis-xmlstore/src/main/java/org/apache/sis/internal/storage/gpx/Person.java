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
package org.apache.sis.internal.storage.gpx;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Role;
import org.opengis.util.InternationalString;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.iso.Types;

// Branch-dependent imports
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;


/**
 * Information about a person or organization.
 * This element provides 3 optional properties:
 *
 * <ul>
 *   <li>The {@linkplain #name}.</li>
 *   <li>The person {@linkplain #email}.</li>
 *   <li>An URI to other information about the person or organization.</li>
 * </ul>
 *
 * Those properties can be read or modified directly. All methods defined in this class are bridges to
 * the ISO 19115 metadata model and can be ignored if the user only wants to manipulate the GPX model.
 *
 * <p>Note that {@link Party} is an abstract type in ISO 19115 model. We are supposed to implement a subtype
 * ({@link org.opengis.metadata.citation.Individual} or {@link org.opengis.metadata.citation.Organisation}).
 * However the GPX metadata does not specifies whether the "person" is actually an individual or an organization.
 * In this situation of doubt, we do not select a subtype for avoiding to provide a wrong information.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class Person implements Responsibility, Party, Contact, Address {
    /**
     * Name of person or organization.
     *
     * @see #getName()
     */
    @XmlElement(name = Tags.NAME)
    public String name;

    /**
     * {@code true} if this name is the creator, {@code false} if it is the author.
     */
    boolean isCreator;

    /**
     * Email address.
     *
     * @see #getElectronicMailAddresses()
     */
    @XmlElement(name = Tags.EMAIL)
    @XmlJavaTypeAdapter(Email.class)
    public String email;

    /**
     * Link to Web site or other external information about person.
     *
     * @see #getOnlineResources()
     */
    @XmlElement(name = Tags.LINK)
    public Link link;

    /**
     * Creates an initially empty instance.
     */
    public Person() {
    }

    /**
     * Creates an instance for the given creator.
     */
    Person(final String creator) {
        name = creator;
        isCreator = true;
    }

    /**
     * Returns the given ISO 19115 metadata as a {@code Person} instance.
     * This method copies the data only if needed.
     *
     * @param  r       the ISO 19115 metadata, or {@code null}.
     * @param  locale  the locale to use for localized strings.
     * @return the GPX metadata, or {@code null}.
     */
    public static Person castOrCopy(final Responsibility r, final Locale locale) {
        if (r == null || r instanceof Person) {
            return (Person) r;
        }
        final Role role = r.getRole();
        final boolean isCreator = Role.ORIGINATOR.equals(role);
        if (isCreator || Role.AUTHOR.equals(role)) {
            for (final Party p : r.getParties()) {
                final String name = Types.toString(p.getName(), locale);
                if (name != null) {
                    final Person pr = new Person();
                    pr.name = name;
                    pr.isCreator = isCreator;
                    return pr;
                }
            }
        }
        return null;
    }

    /**
     * ISO 19115 metadata property fixed to {@link Role#ORIGINATOR} or {@link Role#AUTHOR}.
     *
     * @return function performed by the responsible party.
     */
    @Override
    public Role getRole() {
        return isCreator ? Role.ORIGINATOR : Role.AUTHOR;
    }

    /**
     * ISO 19115 metadata property determined by the {@link #name}, {@link #email} and {@link #link} fields.
     * Invoking this method is one of the steps in the path from the {@code Responsibility} root to the
     * {@link #getName()}, {@link #getElectronicMailAddresses()} and {@link #getOnlineResources()} methods.
     *
     * @return information about the parties.
     *
     * @see #getName()
     * @see #getContactInfo()
     */
    @Override
    public Collection<? extends Party> getParties() {
        return thisOrEmpty(name != null || email != null || link != null);
    }


    /* ---------------------------------------------------------------------------------
     * Implementation of the Party object returned by Responsibility.getParties().
     * Contains information about 'name', 'email' and 'link'.
     * --------------------------------------------------------------------------------- */

    /**
     * ISO 19115 metadata property determined by the {@link #name} field.
     *
     * @return name of the party, or {@code null} if none.
     */
    @Override
    public InternationalString getName() {
        return (name != null) ? new SimpleInternationalString(name) : null;
    }

    /**
     * ISO 19115 metadata property determined by the {@link #email} and {@link #link} fields.
     * Invoking this method is one of the steps in the path from the {@code Responsibility} root
     * to the {@link #getElectronicMailAddresses()} and {@link #getOnlineResources()} methods.
     *
     * @return contact information for the party.
     *
     * @see #getAddresses()
     * @see #getOnlineResources()
     */
    @Override
    public Collection<? extends Contact> getContactInfo() {
        return thisOrEmpty(email != null || link != null);
    }


    /* ---------------------------------------------------------------------------------
     * Implementation of the Contact object returned by Party.getContactInfo().
     * Contains information about 'email' and 'link' only (not 'name').
     * --------------------------------------------------------------------------------- */

    /**
     * ISO 19115 metadata property determined by the {@link #email} field.
     * Invoking this method is one of the steps in the path from the {@code Responsibility} root
     * to the {@link #getElectronicMailAddresses()} method.
     *
     * @return physical and email addresses at which the organization or individual may be contacted.
     *
     * @see #getElectronicMailAddresses()
     */
    @Override
    public Collection<? extends Address> getAddresses() {
        return thisOrEmpty(email != null);
    }

    /**
     * ISO 19115 metadata property determined by the {@link #link} field.
     *
     * @return on-line information that can be used to contact the individual or organization.
     */
    @Override
    public Collection<OnlineResource> getOnlineResources() {
        return (link != null) ? Collections.singleton(link) : Collections.emptySet();
    }


    /* ---------------------------------------------------------------------------------
     * Implementation of the Address object returned by Contact.getAddresses().
     * Contains information about 'email' only (not 'name' or 'link').
     * --------------------------------------------------------------------------------- */

    /**
     * ISO 19115 metadata property determined by the {@link #email} field.
     *
     * @return address of the electronic mailbox of the responsible organization or individual.
     */
    @Override
    public Collection<String> getElectronicMailAddresses() {
        return (email != null) ? Collections.singleton(email) : Collections.emptySet();
    }

    /**
     * Returns this object as a singleton if the given condition is {@code true},
     * or an empty set if the given condition is {@code false}.
     */
    private Collection<Person> thisOrEmpty(final boolean condition) {
        return condition ? Collections.singleton(this) : Collections.emptySet();
    }

    /**
     * Compares this {@code Person} with the given object for equality.
     *
     * @param  obj  the object to compare with this {@code Person}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Person) {
            final Person that = (Person) obj;
            return Objects.equals(this.name,  that.name) &&
                   Objects.equals(this.email, that.email) &&
                   Objects.equals(this.link,  that.link);
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code Person}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, email, link);
    }

    /**
     * Returns a string representation of this person statement.
     * The statement is formatted in a way similar to the email address in client software applications.
     * Example:
     *
     * <blockquote>
     * John Smith {@literal <john.smith@somewhere.com>}
     * http://john.smith.com
     * </blockquote>
     *
     * @return a string representation of this person.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        if (email != null) {
            if (sb.length() != 0) sb.append(' ');
            sb.append('<').append(email).append('>');
        }
        if (link != null) {
            if (sb.length() != 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(link);
        }
        return sb.toString();
    }
}
