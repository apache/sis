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
package org.apache.sis.metadata;

import java.util.Arrays;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.Individual;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.acquisition.Instrument;
import org.opengis.metadata.acquisition.Platform;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.junit.Assert.*;


/**
 * Tests the {@link HashCode} class. This is also used as a relatively simple {@link MetadataVisitor} test.
 * The entry point is the {@link HashCode#walk(MetadataStandard, Class, Object, boolean)} method.
 *
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
public final strictfp class HashCodeTest extends TestCase {
    /**
     * Computes the hash code value of the given object.
     */
    private static Integer hash(final Object metadata) {
        return HashCode.getOrCreate().walk(MetadataStandard.ISO_19115, null, metadata, true);
    }

    /**
     * Tests hash code computation of an object that do not contain other metadata.
     */
    @Test
    public void testSimple() {
        final DefaultCitation instance = new DefaultCitation();
        final int baseCode = Citation.class.hashCode();
        assertEquals("Empty metadata.", Integer.valueOf(baseCode), hash(instance));

        final InternationalString title = new SimpleInternationalString("Some title");
        instance.setTitle(title);
        assertEquals("Metadata with a single value.", Integer.valueOf(baseCode + title.hashCode()), hash(instance));

        final InternationalString alternateTitle = new SimpleInternationalString("An other title");
        instance.setAlternateTitles(singleton(alternateTitle));
        assertEquals("Metadata with two values.",
                     Integer.valueOf(baseCode + title.hashCode() + Arrays.asList(alternateTitle).hashCode()),
                     hash(instance));
    }

    /**
     * Tests hash code computation of an object containing another metadata object.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testNested() {
        final InternationalString   title    = new SimpleInternationalString("Some title");
        final InternationalString   person   = new SimpleInternationalString("Illustre inconnu");
        final DefaultIndividual     party    = new DefaultIndividual(person, null, null);
        final DefaultResponsibility resp     = new DefaultResponsibility(Role.AUTHOR, null, party);
        final DefaultCitation       instance = new DefaultCitation(title);
        instance.getCitedResponsibleParties().add(resp);
        /*
         * Individual hash code is the sum of all its properties, none of them being a collection.
         */
        int expected = Individual.class.hashCode() + person.hashCode();
        assertEquals("Individual", Integer.valueOf(expected), hash(party));
        /*
         * The +31 below come from java.util.List contract, since above Individual is a list member.
         */
        expected += Responsibility.class.hashCode() + Role.AUTHOR.hashCode() + 31;
        assertEquals("Responsibility", Integer.valueOf(expected), hash(resp));
        /*
         * The +31 below come from java.util.List contract, since above Responsibility is a list member.
         */
        expected += Citation.class.hashCode() + title.hashCode() + 31;
        assertEquals("Citation", Integer.valueOf(expected), hash(instance));
    }

    /**
     * Tests hash code computation of an object graph containing a cycle.
     */
    @Test
    @DependsOnMethod("testNested")
    public void testCycle() {
        /*
         * We will create a Platform and an Instrument, both of them with no other property than an identifier.
         * The assertions verifying Identifier hash codes are not the main purpose of this test, but we perform
         * those verifications for making sure that the assertion done at the end of this method has good premises.
         */
        final DefaultIdentifier platformID   = new DefaultIdentifier("P1");
        final DefaultIdentifier instrumentID = new DefaultIdentifier("I1");
        int platformHash   = Identifier.class.hashCode() +   platformID.getCode().hashCode();
        int instrumentHash = Identifier.class.hashCode() + instrumentID.getCode().hashCode();
        assertEquals("platformID",   Integer.valueOf(platformHash),   hash(platformID));
        assertEquals("instrumentID", Integer.valueOf(instrumentHash), hash(instrumentID));
        /*
         * Verify Platform and Instrument hash codes before we link them together.
         */
        final DefaultPlatform   platform   = new DefaultPlatform();
        final DefaultInstrument instrument = new DefaultInstrument();
        platform  .setIdentifier(platformID);
        instrument.setIdentifier(instrumentID);
        platformHash   +=   Platform.class.hashCode();
        instrumentHash += Instrument.class.hashCode();
        assertEquals("Platform",   Integer.valueOf(platformHash),   hash(platform));
        assertEquals("Instrument", Integer.valueOf(instrumentHash), hash(instrument));
        /*
         * Add the instrument to the platform. The +31 below come from java.util.List contract,
         * since the Instrument is contained in a list.
         */
        platform.getInstruments().add(instrument);
        platformHash += instrumentHash + 31;
        assertEquals("Platform", Integer.valueOf(platformHash), hash(platform));
        /*
         * Add a reference from the instrument back to the platform. This is where the graph become cyclic.
         * The hash code computation is expected to behave as if the platform was not specified.
         */
        instrument.setMountedOn(platform);
        assertEquals("Platform", Integer.valueOf(platformHash), hash(platform));
    }
}
