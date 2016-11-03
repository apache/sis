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
package org.apache.sis.internal.gpx;

import java.net.URI;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.Map;
import com.esri.core.geometry.Point;
import org.opengis.util.LocalName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Static;
import org.apache.sis.util.iso.Names;

// Branch-dependent imports
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;


/**
 * GPX XML tags and feature types.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.8
 * @module
 */
public final class GPXConstants extends Static {
    /*
     * Main GPX xml tag.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_GPX = "gpx";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_GPX_VERSION = "version";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_GPX_CREATOR = "creator";

    /**
     * Attributes used a bit everywhere.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_NAME = "name";
    /** used in version : 1.0 */
    public static final String TAG_URL = "url";
    /** used in version : 1.0 */
    public static final String TAG_URLNAME = "urlname";
    /** used in version : 1.1 */
    public static final String TAG_LINK = "link";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_DESC = "desc";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_CMT = "cmt";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_SRC = "src";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_TYPE = "type";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_NUMBER = "number";

    /*
     * Metadata tag.
     */
    /** used in version : 1.1 */
    public static final String TAG_METADATA = "metadata";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_METADATA_TIME = "time";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_METADATA_KEYWORDS = "keywords";

    /*
     * Person tag.
     */
    /** used in version : 1.0(as attribut) and 1.1(as tag) */
    public static final String TAG_AUTHOR = "author";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_AUTHOR_EMAIL = "email";

    /*
     * Copyright tag.
     */
    /** used in version : 1.1 */
    public static final String TAG_COPYRIGHT = "copyright";
    /** used in version : 1.1 */
    public static final String TAG_COPYRIGHT_YEAR = "year";
    /** used in version : 1.1 */
    public static final String TAG_COPYRIGHT_LICENSE = "license";
    /** used in version : 1.1 */
    public static final String ATT_COPYRIGHT_AUTHOR = "author";

    /*
     * Bounds tag.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_BOUNDS = "bounds";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_BOUNDS_MINLAT = "minlat";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_BOUNDS_MINLON = "minlon";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_BOUNDS_MAXLAT = "maxlat";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_BOUNDS_MAXLON = "maxlon";

    /*
     * Link tag.
     */
    /** used in version : 1.1 */
    public static final String TAG_LINK_TEXT = "text";
    /** used in version : 1.1 */
    public static final String TAG_LINK_TYPE = "type";
    /** used in version : 1.1 */
    public static final String ATT_LINK_HREF = "href";

    /*
     * WPT tag.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT = "wpt";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_WPT_LAT = "lat";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_WPT_LON = "lon";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_ELE = "ele";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_TIME = "time";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_MAGVAR = "magvar";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_GEOIHEIGHT = "geoidheight";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_SYM = "sym";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_FIX = "fix";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_SAT = "sat";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_HDOP = "hdop";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_VDOP = "vdop";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_PDOP = "pdop";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_AGEOFGPSDATA = "ageofdgpsdata";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_WPT_DGPSID = "dgpsid";

    /*
     * RTE tag.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_RTE = "rte";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_RTE_RTEPT = "rtept";

    /*
     * TRK tag.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_TRK = "trk";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_TRK_SEG = "trkseg";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_TRK_SEG_PT = "trkpt";

    /**
     * GPX scope name used for feature type names.
     */
    public static final String GPX_NAMESPACE = "http://www.topografix.com";

    /**
     * GPX 1.1 xml namespace
     */
    public static final String GPX_NAMESPACE_V11 = "http://www.topografix.com/GPX/1/1";
    /**
     * GPX 1.0 xml namespace
     */
    public static final String GPX_NAMESPACE_V10 = "http://www.topografix.com/GPX/1/0";

    /**
     * Parent feature type of all gpx types.
     */
    public static final FeatureType TYPE_GPX_ENTITY;

    /**
     * Waypoint GPX feature type.
     */
    public static final FeatureType TYPE_WAYPOINT;

    /**
     * Track GPX feature type.
     */
    public static final FeatureType TYPE_TRACK;

    /**
     * Route GPX feature type.
     */
    public static final FeatureType TYPE_ROUTE;

    /**
     * Track segment GPX feature type.
     */
    public static final FeatureType TYPE_TRACK_SEGMENT;

    static {
        final NameFactory   factory  = DefaultFactories.forBuildin(NameFactory.class);
        final LocalName     geomName = Names.createTypeName(null, null, "geometry");
        final Map<String,?> geomInfo = Collections.singletonMap(AbstractIdentifiedType.NAME_KEY, geomName);

        //-------------------- GENERIC GPX ENTITY ------------------------------
        FeatureTypeBuilder builder = new FeatureTypeBuilder(null, factory, null);
        builder.setDefaultScope(GPX_NAMESPACE).setName("GPXEntity").setAbstract(true);
        builder.addAttribute(Integer.class).setName("index").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        TYPE_GPX_ENTITY = builder.build();

        //------------------- WAY POINT TYPE -----------------------------------
        /*
         * lat="latitudeType [1] ?"
         * lon="longitudeType [1] ?">
         * <ele> xsd:decimal </ele> [0..1] ?
         * <time> xsd:dateTime </time> [0..1] ?
         * <magvar> degreesType </magvar> [0..1] ?
         * <geoidheight> xsd:decimal </geoidheight> [0..1] ?
         * <name> xsd:string </name> [0..1] ?
         * <cmt> xsd:string </cmt> [0..1] ?
         * <desc> xsd:string </desc> [0..1] ?
         * <src> xsd:string </src> [0..1] ?
         * <link> linkType </link> [0..*] ?
         * <sym> xsd:string </sym> [0..1] ?
         * <type> xsd:string </type> [0..1] ?
         * <fix> fixType </fix> [0..1] ?
         * <sat> xsd:nonNegativeInteger </sat> [0..1] ?
         * <hdop> xsd:decimal </hdop> [0..1] ?
         * <vdop> xsd:decimal </vdop> [0..1] ?
         * <pdop> xsd:decimal </pdop> [0..1] ?
         * <ageofdgpsdata> xsd:decimal </ageofdgpsdata> [0..1] ?
         * <dgpsid> dgpsStationType </dgpsid> [0..1] ?
         * <extensions> extensionsType </extensions> [0..1] ?
         */
        builder = new FeatureTypeBuilder(null, factory, null);
        builder.setDefaultScope(GPX_NAMESPACE).setName("WayPoint").setSuperTypes(TYPE_GPX_ENTITY);
        builder.addAttribute(Point.class).setName(geomName)
                .setCRS(CommonCRS.defaultGeographic())
                .addRole(AttributeRole.DEFAULT_GEOMETRY);
        builder.setDefaultCardinality(0, 1);
        builder.addAttribute(Double  .class).setName(TAG_WPT_ELE);
        builder.addAttribute(Temporal.class).setName(TAG_WPT_TIME);
        builder.addAttribute(Double  .class).setName(TAG_WPT_MAGVAR);
        builder.addAttribute(Double  .class).setName(TAG_WPT_GEOIHEIGHT);
        builder.addAttribute(String  .class).setName(TAG_NAME);
        builder.addAttribute(String  .class).setName(TAG_CMT);
        builder.addAttribute(String  .class).setName(TAG_DESC);
        builder.addAttribute(String  .class).setName(TAG_SRC);
        builder.addAttribute(URI     .class).setName(TAG_LINK).setMaximumOccurs(Integer.MAX_VALUE);
        builder.addAttribute(String  .class).setName(TAG_WPT_SYM);
        builder.addAttribute(String  .class).setName(TAG_TYPE);
        builder.addAttribute(String  .class).setName(TAG_WPT_FIX);
        builder.addAttribute(Integer .class).setName(TAG_WPT_SAT);
        builder.addAttribute(Double  .class).setName(TAG_WPT_HDOP);
        builder.addAttribute(Double  .class).setName(TAG_WPT_VDOP);
        builder.addAttribute(Double  .class).setName(TAG_WPT_PDOP);
        builder.addAttribute(Double  .class).setName(TAG_WPT_AGEOFGPSDATA);
        builder.addAttribute(Integer .class).setName(TAG_WPT_DGPSID);
        TYPE_WAYPOINT = builder.build();

        //------------------- ROUTE TYPE ---------------------------------------
        /*
         * <name> xsd:string </name> [0..1] ?
         * <cmt> xsd:string </cmt> [0..1] ?
         * <desc> xsd:string </desc> [0..1] ?
         * <src> xsd:string </src> [0..1] ?
         * <link> linkType </link> [0..*] ?
         * <number> xsd:nonNegativeInteger </number> [0..1] ?
         * <type> xsd:string </type> [0..1] ?
         * <extensions> extensionsType </extensions> [0..1] ?
         * <rtept> wptType </rtept> [0..*] ?
         */
        final DefaultAssociationRole attWayPoints = new DefaultAssociationRole(
                identification(factory, TAG_RTE_RTEPT), TYPE_WAYPOINT, 0, Integer.MAX_VALUE);
        PropertyType[] properties = {
                TYPE_WAYPOINT.getProperty(TAG_NAME),
                TYPE_WAYPOINT.getProperty(TAG_CMT),
                TYPE_WAYPOINT.getProperty(TAG_DESC),
                TYPE_WAYPOINT.getProperty(TAG_SRC),
                TYPE_WAYPOINT.getProperty(TAG_LINK),
                new DefaultAttributeType<>(identification(factory, TAG_NUMBER), Integer.class, 0, 1, null),
                TYPE_WAYPOINT.getProperty(TAG_TYPE),
                attWayPoints,
                new GroupPointsAsPolylineOperation(geomInfo, attWayPoints.getName(), geomName),
                null
        };
        final Map<String,?> envelopeInfo = Collections.singletonMap(AbstractIdentifiedType.NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY);
        try {
            properties[properties.length - 1] = FeatureOperations.envelope(envelopeInfo, null, properties);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
        TYPE_ROUTE = new DefaultFeatureType(identification(factory, "Route"), false,
                new FeatureType[] {TYPE_GPX_ENTITY}, properties);


        //------------------- TRACK SEGMENT TYPE -------------------------------
        /*
         * <trkpt> wptType </trkpt> [0..*] ?
         * <extensions> extensionsType </extensions> [0..1] ?
         */
        final DefaultAssociationRole attTrackPoints = new DefaultAssociationRole(
                identification(factory, TAG_TRK_SEG_PT), TYPE_WAYPOINT, 0, Integer.MAX_VALUE);
        properties = new PropertyType[] {
                attTrackPoints,
                new GroupPointsAsPolylineOperation(geomInfo, attTrackPoints.getName(), geomName),
                null
        };
        try {
            properties[properties.length - 1] = FeatureOperations.envelope(envelopeInfo, null, properties);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
        TYPE_TRACK_SEGMENT = new DefaultFeatureType(identification(factory, "TrackSegment"), false,
                new FeatureType[] {TYPE_GPX_ENTITY}, properties
        );

        //------------------- TRACK TYPE ---------------------------------------
        /*
         * <name> xsd:string </name> [0..1] ?
         * <cmt> xsd:string </cmt> [0..1] ?
         * <desc> xsd:string </desc> [0..1] ?
         * <src> xsd:string </src> [0..1] ?
         * <link> linkType </link> [0..*] ?
         * <number> xsd:nonNegativeInteger </number> [0..1] ?
         * <type> xsd:string </type> [0..1] ?
         * <extensions> extensionsType </extensions> [0..1] ?
         * <trkseg> trksegType </trkseg> [0..*] ?
         */
        final DefaultAssociationRole attTrackSegments = new DefaultAssociationRole(
                identification(factory, TAG_TRK_SEG), TYPE_TRACK_SEGMENT, 0, Integer.MAX_VALUE);
        properties = new PropertyType[] {
                TYPE_ROUTE.getProperty(TAG_NAME),
                TYPE_ROUTE.getProperty(TAG_CMT),
                TYPE_ROUTE.getProperty(TAG_DESC),
                TYPE_ROUTE.getProperty(TAG_SRC),
                TYPE_ROUTE.getProperty(TAG_LINK),
                TYPE_ROUTE.getProperty(TAG_NUMBER),
                TYPE_ROUTE.getProperty(TAG_TYPE),
                attTrackSegments,
                new GroupPolylinesOperation(geomInfo, attTrackSegments.getName(), geomName),
                null
        };
        try {
            properties[properties.length - 1] = FeatureOperations.envelope(envelopeInfo, null, properties);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
        TYPE_TRACK = new DefaultFeatureType(identification(factory, "Track"), false,
                new FeatureType[] {TYPE_GPX_ENTITY}, properties
        );
    }

    private static Map<String,?> identification(final NameFactory factory, final String localPart) {
        return Collections.singletonMap(AbstractIdentifiedType.NAME_KEY,
                factory.createGenericName(null, GPX_NAMESPACE, localPart));
    }

    /**
     * Do not allow instantiation of this class.
     */
    private GPXConstants() {
    }
}
