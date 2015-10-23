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
import org.apache.sis.util.ArgumentChecks;


/**
 * An international string backed by a {@link ResourceBundle}.
 * Resource bundles can be Java classes or {@linkplain Properties properties} files, one for each
 * language. The fully qualified class name of the base resource bundle (without locale suffix)
 * is specified at {@linkplain #ResourceInternationalString(String, String) construction time}.
 * The appropriate resource bundle is loaded at runtime for the client's language by looking for
 * a class or a properties file with the right suffix, for example {@code "_en"} for English or
 * {@code "_fr"} for French.
 * See the {@link ResourceBundle#getBundle(String, Locale, ClassLoader) ResourceBundle.getBundle(…)}
 * Javadoc for more information.
 *
 * <div class="note"><b>Example:</b>
 * if a file named "{@code MyResources.properties}" exists in {@code org.mypackage}
 * and contains the following line:
 *
 * {@preformat text
 *     MyKey = some value
 * }
 *
 * Then an international string for {@code "some value"} can be created using the following code:
 *
 * {@preformat java
 *     InternationalString value = new ResourceInternationalString("org.mypackage.MyResources", "MyKey");
 * }
 *
 * The {@code "some value"} string will be localized if the required properties files exist, for
 * example "{@code MyResources_fr.properties}" for French or "{@code MyResources_it.properties}"
 * for Italian, <i>etc</i>.
 * If needed, users can gain more control by overriding the {@link #getBundle(Locale)} method.
 * </div>
 *
 * <div class="section">Class loaders</div>
 * Developers can specify explicitely the {@link ClassLoader} to use be overriding the
 * {@link #getBundle(Locale)} method. This is recommended if the running environment
 * loads modules in isolated class loaders, as OSGi does for instance.
 *
 * <div class="note"><b>API note:</b>
 * We do not provide {@code ClassLoader} argument in the constructor of this class because class loaders
 * can often be hard-coded (thus avoiding the cost of an extra field) and are usually not serializable.</div>
 *
 * <div class="section">Apache SIS resources</div>
 * Apache SIS has its own resources mechanism, built on top of the standard {@code ResourceBundle}
 * with the addition of type safety and optional arguments to be formatted in the localized string.
 * Those resource bundles provide {@code formatInternational(int, …)} static methods for creating
 * international strings with the same functionality than this {@code ResourceInternationalString}.
 * See {@code org.apache.sis.util.resources} for more information.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe if the bundles created by {@link #getBundle(Locale)}
 * is also immutable. Subclasses may or may not be immutable, at implementation choice. But implementors are
 * encouraged to make sure that subclasses remain immutable for more predictable behavior.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see ResourceBundle#getBundle(String, Locale)
 */
public class ResourceInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8636012022904092254L;

    /**
     * The name of the resource bundle, as a fully qualified class name.
     * This value is given at construction time and can not be {@code null}.
     */
    protected final String resources;

    /**
     * The key for the resource to fetch.
     * This value is given at construction time and can not be {@code null}.
     */
    protected final String key;

    /**
     * Creates a new international string from the specified resource bundle and key.
     * The class loader will be the one of the {@link #toString(Locale)} caller,
     * unless the {@link #getBundle(Locale)} method is overridden.
     *
     * @param resources The name of the resource bundle, as a fully qualified class name.
     * @param key       The key for the resource to fetch.
     */
    public ResourceInternationalString(final String resources, final String key) {
        ArgumentChecks.ensureNonNull("resources", resources);
        ArgumentChecks.ensureNonNull("key",       key);
        this.resources = resources;
        this.key       = key;
    }

    /**
     * Returns the resource bundle for the given locale. The default implementation fetches the
     * bundle from the name given at {@linkplain #ResourceInternationalString construction time}.
     * Subclasses can override this method if they need to fetch the bundle in an other way.
     *
     * <div class="section">Class loaders</div>
     * By default, this method loads the resources using the caller's class loader.
     * Subclasses can override this method in order to specify a different class loader.
     * For example, the code below works well if {@code MyResource} is a class defined
     * in the same module than the one that contain the resources to load:
     *
     * {@preformat java
     *     &#64;Override
     *     protected ResourceBundle getBundle(final Locale locale) {
     *         return ResourceBundle.getBundle(resources, locale, MyResource.class.getClassLoader());
     *     }
     * }
     *
     * @param  locale The locale for which to get the resource bundle.
     * @return The resource bundle for the given locale.
     *
     * @see ResourceBundle#getBundle(String, Locale, ClassLoader)
     */
    protected ResourceBundle getBundle(final Locale locale) {
        return ResourceBundle.getBundle(resources, locale);
    }

    /**
     * Returns a string in the specified locale. If there is no string for the specified
     * {@code locale}, then this method searches for a string in an other locale as
     * specified in the {@link ResourceBundle} class description.
     *
     * <div class="section">Handling of <code>null</code> argument value</div>
     * In the default implementation, the {@code null} locale is handled as a synonymous of
     * {@code Locale.ROOT}. However subclasses are free to use a different fallback. Client
     * code are encouraged to specify only non-null values for more determinist behavior.
     *
     * @param  locale The desired locale for the string to be returned.
     * @return The string in the specified locale, or in a fallback locale.
     * @throws MissingResourceException is the key given to the constructor is invalid.
     */
    @Override
    public String toString(Locale locale) throws MissingResourceException {
        if (locale == null) {
            locale = Locale.ROOT; // For consistency with DefaultInternationalString.
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
            return key.equals(that.key) && resources.equals(that.resources);
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
