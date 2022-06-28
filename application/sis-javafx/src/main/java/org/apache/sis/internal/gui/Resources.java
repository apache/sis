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
         * About…
         */
        public static final short About = 46;

        /**
         * Accessed regions
         */
        public static final short AccessedRegions = 65;

        /**
         * All files
         */
        public static final short AllFiles = 1;

        /**
         * Along {0}
         */
        public static final short Along_1 = 35;

        /**
         * Azimuthal equidistant
         */
        public static final short AzimuthalEquidistant = 42;

        /**
         * Can not close “{0}”. Data may be lost.
         */
        public static final short CanNotClose_1 = 2;

        /**
         * Can not create reference system “{0}”.
         */
        public static final short CanNotCreateCRS_1 = 3;

        /**
         * Can not create XML document.
         */
        public static final short CanNotCreateXML = 4;

        /**
         * Can not fetch tile ({0}, {1}).
         */
        public static final short CanNotFetchTile_2 = 5;

        /**
         * Can not install the resource.
         */
        public static final short CanNotInstallResource = 62;

        /**
         * Can not open “{0}”.
         */
        public static final short CanNotReadFile_1 = 6;

        /**
         * A resource contained in the file can not be read. The cause is given below.
         */
        public static final short CanNotReadResource = 7;

        /**
         * An error occurred while rendering the data.
         */
        public static final short CanNotRender = 8;

        /**
         * Can not use the “{0}” reference system.
         */
        public static final short CanNotUseRefSys_1 = 9;

        /**
         * Centered projection
         */
        public static final short CenteredProjection = 43;

        /**
         * Clear all
         */
        public static final short ClearAll = 55;

        /**
         * Close
         */
        public static final short Close = 10;

        /**
         * Copy
         */
        public static final short Copy = 11;

        /**
         * Copy as
         */
        public static final short CopyAs = 12;

        /**
         * Copy coordinates
         */
        public static final short CopyCoordinates = 50;

        /**
         * Copy file path
         */
        public static final short CopyFilePath = 51;

        /**
         * Display start
         */
        public static final short DisplayStart = 38;

        /**
         * Displayed size
         */
        public static final short DisplayedSize = 41;

        /**
         * Does not cover the area of interest.
         */
        public static final short DoesNotCoverAOI = 13;

        /**
         * Download and install {0} database?
         */
        public static final short DownloadAndInstall_1 = 58;

        /**
         * This geodetic dataset is required for the support of Coordinate Reference Systems defined by
         * {0} codes. The database will use {1} Mb in the “{2}” directory.
         */
        public static final short DownloadDetails_3 = 59;

        /**
         * Enter the URL of the file to open.
         */
        public static final short EnterURL = 71;

        /**
         * An error occurred at the following location:
         */
        public static final short ErrorAt = 53;

        /**
         * Error closing file
         */
        public static final short ErrorClosingFile = 14;

        /**
         * Error creating reference system
         */
        public static final short ErrorCreatingCRS = 15;

        /**
         * Error during data access
         */
        public static final short ErrorDataAccess = 16;

        /**
         * Details about error
         */
        public static final short ErrorDetails = 17;

        /**
         * Error exporting data
         */
        public static final short ErrorExportingData = 18;

        /**
         * Error opening file
         */
        public static final short ErrorOpeningFile = 19;

        /**
         * Exit
         */
        public static final short Exit = 20;

        /**
         * File accesses
         */
        public static final short FileAccesses = 63;

        /**
         * Full screen
         */
        public static final short FullScreen = 22;

        /**
         * {0} geodetic dataset
         */
        public static final short GeodeticDataset_1 = 60;

        /**
         * Geospatial data files
         */
        public static final short GeospatialFiles = 23;

        /**
         * Help
         */
        public static final short Help = 47;

        /**
         * Image start
         */
        public static final short ImageStart = 36;

        /**
         * {0} – `{1}` has an unexpected value.
         */
        public static final short InconsistencyIn_2 = 39;

        /**
         * Generate isolines at constant interval
         * starting from given minimum.
         */
        public static final short IsolinesInRange = 57;

        /**
         * Do you accept the license shown below?
         */
        public static final short LicenseAgreement = 61;

        /**
         * Loading…
         */
        public static final short Loading = 24;

        /**
         * Main window
         */
        public static final short MainWindow = 25;

        /**
         * Mercator
         */
        public static final short Mercator = 44;

        /**
         * New window
         */
        public static final short NewWindow = 26;

        /**
         * No feature type information.
         */
        public static final short NoFeatureTypeInfo = 27;

        /**
         * Open…
         */
        public static final short Open = 28;

        /**
         * Open containing folder
         */
        public static final short OpenContainingFolder = 66;

        /**
         * Open data file
         */
        public static final short OpenDataFile = 29;

        /**
         * Open recent file
         */
        public static final short OpenRecentFile = 54;

        /**
         * Open URL…
         */
        public static final short OpenURL = 70;

        /**
         * Orthographic
         */
        public static final short Orthographic = 52;

        /**
         * Other coordinate reference system…
         */
        public static final short OtherCRS = 72;

        /**
         * Property value
         */
        public static final short PropertyValue = 68;

        /**
         * Range of values…
         */
        public static final short RangeOfValues = 56;

        /**
         * Reference by cell indices
         */
        public static final short ReferenceByCellIndices = 74;

        /**
         * Reference system by identifiers
         */
        public static final short ReferenceByIdentifiers = 73;

        /**
         * Select a coordinate reference system
         */
        public static final short SelectCRS = 30;

        /**
         * For changing the projection, use contextual menu on the map.
         */
        public static final short SelectCrsByContextMenu = 49;

        /**
         * Select parent logger
         */
        public static final short SelectParentLogger = 69;

        /**
         * Send to
         */
        public static final short SendTo = 31;

        /**
         * Size or position
         */
        public static final short SizeOrPosition = 40;

        /**
         * Standard error stream
         */
        public static final short StandardErrorStream = 32;

        /**
         * System monitor
         */
        public static final short SystemMonitor = 64;

        /**
         * Tabular data
         */
        public static final short TabularData = 33;

        /**
         * Tile index start
         */
        public static final short TileIndexStart = 37;

        /**
         * Universal Transverse Mercator
         */
        public static final short UTM = 45;

        /**
         * View
         */
        public static final short View = 67;

        /**
         * Visualize
         */
        public static final short Visualize = 34;

        /**
         * Web site
         */
        public static final short WebSite = 48;

        /**
         * Windows
         */
        public static final short Windows = 21;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param resources  the path of the binary file containing resources, or {@code null} if
     *        there are no resources. The resources may be a file or an entry in a JAR file.
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
