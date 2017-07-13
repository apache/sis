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
import java.util.HashMap;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.setup.GeometryLibrary;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.acquisition.GeometryType;
import org.apache.sis.storage.gps.Fix;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.storage.FeatureCatalogBuilder;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.iso.ResourceInternationalString;
import org.apache.sis.util.iso.DefaultNameFactory;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Temporal;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAttributeType;


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
final class Types {
    /**
     * Way point GPX feature type.
     */
    final DefaultFeatureType wayPoint;

    /**
     * Route GPX feature type.
     */
    final DefaultFeatureType route;

    /**
     * Track GPX feature type.
     */
    final DefaultFeatureType track;

    /**
     * Track segment GPX feature type.
     */
    final DefaultFeatureType trackSegment;

    /**
     * The list of feature types to be given to GPC metadata objects.
     */
    final Collection<ContentInformation> metadata;

    /**
     * Binding from names to feature type instances.
     * Shall not be modified after construction.
     */
    final FeatureNaming<DefaultFeatureType> names;

    /**
     * Accessor to the geometry implementation in use (Java2D, ESRI or JTS).
     */
    final Geometries<?> geometries;

    /**
     * A system-wide instance for {@code FeatureType} instances created using the {@link DefaultNameFactory}.
     * This is normally the only instance used in an application.
     */
    static final Types DEFAULT;
    static {
        try {
            DEFAULT = new Types(DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class), null, null);
        } catch (FactoryException | IllegalNameException e) {
            throw new AssertionError(e);        // Should never happen with DefaultNameFactory implementation.
        }
    }

    /**
     * Creates new {@code FeatureTypes} with feature names and property names created using the given factory.
     *
     * @param  factory   the factory to use for creating names, or {@code null} for the default factory.
     * @param  locale    the locale to use for formatting error messages, or {@code null} for the default locale.
     * @param  library   the required geometry library, or {@code null} for the default.
     * @throws FactoryException if an error occurred while creating an "envelope bounds" operation.
     */
    Types(final NameFactory factory, final Locale locale, final GeometryLibrary library)
            throws FactoryException, IllegalNameException
    {
        geometries = Geometries.implementation(library);
        final Map<String,InternationalString[]> resources = new HashMap<>();
        final ScopedName    geomName = AttributeConvention.GEOMETRY_PROPERTY;
        final Map<String,?> geomInfo = Collections.singletonMap(AbstractIdentifiedType.NAME_KEY, geomName);
        final Map<String,?> envpInfo = Collections.singletonMap(AbstractIdentifiedType.NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY);
        /*
         * The parent of all FeatureTypes to be created in this constructor.
         * This parent has a single property, "sis:identifier" of type Integer,
         * which is not part of GPX specification.
         *
         * GPXEntity
         * ┌────────────────┬─────────┬─────────────┐
         * │ Name           │ Type    │ Cardinality │
         * ├────────────────┼─────────┼─────────────┤
         * │ sis:identifier │ Integer │   [1 … 1]   │      SIS-specific property
         * └────────────────┴─────────┴─────────────┘
         */
        final FeatureTypeBuilder builder = new FeatureTypeBuilder(factory, library, locale);
        builder.setNameSpace(Tags.PREFIX).setName("GPXEntity").setAbstract(true);
        builder.addAttribute(Integer.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);
        final DefaultFeatureType parent = builder.build();
        /*
         * WayPoint ⇾ GPXEntity
         * ┌──────────────────┬────────────────┬────────────────────────┬─────────────┐
         * │ Name             │ Type           │ XML type               │ Cardinality │
         * ├──────────────────┼────────────────┼────────────────────────┼─────────────┤
         * │ sis:identifier   │ Integer        │                        │   [1 … 1]   │
         * │ sis:envelope     │ Envelope       │                        │   [1 … 1]   │
         * │ sis:geometry     │ Point          │ (lat,lon) attributes   │   [1 … 1]   │
         * │ ele              │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ time             │ Temporal       │ xsd:dateTime           │   [0 … 1]   │
         * │ magvar           │ Double         │ gpx:degreesType        │   [0 … 1]   │
         * │ geoidheight      │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ name             │ String         │ xsd:string             │   [0 … 1]   │
         * │ cmt              │ String         │ xsd:string             │   [0 … 1]   │
         * │ desc             │ String         │ xsd:string             │   [0 … 1]   │
         * │ src              │ String         │ xsd:string             │   [0 … 1]   │
         * │ link             │ OnlineResource │ gpx:linkType           │   [0 … ∞]   │
         * │ sym              │ String         │ xsd:string             │   [0 … 1]   │
         * │ type             │ String         │ xsd:string             │   [0 … 1]   │
         * │ fix              │ Fix            │ gpx:fixType            │   [0 … 1]   │
         * │ sat              │ Integer        │ xsd:nonNegativeInteger │   [0 … 1]   │
         * │ hdop             │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ vdop             │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ pdop             │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ ageofdgpsdata    │ Double         │ xsd:decimal            │   [0 … 1]   │
         * │ dgpsid           │ Integer        │ gpx:dgpsStationType    │   [0 … 1]   │
         * └──────────────────┴────────────────┴────────────────────────┴─────────────┘
         */
        builder.clear().setSuperTypes(parent).setNameSpace(Tags.PREFIX).setName("WayPoint");
        builder.addAttribute(GeometryType.POINT).setName(geomName)
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
        wayPoint = create(builder, resources);
        /*
         * Route ⇾ GPXEntity
         * ┌────────────────┬────────────────┬────────────────────────┬─────────────┐
         * │ Name           │ Type           │ XML type               │ Cardinality │
         * ├────────────────┼────────────────┼────────────────────────┼─────────────┤
         * │ sis:identifier │ Integer        │                        │   [1 … 1]   │
         * │ sis:envelope   │ Envelope       │                        │   [1 … 1]   │
         * │ sis:geometry   │ Polyline       │                        │   [1 … 1]   │
         * │ name           │ String         │ xsd:string             │   [0 … 1]   │
         * │ cmt            │ String         │ xsd:string             │   [0 … 1]   │
         * │ desc           │ String         │ xsd:string             │   [0 … 1]   │
         * │ src            │ String         │ xsd:string             │   [0 … 1]   │
         * │ link           │ OnlineResource │ gpx:linkType           │   [0 … ∞]   │
         * │ number         │ Integer        │ xsd:nonNegativeInteger │   [0 … 1]   │
         * │ type           │ String         │ xsd:string             │   [0 … 1]   │
         * │ rtept          │ WayPoint       │ gpx:wptType            │   [0 … ∞]   │
         * └────────────────┴────────────────┴────────────────────────┴─────────────┘
         */
        final DefaultAttributeType<?> groupResult = GroupAsPolylineOperation.getResult(geometries);
        GroupAsPolylineOperation groupOp = new GroupAsPolylineOperation(geomInfo, Tags.ROUTE_POINTS, groupResult);
        builder.clear().setSuperTypes(parent).setNameSpace(Tags.PREFIX).setName("Route");
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
        route = create(builder, resources);
        /*
         * TrackSegment ⇾ GPXEntity
         * ┌────────────────┬──────────┬─────────────┬─────────────┐
         * │ Name           │ Type     │ XML type    │ Cardinality │
         * ├────────────────┼──────────┼─────────────┼─────────────┤
         * │ sis:identifier │ Integer  │             │   [1 … 1]   │
         * │ sis:envelope   │ Envelope │             │   [1 … 1]   │
         * │ sis:geometry   │ Polyline │             │   [1 … 1]   │
         * │ trkpt          │ WayPoint │ gpx:wptType │   [0 … ∞]   │
         * └────────────────┴──────────┴─────────────┴─────────────┘
         */
        groupOp = new GroupAsPolylineOperation(geomInfo, Tags.TRACK_POINTS, groupResult);
        builder.clear().setSuperTypes(parent).setNameSpace(Tags.PREFIX).setName("TrackSegment");
        builder.addProperty(groupOp);
        builder.addProperty(FeatureOperations.envelope(envpInfo, null, groupOp));
        builder.setDefaultCardinality(0, 1);
        builder.addAssociation(wayPoint).setName(Tags.TRACK_POINTS).setMaximumOccurs(Integer.MAX_VALUE);
        trackSegment = create(builder, resources);
        /*
         * Track ⇾ GPXEntity
         * ┌────────────────┬────────────────┬────────────────────────┬─────────────┐
         * │ Name           │ Type           │ XML type               │ Cardinality │
         * ├────────────────┼────────────────┼────────────────────────┼─────────────┤
         * │ sis:identifier │ Integer        │                        │   [1 … 1]   │
         * │ sis:envelope   │ Envelope       │                        │   [1 … 1]   │
         * │ sis:geometry   │ Polyline       │                        │   [1 … 1]   │
         * │ name           │ String         │ xsd:string             │   [0 … 1]   │
         * │ cmt            │ String         │ xsd:string             │   [0 … 1]   │
         * │ desc           │ String         │ xsd:string             │   [0 … 1]   │
         * │ src            │ String         │ xsd:string             │   [0 … 1]   │
         * │ link           │ OnlineResource │ gpx:linkType           │   [0 … ∞]   │
         * │ number         │ Integer        │ xsd:nonNegativeInteger │   [0 … 1]   │
         * │ type           │ String         │ xsd:string             │   [0 … 1]   │
         * │ trkseg         │ TrackSegment   │ gpx:trksegType         │   [0 … ∞]   │
         * └────────────────┴────────────────┴────────────────────────┴─────────────┘
         */
        groupOp = new GroupAsPolylineOperation(geomInfo, Tags.TRACK_SEGMENTS, groupResult);
        builder.clear().setSuperTypes(parent).setNameSpace(Tags.PREFIX).setName("Track");
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
        track = create(builder, resources);

        final FeatureCatalogBuilder fc = new FeatureCatalogBuilder(null);
        fc.define(route);
        fc.define(track);
        fc.define(wayPoint);
        metadata = fc.build(true).getContentInfo();
        names = fc.features;
    }

    /**
     * Adds internationalized designation and definition information for all properties in the given type.
     * Then, returns the result of {@link FeatureTypeBuilder#build()}.
     *
     * @param  builder   the feature type builder for which to add designations and definitions.
     * @param  previous  previously created international strings as array of length 2.
     *                   The first element is the designation and the second element is the definition.
     */
    private static DefaultFeatureType create(final FeatureTypeBuilder builder, final Map<String,InternationalString[]> previous) {
        for (final PropertyTypeBuilder p : builder.properties()) {
            final GenericName name = p.getName();
            if (!AttributeConvention.contains(name)) {
                final String key = name.toString();
                InternationalString[] resources = previous.get(key);
                if (resources == null) {
                    resources = new InternationalString[] {
                        new ResourceInternationalString("org.apache.sis.internal.storage.gpx.Designations", key),
                        new ResourceInternationalString("org.apache.sis.internal.storage.gpx.Definitions",  key)
                    };
                    previous.put(key, resources);
                }
                p.setDefinition (resources[1]);
                p.setDesignation(resources[0]);
            }
        }
        return builder.build();
    }
}
