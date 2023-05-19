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

/**
 * Referencing by coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Maxime Gavens (Geomatys)
 * @version 1.4
 * @since   0.3
 */
module org.apache.sis.referencing {
    requires java.sql;
    requires jakarta.xml.bind;
    requires transitive java.desktop;
    requires transitive org.apache.sis.metadata;

    provides org.apache.sis.internal.metadata.sql.Initializer
        with org.apache.sis.internal.referencing.DatabaseListener;

    provides org.opengis.referencing.crs.CRSFactory
        with org.apache.sis.referencing.factory.GeodeticObjectFactory;

    provides org.opengis.referencing.cs.CSFactory
        with org.apache.sis.referencing.factory.GeodeticObjectFactory;

    provides org.opengis.referencing.datum.DatumFactory
        with org.apache.sis.referencing.factory.GeodeticObjectFactory;

    provides org.opengis.referencing.operation.CoordinateOperationFactory
        with org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;

    provides org.opengis.referencing.operation.MathTransformFactory
        with org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;

    uses     org.opengis.referencing.crs.CRSAuthorityFactory;
    provides org.opengis.referencing.crs.CRSAuthorityFactory
        with org.apache.sis.internal.referencing.EPSGFactoryProxyCRS,
             org.apache.sis.referencing.factory.CommonAuthorityFactory;

    uses     org.opengis.referencing.cs.CSAuthorityFactory;
    provides org.opengis.referencing.cs.CSAuthorityFactory
        with org.apache.sis.internal.referencing.EPSGFactoryProxyCS;

    uses     org.opengis.referencing.datum.DatumAuthorityFactory;
    provides org.opengis.referencing.datum.DatumAuthorityFactory
        with org.apache.sis.internal.referencing.EPSGFactoryProxyDatum;

    uses     org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
    provides org.opengis.referencing.operation.CoordinateOperationAuthorityFactory
        with org.apache.sis.internal.referencing.EPSGFactoryProxyCOP;

    // Heavier classes (e.g. having more dependencies) or classes less likely to be used should be last.
    uses     org.opengis.referencing.operation.OperationMethod;
    provides org.opengis.referencing.operation.OperationMethod
        with org.apache.sis.internal.referencing.provider.Affine,
             org.apache.sis.internal.referencing.provider.AxisOrderReversal,
             org.apache.sis.internal.referencing.provider.AxisOrderReversal3D,
             org.apache.sis.internal.referencing.provider.Geographic3Dto2D,
             org.apache.sis.internal.referencing.provider.Geographic2Dto3D,
             org.apache.sis.internal.referencing.provider.GeographicOffsets,
             org.apache.sis.internal.referencing.provider.GeographicOffsets2D,
             org.apache.sis.internal.referencing.provider.GeographicAndVerticalOffsets,
             org.apache.sis.internal.referencing.provider.VerticalOffset,
             org.apache.sis.internal.referencing.provider.LongitudeRotation,
             org.apache.sis.internal.referencing.provider.GeocentricTranslation,
             org.apache.sis.internal.referencing.provider.PositionVector7Param,
             org.apache.sis.internal.referencing.provider.CoordinateFrameRotation,
             org.apache.sis.internal.referencing.provider.GeographicToGeocentric,
             org.apache.sis.internal.referencing.provider.GeocentricToGeographic,
             org.apache.sis.internal.referencing.provider.GeocentricTranslation3D,
             org.apache.sis.internal.referencing.provider.GeocentricTranslation2D,
             org.apache.sis.internal.referencing.provider.PositionVector7Param3D,
             org.apache.sis.internal.referencing.provider.PositionVector7Param2D,
             org.apache.sis.internal.referencing.provider.CoordinateFrameRotation3D,
             org.apache.sis.internal.referencing.provider.CoordinateFrameRotation2D,
             org.apache.sis.internal.referencing.provider.Molodensky,
             org.apache.sis.internal.referencing.provider.AbridgedMolodensky,
             org.apache.sis.internal.referencing.provider.PseudoPlateCarree,
             org.apache.sis.internal.referencing.provider.Equirectangular,
             org.apache.sis.internal.referencing.provider.Mercator1SP,
             org.apache.sis.internal.referencing.provider.Mercator2SP,
             org.apache.sis.internal.referencing.provider.MercatorSpherical,
             org.apache.sis.internal.referencing.provider.PseudoMercator,
             org.apache.sis.internal.referencing.provider.MercatorAuxiliarySphere,
             org.apache.sis.internal.referencing.provider.RegionalMercator,
             org.apache.sis.internal.referencing.provider.MillerCylindrical,
             org.apache.sis.internal.referencing.provider.LambertConformal1SP,
             org.apache.sis.internal.referencing.provider.LambertConformal2SP,
             org.apache.sis.internal.referencing.provider.LambertConformalWest,
             org.apache.sis.internal.referencing.provider.LambertConformalBelgium,
             org.apache.sis.internal.referencing.provider.LambertConformalMichigan,
             org.apache.sis.internal.referencing.provider.LambertCylindricalEqualArea,
             org.apache.sis.internal.referencing.provider.LambertCylindricalEqualAreaSpherical,
             org.apache.sis.internal.referencing.provider.LambertAzimuthalEqualArea,
             org.apache.sis.internal.referencing.provider.LambertAzimuthalEqualAreaSpherical,
             org.apache.sis.internal.referencing.provider.AlbersEqualArea,
             org.apache.sis.internal.referencing.provider.TransverseMercator,
             org.apache.sis.internal.referencing.provider.TransverseMercatorSouth,
             org.apache.sis.internal.referencing.provider.CassiniSoldner,
             org.apache.sis.internal.referencing.provider.HyperbolicCassiniSoldner,
             org.apache.sis.internal.referencing.provider.PolarStereographicA,
             org.apache.sis.internal.referencing.provider.PolarStereographicB,
             org.apache.sis.internal.referencing.provider.PolarStereographicC,
             org.apache.sis.internal.referencing.provider.PolarStereographicNorth,
             org.apache.sis.internal.referencing.provider.PolarStereographicSouth,
             org.apache.sis.internal.referencing.provider.ObliqueStereographic,
             org.apache.sis.internal.referencing.provider.ObliqueMercator,
             org.apache.sis.internal.referencing.provider.ObliqueMercatorCenter,
             org.apache.sis.internal.referencing.provider.ObliqueMercatorTwoPoints,
             org.apache.sis.internal.referencing.provider.ObliqueMercatorTwoPointsCenter,
             org.apache.sis.internal.referencing.provider.Orthographic,
             org.apache.sis.internal.referencing.provider.ModifiedAzimuthalEquidistant,
             org.apache.sis.internal.referencing.provider.AzimuthalEquidistantSpherical,
             org.apache.sis.internal.referencing.provider.ZonedTransverseMercator,
             org.apache.sis.internal.referencing.provider.Sinusoidal,
             org.apache.sis.internal.referencing.provider.PseudoSinusoidal,
             org.apache.sis.internal.referencing.provider.Polyconic,
             org.apache.sis.internal.referencing.provider.Mollweide,
             org.apache.sis.internal.referencing.provider.SouthPoleRotation,
             org.apache.sis.internal.referencing.provider.NorthPoleRotation,
             org.apache.sis.internal.referencing.provider.NTv2,
             org.apache.sis.internal.referencing.provider.NTv1,
             org.apache.sis.internal.referencing.provider.NADCON,
             org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolation,
             org.apache.sis.internal.referencing.provider.Interpolation1D,
             org.apache.sis.internal.referencing.provider.SatelliteTracking,
             org.apache.sis.internal.referencing.provider.Wraparound,
             org.apache.sis.internal.referencing.provider.GeocentricToTopocentric,
             org.apache.sis.internal.referencing.provider.GeographicToTopocentric;

    provides org.apache.sis.internal.jaxb.TypeRegistration
        with org.apache.sis.internal.referencing.ReferencingTypes;

    provides org.apache.sis.internal.jaxb.AdapterReplacement
        with org.apache.sis.internal.jaxb.referencing.SC_VerticalCRS;

    provides org.apache.sis.internal.metadata.ReferencingServices
        with org.apache.sis.internal.referencing.ServicesForMetadata;

    exports org.apache.sis.geometry;
    exports org.apache.sis.io.wkt;
    exports org.apache.sis.parameter;
    exports org.apache.sis.referencing;
    exports org.apache.sis.referencing.crs;
    exports org.apache.sis.referencing.cs;
    exports org.apache.sis.referencing.datum;
    exports org.apache.sis.referencing.factory;
    exports org.apache.sis.referencing.factory.sql;
    exports org.apache.sis.referencing.operation;
    exports org.apache.sis.referencing.operation.builder;
    exports org.apache.sis.referencing.operation.matrix;
    exports org.apache.sis.referencing.operation.projection;
    exports org.apache.sis.referencing.operation.transform;

    exports org.apache.sis.internal.referencing to
            org.apache.sis.referencing.gazetteer,
            org.apache.sis.feature,
            org.apache.sis.storage,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.earthobservation,
            org.apache.sis.portrayal,
            org.apache.sis.console,
            org.apache.sis.openoffice,
            org.apache.sis.gui;                             // In the "optional" sub-project.

    exports org.apache.sis.internal.referencing.j2d to
            org.apache.sis.referencing.gazetteer,
            org.apache.sis.feature,
            org.apache.sis.storage,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.portrayal,
            org.apache.sis.gui;                             // In the "optional" sub-project.

    exports org.apache.sis.internal.referencing.provider to
            org.apache.sis.referencing.gazetteer,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.earthobservation,
            org.apache.sis.profile.japan;

    exports org.apache.sis.internal.jaxb.referencing to
            org.glassfish.jaxb.runtime,                     // For access to beforeUnmarshal(…).
            org.glassfish.jaxb.core,                        // For access to various classes.
            jakarta.xml.bind;                               // Seems ignored.

    /*
     * Allow JAXB to use reflection for marshalling and
     * unmarshalling Apache SIS objects in XML documents.
     *
     * Opening to implementation is a temporary hack, until
     * we find why opening to Jakarta only is not sufficient.
     */
    opens org.apache.sis.parameter             to jakarta.xml.bind;
    opens org.apache.sis.referencing           to jakarta.xml.bind, org.glassfish.jaxb.core;
    opens org.apache.sis.referencing.cs        to jakarta.xml.bind, org.glassfish.jaxb.core, org.glassfish.jaxb.runtime;
    opens org.apache.sis.referencing.crs       to jakarta.xml.bind, org.glassfish.jaxb.core, org.glassfish.jaxb.runtime;
    opens org.apache.sis.referencing.datum     to jakarta.xml.bind, org.glassfish.jaxb.core, org.glassfish.jaxb.runtime;
    opens org.apache.sis.referencing.operation to jakarta.xml.bind, org.glassfish.jaxb.core, org.glassfish.jaxb.runtime;
}
