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
package org.apache.sis.util;

import java.io.Serializable;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.MissingResourceException;


/**
 * An international string backed by a {@link ResourceBundle}.
 * Resource bundles can be Java classes or {@linkplain Properties properties} files, one for each language.
 * The resource bundle is specified by {@link #getBundle(Locale)}, which must be defined by subclasses.
 * The subclass will typically invoke {@code ResourceBundle.getBundle(resources, locale)} where
 * <var>resources</var> is the fully qualified class name of the base resource bundle (without locale suffix).
 * The appropriate resource bundle is loaded at runtime for the client's language by looking for
 * a class or a properties file with the right suffix, for example {@code "_en"} for English or
 * {@code "_fr"} for French.
 * See the {@link ResourceBundle#getBundle(String, Locale, ClassLoader) ResourceBundle.getBundle(…)}
 * Javadoc for more information.
 *
 * <h2>Example</h2>
 * If a file named "{@code MyResources.properties}" exists in {@code org.mypackage}
 * and contains the following line:
 *
 * <pre class="text">MyKey = some value</pre>
 *
 * Then an international string for {@code "some value"} can be created using the following code:
 *
 * {@snippet lang="java" :
 *     InternationalString value = new ResourceInternationalString("MyKey") {
 *         @Override protected ResourceBundle getBundle(Locale locale) {
 *             return ResourceBundle.getBundle("org.mypackage.MyResources", locale);
 *         }
 *     };
 *     }
 *
 * The {@code "some value"} string will be localized if the required properties files exist, for
 * example "{@code MyResources_fr.properties}" for French or "{@code MyResources_it.properties}"
 * for Italian, <i>etc</i>.
 * If needed, users can gain more control by overriding the {@link #getBundle(Locale)} method.
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus inherently thread-safe if the bundles created by {@link #getBundle(Locale)}
 * is also immutable. Subclasses may or may not be immutable, at implementation choice. But implementers are
 * encouraged to make sure that subclasses remain immutable for more predictable behavior.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.4
 *
 * @see ResourceBundle#getBundle(String, Locale)
 *
 * @since 1.1
 */
public abstract class ResourceInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4354957955509756039L;

    /**
     * The key for the resource to fetch.
     * This value is given at construction time and cannot be {@code null}.
     */
    protected final String key;

    /**
     * Creates a new international string from the specified key.
     *
     * @param key  the key for the resource to fetch.
     */
    protected ResourceInternationalString(final String key) {
        ArgumentChecks.ensureNonNull("key", key);
        this.key = key;
    }

    /**
     * Returns the resource bundle for the given locale.
     * This implementation must be provided by subclasses because, in a modularized application,
     * the call to the {@link ResourceBundle#getBundle(String, Locale)} method is caller-sensitive.
     * This method is typically implemented as below:
     *
     * {@snippet lang="java" :
     *     @Override
     *     protected ResourceBundle getBundle(final Locale locale) {
     *         return ResourceBundle.getBundle(resources, locale, MyResource.class.getClassLoader());
     *     }
     * }
     *
     * @param  locale  the locale for which to get the resource bundle.
     * @return the resource bundle for the given locale.
     *
     * @see ResourceBundle#getBundle(String, Locale, ClassLoader)
     */
    protected abstract ResourceBundle getBundle(Locale locale);

    /**
     * Returns a string in the specified locale. If there is no string for the specified
     * {@code locale}, then this method searches for a string in another locale as
     * specified in the {@link ResourceBundle} class description.
     *
     * <h4>Handling of <code>null</code> argument value</h4>
     * In the default implementation, the {@code null} locale is handled as a synonymous of
     * {@code Locale.ROOT}. However, subclasses are free to use a different fallback. Client
     * code are encouraged to specify only non-null values for more determinist behavior.
     *
     * @param  locale  the desired locale for the string to be returned.
     * @return the string in the specified locale, or in a fallback locale.
     * @throws MissingResourceException if no resource can be found for the key specified
     *         at {@linkplain #ResourceInternationalString(String) construction time}.
     */
    @Override
    public String toString(Locale locale) throws MissingResourceException {
        if (locale == null) {
            locale = Locale.ROOT;               // For consistency with DefaultInternationalString.
        }
        return getBundle(locale).getString(key);
    }

    /**
     * Compares this international string with the specified object for equality.
     *
     * @param  object  the object to compare with this international string.
     * @return {@code true} if the given object is equal to this string.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            return key.equals(((ResourceInternationalString) object).key);
        }
        return false;
    }

    /**
     * Returns a hash code value for this international text.
     *
     * @return a hash code value for this international text.
     */
    @Override
    public int hashCode() {
        return key.hashCode() ^ (int) serialVersionUID;
    }
}
