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
package org.apache.sis.internal.gpx;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.opengis.metadata.citation.Address;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Telephone;
import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;

/**
 * Person object as defined in GPX.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
public class Person implements Party, Contact, Address, Responsibility {

    public String name;
    public String email;
    public URI link;

    @Override
    public Collection<Contact> getContactInfo() {
        return Collections.singleton(this);
    }

    @Override
    public Collection<Telephone> getPhones() {
        return Collections.emptyList();
    }

    @Override
    public Telephone getPhone() {
        return null;
    }

    @Override
    public Collection<Address> getAddresses() {
        return Collections.singleton(this);
    }

    @Override
    public Address getAddress() {
        return this;
    }

    @Override
    public Collection<OnlineResource> getOnlineResources() {
        if (link!=null) {
            return Collections.singleton(new DefaultOnlineResource(link));
        }
        return Collections.emptyList();
    }

    @Override
    public OnlineResource getOnlineResource() {
        if (link!=null) {
            return new DefaultOnlineResource(link);
        }
        return null;
    }

    @Override
    public Collection<InternationalString> getHoursOfService() {
        return Collections.emptyList();
    }

    @Override
    public InternationalString getContactInstructions() {
        return null;
    }

    @Override
    public InternationalString getContactType() {
        return null;
    }

    @Override
    public InternationalString getName() {
        if (name!=null) {
            return new SimpleInternationalString(name);
        }
        return null;
    }

    @Override
    public Collection<InternationalString> getDeliveryPoints() {
        return Collections.emptyList();
    }

    @Override
    public InternationalString getCity() {
        return null;
    }

    @Override
    public InternationalString getAdministrativeArea() {
        return null;
    }

    @Override
    public String getPostalCode() {
        return null;
    }

    @Override
    public InternationalString getCountry() {
        return null;
    }

    @Override
    public Collection<String> getElectronicMailAddresses() {
        if (email != null) {
            return Collections.singleton(email);
        }
        return Collections.emptyList();
    }

    /**
     * ISO 19115 metadata property fixed to {@link Role#ORIGINATOR}.
     *
     * @return function performed by the responsible party.
     */
    @Override
    public Role getRole() {
        return Role.ORIGINATOR;
    }

    @Override
    public Collection<Extent> getExtents() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Party> getParties() {
        return Collections.singleton(this);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Person(");
        sb.append(name).append(',').append(email).append(',').append(link);
        sb.append(')');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Person other = (Person) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.email, other.email)) {
            return false;
        }
        if (!Objects.equals(this.link, other.link)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 45;
    }

}
