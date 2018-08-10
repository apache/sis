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
package org.apache.sis.internal.util;

import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;


/**
 * A trivial implementation of {@link Citation} containing only a title.
 * Opportunistically implements also the identifier interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@SuppressWarnings("ReturnOfCollectionOrArrayField")
final strictfp class CitationMock implements Citation, Identifier {
    /**
     * The title to be returned by {@link #getTitle()}.
     */
    private final String title;

    /**
     * Alphanumeric value identifying an instance in the namespace.
     */
    private final String codeSpace, code;

    /**
     * The identifiers, built from the given code and code space by default.
     */
    List<Identifier> identifiers;

    /**
     * Creates a new object for the given title.
     *
     * @param  title      the title to be returned by {@link #getTitle()}.
     * @param  code       alphanumeric value identifying an instance in the namespace.
     * @param  codeSpace  space in which the code is valid.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public CitationMock(final String title, final String codeSpace, final String code) {
        this.title     = title;
        this.codeSpace = codeSpace;
        this.code      = code;
        identifiers    = (code != null) ? Collections.singletonList(this) : Collections.emptyList();
    }

    @Override public Citation                         getAuthority()               {return this;}
    @Override public InternationalString              getTitle()                   {return new SimpleInternationalString(title);}
    @Override public Collection<InternationalString>  getAlternateTitles()         {return Collections.emptyList();}
    @Override public Collection<CitationDate>         getDates()                   {return Collections.emptyList();}
    @Override public InternationalString              getEdition()                 {return null;}
    @Override public Date                             getEditionDate()             {return null;}
    @Override public Collection<Identifier>           getIdentifiers()             {return identifiers;}
    @Override public String                           getCode()                    {return code;}
    @Override public String                           getCodeSpace()               {return codeSpace;}
    @Override public String                           getVersion()                 {return null;}
    @Override public InternationalString              getDescription()             {return null;}
    @Override public Collection<Responsibility>       getCitedResponsibleParties() {return Collections.emptyList();}
    @Override public Collection<PresentationForm>     getPresentationForms()       {return Collections.emptyList();}
    @Override public Series                           getSeries()                  {return null;}
    @Override public Collection<InternationalString>  getOtherCitationDetails()    {return Collections.emptyList();}
    @Override public Collection<OnlineResource>       getOnlineResources()         {return Collections.emptyList();}
    @Override public Collection<BrowseGraphic>        getGraphics()                {return Collections.emptyList();}
    @Override public String                           getISBN()                    {return null;}
    @Override public String                           getISSN()                    {return null;}
    @Deprecated
    @Override public InternationalString              getCollectiveTitle()         {return null;}

    /**
     * Returns a string representation of this citation for debugging purpose.
     */
    @Override
    public String toString() {
        return "Citation[“" + title + "”]";
    }
}
