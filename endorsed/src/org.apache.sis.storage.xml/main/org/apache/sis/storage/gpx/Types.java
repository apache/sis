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
package org.apache.sis.storage.gpx;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.time.temporal.Temporal;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.acquisition.GeometryType;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.gps.Fix;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAssociationRole;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.util.iso.DefaultNameFactory;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.FeatureType;
import org.opengis.feature.Operation;


/**
 * Feature types that may appear in GPX files. All values defined in this class are immutable and can be shared
 * by many {@link Reader} instances. There is usually only one {@code Types} instance for a running JVM, but we
 * nevertheless allows definition of alternative {@code Types} with names created by different factories.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Types {
    /**
     * The parent of all other feature types.
     */
    final FeatureType parent;

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
     */
    final Collection<ContentInformation> metadata;

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
            DEFAULT = new Types(DefaultNameFactory.provider(), null, null);
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
        geometries = Geometries.factory(library);
        final var resources = new HashMap<String, InternationalString[]>();
        final Map<String,?> geomInfo = Map.of(AbstractIdentifiedType.NAME_KEY, AttributeConvention.GEOMETRY_PROPERTY);
        final Map<String,?> envpInfo = Map.of(AbstractIdentifiedType.NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY);
        /*
         * The parent of all FeatureTypes to be created in this constructor.
         * This parent has a single property, "sis:identifier" of type Integer,
         * which is not part of GPX specification.
         *
         * GPXEntity
         * ┌────────────────┬─────────┬──────────────┐
         * │ Name           │ Type    │ Multiplicity │
         * ├────────────────┼─────────┼──────────────┤
         * │ sis:identifier │ Integer │   [1 … 1]    │      SIS-specific property
         * └────────────────┴─────────┴──────────────┘
         */
        final var builder = new FeatureTypeBuilder(factory, library, locale);
        builder.setNameSpace(Tags.PREFIX).setName("GPXEntity").setAbstract(true);
        builder.addAttribute(Integer.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);
        parent = builder.build();
        /*
         * WayPoint ⇾ GPXEntity
         * ┌──────────────────┬────────────────┬───────────────────────┬──────────────┐
         * │ Name             │ Type           │ XML type              │ Multiplicity │
         * ├──────────────────┼────────────────┼───────────────────────┼──────────────┤
         * │ sis:identifier   │ Integer        │                       │   [1 … 1]    │
         * │ sis:envelope     │ Envelope       │                       │   [1 … 1]    │
         * │ sis:geometry     │ Point          │ (lat,lon) attributes  │   [1 … 1]    │
         * │ ele              │ Double         │ xs:decimal            │   [0 … 1]    │
         * │ time             │ Temporal       │ xs:dateTime           │   [0 … 1]    │
         * │ magvar           │ Double         │ gpx:degreesType       │   [0 … 1]    │
         * │ geoidheight      │ Double         │ xs:decimal            │   [0 … 1]    │
         * │ name             │ String         │ xs:string             │   [0 … 1]    │
         * │ cmt              │ String         │ xs:string             │   [0 … 1]    │
         * │ desc             │ String         │ xs:string             │   [0 … 1]    │
         * │ src              │ String         │ xs:string             │   [0 … 1]    │
         * │ link             │ OnlineResource │ gpx:linkType          │   [0 … ∞]    │
         * │ sym              │ String         │ xs:string             │   [0 … 1]    │
         * │ type             │ String         │ xs:string             │   [0 … 1]    │
         * │ fix              │ Fix            │ gpx:fixType           │   [0 … 1]    │
         * │ sat              │ Integer        │ xs:nonNegativeInteger │   [0 … 1]    │
         * │ hdop             │ Double         │ xs:decimal            │   [0 … 1]    │
         * │ vdop             │ Double         │ xs:decimal            │   [0 … 1]    │
         * │ pdop             │ Double         │ xs:decimal            │   [0 … 1]    │
         * │ ageofdgpsdata    │ Double         │ xs:decimal            │   [0 … 1]    │
         * │ dgpsid           │ Integer        │ gpx:dgpsStationType   │   [0 … 1]    │
         * └──────────────────┴────────────────┴───────────────────────┴──────────────┘
         */
        builder.clear().setSuperTypes(parent).setNameSpace(Tags.PREFIX).setName("WayPoint");
        builder.addAttribute(GeometryType.POINT).setName(AttributeConvention.GEOMETRY_PROPERTY)
                .setCRS(CommonCRS.WGS84.normalizedGeographic())
                .addRole(AttributeRole.DEFAULT_GEOMETRY);
        builder.setDefaultMultiplicity(0, 1);
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
         * ┌────────────────┬────────────────┬───────────────────────┬──────────────┐
         * │ Name           │ Type           │ XML type              │ Multiplicity │
         * ├────────────────┼────────────────┼───────────────────────┼──────────────┤
         * │ sis:identifier │ Integer        │                       │   [1 … 1]    │
         * │ sis:envelope   │ Envelope       │                       │   [1 … 1]    │
         * │ sis:geometry   │ Polyline       │                       │   [1 … 1]    │
         * │ name           │ String         │ xs:string             │   [0 … 1]    │
         * │ cmt            │ String         │ xs:string             │   [0 … 1]    │
         * │ desc           │ String         │ xs:string             │   [0 … 1]    │
         * │ src            │ String         │ xs:string             │   [0 … 1]    │
         * │ link           │ OnlineResource │ gpx:linkType          │   [0 … ∞]    │
         * │ number         │ Integer        │ xs:nonNegativeInteger │   [0 … 1]    │
         * │ type           │ String         │ xs:string             │   [0 … 1]    │
         * │ rtept          │ WayPoint       │ gpx:wptType           │   [0 … ∞]    │
         * └────────────────┴────────────────┴───────────────────────┴──────────────┘
         */
        Operation groupOp = groupAsPolyline(geomInfo, Tags.ROUTE_POINTS, wayPoint);
        builder.clear().setSuperTypes(parent).setNameSpace(Tags.PREFIX).setName("Route");
        builder.addProperty(groupOp);
        builder.addProperty(FeatureOperations.envelope(envpInfo, null, groupOp));
        builder.setDefaultMultiplicity(0, 1);
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
         * ┌────────────────┬──────────┬─────────────┬──────────────┐
         * │ Name           │ Type     │ XML type    │ Multiplicity │
         * ├────────────────┼──────────┼─────────────┼──────────────┤
         * │ sis:identifier │ Integer  │             │   [1 … 1]    │
         * │ sis:envelope   │ Envelope │             │   [1 … 1]    │
         * │ sis:geometry   │ Polyline │             │   [1 … 1]    │
         * │ trkpt          │ WayPoint │ gpx:wptType │   [0 … ∞]    │
         * └────────────────┴──────────┴─────────────┴──────────────┘
         */
        groupOp = groupAsPolyline(geomInfo, Tags.TRACK_POINTS, wayPoint);
        builder.clear().setSuperTypes(parent).setNameSpace(Tags.PREFIX).setName("TrackSegment");
        builder.addProperty(groupOp);
        builder.addProperty(FeatureOperations.envelope(envpInfo, null, groupOp));
        builder.setDefaultMultiplicity(0, 1);
        builder.addAssociation(wayPoint).setName(Tags.TRACK_POINTS).setMaximumOccurs(Integer.MAX_VALUE);
        trackSegment = create(builder, resources);
        /*
         * Track ⇾ GPXEntity
         * ┌────────────────┬────────────────┬───────────────────────┬──────────────┐
         * │ Name           │ Type           │ XML type              │ Multiplicity │
         * ├────────────────┼────────────────┼───────────────────────┼──────────────┤
         * │ sis:identifier │ Integer        │                       │   [1 … 1]    │
         * │ sis:envelope   │ Envelope       │                       │   [1 … 1]    │
         * │ sis:geometry   │ Polyline       │                       │   [1 … 1]    │
         * │ name           │ String         │ xs:string             │   [0 … 1]    │
         * │ cmt            │ String         │ xs:string             │   [0 … 1]    │
         * │ desc           │ String         │ xs:string             │   [0 … 1]    │
         * │ src            │ String         │ xs:string             │   [0 … 1]    │
         * │ link           │ OnlineResource │ gpx:linkType          │   [0 … ∞]    │
         * │ number         │ Integer        │ xs:nonNegativeInteger │   [0 … 1]    │
         * │ type           │ String         │ xs:string             │   [0 … 1]    │
         * │ trkseg         │ TrackSegment   │ gpx:trksegType        │   [0 … ∞]    │
         * └────────────────┴────────────────┴───────────────────────┴──────────────┘
         */
        groupOp = groupAsPolyline(geomInfo, Tags.TRACK_SEGMENTS, trackSegment);
        builder.clear().setSuperTypes(parent).setNameSpace(Tags.PREFIX).setName("Track");
        builder.addProperty(groupOp);
        builder.addProperty(FeatureOperations.envelope(envpInfo, null, groupOp));
        builder.setDefaultMultiplicity(0, 1);
        builder.addProperty(route.getProperty(Tags.NAME));
        builder.addProperty(route.getProperty(Tags.COMMENT));
        builder.addProperty(route.getProperty(Tags.DESCRIPTION));
        builder.addProperty(route.getProperty(Tags.SOURCE));
        builder.addProperty(route.getProperty(Tags.LINK));
        builder.addProperty(route.getProperty(Tags.NUMBER));
        builder.addProperty(route.getProperty(Tags.TYPE));
        builder.addAssociation(trackSegment).setName(Tags.TRACK_SEGMENTS).setMaximumOccurs(Integer.MAX_VALUE);
        track = create(builder, resources);

        final var fc = new MetadataBuilder();
        fc.addFeatureType(route,    -1);
        fc.addFeatureType(track,    -1);
        fc.addFeatureType(wayPoint, -1);
        metadata = fc.buildAndFreeze().getContentInfo();
    }

    /**
     * Adds internationalized designation and definition information for all properties in the given type.
     * Then, returns the result of {@link FeatureTypeBuilder#build()}.
     *
     * @param  builder   the feature type builder for which to add designations and definitions.
     * @param  previous  previously created international strings as array of length 2.
     *                   The first element is the designation and the second element is the definition.
     */
    private static FeatureType create(final FeatureTypeBuilder builder, final Map<String,InternationalString[]> previous) {
        for (final PropertyTypeBuilder p : builder.properties()) {
            final GenericName name = p.getName();
            if (!AttributeConvention.contains(name)) {
                final InternationalString[] resources = previous.computeIfAbsent(name.toString(), (key) -> new InternationalString[] {
                    new Description("org.apache.sis.storage.gpx.Designations", key),
                    new Description("org.apache.sis.storage.gpx.Definitions",  key)
                });
                p.setDefinition (resources[1]);
                p.setDesignation(resources[0]);
            }
        }
        return builder.build();
    }

    /**
     * Creates a new operation which will group the geometries in the given property into a single polyline.
     *
     * @param geomInfo    the name of the operation, together with optional information.
     * @param components  name of the property providing the geometries to group as a polyline.
     * @param type        type of the property identified by {@code components}.
     */
    private Operation groupAsPolyline(final Map<String,?> geomInfo, final String components, final FeatureType type) {
        var c = new DefaultAssociationRole(Map.of(DefaultAssociationRole.NAME_KEY, components), type, 1, 1);
        return FeatureOperations.groupAsPolyline(geomInfo, geometries.library, c);
    }
}
