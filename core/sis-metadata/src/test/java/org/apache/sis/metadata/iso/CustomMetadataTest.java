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
package org.apache.sis.metadata.iso;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Locale;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.xml.XML;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.junit.Assert.*;


/**
 * Tests XML marshalling of custom implementation of metadata interfaces. The custom implementations
 * need to be converted to implementations from the {@link org.apache.sis.metadata.iso} package by
 * the JAXB converters.
 *
 * @author  Damiano Albani (for code snippet on the mailing list)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.14)
 * @version 0.5
 * @module
 */
public final strictfp class CustomMetadataTest extends XMLTestCase {
    /**
     * Tests the marshalling of a metadata implemented by {@link Proxy}.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testProxy() throws JAXBException {
        /*
         * A trivial metadata implementation which return the method name
         * for every attribute of type String.
         */
        final InvocationHandler handler = new InvocationHandler() {
            @Override public Object invoke(Object proxy, Method method, Object[] args) {
                if (method.getReturnType() == String.class) {
                    return method.getName();
                }
                return null;
            }
        };
        Metadata data = (Metadata) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { Metadata.class }, handler);
        /*
         * Wrap the metadata in a DefaultMetadata, and ensure
         * we can marshall it without an exception being throw.
         */
        data = new DefaultMetadata(data);
        final String xml = XML.marshal(data);
        /*
         * A few simple checks.
         */
        assertTrue(xml.contains("getMetadataStandardName"));
        assertTrue(xml.contains("getMetadataStandardVersion"));
    }

    /**
     * Tests that the attributes defined in subtypes are also marshalled.
     *
     * @throws JAXBException Should never happen.
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-108">GEOTK-108</a>
     */
    @Test
    public void testSubtypeAttributes() throws JAXBException {
        final DataIdentification identification = new DataIdentification() {
            @Override public InternationalString getAbstract() {
                Map<Locale, String> names = new HashMap<Locale, String>();
                names.put(Locale.ENGLISH, "Description");
                return DefaultFactories.SIS_NAMES.createInternationalString(names);
            }

            @Override public InternationalString getEnvironmentDescription() {
                Map<Locale, String> names = new HashMap<Locale, String>();
                names.put(Locale.ENGLISH, "Environment");
                return DefaultFactories.SIS_NAMES.createInternationalString(names);
            }

            @Override public InternationalString                   getSupplementalInformation()    {return null;}
            @Override public Citation                              getCitation()                   {return null;}
            @Override public InternationalString                   getPurpose()                    {return null;}
            @Override public Collection<SpatialRepresentationType> getSpatialRepresentationTypes() {return null;}
            @Override public Collection<Resolution>                getSpatialResolutions()         {return null;}
            @Override public Collection<Locale>                    getLanguages()                  {return null;}
            @Override public Collection<CharacterSet>              getCharacterSets()              {return null;}
            @Override public Collection<TopicCategory>             getTopicCategories()            {return null;}
            @Override public Collection<Extent>                    getExtents()                    {return null;}
            @Override public Collection<String>                    getCredits()                    {return null;}
            @Override public Collection<Progress>                  getStatus()                     {return null;}
            @Override public Collection<ResponsibleParty>          getPointOfContacts()            {return null;}
            @Override public Collection<MaintenanceInformation>    getResourceMaintenances()       {return null;}
            @Override public Collection<BrowseGraphic>             getGraphicOverviews()           {return null;}
            @Override public Collection<Format>                    getResourceFormats()            {return null;}
            @Override public Collection<Keywords>                  getDescriptiveKeywords()        {return null;}
            @Override public Collection<Usage>                     getResourceSpecificUsages()     {return null;}
            @Override public Collection<Constraints>               getResourceConstraints()        {return null;}
@Deprecated @Override public Collection<AggregateInformation>      getAggregationInfo()            {return null;}
        };
        final DefaultMetadata data = new DefaultMetadata();
        data.setIdentificationInfo(singleton(identification));
        final String xml = XML.marshal(data);
        /*
         * A few simple checks.
         */
        assertTrue("Missing Identification attribute.",     xml.contains("Description"));
        assertTrue("Missing DataIdentification attribute.", xml.contains("Environment"));
    }
}
