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
package org.apache.sis.internal.simple;

import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;

// Related to JDK7
import java.util.Objects;


/**
 * A trivial implementation of {@link Citation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.19)
 * @version 0.3
 * @module
 */
public class SimpleCitation implements Citation, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1490465918703910949L;

    /**
     * The title to be returned by {@link #getTitle()}.
     */
    protected final String title;

    /**
     * Creates a new object for the given name.
     *
     * @param title The title to be returned by {@link #getTitle()}.
     */
    public SimpleCitation(final String title) {
        this.title = title;
    }

    /**
     * Returns the title as an international string.
     */
    @Override
    public InternationalString getTitle() {
        return new SimpleInternationalString(title);
    }

    /**
     * Methods inherited from the {@link Citation} interface which are
     * not of interest to this {@code SimpleCitation} implementation.
     * Those methods will be removed in the JDK8 branch.
     */
    @Override public Collection<InternationalString>  getAlternateTitles()         {return Collections.emptyList();}
    @Override public Collection<CitationDate>         getDates()                   {return Collections.emptyList();}
    @Override public InternationalString              getEdition()                 {return null;}
    @Override public Date                             getEditionDate()             {return null;}
    @Override public Collection<? extends Identifier> getIdentifiers()             {return Collections.emptyList();}
    @Override public Collection<ResponsibleParty>     getCitedResponsibleParties() {return Collections.emptyList();}
    @Override public Collection<PresentationForm>     getPresentationForms()       {return Collections.emptyList();}
    @Override public Series                           getSeries()                  {return null;}
    @Override public InternationalString              getOtherCitationDetails()    {return null;}
    @Override public InternationalString              getCollectiveTitle()         {return null;}
    @Override public String                           getISBN()                    {return null;}
    @Override public String                           getISSN()                    {return null;}

    /**
     * Compares the given object with this citation for equality.
     *
     * @param  obj The object to compare with this citation.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            return Objects.equals(title, ((SimpleCitation) obj).title);
        }
        return false;
    }

    /**
     * Returns a hash code value for this citation.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(title) ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this citation.
     */
    @Override
    public String toString() {
        return "Citation[\"" + title + "\"]";
    }
}
