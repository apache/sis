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
package org.apache.sis.internal.gui;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import org.apache.sis.util.resources.KeyConstants;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * Messages that are specific to the {@code sis-javafx} module.
 * Resources in this file should not be used by any other module. For resources shared by
 * all modules in the Apache SIS project, see {@link org.apache.sis.util.resources} package.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class Resources extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
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
         * Abstract
         */
        public static final short Abstract = 14;

        /**
         * All files
         */
        public static final short AllFiles = 3;

        /**
         * Coordinate Reference Systems
         */
        public static final short CRSs = 1;

        /**
         * Can not close “{0}”. Data may be lost.
         */
        public static final short CanNotClose_1 = 12;

        /**
         * Can not create reference system “{0}”.
         */
        public static final short CanNotCreateCRS_1 = 35;

        /**
         * Can not create XML document.
         */
        public static final short CanNotCreateXML = 47;

        /**
         * Can not fetch tile ({0}, {1}).
         */
        public static final short CanNotFetchTile_2 = 45;

        /**
         * Can not open “{0}”.
         */
        public static final short CanNotReadFile_1 = 5;

        /**
         * A resource contained in the file can not be read. The cause is given below.
         */
        public static final short CanNotReadResource = 55;

        /**
         * Cell geometry
         */
        public static final short CellGeometry = 15;

        /**
         * Close
         */
        public static final short Close = 8;

        /**
         * Copy
         */
        public static final short Copy = 31;

        /**
         * Copy as
         */
        public static final short CopyAs = 46;

        /**
         * Creation date
         */
        public static final short CreationDate = 16;

        /**
         * Credit
         */
        public static final short Credit = 17;

        /**
         * Data
         */
        public static final short Data = 32;

        /**
         * Date
         */
        public static final short Date = 18;

        /**
         * Dimensions
         */
        public static final short Dimensions = 19;

        /**
         * Display
         */
        public static final short Display = 41;

        /**
         * Does not cover the area of interest.
         */
        public static final short DoesNotCoverAOI = 57;

        /**
         * Error closing file
         */
        public static final short ErrorClosingFile = 13;

        /**
         * Error creating reference system
         */
        public static final short ErrorCreatingCRS = 36;

        /**
         * Error during data access
         */
        public static final short ErrorDataAccess = 40;

        /**
         * Details about error
         */
        public static final short ErrorDetails = 56;

        /**
         * Error exporting data
         */
        public static final short ErrorExportingData = 48;

        /**
         * Error opening file
         */
        public static final short ErrorOpeningFile = 6;

        /**
         * Exit
         */
        public static final short Exit = 9;

        /**
         * Extent
         */
        public static final short Extent = 20;

        /**
         * File
         */
        public static final short File = 10;

        /**
         * Filter
         */
        public static final short Filter = 34;

        /**
         * Format
         */
        public static final short Format = 38;

        /**
         * From metadata
         */
        public static final short FromMetadata = 53;

        /**
         * Full screen
         */
        public static final short FullScreen = 42;

        /**
         * Geospatial data files
         */
        public static final short GeospatialFiles = 4;

        /**
         * Identifiers
         */
        public static final short Identifiers = 54;

        /**
         * Loading…
         */
        public static final short Loading = 7;

        /**
         * Main window
         */
        public static final short MainWindow = 44;

        /**
         * Metadata
         */
        public static final short Metadata = 30;

        /**
         * New window
         */
        public static final short NewWindow = 39;

        /**
         * No feature type information.
         */
        public static final short NoFeatureTypeInfo = 33;

        /**
         * Number of dimensions
         */
        public static final short NumberOfDimensions = 27;

        /**
         * Open…
         */
        public static final short Open = 11;

        /**
         * Open data file
         */
        public static final short OpenDataFile = 2;

        /**
         * Publication date
         */
        public static final short PublicationDate = 21;

        /**
         * Purpose
         */
        public static final short Purpose = 28;

        /**
         * Reference system
         */
        public static final short ReferenceSystem = 22;

        /**
         * Resource identification
         */
        public static final short ResourceIdentification = 23;

        /**
         * Select a coordinate reference system
         */
        public static final short SelectCRS = 37;

        /**
         * Send to
         */
        public static final short SendTo = 49;

        /**
         * Spatial representation
         */
        public static final short SpatialRepresentation = 24;

        /**
         * Standard error stream
         */
        public static final short StandardErrorStream = 50;

        /**
         * Summary
         */
        public static final short Summary = 29;

        /**
         * Tabular data
         */
        public static final short TabularData = 51;

        /**
         * Topic category
         */
        public static final short TopicCategory = 25;

        /**
         * Type of resource
         */
        public static final short TypeOfResource = 26;

        /**
         * Visualize
         */
        public static final short Visualize = 52;

        /**
         * Windows
         */
        public static final short Windows = 43;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources  the path of the binary file containing resources, or {@code null} if
     *        there is no resources. The resources may be a file or an entry in a JAR file.
     */
    public Resources(final URL resources) {
        super(resources);
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
     * @throws MissingResourceException if resources can not be found.
     */
    public static Resources forLocale(final Locale locale) throws MissingResourceException {
        return getBundle(Resources.class, locale);
    }

    /**
     * Returns resources in the default locale.
     *
     * @return resources in the default locale.
     * @throws MissingResourceException if resources can not be found.
     */
    public static Resources getInstance() throws MissingResourceException {
        return getBundle(Resources.class, null);
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key  the key for the desired string.
     * @return the string for the given key.
     * @throws MissingResourceException if no object for the given key can be found.
     */
    public static String format(final short key) throws MissingResourceException {
        return getInstance().getString(key);
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
        return getInstance().getString(key, arg0);
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
        return getInstance().getString(key, arg0, arg1);
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
        return getInstance().getString(key, arg0, arg1, arg2);
    }

    /**
     * Creates a new menu item with a localized text specified by the given key.
     *
     * @param  key       the key for the text of the menu item.
     * @param  onAction  action to execute when the menu is selected.
     * @return the menu item with the specified text and action.
     */
    public MenuItem menu(final short key, final EventHandler<ActionEvent> onAction) {
        final MenuItem item = new MenuItem(getString(key));
        item.setOnAction(onAction);
        return item;
    }
}
