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
package org.apache.sis.internal.storage.gpx;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import com.esri.core.geometry.Point;
import org.opengis.util.LocalName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.content.ContentInformation;
import org.apache.sis.storage.gps.Fix;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.storage.FeatureCatalogBuilder;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.Static;

// Branch-dependent imports
import java.time.temporal.Temporal;
import org.opengis.feature.FeatureType;


/**
 * Feature types that may appear in GPX files. All values defined in this class are immutable and can be shared
 * by many {@link Reader} instances. There is usually only one {@code Types} instance for a running JVM, but we
 * nevertheless allows definition of alternative {@code Types} with names created by different factories.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class Types extends Static {
    /**
     * Way point GPX feature type.
     */
    final FeatureType wayPoint;

    /**
     * Route GPX feature type.
     */
    final FeatureType route;

    /**
     * Track GPX feature type.
     */
    final FeatureType track;

    /**
     * Track segment GPX feature type.
     */
    final FeatureType trackSegment;

    /**
     * The list of feature types to be given to GPC metadata objects.
     *
     * @see Metadata#features
     */
    final Collection<ContentInformation> metadata;

    /**
     * Binding from names to feature type instances.
     * Shall not be modified after construction.
     */
    final FeatureNaming<FeatureType> names;

    /**
     * A system-wide instance for {@code FeatureType} instances created using the {@link DefaultNameFactory}.
     * This is normally the only instance used in an application.
     */
    static final Types DEFAULT;
    static {
        try {
            DEFAULT = new Types(DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class), null);
        } catch (FactoryException | IllegalNameException e) {
            throw new AssertionError(e);        // Should never happen with DefaultNameFactory implementation.
        }
    }

    /**
     * Creates new {@code FeatureTypes} with feature names and property names created using the given factory.
     *
     * @param  factory   the factory to use for creating names, or {@code null} for the default factory.
     * @param  locale    the locale to use for formatting error messages, or {@code null} for the default locale.
     * @throws FactoryException if an error occurred while creating an "envelope bounds" operation.
     */
    Types(final NameFactory factory, final Locale locale) throws FactoryException, IllegalNameException {
        final LocalName     geomName = AttributeConvention.GEOMETRY_PROPERTY;
        final Map<String,?> geomInfo = Collections.singletonMap(AbstractIdentifiedType.NAME_KEY, geomName);
        final Map<String,?> envpInfo = Collections.singletonMap(AbstractIdentifiedType.NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY);
        /*
         * The parent of all FeatureTypes to be created in this constructor.
         * This parent has a single property, "@identifier" of type Integer,
         * which is not part of GPX specification.
         *
         * http://www.topografix.com/GPX/GPXEntity
         * ┌─────────────┬─────────┬─────────────┐
         * │ Name        │ Type    │ Cardinality │
         * ├─────────────┼─────────┼─────────────┤
         * │ @identifier │ Integer │   [1 … 1]   │      SIS-specific property
         * └─────────────┴─────────┴─────────────┘
         */
        FeatureTypeBuilder builder = new FeatureTypeBuilder(null, factory, locale);
        builder.setDefaultScope(Tags.NAMESPACE).setName("GPXEntity").setAbstract(true);
        builder.addAttribute(Integer.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);
        final FeatureType parent = builder.build();
        /*
         * http://www.topografix.com/GPX/WayPoint ⇾ GPXEntity
         * ┌───────────────┬────────────────┬────────────────────────┬─────────────┐
         * │ Name          │ Type           │ XML type               │ Cardinality │
         * ├───────────────┼────────────────┼────────────────────────┼─────────────┤
         * │ @identifier   │ Integer        │                        │   [1 … 1]   │
         * │ @envelope     │ Envelope       │                        │   [1 … 1]   │
         * │ @geometry     │ Point          │ (lat,lon) attributes   │   [1 … 1]   │
         * │ ele           │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ time          │ Temporal       │ xsd:dateTime           │   [0 … 1]   │
         * │ magvar        │ Double         │ gpx:degreesType        │   [0 … 1]   │
         * │ geoidheight   │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ name          │ String         │ xsd:string             │   [0 … 1]   │
         * │ cmt           │ String         │ xsd:string             │   [0 … 1]   │
         * │ desc          │ String         │ xsd:string             │   [0 … 1]   │
         * │ src           │ String         │ xsd:string             │   [0 … 1]   │
         * │ link          │ OnlineResource │ gpx:linkType           │   [0 … ∞]   │
         * │ sym           │ String         │ xsd:string             │   [0 … 1]   │
         * │ type          │ String         │ xsd:string             │   [0 … 1]   │
         * │ fix           │ Fix            │ gpx:fixType            │   [0 … 1]   │
         * │ sat           │ Integer        │ xsd:nonNegativeInteger │   [0 … 1]   │
         * │ hdop          │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ vdop          │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ pdop          │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ ageofdgpsdata │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ dgpsid        │ Integer        │ gpx:dgpsStationType    │   [0 … 1]   │
         * └───────────────┴────────────────┴────────────────────────┴─────────────┘
         */
        builder = new FeatureTypeBuilder(null, factory, locale).setSuperTypes(parent);
        builder.setDefaultScope(Tags.NAMESPACE).setName("WayPoint");
        builder.addAttribute(Point.class).setName(geomName)
                .setCRS(CommonCRS.WGS84.normalizedGeographic())
                .addRole(AttributeRole.DEFAULT_GEOMETRY);
        builder.setDefaultCardinality(0, 1);
        builder.addAttribute(Double        .class).setName(Tags.ELEVATION);
        builder.addAttribute(Temporal      .class).setName(Tags.TIME);
        builder.addAttribute(Double        .class).setName(Tags.MAGNETIC_VAR);
        builder.addAttribute(Double        .class).setName(Tags.GEOID_HEIGHT);
        builder.addAttribute(String        .class).setName(Tags.NAME);
        builder.addAttribute(String        .class).setName(Tags.COMMENT);
        builder.addAttribute(String        .class).setName(Tags.DESCRIPTION);
        builder.addAttribute(String        .class).setName(Tags.SOURCE);
        builder.addAttribute(OnlineResource.class).setName(Tags.LINK).setMaximumOccurs(Integer.MAX_VALUE);
        builder.addAttribute(String        .class).setName(Tags.SYMBOL);
        builder.addAttribute(String        .class).setName(Tags.TYPE);
        builder.addAttribute(Fix           .class).setName(Tags.FIX);
        builder.addAttribute(Integer       .class).setName(Tags.SATELITTES);
        builder.addAttribute(Double        .class).setName(Tags.HDOP);
        builder.addAttribute(Double        .class).setName(Tags.VDOP);
        builder.addAttribute(Double        .class).setName(Tags.PDOP);
        builder.addAttribute(Double        .class).setName(Tags.AGE_OF_GPS_DATA);
        builder.addAttribute(Integer       .class).setName(Tags.DGPS_ID);
        wayPoint = builder.build();
        /*
         * http://www.topografix.com/GPX/Route ⇾ GPXEntity
         * ┌─────────────┬────────────────┬────────────────────────┬─────────────┐
         * │ Name        │ Type           │ XML type               │ Cardinality │
         * ├─────────────┼────────────────┼────────────────────────┼─────────────┤
         * │ @identifier │ Integer        │                        │   [1 … 1]   │
         * │ @envelope   │ Envelope       │                        │   [1 … 1]   │
         * │ @geometry   │ Polyline       │                        │   [1 … 1]   │
         * │ name        │ String         │ xsd:string             │   [0 … 1]   │
         * │ cmt         │ String         │ xsd:string             │   [0 … 1]   │
         * │ desc        │ String         │ xsd:string             │   [0 … 1]   │
         * │ src         │ String         │ xsd:string             │   [0 … 1]   │
         * │ link        │ OnlineResource │ gpx:linkType           │   [0 … ∞]   │
         * │ number      │ Integer        │ xsd:nonNegativeInteger │   [0 … 1]   │
         * │ type        │ String         │ xsd:string             │   [0 … 1]   │
         * │ rtept       │ WayPoint       │ gpx:wptType            │   [0 … ∞]   │
         * └─────────────┴────────────────┴────────────────────────┴─────────────┘
         */
        GroupAsPolylineOperation groupOp = new GroupPointsAsPolylineOperation(geomInfo, Tags.ROUTE_POINTS);
        builder = new FeatureTypeBuilder(null, factory, locale).setSuperTypes(parent);
        builder.setDefaultScope(Tags.NAMESPACE).setName("Route");
        builder.addProperty(groupOp);
        builder.addProperty(FeatureOperations.envelope(envpInfo, null, groupOp));
        builder.setDefaultCardinality(0, 1);
        builder.addProperty(wayPoint.getProperty(Tags.NAME));
        builder.addProperty(wayPoint.getProperty(Tags.COMMENT));
        builder.addProperty(wayPoint.getProperty(Tags.DESCRIPTION));
        builder.addProperty(wayPoint.getProperty(Tags.SOURCE));
        builder.addProperty(wayPoint.getProperty(Tags.LINK));
        builder.addAttribute(Integer.class).setName(Tags.NUMBER);
        builder.addProperty(wayPoint.getProperty(Tags.TYPE));
        builder.addAssociation(wayPoint).setName(Tags.ROUTE_POINTS).setMaximumOccurs(Integer.MAX_VALUE);
        route = builder.build();
        /*
         * http://www.topografix.com/GPX/TrackSegment ⇾ GPXEntity
         * ┌─────────────┬──────────┬─────────────┬─────────────┐
         * │ Name        │ Type     │ XML type    │ Cardinality │
         * ├─────────────┼──────────┼─────────────┼─────────────┤
         * │ @identifier │ Integer  │             │   [1 … 1]   │
         * │ @envelope   │ Envelope │             │   [1 … 1]   │
         * │ @geometry   │ Polyline │             │   [1 … 1]   │
         * │ trkpt       │ WayPoint │ gpx:wptType │   [0 … ∞]   │
         * └─────────────┴──────────┴─────────────┴─────────────┘
         */
        groupOp = new GroupPointsAsPolylineOperation(geomInfo, Tags.TRACK_POINTS);
        builder = new FeatureTypeBuilder(null, factory, locale).setSuperTypes(parent);
        builder.setDefaultScope(Tags.NAMESPACE).setName("TrackSegment");
        builder.addProperty(groupOp);
        builder.addProperty(FeatureOperations.envelope(envpInfo, null, groupOp));
        builder.setDefaultCardinality(0, 1);
        builder.addAssociation(wayPoint).setName(Tags.TRACK_POINTS).setMaximumOccurs(Integer.MAX_VALUE);
        trackSegment = builder.build();
        /*
         * http://www.topografix.com/GPX/Track ⇾ GPXEntity
         * ┌─────────────┬────────────────┬────────────────────────┬─────────────┐
         * │ Name        │ Type           │ XML type               │ Cardinality │
         * ├─────────────┼────────────────┼────────────────────────┼─────────────┤
         * │ @identifier │ Integer        │                        │   [1 … 1]   │
         * │ @envelope   │ Envelope       │                        │   [1 … 1]   │
         * │ @geometry   │ Polyline       │                        │   [1 … 1]   │
         * │ name        │ String         │ xsd:string             │   [0 … 1]   │
         * │ cmt         │ String         │ xsd:string             │   [0 … 1]   │
         * │ desc        │ String         │ xsd:string             │   [0 … 1]   │
         * │ src         │ String         │ xsd:string             │   [0 … 1]   │
         * │ link        │ OnlineResource │ gpx:linkType           │   [0 … ∞]   │
         * │ number      │ Integer        │ xsd:nonNegativeInteger │   [0 … 1]   │
         * │ type        │ String         │ xsd:string             │   [0 … 1]   │
         * │ trkseg      │ TrackSegment   │ gpx:trksegType         │   [0 … ∞]   │
         * └─────────────┴────────────────┴────────────────────────┴─────────────┘
         */
        groupOp = new GroupAsPolylineOperation(geomInfo, Tags.TRACK_SEGMENTS);
        builder = new FeatureTypeBuilder(null, factory, locale).setSuperTypes(parent);
        builder.setDefaultScope(Tags.NAMESPACE).setName("Track");
        builder.addProperty(groupOp);
        builder.addProperty(FeatureOperations.envelope(envpInfo, null, groupOp));
        builder.setDefaultCardinality(0, 1);
        builder.addProperty(route.getProperty(Tags.NAME));
        builder.addProperty(route.getProperty(Tags.COMMENT));
        builder.addProperty(route.getProperty(Tags.DESCRIPTION));
        builder.addProperty(route.getProperty(Tags.SOURCE));
        builder.addProperty(route.getProperty(Tags.LINK));
        builder.addProperty(route.getProperty(Tags.NUMBER));
        builder.addProperty(route.getProperty(Tags.TYPE));
        builder.addAssociation(trackSegment).setName(Tags.TRACK_SEGMENTS).setMaximumOccurs(Integer.MAX_VALUE);
        track = builder.build();

        final FeatureCatalogBuilder fc = new FeatureCatalogBuilder(null);
        fc.define(route);
        fc.define(track);
        fc.define(wayPoint);
        metadata = fc.build(true).getContentInfo();
        names = fc.features;
    }
}
