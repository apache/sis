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
package org.apache.sis.metadata.simple;

import java.util.Objects;
import java.io.Serializable;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.InternationalString;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.internal.shared.Strings;

// Specific to the main branch:
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;


/**
 * A trivial implementation of {@link Citation} containing only a title.
 *
 * <h2>Design note</h2>
 * We do not put more field than {@link #title} in this {@code SimpleCitation} in order to keep it simple,
 * because the title is the only "universal" property (the need for all other fields will be determined in
 * subclasses on a case-by-case basis) and because {@code SimpleCitation} are sometimes only proxy identified
 * by the {@link #title}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class SimpleCitation implements Citation, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4818846034764528263L;

    /**
     * The title to be returned by {@link #getTitle()}.
     */
    public final String title;

    /**
     * Creates a new object for the given title.
     *
     * @param  title  the title to be returned by {@link #getTitle()}.
     */
    public SimpleCitation(final String title) {
        this.title = title;
    }

    /**
     * Returns the title as an international string.
     *
     * @return the title given at construction time.
     */
    @Override
    public InternationalString getTitle() {
        return new SimpleInternationalString(title);
    }

    /**
     * Methods inherited from the {@link Citation} interface which are not of interest to this
     * {@code SimpleCitation} implementation.
     *
     * @return an empty list.
     */
    @Override public Collection<? extends InternationalString>  getAlternateTitles()         {return Collections.emptyList();}
    @Override public Collection<? extends CitationDate>         getDates()                   {return Collections.emptyList();}
    @Override public InternationalString                        getEdition()                 {return null;}
    @Override public Date                                       getEditionDate()             {return null;}
    @Override public Collection<? extends Identifier>           getIdentifiers()             {return Collections.emptyList();}
    @Override public Collection<? extends ResponsibleParty>     getCitedResponsibleParties() {return Collections.emptyList();}
    @Override public Collection<PresentationForm>               getPresentationForms()       {return Collections.emptyList();}
    @Override public Series                                     getSeries()                  {return null;}
    @Override public InternationalString                        getOtherCitationDetails()    {return null;}
    @Override public String                                     getISBN()                    {return null;}
    @Override public String                                     getISSN()                    {return null;}
    @Deprecated
    @Override public InternationalString                        getCollectiveTitle()         {return null;}

    /**
     * Compares the given object with this citation for equality.
     *
     * @param  obj  the object to compare with this citation.
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
     * Returns a string representation of this citation for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.bracket("Citation", title);
    }
}
