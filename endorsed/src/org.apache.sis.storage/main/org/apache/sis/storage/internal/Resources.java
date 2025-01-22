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
package org.apache.sis.storage.internal;

import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;
import org.apache.sis.util.resources.ResourceInternationalString;


/**
 * Warning and error messages that are specific to the {@code org.apache.sis.storage} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@code org.apache.sis.util.resources} package.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     */
    public static final class Keys extends KeyConstants {
        /**
         * The unique instance of key constants handler.
         */
        static final Keys INSTANCE = new Keys();

        /**
         * For {@link #INSTANCE} creation only.
         */
        private Keys() {
        }

        /**
         * Name “{3}” is ambiguous because it can be understood as either “{1}” or “{2}” in the context
         * of “{0}” data.
         */
        public static final short AmbiguousName_4 = 15;

        /**
         * Auxiliary file “{0}” seems too large.
         */
        public static final short AuxiliaryFileTooLarge_1 = 71;

        /**
         * Cannot create resources based on the content of “{0}” directory.
         */
        public static final short CanNotCreateFolderStore_1 = 43;

        /**
         * Cannot infer the feature type resulting from “{0}” filtering.
         */
        public static final short CanNotDeriveTypeFromFeature_1 = 55;

        /**
         * Cannot get metadata common to “{0}” files. The reason is: {1}
         */
        public static final short CanNotGetCommonMetadata_2 = 39;

        /**
         * Cannot intersect “{0}” data with specified query.
         */
        public static final short CanNotIntersectDataWithQuery_1 = 57;

        /**
         * Cannot read the “{0}” auxiliary file.
         */
        public static final short CanNotReadAuxiliaryFile_1 = 66;

        /**
         * Cannot read the Coordinate Reference System (CRS) Well Known Text (WKT) in “{0}”.
         */
        public static final short CanNotReadCRS_WKT_1 = 37;

        /**
         * Cannot read “{0}” directory.
         */
        public static final short CanNotReadDirectory_1 = 34;

        /**
         * Cannot read “{1}” as a file in the {0} format.
         */
        public static final short CanNotReadFile_2 = 1;

        /**
         * Cannot read line {2} of “{1}” as part of a file in the {0} format.
         */
        public static final short CanNotReadFile_3 = 2;

        /**
         * Cannot read after column {3} of line {2} of “{1}” as part of a file in the {0} format.
         */
        public static final short CanNotReadFile_4 = 3;

        /**
         * Cannot read pixel at ({0}, {1}) indices in the “{2}” file.
         */
        public static final short CanNotReadPixel_3 = 68;

        /**
         * Cannot read slice or tile “{0}”.
         */
        public static final short CanNotReadSlice_1 = 78;

        /**
         * Cannot remove resource “{1}” from aggregate “{0}”.
         */
        public static final short CanNotRemoveResource_2 = 49;

        /**
         * Cannot render an image for the “{0}” coverage.
         */
        public static final short CanNotRenderImage_1 = 61;

        /**
         * Cannot select a slice.
         */
        public static final short CanNotSelectSlice = 53;

        /**
         * Cannot save resources of type ‘{1}’ in a “{0}” store.
         */
        public static final short CanNotStoreResourceType_2 = 41;

        /**
         * Cannot write the “{0}” resource.
         */
        public static final short CanNotWriteResource_1 = 69;

        /**
         * This {0} reader is closed.
         */
        public static final short ClosedReader_1 = 4;

        /**
         * This storage connector is closed.
         */
        public static final short ClosedStorageConnector = 56;

        /**
         * This {0} writer is closed.
         */
        public static final short ClosedWriter_1 = 5;

        /**
         * One or more read operations are in progress in the “{0}” data store.
         */
        public static final short ConcurrentRead_1 = 19;

        /**
         * A write operation is in progress in the “{0}” data store.
         */
        public static final short ConcurrentWrite_1 = 20;

        /**
         * Whether to allow new data store creation if the source to open does not already exist.
         */
        public static final short DataStoreCreate = 51;

        /**
         * Character encoding used by the data store.
         */
        public static final short DataStoreEncoding = 29;

        /**
         * Formatting conventions of dates and numbers.
         */
        public static final short DataStoreLocale = 30;

        /**
         * Data store location as a file or URL.
         */
        public static final short DataStoreLocation = 31;

        /**
         * Timezone of dates in the data store.
         */
        public static final short DataStoreTimeZone = 32;

        /**
         * Name of the format to use for reading or writing the directory content.
         */
        public static final short DirectoryContentFormatName = 40;

        /**
         * Content of “{0}” directory.
         */
        public static final short DirectoryContent_1 = 35;

        /**
         * Query property “{0}” is duplicated at indices {1} and {2}.
         */
        public static final short DuplicatedQueryProperty_3 = 54;

        /**
         * Exception occurred in a listener for events of type ‘{0}’.
         */
        public static final short ExceptionInListener_1 = 74;

        /**
         * Header in the “{0}” file is too large.
         */
        public static final short ExcessiveHeaderSize_1 = 67;

        /**
         * Character string in the “{0}” file is too long. The string has {2} characters while the
         * limit is {1}.
         */
        public static final short ExcessiveStringSize_3 = 6;

        /**
         * A feature named “{1}” is already present in the “{0}” data store.
         */
        public static final short FeatureAlreadyPresent_2 = 16;

        /**
         * Feature “{1}” has not been found in the “{0}” data store.
         */
        public static final short FeatureNotFound_2 = 17;

        /**
         * A {1,choice,0#file|1#directory} already exists at “{0}”.
         */
        public static final short FileAlreadyExists_2 = 45;

        /**
         * The “{0}” file is not a directory of resources.
         */
        public static final short FileIsNotAResourceDirectory_1 = 44;

        /**
         * Whether to assemble trajectory fragments (lines in CSV file) in a single feature instance.
         */
        public static final short FoliationRepresentation = 38;

        /**
         * This resource should not fire events of type “{0}”.
         */
        public static final short IllegalEventType_1 = 65;

        /**
         * The {0} data store does not accept features of type “{1}”.
         */
        public static final short IllegalFeatureType_2 = 7;

        /**
         * The {0} reader does not accept inputs of type ‘{1}’.
         */
        public static final short IllegalInputTypeForReader_2 = 8;

        /**
         * The {0} writer does not accept outputs of type ‘{1}’.
         */
        public static final short IllegalOutputTypeForWriter_2 = 9;

        /**
         * The aggregate “{0}” does not accept resources of type ‘{2}’. An instance of ‘{1}’ was
         * expected.
         */
        public static final short IllegalResourceTypeForAggregate_3 = 80;

        /**
         * All coverages must have the same grid geometry.
         */
        public static final short IncompatibleGridGeometry = 72;

        /**
         * Components of the “{1}” name are inconsistent with those of the name previously binded in
         * “{0}” data store.
         */
        public static final short InconsistentNameComponents_2 = 10;

        /**
         * Invalid or unsupported “{1}” expression at index {0}.
         */
        public static final short InvalidExpression_2 = 60;

        /**
         * Loaded grid coverage between {1} – {2} and {3} – {4} from file “{0}” in {5} seconds.
         */
        public static final short LoadedGridCoverage_6 = 59;

        /**
         * Marks are not supported on “{0}” stream.
         */
        public static final short MarkNotSupported_1 = 62;

        /**
         * Relative path to metadata.
         */
        public static final short MetadataLocation = 81;

        /**
         * Resource “{0}” does not have an identifier.
         */
        public static final short MissingResourceIdentifier_1 = 42;

        /**
         * Missing scheme in “{0}” URI.
         */
        public static final short MissingSchemeInURI_1 = 11;

        /**
         * No feature type is common to all the features to aggregate.
         */
        public static final short NoCommonFeatureType = 75;

        /**
         * Cell coordinate {1} in dimension “{0}” maps to {2} slices or tiles. A smaller extent or a
         * merge strategy should be specified.
         */
        public static final short NoSliceMapped_3 = 79;

        /**
         * Extent in dimension “{0}” should be a slice, but {1} cells were specified.
         */
        public static final short NoSliceSpecified_2 = 52;

        /**
         * No directory of resources found at “{0}”.
         */
        public static final short NoSuchResourceDirectory_1 = 46;

        /**
         * Resource “{1}” is not part of aggregate “{0}”.
         */
        public static final short NoSuchResourceInAggregate_2 = 50;

        /**
         * Resource “{0}” is not a writable feature set.
         */
        public static final short NotAWritableFeatureSet_1 = 47;

        /**
         * Processing executed on {0}.
         */
        public static final short ProcessingExecutedOn_1 = 12;

        /**
         * Read with {0} version {1}.
         */
        public static final short ReadBy_2 = 83;

        /**
         * The request [{3} … {4}] is outside the [{1} … {2}] domain for “{0}” axis.
         */
        public static final short RequestOutOfBounds_5 = 64;

        /**
         * A resource already exists at “{0}”.
         */
        public static final short ResourceAlreadyExists_1 = 48;

        /**
         * More than one resource have the “{1}” identifier in the “{0}” data store.
         */
        public static final short ResourceIdentifierCollision_2 = 23;

        /**
         * No resource found for the “{1}” identifier in the “{0}” data store.
         */
        public static final short ResourceNotFound_2 = 24;

        /**
         * This resource has been removed from its data store.
         */
        public static final short ResourceRemoved = 73;

        /**
         * The “{0}” format does not support rotations.
         */
        public static final short RotationNotSupported_1 = 70;

        /**
         * The “{1}” element must be declared before “{0}”.
         */
        public static final short ShallBeDeclaredBefore_2 = 22;

        /**
         * The “{0}” directory is used more than once because of symbolic links.
         */
        public static final short SharedDirectory_1 = 36;

        /**
         * Write operations are not supported.
         */
        public static final short StoreIsReadOnly = 28;

        /**
         * Stream has not mark.
         */
        public static final short StreamHasNoMark = 63;

        /**
         * Cannot move backward in the “{0}” stream.
         */
        public static final short StreamIsForwardOnly_1 = 13;

        /**
         * Stream “{0}” is not readable.
         */
        public static final short StreamIsNotReadable_1 = 25;

        /**
         * Stream “{0}” is not writable.
         */
        public static final short StreamIsNotWritable_1 = 26;

        /**
         * The “{0}” data store can be read only once.
         */
        public static final short StreamIsReadOnce_1 = 18;

        /**
         * Cannot modify previously written data in “{0}”.
         */
        public static final short StreamIsWriteOnce_1 = 21;

        /**
         * Query a subset of “{0}”.
         */
        public static final short SubsetQuery_1 = 77;

        /**
         * Cannot open {0} data store without “{1}” parameter.
         */
        public static final short UndefinedParameter_2 = 27;

        /**
         * The “{0}” feature at {1} has a {3} coordinate values, while we expected a multiple of {2}.
         */
        public static final short UnexpectedNumberOfCoordinates_4 = 58;

        /**
         * Expected a resolution of {0} but got {1} for the slice or tile “{2}”.
         */
        public static final short UnexpectedSliceResolution_3 = 84;

        /**
         * Unfiltered data.
         */
        public static final short UnfilteredData = 76;

        /**
         * Format of “{0}” is not recognized.
         */
        public static final short UnknownFormatFor_1 = 14;

        /**
         * Using {0} JDBC driver version {1}.{2}.
         */
        public static final short UseJdbcDriverVersion_3 = 82;

        /**
         * Used only if this information is not encoded with the data.
         */
        public static final short UsedOnlyIfNotEncoded = 33;
    }

    /**
     * Constructs a new resource bundle loading data from
     * the resource file of the same name as this class.
     */
    public Resources() {
    }

    /**
     * Opens the binary file containing the localized resources to load.
     * This method delegates to {@link Class#getResourceAsStream(String)},
     * but this delegation must be done from the same module as the one
     * that provides the binary file.
     */
    @Override
    protected InputStream getResourceAsStream(final String name) {
        return getClass().getResourceAsStream(name);
    }

    /**
     * Returns the handle for the {@code Keys} constants.
     *
     * @return a handler for the constants declared in the inner {@code Keys} class.
     */
    @Override
    protected KeyConstants getKeyConstants() {
        return Keys.INSTANCE;
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale  the locale, or {@code null} for the default locale.
     * @return resources in the given locale.
     * @throws MissingResourceException if resources cannot be found.
     */
    public static Resources forLocale(final Locale locale) {
        /*
         * We cannot factorize this method into the parent class, because we need to call
         * `ResourceBundle.getBundle(String)` from the module that provides the resources.
         * We do not cache the result because `ResourceBundle` already provides a cache.
         */
        return (Resources) getBundle(Resources.class.getName(), nonNull(locale));
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key  the key for the desired string.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return forLocale(null).getString(key);
    }

    /**
     * Gets a string for the given key and replaces all occurrence of "{0}"
     * with value of {@code arg0}.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0);
    }

    /**
     * Gets a string for the given key and replaces all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @param  arg1  value to substitute to "{1}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0, arg1);
    }

    /**
     * Gets a string for the given key and replaces all occurrence of "{0}",
     * "{1}", with values of {@code arg0}, {@code arg1}, etc.
     *
     * @param  key   the key for the desired string.
     * @param  arg0  value to substitute to "{0}".
     * @param  arg1  value to substitute to "{1}".
     * @param  arg2  value to substitute to "{2}".
     * @return the formatted string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short  key,
                                final Object arg0,
                                final Object arg1,
                                final Object arg2) throws MissingResourceException
    {
        return forLocale(null).getString(key, arg0, arg1, arg2);
    }

    /**
     * The international string to be returned by {@link formatInternational}.
     */
    private static final class International extends ResourceInternationalString {
        private static final long serialVersionUID = -7265791441872360274L;

        International(short key)                           {super(key);}
        International(short key, Object args)              {super(key, args);}
        @Override protected KeyConstants getKeyConstants() {return Keys.INSTANCE;}
        @Override protected IndexedResourceBundle getBundle(final Locale locale) {
            return forLocale(locale);
        }
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key  the key for the desired string.
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key) {
        return new International(key);
    }

    /**
     * Gets an international string for the given key. This method does not check for the key
     * validity. If the key is invalid, then a {@link MissingResourceException} may be thrown
     * when a {@link InternationalString#toString(Locale)} method is invoked.
     *
     * @param  key   the key for the desired string.
     * @param  args  values to substitute to "{0}", "{1}", <i>etc</i>.
     * @return an international string for the given key.
     */
    public static InternationalString formatInternational(final short key, final Object... args) {
        return new International(key, args);
    }
}
