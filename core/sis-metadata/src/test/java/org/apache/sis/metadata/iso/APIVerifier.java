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

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import org.opengis.annotation.UML;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Verifies the API changes caused by the ISO 19115:2003 to ISO 19115:2014 upgrade.
 * This class compares the presence of {@link Deprecated} and {@link UML} annotations
 * against a the content of an automatically generated {@code api-changes.properties} file.
 * The intend is to ensure that we did not forgot an annotation or put the wrong one.
 *
 * <p>The content of the {@code api-changes.properties} files is typically empty on Apache SIS
 * branches that use a snapshot version of GeoAPI, thus making this test a no-op. However the
 * {@code api-changes.properties} file content is non-empty on trunk if the GeoAPI release used
 * by the trunk is behind the snapshot developments.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class APIVerifier extends TestCase {
    /**
     * The method being verified.
     * This is used in case of errors for providing information about the problematic method.
     */
    private Method method;

    /**
     * Verifies the API changes for the ISO 19115 standard.
     *
     * @throws IOException If an error occurred while reading the {@code "api-changes.properties"} file.
     * @throws ClassNotFoundException If a class declared in {@code "api-changes.properties"} has not been found.
     * @throws NoSuchMethodException If a method declared in {@code "api-changes.properties"} has not been found.
     */
    @Test
    public void verifyISO1915() throws IOException, ClassNotFoundException, NoSuchMethodException {
        final Properties changes = new Properties();
        final InputStream in = APIVerifier.class.getResourceAsStream("api-changes.properties");
        assertNotNull("Missing test resource file.", in);
        changes.load(in);
        in.close();
        try {
            verifyAPI(MetadataStandard.ISO_19115, changes);
        } catch (AssertionError e) {
            out.println("Method " + method);
            throw e;
        }
    }

    /**
     * Implementation of {@link #verifyISO1915()}.
     *
     * @param standard The metadata standard.
     * @param changes The list of changes in the given metadata standard.
     */
    private void verifyAPI(final MetadataStandard standard, final Properties changes)
            throws ClassNotFoundException, NoSuchMethodException
    {
        final Set<Method> classChanges = new HashSet<Method>();
        for (final Map.Entry<Object,Object> entry : changes.entrySet()) {
            final Class<?> implementation = standard.getImplementation(Class.forName((String) entry.getKey()));
            for (final String change : (String[]) CharSequences.split((String) entry.getValue(), ' ')) {
                switch (change.charAt(0)) {
                    case '~': {
                        continue;
                    }
                    case '-': {
                        method = implementation.getMethod(change.substring(1));
                        assertTrue("Expected @Deprecated annotation", method.isAnnotationPresent(Deprecated.class));
                        assertFalse("Expected no @UML annotation", method.isAnnotationPresent(UML.class));
                        break;
                    }
                    case '+': {
                        final int s = change.indexOf(':');
                        assertTrue(change, s >= 0);
                        method = implementation.getMethod(change.substring(1, s));
                        assertFalse("Expected no @Deprecated annotation", method.isAnnotationPresent(Deprecated.class));
                        final UML uml = method.getAnnotation(UML.class);
                        assertNotNull("Expected @UML annotation.", uml);
                        assertEquals(change.substring(s+1), uml.identifier());
                        break;
                    }
                    default: {
                        fail("Unrecognized change: " + change);
                        break;
                    }
                }
                assertTrue(classChanges.add(method));
            }
            /*
             * At this point all added/removed methods have been verified.
             * Now verify that remaining methods have no UML or Deprecated annotation.
             */
            final Method[] methods = implementation.getDeclaredMethods();
            for (int i=0; i<methods.length; i++) {
                method = methods[i];
                if (!classChanges.remove(method) && Classes.isPossibleGetter(method)) {
                    assertFalse("Expected no @Deprecated annotation", method.isAnnotationPresent(Deprecated.class));
                    assertFalse("Expected no @UML annotation", method.isAnnotationPresent(UML.class));
                }
            }
            assertTrue(classChanges.isEmpty());
        }
    }
}
