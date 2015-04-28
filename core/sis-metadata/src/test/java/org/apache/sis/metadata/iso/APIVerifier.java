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

import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.opengis.annotation.UML;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Verifies the API changes caused by the ISO 19115:2003 to ISO 19115:2014 upgrade.
 * This class compares the presence of {@link Deprecated} and {@link UML} annotations against the content of an
 * {@linkplain #listAPIChanges(File, File, Appendable) automatically generated} {@code api-changes.properties} file.
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
            // May still be non-empty if some methods were defined in parent classes.
            classChanges.clear();
        }
    }

    /**
     * Generates the content of the {@code api-changes.properties} file, except for the comments.
     * This method can be invoked by the {@code sis-metadata} module maintainer when the Apache SIS
     * API diverges from the GeoAPI interfaces.
     *
     * <p>This method also opportunistically lists method signature changes if some are found.
     * This is is for information purpose and shall not be included in the {@code api-changes.properties} file.</p>
     *
     * @param  releasedJAR Path to the JAR file of the GeoAPI interfaces implemented by the stable version of Apache SIS.
     * @param  snapshotJAR Path to the JAR file of the GeoAPI interfaces that we would implement if it was released.
     * @param  unitsJAR    Path to the JAR file containing the {@code Unit} class. This is a GeoAPI dependency.
     * @param  out Where to write the API differences between {@code releasedJAR} and {@code snapshotJAR}.
     * @throws Exception if an error occurred while processing the JAR file content.
     */
    public static void listAPIChanges(final File releasedJAR, final File snapshotJAR, final File unitsJAR,
            final Appendable out) throws Exception
    {
        final String lineSeparator = JDK7.lineSeparator();
        final Map<String,Boolean> methodChanges = new TreeMap<String,Boolean>();
        final List<String> incompatibleChanges = new ArrayList<String>();
        final ClassLoader parent = APIVerifier.class.getClassLoader().getParent();
        final URL dependency = unitsJAR.toURI().toURL();
        final JarFile newJARContent = new JarFile(snapshotJAR);
        final URLClassLoader oldAPI = new URLClassLoader(new URL[] {releasedJAR.toURI().toURL(), dependency}, parent);
        final URLClassLoader newAPI = new URLClassLoader(new URL[] {snapshotJAR.toURI().toURL(), dependency}, parent);
        try {
            final Class<? extends Annotation> newUML = Class.forName("org.opengis.annotation.UML", false, newAPI).asSubclass(Annotation.class);
            final Method newIdentifier = newUML.getMethod("identifier", (Class[]) null);
            final Enumeration<JarEntry> entries = newJARContent.entries();
            while (entries.hasMoreElements()) {
                String className = entries.nextElement().getName();
                if (className.endsWith(".class")) {
                    className = className.substring(0, className.length() - 6).replace('/', '.');
                    final Class<?> newClass = Class.forName(className, false, newAPI);
                    if (!newClass.isInterface() || !Modifier.isPublic(newClass.getModifiers())) {
                        continue;
                    }
                    final Class<?> oldClass;
                    try {
                        oldClass = Class.forName(className, false, oldAPI);
                    } catch (ClassNotFoundException e) {
                        // New class that did not existed in previous release. Ignore.
                        continue;
                    }
                    methodChanges.clear();
                    for (final Method newMethod : newClass.getDeclaredMethods()) {
                        if (!Modifier.isPublic(newMethod.getModifiers())) {
                            continue;
                        }
                        final String methodName = newMethod.getName();
                        final Class<?>[] parameterTypes = newMethod.getParameterTypes();
                        Method oldMethod;
                        try {
                            oldMethod = oldClass.getDeclaredMethod(methodName, parameterTypes);
                        } catch (NoSuchMethodException e) {
                            oldMethod = null;
                        }
                        if (oldMethod != null) {
                            final String oldType = oldMethod.getGenericReturnType().toString();
                            final String newType = newMethod.getGenericReturnType().toString();
                            if (!newType.equals(oldType)) {
                                incompatibleChanges.add(className + '.' + methodName + lineSeparator
                                        + "    (old) " + oldType + lineSeparator
                                        + "    (new) " + newType + lineSeparator);
                            }
                        }
                        if (parameterTypes.length == 0) {
                            if (newMethod.isAnnotationPresent(Deprecated.class)) {
                                methodChanges.put(methodName, Boolean.FALSE);
                            } else {
                                final Object uml = newMethod.getAnnotation(newUML);
                                if (uml != null && oldMethod == null) {
                                    methodChanges.put(methodName + ':' + newIdentifier.invoke(uml, (Object[]) null), Boolean.TRUE);
                                }
                            }
                        }
                    }
                    if (!methodChanges.isEmpty()) {
                        out.append(className);
                        char separator = '=';
                        for (final Map.Entry<String,Boolean> entry : methodChanges.entrySet()) {
                            out.append(separator).append(entry.getValue() ? '+' : '-').append(entry.getKey());
                            separator = ' ';
                        }
                        out.append(lineSeparator);
                    }
                }
            }
        } finally {
            newJARContent.close();
        }
        if (!incompatibleChanges.isEmpty()) {
            out.append(lineSeparator)
               .append("═════════════════════════════").append(lineSeparator)
               .append("Incompatible changes detected").append(lineSeparator)
               .append("═════════════════════════════").append(lineSeparator);
            for (final String m : incompatibleChanges) {
                out.append(m);
            }
        }
    }
}
