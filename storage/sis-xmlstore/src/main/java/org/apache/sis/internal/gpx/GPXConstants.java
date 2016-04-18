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

import com.esri.core.geometry.Point;
import java.net.URI;
import java.time.temporal.Temporal;
import java.util.Collections;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.internal.feature.AttributeTypeBuilder;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.internal.feature.FeatureTypeBuilder;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Static;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Operation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;

/**
 * GPX XML tags and feature types.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class GPXConstants extends Static {

    /**
     * Main GPX xml tag.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_GPX = "gpx";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_GPX_VERSION = "version";
    /** used in version : 1.0 and 1.1 */
    public static final String ATT_GPX_CREATOR = "creator";

    /**
     * Attributs used a bit everywhere.
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

    /**
     * Metadata tag.
     */
    /** used in version : 1.1 */
    public static final String TAG_METADATA = "metadata";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_METADATA_TIME = "time";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_METADATA_KEYWORDS = "keywords";

    /**
     * Person tag.
     */
    /** used in version : 1.0(as attribut) and 1.1(as tag) */
    public static final String TAG_AUTHOR = "author";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_AUTHOR_EMAIL = "email";

    /**
     * CopyRight tag.
     */
    /** used in version : 1.1 */
    public static final String TAG_COPYRIGHT = "copyright";
    /** used in version : 1.1 */
    public static final String TAG_COPYRIGHT_YEAR = "year";
    /** used in version : 1.1 */
    public static final String TAG_COPYRIGHT_LICENSE = "license";
    /** used in version : 1.1 */
    public static final String ATT_COPYRIGHT_AUTHOR = "author";

    /**
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

    /**
     * Link tag.
     */
    /** used in version : 1.1 */
    public static final String TAG_LINK_TEXT = "text";
    /** used in version : 1.1 */
    public static final String TAG_LINK_TYPE = "type";
    /** used in version : 1.1 */
    public static final String ATT_LINK_HREF = "href";

    /**
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

    /**
     * RTE tag.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_RTE = "rte";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_RTE_RTEPT = "rtept";

    /**
     * TRK tag.
     */
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_TRK = "trk";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_TRK_SEG = "trkseg";
    /** used in version : 1.0 and 1.1 */
    public static final String TAG_TRK_SEG_PT = "trkpt";

    /**
     * Coordinate reference system used by gpx files.
     */
    public static final CoordinateReferenceSystem GPX_CRS = CommonCRS.WGS84.normalizedGeographic();
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
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        final AttributeTypeBuilder atb = new AttributeTypeBuilder();

        //-------------------- GENERIC GPX ENTITY ------------------------------
        final AttributeType<Integer> attIndex = createAttribute("index", Integer.class, 1, 1);
        final String geomName = "geometry";

        ftb.clear();
        ftb.setName(GPX_NAMESPACE, "GPXEntity");
        ftb.setAbstract(true);
        ftb.addProperty(attIndex);
        TYPE_GPX_ENTITY = ftb.build();

        //------------------- WAY POINT TYPE -----------------------------------
        //lat="latitudeType [1] ?"
        //lon="longitudeType [1] ?">
        //<ele> xsd:decimal </ele> [0..1] ?
        //<time> xsd:dateTime </time> [0..1] ?
        //<magvar> degreesType </magvar> [0..1] ?
        //<geoidheight> xsd:decimal </geoidheight> [0..1] ?
        //<name> xsd:string </name> [0..1] ?
        //<cmt> xsd:string </cmt> [0..1] ?
        //<desc> xsd:string </desc> [0..1] ?
        //<src> xsd:string </src> [0..1] ?
        //<link> linkType </link> [0..*] ?
        //<sym> xsd:string </sym> [0..1] ?
        //<type> xsd:string </type> [0..1] ?
        //<fix> fixType </fix> [0..1] ?
        //<sat> xsd:nonNegativeInteger </sat> [0..1] ?
        //<hdop> xsd:decimal </hdop> [0..1] ?
        //<vdop> xsd:decimal </vdop> [0..1] ?
        //<pdop> xsd:decimal </pdop> [0..1] ?
        //<ageofdgpsdata> xsd:decimal </ageofdgpsdata> [0..1] ?
        //<dgpsid> dgpsStationType </dgpsid> [0..1] ?
        //<extensions> extensionsType </extensions> [0..1] ?
        atb.setName(createName(geomName));
        atb.setValueClass(Point.class);
        atb.setCRSCharacteristic(GPX_CRS);
        atb.setMinimumOccurs(1);
        atb.setMaximumOccurs(1);
        final AttributeType attPointGeometry =    atb.build();
        final AttributeType attWptEle =           createAttribute(TAG_WPT_ELE,            Double.class);
        final AttributeType attWptTime =          createAttribute(TAG_WPT_TIME,           Temporal.class);
        final AttributeType attWptMagvar =        createAttribute(TAG_WPT_MAGVAR,         Double.class);
        final AttributeType attWptGeoHeight =     createAttribute(TAG_WPT_GEOIHEIGHT,     Double.class);
        final AttributeType attName =             createAttribute(TAG_NAME,               String.class);
        final AttributeType attCmt =              createAttribute(TAG_CMT,                String.class);
        final AttributeType attDesc =             createAttribute(TAG_DESC,               String.class);
        final AttributeType attSrc =              createAttribute(TAG_SRC,                String.class);
        final AttributeType attLink =             createAttribute(TAG_LINK,               URI.class, 0, Integer.MAX_VALUE);
        final AttributeType attWptSym =           createAttribute(TAG_WPT_SYM,            String.class);
        final AttributeType attType =             createAttribute(TAG_TYPE,               String.class);
        final AttributeType attWptFix =           createAttribute(TAG_WPT_FIX,            String.class);
        final AttributeType attWptSat =           createAttribute(TAG_WPT_SAT,            Integer.class);
        final AttributeType attWptHdop =          createAttribute(TAG_WPT_HDOP,           Double.class);
        final AttributeType attWptVdop =          createAttribute(TAG_WPT_VDOP,           Double.class);
        final AttributeType attWptPdop =          createAttribute(TAG_WPT_PDOP,           Double.class);
        final AttributeType attWptAgeofGpsData =  createAttribute(TAG_WPT_AGEOFGPSDATA,   Double.class);
        final AttributeType attWptDgpsid =        createAttribute(TAG_WPT_DGPSID,         Integer.class);

        ftb.clear();
        ftb.setName(GPX_NAMESPACE, "WayPoint");
        ftb.setSuperTypes(TYPE_GPX_ENTITY);
        ftb.addProperty(attIndex);
        ftb.addProperty(attPointGeometry);
        ftb.addProperty(attWptEle);
        ftb.addProperty(attWptTime);
        ftb.addProperty(attWptMagvar);
        ftb.addProperty(attWptGeoHeight);
        ftb.addProperty(attName);
        ftb.addProperty(attCmt);
        ftb.addProperty(attDesc);
        ftb.addProperty(attSrc);
        ftb.addProperty(attLink);
        ftb.addProperty(attWptSym);
        ftb.addProperty(attType);
        ftb.addProperty(attWptFix);
        ftb.addProperty(attWptSat);
        ftb.addProperty(attWptHdop);
        ftb.addProperty(attWptVdop);
        ftb.addProperty(attWptPdop);
        ftb.addProperty(attWptAgeofGpsData);
        ftb.addProperty(attWptDgpsid);
        ftb.setDefaultGeometryOperation(attPointGeometry.getName());
        TYPE_WAYPOINT = ftb.build();


        //------------------- ROUTE TYPE ---------------------------------------
        //<name> xsd:string </name> [0..1] ?
        //<cmt> xsd:string </cmt> [0..1] ?
        //<desc> xsd:string </desc> [0..1] ?
        //<src> xsd:string </src> [0..1] ?
        //<link> linkType </link> [0..*] ?
        //<number> xsd:nonNegativeInteger </number> [0..1] ?
        //<type> xsd:string </type> [0..1] ?
        //<extensions> extensionsType </extensions> [0..1] ?
        //<rtept> wptType </rtept> [0..*] ?
        final AttributeType<Integer> attNumber = createAttribute(TAG_NUMBER, Integer.class);

        ftb.clear();
        ftb.setName(GPX_NAMESPACE, "Route");
        ftb.setSuperTypes(TYPE_GPX_ENTITY);
        ftb.addProperty(attIndex);
        ftb.addProperty(attName);
        ftb.addProperty(attCmt);
        ftb.addProperty(attDesc);
        ftb.addProperty(attSrc);
        ftb.addProperty(attLink);
        ftb.addProperty(attNumber);
        ftb.addProperty(attType);
        final FeatureAssociationRole attWayPoints = ftb.addAssociation(createName(TAG_RTE_RTEPT),TYPE_WAYPOINT,0,Integer.MAX_VALUE);
        final Operation attRouteGeometry = new GroupPointsAsPolylineOperation(createName(geomName),attWayPoints.getName(), attPointGeometry.getName());
        ftb.addProperty(attRouteGeometry);
        ftb.setDefaultGeometryOperation(attRouteGeometry.getName());
        TYPE_ROUTE = ftb.build();


        //------------------- TRACK SEGMENT TYPE -------------------------------
        //<trkpt> wptType </trkpt> [0..*] ?
        //<extensions> extensionsType </extensions> [0..1] ?
        ftb.clear();
        ftb.setName(GPX_NAMESPACE, "TrackSegment");
        ftb.addProperty(attIndex);
        final FeatureAssociationRole attTrackPoints = ftb.addAssociation(createName(TAG_TRK_SEG_PT),TYPE_WAYPOINT,0,Integer.MAX_VALUE);
        final Operation attTrackSegGeometry = new GroupPointsAsPolylineOperation(createName(geomName),attTrackPoints.getName(), attPointGeometry.getName());
        ftb.addProperty(attTrackSegGeometry);
        ftb.setDefaultGeometryOperation(attTrackSegGeometry.getName());
        TYPE_TRACK_SEGMENT = ftb.build();

        //------------------- TRACK TYPE ---------------------------------------
        //<name> xsd:string </name> [0..1] ?
        //<cmt> xsd:string </cmt> [0..1] ?
        //<desc> xsd:string </desc> [0..1] ?
        //<src> xsd:string </src> [0..1] ?
        //<link> linkType </link> [0..*] ?
        //<number> xsd:nonNegativeInteger </number> [0..1] ?
        //<type> xsd:string </type> [0..1] ?
        //<extensions> extensionsType </extensions> [0..1] ?
        //<trkseg> trksegType </trkseg> [0..*] ?
        ftb.clear();
        ftb.setName(GPX_NAMESPACE, "Track");
        ftb.setSuperTypes(TYPE_GPX_ENTITY);
        ftb.addProperty(attIndex);
        ftb.addProperty(attName);
        ftb.addProperty(attCmt);
        ftb.addProperty(attDesc);
        ftb.addProperty(attSrc);
        ftb.addProperty(attLink);
        ftb.addProperty(attNumber);
        ftb.addProperty(attType);
        final FeatureAssociationRole attTrackSegments = ftb.addAssociation(createName(TAG_TRK_SEG), TYPE_TRACK_SEGMENT, 0, Integer.MAX_VALUE);
        final Operation attTrackGeometry = new GroupPolylinesOperation(createName(geomName),attTrackSegments.getName(), attTrackSegGeometry.getName());
        ftb.addProperty(attTrackGeometry);
        ftb.setDefaultGeometryOperation(attTrackGeometry.getName());
        TYPE_TRACK = ftb.build();

    }

    /**
     * Shortcut method to create attribute type.
     */
    private static <V> AttributeType<V> createAttribute(String name, Class<V> valueClass, int min, int max) {
        return new DefaultAttributeType<>(Collections.singletonMap(AbstractIdentifiedType.NAME_KEY,
                createName(name)), valueClass, min, max, null);
    }

    private static <V> AttributeType<V> createAttribute(String name, Class<V> valueClass) {
        return createAttribute(name, valueClass, 0, 1);
    }

    /**
     * Shortcut method to create generic name in GPX scope.
     */
    private static GenericName createName(String localPart) {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        return factory.createGenericName(null, GPX_NAMESPACE, localPart);
    }

    private GPXConstants() {
    };
}
