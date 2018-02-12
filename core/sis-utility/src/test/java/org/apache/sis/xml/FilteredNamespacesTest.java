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
package org.apache.sis.xml;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import javax.xml.namespace.NamespaceContext;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link FilteredNamespaces}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class FilteredNamespacesTest extends TestCase implements NamespaceContext {
    /**
     * All prefixes declared in this test.
     */
    private static Collection<String> prefixes() {
        return Arrays.asList("mdq", "ns1", "ns2", "cit", "mdb", "gex");
    }

    /**
     * Tests {@link FilteredNamespaces#getPrefixes(String)}.
     */
    @Test
    public void testGetPrefixes() {
        final FilteredNamespaces fns = new FilteredNamespaces(this, FilterVersion.ISO19139);
        final Iterator<String> it = fns.getPrefixes(LegacyNamespaces.GMD);
        final Set<String> prefixes = new HashSet<>();
        while (it.hasNext()) {
            assertTrue(prefixes.add(it.next()));
        }
        assertSetEquals(prefixes(), prefixes);
    }

    /**
     * Dummy implementation of {@link NamespaceContext#getPrefixes(String)} for {@link #testGetPrefixes()} purpose.
     * This method will be invoked many times by the prefix iterator implemented by {@link FilteredNamespaces}.
     * This method implementation uses a hard-coded list of arbitrary prefixes.
     *
     * @param  namespaceURI  the namespace for which to get one or more prefixes.
     * @return prefixes for the given namespace.
     */
    @Override
    public Iterator<String> getPrefixes(final String namespaceURI) {
        final Collection<String> prefixes;
        switch (namespaceURI) {
            case Namespaces.MDQ: {
                // Arbitrarily return more than one prefix for that namespace.
                prefixes = Arrays.asList("mdq", "ns1", "ns2");
                break;
            }
            default: {
                final String p = getPrefix(namespaceURI);
                prefixes = (p != null) ? Collections.singleton(p) : Collections.emptySet();
                break;
            }
        }
        return prefixes.iterator();
    }

    /**
     * Tests {@link FilteredNamespaces#getPrefix(String)}.
     */
    @Test
    public void testGetPrefix() {
        final FilteredNamespaces fns = new FilteredNamespaces(this, FilterVersion.ISO19139);
        /*
         * Following tests are not really interesting since FilteredNamespaces,
         * after failing to find a mapping, just delegates to this.getPrefix(…).
         */
        assertEquals("cit", fns.getPrefix(Namespaces.CIT));
        assertEquals("mdb", fns.getPrefix(Namespaces.MDB));
        assertEquals("gex", fns.getPrefix(Namespaces.GEX));
        /*
         * This is the interesting test: FilteredNamespaces replaces the gmd namespace
         * by one of the namespace recognized by this NamespaceContext, then delegates.
         */
        assertNull(getPrefix(LegacyNamespaces.GMD));                // This test is useless if this is non-null.
        final String prefix = fns.getPrefix(LegacyNamespaces.GMD);
        assertTrue(prefix, prefixes().contains(prefix));
    }

    /**
     * Dummy implementation of {@link NamespaceContext#getPrefix(String)} for {@link #testGetPrefix()} purpose.
     * This method will be invoked many times by the prefix iterator implemented by {@link FilteredNamespaces}.
     * This method implementation uses a hard-coded list of arbitrary prefixes.
     *
     * @param  namespaceURI  the namespace for which to get an arbitrary prefixes.
     * @return a prefix for the given namespace.
     */
    @Override
    public String getPrefix(final String namespaceURI) {
        switch (namespaceURI) {
            case Namespaces.CIT: return "cit";
            case Namespaces.MDB: return "mdb";
            case Namespaces.GEX: return "gex";
            default: return null;
        }
    }

    /**
     * Not needed for this test.
     *
     * @param  prefix  ignored.
     * @return never return.
     */
    @Override
    public String getNamespaceURI(String prefix) {
        throw new UnsupportedOperationException();
    }
}
