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
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Debug;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.internal.util.MetadataServices;


/**
 * Base class for the {@code public static final Citation} constants defined in some SIS classes.
 * This base class contains only an abbreviation (e.g. {@code "OGC"} or {@code "EPSG"}) which can
 * be used as the primary key where to search for more information in a database. If no database
 * is available, then that simple primary key will be used as the citation title.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 *
 * @see IdentifierSpace
 * @see org.apache.sis.metadata.iso.citation.Citations
 */
public class CitationConstant extends SimpleCitation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7812463864874105717L;

    /**
     * Class of {@code public static final Citation} constants which are also used as namespace for identifiers.
     * The most typical example is the "EPSG" authority which manage the codes identifying Coordinate Reference
     * System (CRS) objects in the EPSG namespace.
     *
     * @param <T> The type of object used as identifier values.
     */
    public static class Authority<T> extends CitationConstant implements IdentifierSpace<T> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2067932813561130294L;

        /**
         * Creates a new citation for an authority managing codes in the given namespace.
         *
         * @param namespace The namespace of codes managed by this authority (e.g. "EPSG").
         */
        public Authority(final String namespace) {
            super(namespace);
        }

        /**
         * Returns the name space given at construction time. Can be one of the following:
         * <ul>
         *   <li>Abbreviation of the authority managing the codes (e.g. {@code "EPSG"} or {@code "ISBN"}).</li>
         *   <li>XML attribute name with its prefix (e.g. {@code "gml:id"}, {@code "gco:uuid"} or {@code "xlink:href"}).</li>
         * </ul>
         */
        @Override
        public String getName() {
            return title;
        }

        /**
         * Returns a string representation of this identifier space.
         */
        @Debug
        @Override
        public final String toString() {
            return "IdentifierSpace[" + title + ']';
        }
    }

    /**
     * The citation which contain the "real" data, or {@code null} if not yet created.
     */
    private transient volatile Citation delegate;

    /**
     * Creates a new proxy for the given primary key.
     * The key should be readable enough for being usable as a fallback if the database is not available.
     *
     * @param name A human-understandable primary key for fetching more information.
     */
    public CitationConstant(final String name) {
        super(name);
    }

    /**
     * Notify this instance that the database content may have changed, or that the classpath has changed.
     */
    public final void refresh() {
        delegate = null;
    }

    /**
     * Returns the citation instance which contain the actual data. That instance is provided by the
     * {@code sis-metadata} module, which is optional.  If that module is not on the classpath, then
     * this {@code delegate()} method will use the few information provided by the current instance.
     *
     * <p>Note that it should be very rare to not have {@code sis-metadata} on the classpath,
     * since that module is required by {@code sis-referencing} which is itself required by
     * almost all other SIS modules.</p>
     */
    @SuppressWarnings("DoubleCheckedLocking")
    private Citation delegate() {
        Citation c = delegate;
        if (c == null) {
            synchronized (this) {
                c = delegate;
                if (c == null) {
                    c = MetadataServices.getInstance().createCitation(title);
                    if (c == null) {
                        // 'sis-metadata' module not on the classpath (should be very rare)
                        // or no citation defined for the given primary key.
                        c = new SimpleCitation(title);
                    }
                    delegate = c;
                }
            }
        }
        return c;
    }

    /**
     * Redirects the call to the delegate citation (the instance which actually contain the data).
     *
     * @return The value returned by the delegate.
     */
    @Override public InternationalString                        getTitle()                   {return delegate().getTitle();}
    @Override public Collection<? extends InternationalString>  getAlternateTitles()         {return delegate().getAlternateTitles();}
    @Override public Collection<? extends CitationDate>         getDates()                   {return delegate().getDates();}
    @Override public InternationalString                        getEdition()                 {return delegate().getEdition();}
    @Override public Date                                       getEditionDate()             {return delegate().getEditionDate();}
    @Override public Collection<? extends Identifier>           getIdentifiers()             {return delegate().getIdentifiers();}
    @Override public Collection<? extends ResponsibleParty>     getCitedResponsibleParties() {return delegate().getCitedResponsibleParties();}
    @Override public Collection<PresentationForm>               getPresentationForms()       {return delegate().getPresentationForms();}
    @Override public Series                                     getSeries()                  {return delegate().getSeries();}
    @Override public InternationalString                        getOtherCitationDetails()    {return delegate().getOtherCitationDetails();}
    @Override public String                                     getISBN()                    {return delegate().getISBN();}
    @Override public String                                     getISSN()                    {return delegate().getISSN();}

    /**
     * Invoked at deserialization time in order to replace the deserialized instance by the existing
     * instance defined in the {@link org.apache.sis.metadata.iso.citation.Citations} class.
     *
     * @return The instance to use, as an unique instance if possible.
     */
    protected Object readResolve() {
        CitationConstant c = MetadataServices.getInstance().getCitationConstant(title);
        if (c == null) {
            /*
             * Should happen only if the sis-metadata module is not on the classpath (should be rare)
             * or if the Citation has been serialized on a more recent version of Apache SIS than the
             * current version.
             */
            c = this;
        }
        return c;
    }
}
