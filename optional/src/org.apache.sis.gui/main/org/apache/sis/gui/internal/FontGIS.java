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
package org.apache.sis.gui.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javafx.scene.text.Font;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToggleButton;
import org.apache.sis.system.Modules;
import org.apache.sis.util.logging.Logging;


/**
 * Creates labels with an icon rendered using Font-GIS.
 * Font-GIS is a set of SVG icons and font to use with GIS and spatial analysis tools.
 * Those font are not included in source code and are not managed as a Maven dependency neither.
 * They are downloaded from the <a href="https://www.npmjs.com/package/font-gis">NPM package</a>
 * by the {@code build.gradle.kts} script where the {@code jar} task is executed.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://viglino.github.io/font-gis/">Font-GIS project</a>
 */
public final class FontGIS {
    /**
     * The font, or {@code null} if not found or if an error occurred.
     * This is loaded at class-initialization time.
     */
    private static final Font FONT;
    static {
        Font font = null;
        final String filename = "font-gis.ttf";
        try {
            InputStream in = Code.class.getResourceAsStream(filename);
            if (in != null) try (in) {
                font = Font.loadFont(in, 0);
            } else {
                // Intentionally caught immediately below.
                throw new FileNotFoundException(filename);
            }
        } catch (IOException e) {
            Logging.unexpectedException(Logger.getLogger(Modules.APPLICATION), FontGIS.class, "<cinit>", e);
        }
        FONT = font;
    }

    /**
     * Default font size in points. The value was chosen empirically.
     */
    private static final int DEFAULT_SIZE = 18;

    /**
     * Default horizontal and vertical padding, in points. The value was chosen empirically.
     */
    private static final int DEFAULT_PADDING = 3;

    /**
     * The default fallback for a glyph than cannot be rendered.
     * Current implementation uses the dotted square.
     */
    private static final String FALLBACK = "\u2B1A";

    /**
     * Do not allow instantiation of this class.
     */
    private FontGIS() {
    }

    /**
     * Creates a button with a Font-GIS glyph for the given code.
     * The code should be one of the {@link Code} constants.
     * The font size and the padding are set to empirical values.
     *
     * @param  code  one of the {@link Code} constants, or 0 for using {@code text} with the default font.
     * @param  text  a fallback to show if Font-GIS couldn't be loaded, or {@code null} for a default fallback.
     * @return the button to use as a scaled glyph for the given code.
     */
    public static Button button(final char code, final String text) {
        final var glyph = new Button();
        setGlyph(glyph, code, text, DEFAULT_SIZE, DEFAULT_PADDING);
        return glyph;
    }

    /**
     * Creates a taggle button with a Font-GIS glyph for the given code.
     * The code should be one of the {@link Code} constants.
     * The font size and the padding are set to empirical values.
     *
     * @param  code  one of the {@link Code} constants, or 0 for using {@code text} with the default font.
     * @param  text  a fallback to show if Font-GIS couldn't be loaded, or {@code null} for a default fallback.
     * @return the button to use as a scaled glyph for the given code.
     */
    public static ToggleButton toggle(final char code, final String text) {
        final var glyph = new ToggleButton();
        setGlyph(glyph, code, text, DEFAULT_SIZE, DEFAULT_PADDING);
        return glyph;
    }

    /**
     * Sets the font, the padding and the text of the given button or label using CSS styles.
     * This method is used because {@link Labeled#setFont(Font)} seems to have no effect on buttons
     * (tested on OpenJFX 20.0.1 and 21.0.2).
     *
     * @param  ctrl  the label or button on which to set the text and font.
     * @param  code  one of the {@link Code} constants, or 0 for using {@code text} with the default font.
     * @param  text  a fallback to show if Font-GIS couldn't be loaded, or {@code null} for a default fallback.
     * @param  size  the font size in points, or -1 for the default size.
     * @param  pads  horizontal and vertical padding in points, or -1 for the default padding.
     */
    public static void setGlyph(final Labeled ctrl, final char code, String text, final int size, final int pads) {
        /*
         * Note 1: in our tests, "-fx-font" didn't worked. We have to use "-fx-font-family".
         * Note 2: setting different values for horizontal and vertical padding does not seem to work.
         */
        final var style = new StringBuilder();
        if (code != 0 && FONT != null) {
            style.append("-fx-font-family:'").append(FONT.getFamily()).append("';");
            text = new String(new char[] {code});
        } else if (text == null) {
            text = FALLBACK;
        }
        if (size >= 0) {
            style.append(" -fx-font-size:").append(size).append("pt;");
        }
        if (pads >= 0) {
            style.append(" -fx-padding:").append(pads).append("pt;");
        }
        ctrl.setStyle(style.toString());
        ctrl.setText(text);
    }

    /**
     * Unicode values for Font-GIS icons.
     * See the <a href="https://viglino.github.io/font-gis/">Font-GIS project</a> for browsing the icons.
     *
     * <h2>Implementation note</h2>
     * Those constants are defined in a separated class for avoiding to load hundreds of field names in memory.
     * Because all field values are constants of primitive type, the compiler should inline them in the caller code.
     */
    public static final class Code {
        /**
         * Do not allow instantiation of this class.
         */
        private Code() {
        }

        /**
         * Unicode value of a Font-GIS icon.
         */
        public static final char
            NORTH_ARROW         = '\uEA8B',
            NORTH_ARROW_N       = '\uEA8C',
            COMPASS             = '\uEA90',
            COMPASS_NEEDLE      = '\uEA91',
            COMPASS_ROSE        = '\uEA92',
            COMPASS_ROSE_N      = '\uEA93',
            COMPASS_ALT         = '\uEB06',
            COMPASS_ALT_O       = '\uEB07',
            ARROW_O             = '\uEA3A',
            ARROW               = '\uEA3B',
            MODIFY_LINE         = '\uEA3C',
            MODIFY_POLY         = '\uEA3D',
            MODIFY_POLY_O       = '\uEA40',
            COPY_POINT          = '\uEA4F',
            COPY_LINE           = '\uEA50',
            COPY_POLY           = '\uEA51',
            BUFFER              = '\uEA6E',
            DIFFERENCE          = '\uEA6F',
            INTERSECTION        = '\uEA70',
            UNION               = '\uEA71',
            SYM_DIFFERENCE      = '\uEA72',
            MOVE                = '\uEA73',
            MOVE_ALT            = '\uEA74',
            OFFSET              = '\uEA75',
            SNAP                = '\uEA76',
            SPLIT               = '\uEA77',
            SPLIT_LINE          = '\uEA78',
            SPLIT_POLYGON       = '\uEA79',
            CONVEX_HULL         = '\uEAA8',
            SELECT_EXTENT       = '\uEAAD',
            SNAP_ORTHO          = '\uEAAE',
            COLOR               = '\uEAAF',
            ROTATE              = '\uEAE3',
            FLIP_H              = '\uEAE4',
            FLIP_V              = '\uEAE5',
            SIMPLIFY            = '\uEAE6',
            PROJ_POINT          = '\uEAE7',
            SCALE_POLY          = '\uEAE8',
            SKELETONIZE         = '\uEB17',
            DILATATION          = '\uEB18',
            EROSION             = '\uEB19',
            TRANSLATE           = '\uEB26',
            TRANSLATE_X         = '\uEB27',
            TRANSLATE_Y         = '\uEB28',
            GPX_FILE            = '\uEA99',
            GEOJSON_FILE        = '\uEA9A',
            KML_FILE            = '\uEA9B',
            WMS                 = '\uEA9C',
            WMTS                = '\uEA9D',
            WFS                 = '\uEA9E',
            WFS_T               = '\uEA9F',
            MVT                 = '\uEAA0',
            XYZ                 = '\uEAA1',
            SHAPE_FILE          = '\uEAA2',
            ESRI_JSON_FILE      = '\uEAA3',
            TOPOJSON_FILE       = '\uEAA4',
            FOLDER_MAP          = '\uEB2F',
            WORLD_FOLDER_O      = '\uEB30',
            WORLD_FOLDER        = '\uEB31',
            FOLDER_GLOBE        = '\uEB32',
            FOLDER_GLOBE_O      = '\uEB33',
            FOLDER_MAPS         = '\uEB34',
            FOLDER_POI          = '\uEB35',
            FOLDER_POI_O        = '\uEB36',
            FOLDER_POIS         = '\uEB37',
            EARTH_NET           = '\uEB38',
            EARTH_NET_O         = '\uEB39',
            WCS                 = '\uEB59',
            POINT               = '\uEA01',
            POLYLINE_PT         = '\uEA02',
            POLYGON_PT          = '\uEA03',
            POLYGON_HOLE_PT     = '\uEA04',
            RECTANGLE_PT        = '\uEA05',
            SQUARE_PT           = '\uEA06',
            CIRCLE_O            = '\uEA07',
            POLYLINE            = '\uEA09',
            POLYGON_O           = '\uEA0A',
            POLYGON_HOLE_O      = '\uEA0B',
            RECTANGLE_O         = '\uEA0C',
            SQUARE_O            = '\uEA0D',
            POLYGON_HOLE        = '\uEA0E',
            POLYGON             = '\uEA0F',
            RECTANGLE           = '\uEA10',
            SQUARE              = '\uEA11',
            CIRCLE              = '\uEA12',
            MULTIPOINT          = '\uEA52',
            BBOX_ALT            = '\uEAA9',
            EXTENT_ALT          = '\uEAAA',
            BBOX                = '\uEAAB',
            EXTENT              = '\uEAAC',
            MAP_EXTENT          = '\uEAB0',
            REGULAR_SHAPE_PT    = '\uEAEB',
            REGULAR_SHAPE_O     = '\uEAEC',
            REGULAR_SHAPE       = '\uEAED',
            LAYER               = '\uEA41',
            LAYER_O             = '\uEA42',
            LAYERS              = '\uEA43',
            LAYERS_O            = '\uEA44',
            LAYER_UP            = '\uEA45',
            LAYER_DOWN          = '\uEA46',
            LAYER_ALT           = '\uEA47',
            LAYER_ALT_O         = '\uEA48',
            LAYER_STACK         = '\uEA49',
            LAYER_STACK_O       = '\uEA4A',
            LAYER_ADD           = '\uEA4B',
            LAYER_ADD_O         = '\uEA4C',
            LAYER_RM            = '\uEA4D',
            LAYER_RM_O          = '\uEA4E',
            LAYER_POI           = '\uEA6A',
            LAYER_DOWNLOAD      = '\uEA97',
            LAYER_UPLOAD        = '\uEA98',
            LAYER_ROAD          = '\uEAF0',
            LAYER_HYDRO         = '\uEAF1',
            LAYER_LANDCOVER     = '\uEAF2',
            LAYER_CONTOUR       = '\uEAF3',
            LAYER_STAT          = '\uEAF4',
            LAYER_STAT_ALT      = '\uEB29',
            LAYER_EDIT          = '\uEB2D',
            LAYER_ALT_EDIT      = '\uEB2E',
            LAYER_HEIGHT        = '\uEB41',
            LAYER_2_ADD_O       = '\uEB46',
            LAYER_2_RM_O        = '\uEB47',
            LAYER_ALT_ADD_O     = '\uEB48',
            LAYER_ALT_RM_O      = '\uEB49',
            LAYER_ALT_X_O       = '\uEB4A',
            LAYERS_POI          = '\uEB4F',
            LAYER_ALT_POI       = '\uEB50',
            EARTH               = '\uEA22',
            EARTH_EURO_AFRICA   = '\uEA23',
            EARTH_ATLANTIC      = '\uEA24',
            EARTH_AMERICA       = '\uEA25',
            EARTH_PACIFIC       = '\uEA26',
            EARTH_AUSTRALIA     = '\uEA27',
            EARTH_ASIA          = '\uEA28',
            EARTH_NORTH         = '\uEA29',
            EARTH_SOUTH         = '\uEA2A',
            EARTH_O             = '\uEA2B',
            EARTH_EURO_AFRICA_O = '\uEA2C',
            EARTH_ATLANTIC_O    = '\uEA2D',
            EARTH_AMERICA_O     = '\uEA2E',
            EARTH_PACIFIC_O     = '\uEA2F',
            EARTH_AUSTRALIA_O   = '\uEA30',
            EARTH_ASIA_O        = '\uEA31',
            EARTH_NORTH_O       = '\uEA32',
            EARTH_SOUTH_O       = '\uEA33',
            GLOBE               = '\uEA36',
            GLOBE_O             = '\uEA37',
            GLOBE_ALT           = '\uEA38',
            GLOBE_ALT_O         = '\uEA39',
            GLOBE_POI           = '\uEA82',
            NETWORK             = '\uEABB',
            NETWORK_O           = '\uEABC',
            TAG                 = '\uEAC1',
            TAG_O               = '\uEAC2',
            TAGS                = '\uEAC3',
            TAGS_O              = '\uEAC4',
            EARTH_GEAR          = '\uEAD5',
            GLOBE_EARTH         = '\uEAF8',
            GLOBE_EARTH_ALT     = '\uEAF9',
            GLOBE_FAVORITE      = '\uEAFB',
            GLOBE_OPTIONS       = '\uEAFC',
            GLOBE_SHARE         = '\uEAFD',
            GLOBE_STAR          = '\uEAFE',
            GLOBE_SMILEY        = '\uEAFF',
            GLOBE_USER          = '\uEB0C',
            GLOBE_USERS         = '\uEB0D',
            GLOBE_SHIELD        = '\uEB0E',
            EARTH_NETWORK       = '\uEB0F',
            EARTH_NETWORK_O     = '\uEB10',
            GLOBE_GEAR          = '\uEB11',
            MAP                 = '\uEA53',
            MAP_O               = '\uEA54',
            MAP_POI             = '\uEA55',
            WORLD_MAP_ALT       = '\uEA56',
            MAP_ROUTE           = '\uEA57',
            ROAD_MAP            = '\uEA58',
            CADASTRE_MAP        = '\uEA59',
            LANDCOVER_MAP       = '\uEA5A',
            BUS_MAP             = '\uEA5B',
            CONTOUR_MAP         = '\uEA5C',
            HYDRO_MAP           = '\uEA5D',
            WORLD_MAP           = '\uEA68',
            PIRATE_MAP          = '\uEA6B',
            STORY_MAP           = '\uEA6D',
            MAP_BOOK            = '\uEA7A',
            MAP_LEGEND          = '\uEA85',
            MAP_LEGEND_O        = '\uEA86',
            MAP_OPTIONS         = '\uEA94',
            MAP_OPTIONS_ALT     = '\uEA95',
            MAP_PRINT           = '\uEA96',
            WORLD_MAP_ALT_O     = '\uEAB1',
            FLOW_MAP            = '\uEAB2',
            MAP_STAT            = '\uEAB3',
            STATISTIC_MAP       = '\uEAB4',
            VORONOI_MAP         = '\uEAB7',
            TRIANGLE_MAP        = '\uEAB8',
            PHONE_MAP           = '\uEAB9',
            HEX_MAP             = '\uEABA',
            MAP_BOOKMARK        = '\uEABD',
            MAP_TAG             = '\uEABF',
            MAP_TAGS            = '\uEAC0',
            COMPARE_MAP         = '\uEAD8',
            SWIPE_MAP_V         = '\uEAD9',
            SWIPE_MAP_H         = '\uEADA',
            MAGNIFY_MAP         = '\uEADB',
            MAP_SHARE           = '\uEAE0',
            MAP_SEND            = '\uEAE1',
            MAP_SHARE_ALT       = '\uEAE2',
            MAP_ADD             = '\uEAE9',
            MAP_RM              = '\uEAEA',
            MAP_TIME            = '\uEAEE',
            TIME_MAP            = '\uEAEF',
            MAP_PLAY            = '\uEAF5',
            MAP_STAR            = '\uEAF6',
            MAP_FAVORITE        = '\uEAF7',
            MAP_SMILEY          = '\uEB00',
            MAP_CONTROL         = '\uEB02',
            MAP_LOCK            = '\uEB04',
            MAP_UNLOCK          = '\uEB05',
            WEATHER_MAP         = '\uEB0B',
            STORY_MAP_O         = '\uEB2A',
            STORY_MAPS          = '\uEB2B',
            MAP_EDIT            = '\uEB2C',
            HEIGHT_MAP          = '\uEB40',
            MAP_USER            = '\uEB4B',
            MAP_USERS           = '\uEB4C',
            MEASURE             = '\uEA08',
            MEASURE_LINE        = '\uEA13',
            MEASURE_AREA        = '\uEA14',
            MEASURE_AREA_ALT    = '\uEA15',
            SCALE               = '\uEB01',
            AZIMUTH             = '\uEB53',
            HELP_LARROW         = '\uEA3E',
            HELP_RARROW         = '\uEA3F',
            HOME                = '\uEB14',
            SATELLITE           = '\uEB3A',
            SATELLITE_EARTH     = '\uEB3B',
            DRONE               = '\uEB3F',
            POI                 = '\uEA16',
            POI_O               = '\uEA17',
            POI_ALT             = '\uEA18',
            POI_ALT_O           = '\uEA19',
            PIN                 = '\uEA1A',
            PUSHPIN             = '\uEA1B',
            POIS                = '\uEA1C',
            POIS_O              = '\uEA1D',
            POI_FAVORITE        = '\uEA1E',
            POI_FAVORITE_O      = '\uEA1F',
            POI_HOME            = '\uEA20',
            POI_HOME_O          = '\uEA21',
            POI_EARTH           = '\uEA34',
            PIN_EARTH           = '\uEA35',
            PIRATE_POI          = '\uEA6C',
            LOCATION_POI        = '\uEA83',
            LOCATION_POI_O      = '\uEA84',
            BOOKMARK_POI        = '\uEABE',
            BOOKMARK_POI_B      = '\uEACF',
            POI_MAP             = '\uEAD6',
            POI_MAP_O           = '\uEAD7',
            LOCATION_MAN        = '\uEB15',
            LOCATION_MAN_ALT    = '\uEB16',
            POI_INFO            = '\uEB1C',
            POI_INFO_O          = '\uEB1D',
            POSITION            = '\uEB22',
            POSITION_O          = '\uEB23',
            POSITION_MAN        = '\uEB24',
            POI_SLASH           = '\uEB4D',
            POI_SLASH_O         = '\uEB4E',
            ROUTE               = '\uEA7B',
            ROUTE_START         = '\uEA7C',
            ROUTE_END           = '\uEA7D',
            CAR                 = '\uEA7E',
            BICYCLE             = '\uEA7F',
            PEDESTRIAN          = '\uEA80',
            HIKER               = '\uEA81',
            LOCATION_ARROW      = '\uEA87',
            LOCATION_ARROW_O    = '\uEA88',
            LOCATION            = '\uEA89',
            LOCATION_ON         = '\uEA8A',
            DIRECT              = '\uEA8D',
            REVERS              = '\uEA8E',
            TIMER               = '\uEA8F',
            SIGNPOST            = '\uEAB5',
            DIRECTION           = '\uEAB6',
            FLAG                = '\uEAC5',
            FLAG_O              = '\uEAC6',
            FLAG_START          = '\uEAC7',
            FLAG_START_O        = '\uEAC8',
            FLAG_FINISH         = '\uEAC9',
            FLAG_B              = '\uEACA',
            FLAB_B_O            = '\uEACB',
            FLAG_START_B        = '\uEACC',
            FLAG_START_B_O      = '\uEACD',
            FLAG_FINISH_B_O     = '\uEACE',
            START               = '\uEAD0',
            START_O             = '\uEAD1',
            STEP                = '\uEAD2',
            STEP_O              = '\uEAD3',
            FINISH              = '\uEAD4',
            DIRECTIONS          = '\uEB03',
            PHONE_ROUTE         = '\uEB08',
            PHONE_ROUTE_ALT     = '\uEB09',
            PHONE_ROUTE_ALT_R   = '\uEB0A',
            MAP_SEARCH          = '\uEA5E',
            SEARCH_MAP          = '\uEA5F',
            SEARCH_POI          = '\uEA60',
            SEARCH_GLOBE        = '\uEA61',
            SEARCH_HOME         = '\uEA62',
            SEARCH_ADDRESS      = '\uEA63',
            SEARCH_ATTRIBTUES   = '\uEA64',
            SEARCH_PROPERTIE    = '\uEA65',
            SEARCH_FEATURE      = '\uEA66',
            SEARCH_LAYER        = '\uEA67',
            SEARCH_COUNTRY      = '\uEA69',
            SEARCH_GLOBE_ALT    = '\uEAFA',
            SEARCH_COORD        = '\uEB12',
            SEARCH_DATA         = '\uEB13',
            ZOOM_IN             = '\uEAA5',
            ZOOM_OUT            = '\uEAA6',
            FULL_SCREEN         = '\uEAA7',
            SCREEN_DUB          = '\uEADC',
            SCREEN_SPLIT_H      = '\uEADD',
            SCREEN_SPLIT_V      = '\uEADE',
            SCREEN_MAG          = '\uEADF',
            COORD_SYSTEM        = '\uEB1A',
            COORD_SYSTEM_3D     = '\uEB1B',
            COORD_SYSTEM_ALT    = '\uEB1E',
            COORD_SYSTEM_3D_ALT = '\uEB1F',
            GRID                = '\uEB20',
            CUBE_3D             = '\uEB21',
            COORD_GRID          = '\uEB25',
            PHOTOGRAMMETRY      = '\uEB3C',
            D360                = '\uEB3D',
            TOPOGRAPHY          = '\uEB3E',
            GNSS                = '\uEB42',
            GNSS_ANTENNA        = '\uEB43',
            TACHEOMETER         = '\uEB44',
            THEODOLITE          = '\uEB45',
            PROFILE             = '\uEB51',
            PROFILE_O           = '\uEB52',
            SCREEN_DUB1         = '\uEB54',
            SCREEN_DUB2         = '\uEB55',
            SCREEN_DUB_O        = '\uEB56',
            SCREEN_MAG_O        = '\uEB57',
            SCREEN_MAG_ALT      = '\uEB58';
    }
}
