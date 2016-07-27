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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.Releasability;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.metadata.maintenance.Scope;
import org.opengis.util.InternationalString;

/**
 * Copyright object as defined in GPX.
 * 
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
public class CopyRight implements LegalConstraints, Responsibility, Party, Citation, CitationDate {

    public String author;
    public Integer year;
    public URI license;

    @Override
    public Collection<Restriction> getAccessConstraints() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<Restriction> getUseConstraints() {
        if (license!=null) {
            return Arrays.asList(Restriction.LICENCE,Restriction.COPYRIGHT);
        } else {
            return Collections.singleton(Restriction.COPYRIGHT);
        }
    }

    @Override
    public Collection<? extends InternationalString> getOtherConstraints() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends InternationalString> getUseLimitations() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Scope getConstraintApplicationScope() {
        return null;
    }

    @Override
    public Collection<? extends BrowseGraphic> getGraphics() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Citation> getReferences() {
        return Collections.singleton(this);
    }

    @Override
    public Releasability getReleasability() {
        return null;
    }

    @Override
    public Collection<? extends Responsibility> getResponsibleParties() {
        return Collections.singleton(this);
    }

    @Override
    public InternationalString getTitle() {
        return null;
    }

    @Override
    public Collection<? extends InternationalString> getAlternateTitles() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends CitationDate> getDates() {
        return Collections.singleton(this);
    }

    @Override
    public InternationalString getEdition() {
        return null;
    }

    @Override
    public Date getEditionDate() {
        return null;
    }

    @Override
    public Collection<? extends Identifier> getIdentifiers() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Responsibility> getCitedResponsibleParties() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<PresentationForm> getPresentationForms() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Series getSeries() {
        return null;
    }

    @Override
    public Collection<? extends InternationalString> getOtherCitationDetails() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public InternationalString getCollectiveTitle() {
        return null;
    }

    @Override
    public String getISBN() {
        return null;
    }

    @Override
    public String getISSN() {
        return null;
    }

    @Override
    public Collection<? extends OnlineResource> getOnlineResources() {
        if (license != null) {
            return Collections.singleton(new DefaultOnlineResource(license));
        }
        return null;
    }

    @Override
    public Date getDate() {
        if (year != null) {
            return new Date(year, 0, 0);
        }
        return null;
    }

    @Override
    public DateType getDateType() {
        return DateType.IN_FORCE;
    }

    @Override
    public Role getRole() {
        return Role.OWNER;
    }

    @Override
    public Collection<? extends Extent> getExtents() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<? extends Party> getParties() {
        return Collections.singleton(this);
    }

    @Override
    public InternationalString getName() {
        if (author != null){
            return new SimpleInternationalString(author);
        }
        return null;
    }

    @Override
    public Collection<? extends Contact> getContactInfo() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CopyRight(");
        sb.append(author).append(',').append(year).append(',').append(license);
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
        final CopyRight other = (CopyRight) obj;
        if (!Objects.equals(this.author, other.author)) {
            return false;
        }
        if (!Objects.equals(this.year, other.year)) {
            return false;
        }
        if (!Objects.equals(this.license, other.license)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 44;
    }
    
}
