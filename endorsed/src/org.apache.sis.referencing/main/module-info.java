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
 * @version 1.5
 * @since   0.3
 */
module org.apache.sis.referencing {
    requires java.sql;
    requires jakarta.xml.bind;
    requires transitive java.desktop;
    requires transitive org.apache.sis.metadata;

    provides org.apache.sis.metadata.sql.internal.shared.Initializer
        with org.apache.sis.referencing.internal.DatabaseListener;

    provides org.opengis.referencing.RegisterOperations
        with org.apache.sis.referencing.MultiRegisterOperations;

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
        with org.apache.sis.referencing.internal.EPSGFactoryProxyCRS,
             org.apache.sis.referencing.factory.CommonAuthorityFactory;

    uses     org.opengis.referencing.cs.CSAuthorityFactory;
    provides org.opengis.referencing.cs.CSAuthorityFactory
        with org.apache.sis.referencing.internal.EPSGFactoryProxyCS;

    uses     org.opengis.referencing.datum.DatumAuthorityFactory;
    provides org.opengis.referencing.datum.DatumAuthorityFactory
        with org.apache.sis.referencing.internal.EPSGFactoryProxyDatum;

    uses     org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
    provides org.opengis.referencing.operation.CoordinateOperationAuthorityFactory
        with org.apache.sis.referencing.internal.EPSGFactoryProxyCOP;

    // Heavier classes (e.g. having more dependencies) or classes less likely to be used should be last.
    uses     org.opengis.referencing.operation.OperationMethod;
    provides org.opengis.referencing.operation.OperationMethod
        with org.apache.sis.referencing.operation.provider.Affine,
             org.apache.sis.referencing.operation.provider.AxisOrderReversal,
             org.apache.sis.referencing.operation.provider.AxisOrderReversal3D,
             org.apache.sis.referencing.operation.provider.Spherical3Dto2D,
             org.apache.sis.referencing.operation.provider.Spherical2Dto3D,
             org.apache.sis.referencing.operation.provider.Geographic3Dto2D,
             org.apache.sis.referencing.operation.provider.Geographic2Dto3D,
             org.apache.sis.referencing.operation.provider.GeographicOffsets,
             org.apache.sis.referencing.operation.provider.GeographicOffsets2D,
             org.apache.sis.referencing.operation.provider.GeographicAndVerticalOffsets,
             org.apache.sis.referencing.operation.provider.VerticalOffset,
             org.apache.sis.referencing.operation.provider.LongitudeRotation,
             org.apache.sis.referencing.operation.provider.GeocentricTranslation,
             org.apache.sis.referencing.operation.provider.PositionVector7Param,
             org.apache.sis.referencing.operation.provider.CoordinateFrameRotation,
             org.apache.sis.referencing.operation.provider.GeographicToGeocentric,
             org.apache.sis.referencing.operation.provider.GeocentricToGeographic,
             org.apache.sis.referencing.operation.provider.GeocentricTranslation3D,
             org.apache.sis.referencing.operation.provider.GeocentricTranslation2D,
             org.apache.sis.referencing.operation.provider.PositionVector7Param3D,
             org.apache.sis.referencing.operation.provider.PositionVector7Param2D,
             org.apache.sis.referencing.operation.provider.CoordinateFrameRotation3D,
             org.apache.sis.referencing.operation.provider.CoordinateFrameRotation2D,
             org.apache.sis.referencing.operation.provider.Molodensky,
             org.apache.sis.referencing.operation.provider.AbridgedMolodensky,
             org.apache.sis.referencing.operation.provider.PseudoPlateCarree,
             org.apache.sis.referencing.operation.provider.Equirectangular,
             org.apache.sis.referencing.operation.provider.Mercator1SP,
             org.apache.sis.referencing.operation.provider.Mercator2SP,
             org.apache.sis.referencing.operation.provider.MercatorSpherical,
             org.apache.sis.referencing.operation.provider.PseudoMercator,
             org.apache.sis.referencing.operation.provider.MercatorAuxiliarySphere,
             org.apache.sis.referencing.operation.provider.RegionalMercator,
             org.apache.sis.referencing.operation.provider.MillerCylindrical,
             org.apache.sis.referencing.operation.provider.LambertConformal1SP,
             org.apache.sis.referencing.operation.provider.LambertConformal2SP,
             org.apache.sis.referencing.operation.provider.LambertConformalWest,
             org.apache.sis.referencing.operation.provider.LambertConformalBelgium,
             org.apache.sis.referencing.operation.provider.LambertConformalMichigan,
             org.apache.sis.referencing.operation.provider.LambertCylindricalEqualArea,
             org.apache.sis.referencing.operation.provider.LambertCylindricalEqualAreaSpherical,
             org.apache.sis.referencing.operation.provider.LambertAzimuthalEqualArea,
             org.apache.sis.referencing.operation.provider.LambertAzimuthalEqualAreaSpherical,
             org.apache.sis.referencing.operation.provider.AlbersEqualArea,
             org.apache.sis.referencing.operation.provider.TransverseMercator,
             org.apache.sis.referencing.operation.provider.TransverseMercatorSouth,
             org.apache.sis.referencing.operation.provider.CassiniSoldner,
             org.apache.sis.referencing.operation.provider.HyperbolicCassiniSoldner,
             org.apache.sis.referencing.operation.provider.PolarStereographicA,
             org.apache.sis.referencing.operation.provider.PolarStereographicB,
             org.apache.sis.referencing.operation.provider.PolarStereographicC,
             org.apache.sis.referencing.operation.provider.PolarStereographicNorth,
             org.apache.sis.referencing.operation.provider.PolarStereographicSouth,
             org.apache.sis.referencing.operation.provider.ObliqueStereographic,
             org.apache.sis.referencing.operation.provider.ObliqueMercator,
             org.apache.sis.referencing.operation.provider.ObliqueMercatorCenter,
             org.apache.sis.referencing.operation.provider.ObliqueMercatorTwoPoints,
             org.apache.sis.referencing.operation.provider.ObliqueMercatorTwoPointsCenter,
             org.apache.sis.referencing.operation.provider.Orthographic,
             org.apache.sis.referencing.operation.provider.ModifiedAzimuthalEquidistant,
             org.apache.sis.referencing.operation.provider.AzimuthalEquidistantSpherical,
             org.apache.sis.referencing.operation.provider.EquidistantCylindrical,
             org.apache.sis.referencing.operation.provider.ZonedTransverseMercator,
             org.apache.sis.referencing.operation.provider.Sinusoidal,
             org.apache.sis.referencing.operation.provider.PseudoSinusoidal,
             org.apache.sis.referencing.operation.provider.Polyconic,
             org.apache.sis.referencing.operation.provider.Mollweide,
             org.apache.sis.referencing.operation.provider.Robinson,
             org.apache.sis.referencing.operation.provider.SouthPoleRotation,
             org.apache.sis.referencing.operation.provider.NorthPoleRotation,
             org.apache.sis.referencing.operation.provider.NTv2,
             org.apache.sis.referencing.operation.provider.NTv1,
             org.apache.sis.referencing.operation.provider.NADCON,
             org.apache.sis.referencing.operation.provider.FranceGeocentricInterpolation,
             org.apache.sis.referencing.operation.provider.Interpolation1D,
             org.apache.sis.referencing.operation.provider.SatelliteTracking,
             org.apache.sis.referencing.operation.provider.Wraparound,
             org.apache.sis.referencing.operation.provider.GeocentricToTopocentric,
             org.apache.sis.referencing.operation.provider.GeographicToTopocentric;

    provides org.apache.sis.xml.bind.TypeRegistration
        with org.apache.sis.referencing.internal.ReferencingTypes;

    provides org.apache.sis.xml.bind.AdapterReplacement
        with org.apache.sis.xml.bind.referencing.SC_VerticalCRS;

    provides org.apache.sis.metadata.internal.shared.ReferencingServices
        with org.apache.sis.referencing.internal.ServicesForMetadata;

    exports org.apache.sis.coordinate;
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
    exports org.apache.sis.referencing.operation.transform;

    exports org.apache.sis.referencing.internal.shared to
            org.apache.sis.referencing.gazetteer,
            org.apache.sis.feature,
            org.apache.sis.geometry,                        // In the "incubator" sub-project.
            org.apache.sis.storage,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.geoheif,                 // In the "incubator" sub-project.
            org.apache.sis.storage.earthobservation,
            org.apache.sis.storage.gdal,                    // In the "optional" sub-project.
            org.apache.sis.portrayal,
            org.apache.sis.console,
            org.apache.sis.openoffice,
            org.apache.sis.gui;                             // In the "optional" sub-project.

    exports org.apache.sis.referencing.operation.provider to
            org.apache.sis.referencing.gazetteer,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.gdal,                    // In the "optional" sub-project.
            org.apache.sis.storage.earthobservation,
            org.apache.sis.profile.japan;

    exports org.apache.sis.xml.bind.referencing to
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
