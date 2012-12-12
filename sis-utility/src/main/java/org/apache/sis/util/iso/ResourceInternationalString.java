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
package org.apache.sis.util.iso;

import java.io.Serializable;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import net.jcip.annotations.Immutable;
import org.opengis.util.InternationalString;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import java.util.Objects;


/**
 * An {@linkplain InternationalString international string} backed by a {@linkplain ResourceBundle
 * resource bundle}. A resource bundle can be a Java class or a {@linkplain Properties properties}
 * file, one for each language. The constructor expects the fully qualified class name of the base
 * resource bundle (the one used when no resource was found in the client's language). The appropriate
 * resource bundle is loaded at runtime for the client's language by looking for a class or a
 * properties file with the right suffix, for example {@code "_en"} for English or {@code "_fr"}
 * for French. This mechanism is explained in J2SE javadoc for the
 * {@link ResourceBundle#getBundle(String, Locale, ClassLoader) getBundle(…)} static method.
 *
 * {@section Example}
 * If a file named "{@code MyResources.properties}" exists in the package {@code org.mypackage}
 * and contains a line like "{@code MyKey = some value}", then an international string for
 * {@code "some value"} can be created using the following code:
 *
 * {@preformat java
 *     InternationalString value = new ResourceInternationalString("org.mypackage.MyResources", "MyKey");
 * }
 *
 * The {@code "some value"} string will be localized if the required properties files exist, for
 * example "{@code MyResources_fr.properties}" for French, "{@code MyResources_it.properties}"
 * for Italian, <i>etc</i>.
 * If needed, users can gain more control by overriding the {@link #getBundle(Locale)} method.
 *
 * {@section Apache SIS resources}
 * Apache SIS has its own resources mechanism, built on top of the standard {@code ResourceBundle}
 * with the addition of type safety and optional arguments to be formatted in the localized string.
 * Those resource bundles provide {@code formatInternational(int, …)} static methods for creating
 * international strings with the same functionality than this {@code ResourceInternationalString}.
 * See {@link org.apache.sis.util.resources} for more information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@Immutable
public class ResourceInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6339944890723487336L;

    /**
     * The name of the resource bundle from which to fetch the string.
     */
    private final String resources;

    /**
     * The key for the resource to fetch.
     */
    private final String key;

    /**
     * The class loader to use for loading the resources file, or {@code null} for the default
     * class loader.
     */
    private final transient ClassLoader loader;

    /**
     * Creates a new international string from the specified resource bundle, key and class loader.
     *
     * @param resources The name of the resource bundle, as a fully qualified class name.
     * @param key The key for the resource to fetch.
     * @param loader The class loader to use for loading the resources file,
     *        or {@code null} for the default class loader.
     */
    public ResourceInternationalString(final String resources, final String key, final ClassLoader loader) {
        this.resources = resources;
        this.key       = key;
        this.loader    = loader;
        ensureNonNull("resources", resources);
        ensureNonNull("key",       key);
    }

    /**
     * Returns the resource bundle for the given locale. The default implementation fetches the
     * bundle from the name given at {@linkplain #ResourceInternationalString(String,String)
     * construction time}. Subclasses can override this method if they need to fetch the
     * bundle in an other way.
     *
     * @param  locale The locale for which to get the resource bundle.
     * @return The resource bundle for the given locale.
     *
     * @see ResourceBundle#getBundle(String, Locale)
     */
    protected ResourceBundle getBundle(final Locale locale) {
        return (loader == null) ? ResourceBundle.getBundle(resources, locale) :
                ResourceBundle.getBundle(resources, locale, loader);
    }

    /**
     * Returns a string in the specified locale. If there is no string for the specified
     * {@code locale}, then this method search for a string in an other locale as
     * specified in the {@link ResourceBundle} class description.
     *
     * @param  locale The locale to look for, or {@code null} for an unlocalized version.
     * @return The string in the specified locale, or in a default locale.
     * @throws MissingResourceException is the key given to the constructor is invalid.
     */
    @Override
    public String toString(Locale locale) throws MissingResourceException {
        if (locale == null) {
            // The English locale (NOT the system default) is often used
            // as the real identifier in OGC IdentifiedObject naming. If
            // a user wants a string in the system default locale, he
            // should invokes the 'toString()' method instead.
            locale = Locale.ENGLISH;
        }
        return getBundle(locale).getString(key);
    }

    /**
     * Compares this international string with the specified object for equality.
     *
     * @param object The object to compare with this international string.
     * @return {@code true} if the given object is equal to this string.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final ResourceInternationalString that = (ResourceInternationalString) object;
            return Objects.equals(this.key,       that.key) &&
                   Objects.equals(this.resources, that.resources);
        }
        return false;
    }

    /**
     * Returns a hash code value for this international text.
     *
     * @return A hash code value for this international text.
     */
    @Override
    public int hashCode() {
        return key.hashCode() ^ resources.hashCode() ^ (int) serialVersionUID;
    }
}
