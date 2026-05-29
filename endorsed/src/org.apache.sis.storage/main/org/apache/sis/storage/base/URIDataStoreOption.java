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
package org.apache.sis.storage.base;

import java.net.URI;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Collection;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.OptionKey;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.IllegalOpenParameterException;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;

// Specific to the main branch:
import org.apache.sis.parameter.DefaultParameterDescriptor;


/**
 * Options supported by {@link URIDataStoreProvider}.
 * This enumeration does the link between {@link ParameterDescriptor} and {@link OptionKey}.
 * This enumeration contains all parameters used by all subclasses of {@link URIDataStore}
 * in the Apache <abbr>SIS</abbr> code base. This is an enumeration for a closed universe,
 * which is why it cannot be in exported <abbr>API</abbr>.
 * This enumeration is useful for data stores that are containers of other data stores,
 * such as the data store that read all files in a folder.
 *
 * <h4>Deferred construction of parameter descriptor</h4>
 * Some parameters are used by only one {@link URIDataStore}. These parameters are constructed
 * during the static initialization of the {@link URIDataStoreProvider} that use them.
 *
 * <h4>Future evolution</h4>
 * This class is an attempt to bring a little bit of order in the mapping between parameters and option keys,
 * at least for the most frequently used parameters. This class may change in any future <abbr>SIS</abbr>
 * version according experience.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum URIDataStoreOption {
    /**
     * Description of the mandatory {@value URIDataStoreProvider#LOCATION} parameter.
     * This is the only mandatory parameter in all {@link URIDataStoreProvider} implementations.
     *
     * @see URIDataStoreProvider#LOCATION
     * @see #createForLocationOnly(String)
     */
    LOCATION(URIDataStoreProvider.LOCATION, Resources.Keys.DataStoreLocation, URI.class),

    /**
     * Description of the optional {@value URIDataStoreProvider#FORMAT} parameter.
     *
     * @see URIDataStoreProvider#FORMAT
     */
    FORMAT(URIDataStoreProvider.FORMAT, Resources.Keys.DirectoryContentFormatName, String.class),

    /**
     * Description of the optional {@value URIDataStoreProvider#CREATE} parameter of writable data stores.
     *
     * @see URIDataStoreProvider#CREATE
     * @see OptionKey#OPEN_OPTIONS
     */
    CREATE(URIDataStoreProvider.CREATE, Resources.Keys.DataStoreCreate, Boolean.class),

    /**
     * Description of the optional parameter for character encoding used by the data store.
     * This is used only when the data store does not have explicit encoding information.
     *
     * @see OptionKey#ENCODING
     */
    ENCODING(OptionKey.ENCODING),

    /**
     * Description of the parameter for formatting conventions of dates and numbers.
     * This is used only when the data store does not have explicit locale information.
     *
     * @see OptionKey#LOCALE
     */
    LOCALE(OptionKey.LOCALE),

    /**
     * Description of the optional parameter for the time zone used by the data store.
     * This is used only when the data store does not have explicit time zone information.
     *
     * @see OptionKey#TIMEZONE
     */
    TIMEZONE(OptionKey.TIMEZONE),

    /**
     * Description of the optional parameter for the default coordinate reference system.
     * This is used only when the data store does not have explicit <abbr>CRS</abbr> information.
     *
     * @see OptionKey#DEFAULT_CRS
     * @todo Use {@link CodeType} when parsing from text.
     */
    DEFAULT_CRS(OptionKey.DEFAULT_CRS),

    /**
     * Description of the optional "metadata" parameter.
     *
     * @see OptionKey#METADATA_PATH
     */
    METADATA(OptionKey.METADATA_PATH),

    /**
     * Description of the optional parameter for specifying whether to assemble
     * distinct lines into a single {@code Feature} instance forming a foliation.
     */
    FOLIATION(OptionKey.FOLIATION_REPRESENTATION);

    /**
     * The option key for storing the parameter value in a storage connector,
     * or {@code null} if none.
     */
    private final OptionKey<?> option;

    /**
     * The parameter descriptor for this option, or {@code null} if not yet constructed.
     *
     * @see #getParameterDescriptor()
     */
    private volatile ParameterDescriptor<?> parameter;

    /**
     * Creates a new enumeration value with deferred construction of the parameter descriptor.
     *
     * @param option  associated option in {@link StorageConnector}.
     */
    private URIDataStoreOption(final OptionKey<?> option) {
        this.option = option;
    }

    /**
     * Creates a new enumeration value with immediate construction of the parameter descriptor.
     *
     * @param parameterName  name of the parameter to create.
     * @param description    constant from {@link Resources} keys for the localized description.
     * @param valueClass     type of value of the parameter.
     */
    private <V> URIDataStoreOption(final String parameterName, final short description, final Class<V> valueClass) {
        option = null;
        parameter = new ParameterBuilder()
                .addName(parameterName)
                .setDescription(Resources.formatInternational(description))
                .setRequired(ordinal() == 0)
                .create(valueClass, null);
    }

    /**
     * Returns the parameter associated to this enumeration.
     *
     * @return the parameter (never {@code null}).
     * @throws IllegalStateException if the parameter has not yet been initialized.
     */
    public final ParameterDescriptor<?> getParameterDescriptor() {
        ParameterDescriptor<?> p = parameter;
        if (p == null) {
            p = option.asOpenParameter().orElseThrow(() -> new IllegalStateException(name()));
        }
        return p;
    }

    /**
     * Returns the parameter name.
     */
    private String parameterName() {
        return getParameterDescriptor().getName().getCode();
    }

    /**
     * Creates a parameter with the same name as this parameter but different type or remarks.
     *
     * @param  builder     the builder to use for creating the parameter.
     * @param  valueClass  the new value class, or {@code null} for keeping the same class.
     * @param  remarks     the new parameter remarks, or {@code null} for keeping the same remarks.
     * @return parameter of the same name as {@link #parameter()} but the specified value class and remarks.
     */
    public final ParameterDescriptor<?> deriveParameterDescriptor(
            final ParameterBuilder builder, Class<?> valueClass, CharSequence remarks)
    {
        var p = (DefaultParameterDescriptor<?>) getParameterDescriptor();
        if (valueClass == null) valueClass = p.getValueClass();
        if (remarks    == null) remarks    = p.getRemarks();
        return builder.addName(p.getName())
                .setDescription(p.getDescription().orElse(null))
                .setRemarks(remarks)
                .create(valueClass, null);
    }

    /**
     * Creates a parameter descriptor group containing only the mandatory parameters.
     * The mandatory parameter is: {@link #LOCATION}.
     *
     * @param  name  short name of the data store format.
     * @return the descriptor for open parameters with only mandatory parameters.
     *
     * @see URIDataStoreProvider#createOpenParameters(ParameterBuilder)
     */
    public static ParameterDescriptorGroup createForLocationOnly(final String name) {
        return new ParameterBuilder().addName(name).createGroup(LOCATION.getParameterDescriptor());
    }

    /**
     * Returns a group of parameters set to the given mandatory values.
     * This is a shortcut when only the mandatory parameters are defined.
     * If the parameter descriptor group of the given provider contains parameters other than location,
     * those additional parameters are present but without values.
     *
     * @param  provider  provider of the data store for which to get the parameters, or {@code null}.
     * @param  location  value of {@link #LOCATION}, or {@code null} if none.
     * @return the parameters with the given values, or {@code null} if {@code provider} or {@code location} is null.
     */
    public static ParameterValueGroup createWithLocationOnly(final DataStoreProvider provider, final URI location) {
        if (provider != null && location != null) {
            final ParameterDescriptorGroup descriptor = provider.getOpenParameters();
            if (descriptor != null) {
                final ParameterValueGroup pg = descriptor.createValue();
                pg.parameter(URIDataStoreProvider.LOCATION).setValue(location);
                return pg;
            }
        }
        return null;
    }

    /**
     * Sets the value of the parameter described by this enumeration value.
     *
     * @param parameters  the parameters where to set the parameter.
     * @param value       the value to set, or {@code null} if none.
     */
    public final void setValueOf(final ParameterValueGroup parameters, final Object value) {
        if (value != null) {
            parameters.parameter(parameterName()).setValue(value);
        }
    }

    /**
     * Creates a storage connector initialized to the location declared in the given parameters.
     * The {@value #LOCATION} parameter is unconditionally requested. The other parameters to be
     * requested are specified in the {@code options} collection.
     * Missing optional parameters are ignored.
     *
     * @param  provider     the provider for which to create a storage connector (for error messages).
     * @param  parameters   the parameters to use for creating a storage connector.
     * @param  options      options to include, or {@code null} for only the mandatory parameters.
     * @param  openOptions  where to store open options, or {@code null} for storing them in the storage connector.
     * @return the storage connector initialized to the location specified in the parameters.
     * @throws IllegalOpenParameterException if no {@value URIDataStoreProvider#LOCATION} parameter has been found.
     */
    public static StorageConnector connector(final DataStoreProvider provider,
                                             final ParameterValueGroup parameters,
                                             Collection<URIDataStoreOption> options,
                                             EnumSet<StandardOpenOption> openOptions)
            throws IllegalOpenParameterException
    {
        final StorageConnector connector;
        try {
            connector = new StorageConnector(parameters.parameter(URIDataStoreProvider.LOCATION).getValue());
        } catch (ParameterNotFoundException | NullPointerException e) {
            throw new IllegalOpenParameterException(
                    Resources.format(Resources.Keys.UndefinedParameter_2,
                                     provider.getShortName(),
                                     URIDataStoreProvider.LOCATION), e);
        }
        if (options != null) {
            for (final URIDataStoreOption option : options) {
                if (option.option == null) continue;
                final Object value;
                try {
                    value = parameters.parameter(option.parameterName()).getValue();
                } catch (ParameterNotFoundException e) {
                    Logging.ignorableException(provider.getLogger(), URIDataStoreOption.class, "connector", e);
                    continue;
                }
                if (value != null) try {
                    switch (option) {
                        case CREATE: {
                            final boolean store = (openOptions == null);
                            if (store) {
                                openOptions = EnumSet.of(StandardOpenOption.WRITE);
                            } else {
                                openOptions.add(StandardOpenOption.WRITE);
                            }
                            if (ObjectConverters.convert(value, Boolean.class)) {
                                openOptions.add(StandardOpenOption.CREATE);
                            }
                            if (store) {
                                connector.setOption(OptionKey.OPEN_OPTIONS, openOptions.toArray(StandardOpenOption[]::new));
                            }
                            break;
                        }
                        default: {
                            convertAndSet(option.option, value, connector);
                            break;
                        }
                    }
                } catch (UnconvertibleObjectException e) {
                    throw new IllegalOpenParameterException(Errors.format(
                            Errors.Keys.IllegalOptionValue_2, option.parameterName(), value), e);
                }
            }
        }
        return connector;
    }

    /**
     * Set the given option to the given value in the given storage connector.
     * Defined as a separated method for type-safety.
     */
    private static <V> void convertAndSet(final OptionKey<V> option, final Object value, final StorageConnector connector) {
        connector.setOption(option, ObjectConverters.convert(value, option.getElementType()));
    }
}
