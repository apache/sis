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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.apache.sis.util.Numbers;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assume.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link OGCNamespacePrefixMapper}.
 * This class performs two kind of tests:
 *
 * <ul>
 *   <li>Invoke every public methods from the {@code NamespacePrefixMapper} class.
 *       If we failed to override an abstract method, we will get an {@link AbstractMethodError}.</li>
 *   <li>For specific methods like {@link OGCNamespacePrefixMapper#getPreferredPrefix(String, String,
 *       boolean)}, use some value which will prove us that the overriden method has been invoked.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class OGCNamespacePrefixMapperTest extends TestCase {
    /**
     * Ensures that the {@link OGCNamespacePrefixMapper} class overrides all abstract methods
     * defined in the JDK class. This test is ignored if the Java framework running this test
     * is not the Oracle one (i.e. if it does not bundle the Sun internal JAXB implementation).
     *
     * @throws Exception If an error occurred while invoking a method by
     *         the reflection API.
     */
    @Test
    public void testInternalJAXB() throws Exception {
        try {
            ensureOverrideMethods(new OGCNamespacePrefixMapper(null));
        } catch (NoClassDefFoundError e) {
            // Ignore the exception, since it may be normal.
        }
    }

    /**
     * Ensures that the {@link OGCNamespacePrefixMapper_Endorsed} class overrides all abstract
     * methods defined in the JAXB class. This test is ignored if the Java framework running
     * this test does not contains JAXB in its endorsed directory.
     *
     * @throws Exception If an error occurred while invoking a method by
     *         the reflection API.
     */
    @Test
    public void testEndorsedJAXB() throws Exception {
        try {
            ensureOverrideMethods(new OGCNamespacePrefixMapper_Endorsed(null));
        } catch (NoClassDefFoundError e) {
            // Ignore the exception, since it may be normal.
        }
    }

    /**
     * Ensures that the class of the given instance overrides all abstract methods defined by JAXB.
     * This test invokes every public methods declared in {@code NamespacePrefixMapper},
     * which will throw {@link AbstractMethodError} if we forgot to override an abstract
     * method. Additionally, this test checks the result of some method calls in order to
     * ensure that the invoked method was the one defined in {@link OGCNamespacePrefixMapper}.
     *
     * @param  The {@code OGCNamespacePrefixMapper} or {@code OGCNamespacePrefixMapper_Endorsed}
     *         instance to check.
     * @throws Exception If an error occurred while invoking a method by
     *         the reflection API.
     */
    private void ensureOverrideMethods(final Object mapper) throws Exception {
        String preferredPrefix = "getPreferredPrefix_method_has_not_been_found";
        final Method[] methods = mapper.getClass().getSuperclass().getDeclaredMethods();
        /*
         * The methods array is empty if the JVM has loaded the SIS placeholder instead than the
         * real JAXB class.  It should never happen during the Maven build because those classes
         * are deleted before the tests execution. However this may happen if the tests are run
         * from an IDE.
         */
        assumeTrue(methods.length != 0);
        for (final Method method : methods) {
            final int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                /*
                 * Prepare an array of parameter values with all references set to null
                 * and all primitive values set to 0 (for numbers) or false (for booleans).
                 */
                final Class<?>[] parameters = method.getParameterTypes();
                final Object[] values = new Object[parameters.length];
                for (int i=0; i<values.length; i++) {
                    values[i] = Numbers.valueOfNil(parameters[i]);
                }
                /*
                 * In the case of the getPreferredPrefix(…) method, set the namespace argument
                 * to "http://www.inspire.org". We have to set some values in order to avoid a
                 * NullPointerException. Since we are at it, we will opportunistically ensure
                 * that we get the expected "ins" prefix.
                 */
                final boolean isGetPreferredPrefix = method.getName().equals("getPreferredPrefix");
                if (isGetPreferredPrefix) {
                    values[0] = "http://www.inspire.org";
                    values[1] = "this_suggestion_should_not_be_retained";
                }
                final Object result = method.invoke(mapper, values);
                if (isGetPreferredPrefix) {
                    preferredPrefix = (String) result;
                }
            }
        }
        assertEquals("The getPreferredPrefix(…) method returned a wrong value. "
                + "Is this method correctly overriden?", "ins", preferredPrefix);
    }
}
