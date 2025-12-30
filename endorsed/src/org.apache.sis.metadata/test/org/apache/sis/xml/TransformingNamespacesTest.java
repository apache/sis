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

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import javax.xml.namespace.NamespaceContext;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSetEquals;


/**
 * Tests {@link TransformingNamespaces}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class TransformingNamespacesTest extends TestCase implements NamespaceContext {
    /**
     * All prefixes declared in this test.
     */
    private static Collection<String> prefixes() {
        return List.of("mdq", "ns1", "ns2", "cit", "mdb", "gex");
    }

    /**
     * Creates a new test case.
     */
    public TransformingNamespacesTest() {
    }

    /**
     * Tests {@link TransformingNamespaces#getPrefixes(String)}.
     */
    @Test
    public void testGetPrefixes() {
        final NamespaceContext fns = TransformingNamespaces.asXML(this, TransformVersion.ISO19139);
        final Iterator<String> it = fns.getPrefixes(LegacyNamespaces.GMD);
        final Set<String> prefixes = new HashSet<>();
        while (it.hasNext()) {
            assertTrue(prefixes.add(it.next()));
        }
        assertSetEquals(prefixes(), prefixes);
    }

    /**
     * Dummy implementation of {@link NamespaceContext#getPrefixes(String)} for {@link #testGetPrefixes()} purpose.
     * This method will be invoked many times by the prefix iterator implemented by {@link TransformingNamespaces}.
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
                prefixes = List.of("mdq", "ns1", "ns2");
                break;
            }
            default: {
                final String p = getPrefix(namespaceURI);
                prefixes = (p != null) ? Set.of(p) : Set.of();
                break;
            }
        }
        return prefixes.iterator();
    }

    /**
     * Tests {@link TransformingNamespaces#getPrefix(String)}.
     */
    @Test
    public void testGetPrefix() {
        final NamespaceContext fns = TransformingNamespaces.asXML(this, TransformVersion.ISO19139);
        /*
         * Following tests are not really interesting since TransformingNamespaces,
         * after failing to find a mapping, just delegates to this.getPrefix(…).
         */
        assertEquals("cit", fns.getPrefix(Namespaces.CIT));
        assertEquals("mdb", fns.getPrefix(Namespaces.MDB));
        assertEquals("gex", fns.getPrefix(Namespaces.GEX));
        /*
         * This is the interesting test: TransformingNamespaces replaces the gmd namespace
         * by one of the namespace recognized by this NamespaceContext, then delegates.
         */
        assertNull(getPrefix(LegacyNamespaces.GMD));                // This test is useless if this is non-null.
        final String prefix = fns.getPrefix(LegacyNamespaces.GMD);
        assertTrue(prefixes().contains(prefix), prefix);
    }

    /**
     * Dummy implementation of {@link NamespaceContext#getPrefix(String)} for {@link #testGetPrefix()} purpose.
     * This method will be invoked many times by the prefix iterator implemented by {@link TransformingNamespaces}.
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
