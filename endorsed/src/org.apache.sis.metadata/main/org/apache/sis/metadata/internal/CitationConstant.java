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
package org.apache.sis.metadata.internal;

import java.util.Collection;
import java.util.logging.Logger;
import java.io.ObjectStreamException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.Series;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.simple.SimpleCitation;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.logging.Logging;

// Specific to the main and geoapi-3.1 branches:
import java.util.Date;
import org.opengis.metadata.citation.ResponsibleParty;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.identification.BrowseGraphic;


/**
 * Base class for the {@code public static final Citation} constants defined in some SIS classes.
 * This base class contains only an abbreviation (e.g. {@code "OGC"} or {@code "EPSG"}) which can
 * be used as the primary key where to search for more information in a database. If no database
 * is available, then that simple primary key will be used as the citation title.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see IdentifierSpace
 * @see Citations
 */
public class CitationConstant extends SimpleCitation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8429121584437634107L;

    /**
     * Where to log warnings when searching for an entry in the database.
     */
    private static final Logger LOGGER = Logger.getLogger(Loggers.SQL);

    /**
     * Class of {@code public static final Citation} constants which are also used as namespace for identifiers.
     * The most typical example is the "EPSG" authority which manage the codes identifying Coordinate Reference
     * System (CRS) objects in the EPSG namespace.
     *
     * @param <T>  the type of object used as identifier values.
     */
    public static class Authority<T> extends CitationConstant implements IdentifierSpace<T> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 2067932813561130294L;

        /**
         * Creates a new citation for an authority managing codes in the given namespace.
         * This constructor assumes that the namespace is the same as the abbreviation given as citation title.
         *
         * @param  namespace  the namespace of codes managed by this authority (e.g. "EPSG").
         */
        public Authority(final String namespace) {
            super(namespace);
        }

        /**
         * Creates a new citation for an authority managing codes in the given namespace.
         *
         * @param  name       a human-understandable primary key for fetching more information.
         * @param  namespace  the namespace of codes managed by this authority (e.g. "EPSG").
         */
        public Authority(final String name, final String namespace) {
            super(name, namespace);
        }

        /**
         * Returns the name space given at construction time. Can be one of the following:
         * <ul>
         *   <li>Abbreviation of the authority managing the codes (e.g. {@code "EPSG"} or {@code "ISBN"}).</li>
         *   <li>XML attribute name with its prefix (e.g. {@code "gml:id"}, {@code "gco:uuid"} or {@code "xlink:href"}).</li>
         * </ul>
         */
        @Override
        public final String getName() {
            return namespace;
        }

        /**
         * Returns a string representation of this identifier space.
         */
        @Override
        public final String toString() {
            return Strings.bracket(IdentifierSpace.class, title);
        }
    }

    /**
     * The name by which this citation is known to {@link Citations#fromName(String)}.
     * Often the same as the abbreviation that {@link CitationConstant} uses as the title.
     * If this {@code CitationConstant} is a {@link Authority} subtype, then this is also
     * the authority namespace.
     */
    public final String namespace;

    /**
     * The citation which contain the "real" data, or {@code null} if not yet created.
     * This is usually an instance created by {@link MetadataSource}. Those instances
     * manage their own caching, so accesses to the database should not occur often.
     */
    private transient volatile Citation delegate;

    /**
     * Creates a new proxy for the given primary key.
     * The key should be readable enough for being usable as a fallback if the database is not available.
     *
     * @param  name  a human-understandable primary key for fetching more information.
     */
    public CitationConstant(final String name) {
        super(name);
        this.namespace = name;
    }

    /**
     * Creates a new proxy for the given primary key but a different programmatic name.
     * The key should be readable enough for being usable as a fallback if the database is not available.
     *
     * @param  name       a human-understandable primary key for fetching more information.
     * @param  namespace  the name by which this citation is known to {@link Citations#fromName(String)}.
     */
    public CitationConstant(final String name, final String namespace) {
        super(name);
        this.namespace = namespace;
    }

    /**
     * Notify this instance that the database content may have changed, or that the module path has changed.
     */
    public final void refresh() {
        delegate = null;
    }

    /**
     * Returns the citation instance which contain the actual data. That instance is provided by the
     * {@code org.apache.sis.metadata} module, which is optional. If that module is not on the module path,
     * then this {@code delegate()} method will use the few information provided by the current instance.
     *
     * <p>Note that it should be very rare to not have {@code org.apache.sis.metadata} on the module path,
     * since that module is required by {@code org.apache.sis.referencing} which is itself required by
     * almost all other SIS modules.</p>
     */
    @SuppressWarnings("DoubleCheckedLocking")
    private Citation delegate() {
        Citation c = delegate;
        if (c == null) {
            synchronized (this) {
                c = delegate;
                if (c == null) {
                    try {
                        c = MetadataSource.getProvided().lookup(Citation.class, title);
                    } catch (MetadataStoreException e) {
                        /*
                         * If no database was available, MetadataSource.getProvided() was supposed to fallback on
                         * the MetadataFallback class. So if we get this exception, a more serious error occurred.
                         * It is still not fatal however, since most of Citation content is informative.
                         */
                        Logging.unexpectedException(LOGGER, CitationConstant.class, "delegate", e);
                        c = new SimpleCitation(title);
                    }
                    delegate = c;
                }
            }
        }
        return c;
    }

    /**
     * Returns the title, which is mandatory.
     */
    @Override
    public InternationalString getTitle() {
        InternationalString title = delegate().getTitle();
        return (title != null) ? title : super.getTitle();
    }

    /**
     * Redirects the call to the delegate citation (the instance which actually contain the data).
     *
     * @return the value returned by the delegate.
     */
    @Override public Collection<? extends InternationalString>  getAlternateTitles()         {return delegate().getAlternateTitles();}
    @Override public Collection<? extends CitationDate>         getDates()                   {return delegate().getDates();}
    @Override public InternationalString                        getEdition()                 {return delegate().getEdition();}
    @Override public Date                                       getEditionDate()             {return delegate().getEditionDate();}
    @Override public Collection<? extends Identifier>           getIdentifiers()             {return delegate().getIdentifiers();}
    @Override public Collection<? extends ResponsibleParty>     getCitedResponsibleParties() {return delegate().getCitedResponsibleParties();}
    @Override public Collection<PresentationForm>               getPresentationForms()       {return delegate().getPresentationForms();}
    @Override public Series                                     getSeries()                  {return delegate().getSeries();}
    @Override public InternationalString                        getOtherCitationDetails()    {return delegate().getOtherCitationDetails();}
    @Override public Collection<? extends OnlineResource>       getOnlineResources()         {return delegate().getOnlineResources();}
    @Override public Collection<? extends BrowseGraphic>        getGraphics()                {return delegate().getGraphics();}
    @Override public String                                     getISBN()                    {return delegate().getISBN();}
    @Override public String                                     getISSN()                    {return delegate().getISSN();}

    /**
     * Invoked at deserialization time in order to replace the deserialized instance by the existing
     * instance defined in the {@link org.apache.sis.metadata.iso.citation.Citations} class.
     *
     * @return the instance to use, as an unique instance if possible.
     * @throws ObjectStreamException never thrown.
     */
    protected Object readResolve() throws ObjectStreamException {
        final Citation c = Citations.fromName(title);
        return (c instanceof CitationConstant) ? c : this;
        /*
         * Returns `this` should happen only if the Citation has been serialized
         * on a more recent version of Apache SIS than the current version.
         */
    }
}
