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
package org.apache.sis.referencing.factory;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import javax.measure.unit.Unit;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.metadata.extent.Extent;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Errors;


/**
 * Delegates object creations to one of the {@code create} methods in a backing {@code AuthorityFactory}.
 * It is possible to use the generic {@link AuthorityFactory#createObject(String)} method instead of this class,
 * but some factories are more efficient when we use the most specific {@code create} method.
 * For example when using a {@linkplain org.apache.sis.referencing.factory.epsg.EPSGDataAccess},
 * invoking {@link GeodeticAuthorityFactory#createProjectedCRS(String)} instead of
 * {@code AuthorityFactory.createObject(String)} method reduce the amount of tables to be queried.
 *
 * <p>This class is useful when the same {@code create} method need to be invoked often, but is unknown at compile time.
 * It may also be used as a workaround for authority factories that do not implement the {@code createObject(String)}
 * method.</p>
 *
 * <div class="note"><b>Example:</b>
 * the following code creates a proxy which will delegates its work to the
 * {@link GeodeticAuthorityFactory#createGeographicCRS createGeographicCRS} method.
 *
 * {@preformat java
 *     String code = ...;
 *     AuthorityFactory factory = ...;
 *     AuthorityFactoryProxy proxy = AuthorityFactoryProxy.getInstance(GeographicCRS.class);
 *     GeographicCRS crs = proxy.create(factory, code); // Invokes factory.createGeographicCRS(code);
 * }</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
abstract class AuthorityFactoryProxy<T> {
    /**
     * The type of objects to be created.
     */
    final Class<T> type;

    /**
     * The type of factory needed for creating objects,
     * as one of the constants defined in {@link AuthorityFactoryIdentifier}.
     */
    final byte factoryType;

    /**
     * Creates a new proxy for objects of the given type.
     */
    AuthorityFactoryProxy(final Class<T> type, final byte factoryType) {
        this.type = type;
        this.factoryType = factoryType;
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return "AuthorityFactoryProxy[" + type.getSimpleName() + ']';
    }

    /**
     * Casts the given factory into a datum authority factory, or throws a {@code FactoryException}
     * if the given factory is not of the expected type.
     */
    final DatumAuthorityFactory datumFactory(final AuthorityFactory factory) throws FactoryException {
        if (factory instanceof DatumAuthorityFactory) {
            return (DatumAuthorityFactory) factory;
        }
        throw factoryNotFound(DatumAuthorityFactory.class);
    }

    /**
     * Casts the given factory into a CS authority factory, or throws a {@code FactoryException}
     * if the given factory is not of the expected type.
     */
    final CSAuthorityFactory csFactory(final AuthorityFactory factory) throws FactoryException {
        if (factory instanceof CSAuthorityFactory) {
            return (CSAuthorityFactory) factory;
        }
        throw factoryNotFound(CSAuthorityFactory.class);
    }

    /**
     * Casts the given factory into a CRS authority factory, or throws a {@code FactoryException}
     * if the given factory is not of the expected type.
     */
    final CRSAuthorityFactory crsFactory(final AuthorityFactory factory) throws FactoryException {
        if (factory instanceof CRSAuthorityFactory) {
            return (CRSAuthorityFactory) factory;
        }
        throw factoryNotFound(CRSAuthorityFactory.class);
    }

    /**
     * Casts the given factory into an operation authority factory, or throws a {@code FactoryException}
     * if the given factory is not of the expected type.
     */
    final CoordinateOperationAuthorityFactory opFactory(final AuthorityFactory factory) throws FactoryException {
        if (factory instanceof CoordinateOperationAuthorityFactory) {
            return (CoordinateOperationAuthorityFactory) factory;
        }
        throw factoryNotFound(CoordinateOperationAuthorityFactory.class);
    }

    /**
     * Casts the given factory into a geodetic authority factory, or throws a {@code FactoryException}
     * if the given factory is not of the expected type.
     */
    final GeodeticAuthorityFactory geodeticFactory(final AuthorityFactory factory) throws FactoryException {
        if (factory instanceof CRSAuthorityFactory) {
            return (GeodeticAuthorityFactory) factory;
        }
        throw factoryNotFound(GeodeticAuthorityFactory.class);
    }

    /**
     * Returns the exception to be thrown when a factory is not found.
     */
    private static FactoryException factoryNotFound(final Class<? extends AuthorityFactory> type) {
        return new FactoryException(Errors.format(Errors.Keys.FactoryNotFound_1, type));
    }

    /**
     * Creates the object for the given code.
     *
     * @param  factory The factory to use for creating the object.
     * @param  code    The code for which to create an object.
     * @return The object created from the given code.
     * @throws FactoryException If an error occurred while creating the object.
     */
    T create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
        return createFromAPI(factory, code);
    }

    /**
     * Creates the object for the given code using only GeoAPI interfaces.
     * This method is slightly less efficient than the above {@link #create} method.
     *
     * @param  factory The factory to use for creating the object.
     * @param  code    The code for which to create an object.
     * @return The object created from the given code.
     * @throws FactoryException If an error occurred while creating the object.
     */
    abstract T createFromAPI(AuthorityFactory factory, String code) throws FactoryException;

    /**
     * The proxy for the {@link GeodeticAuthorityFactory#getDescriptionText(String)} method.
     */
    static final AuthorityFactoryProxy<InternationalString> DESCRIPTION =
        new AuthorityFactoryProxy<InternationalString>(InternationalString.class, AuthorityFactoryIdentifier.ANY) {
            @Override InternationalString createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return factory.getDescriptionText(code);
            }
    };

    /**
     * The proxy for the {@link GeodeticAuthorityFactory#createObject(String)} method.
     */
    static final AuthorityFactoryProxy<IdentifiedObject> OBJECT =
        new AuthorityFactoryProxy<IdentifiedObject>(IdentifiedObject.class, AuthorityFactoryIdentifier.ANY) {
            @Override IdentifiedObject createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return factory.createObject(code);
            }
    };

    static final AuthorityFactoryProxy<Datum> DATUM =
        new AuthorityFactoryProxy<Datum>(Datum.class, AuthorityFactoryIdentifier.DATUM) {
            @Override Datum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createDatum(code);
            }
            @Override Datum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createDatum(code);
            }
    };

    static final AuthorityFactoryProxy<EngineeringDatum> ENGINEERING_DATUM =
        new AuthorityFactoryProxy<EngineeringDatum>(EngineeringDatum.class, AuthorityFactoryIdentifier.DATUM) {
            @Override EngineeringDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createEngineeringDatum(code);
            }
            @Override EngineeringDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createEngineeringDatum(code);
            }
    };

    static final AuthorityFactoryProxy<ImageDatum> IMAGE_DATUM =
        new AuthorityFactoryProxy<ImageDatum>(ImageDatum.class, AuthorityFactoryIdentifier.DATUM) {
            @Override ImageDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createImageDatum(code);
            }
            @Override ImageDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createImageDatum(code);
            }
    };

    static final AuthorityFactoryProxy<VerticalDatum> VERTICAL_DATUM =
        new AuthorityFactoryProxy<VerticalDatum>(VerticalDatum.class, AuthorityFactoryIdentifier.DATUM) {
            @Override VerticalDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createVerticalDatum(code);
            }
            @Override VerticalDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createVerticalDatum(code);
            }
    };

    static final AuthorityFactoryProxy<TemporalDatum> TEMPORAL_DATUM =
        new AuthorityFactoryProxy<TemporalDatum>(TemporalDatum.class, AuthorityFactoryIdentifier.DATUM) {
            @Override TemporalDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createTemporalDatum(code);
            }
            @Override TemporalDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createTemporalDatum(code);
            }
    };

    static final AuthorityFactoryProxy<GeodeticDatum> GEODETIC_DATUM =
        new AuthorityFactoryProxy<GeodeticDatum>(GeodeticDatum.class, AuthorityFactoryIdentifier.DATUM) {
            @Override GeodeticDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createGeodeticDatum(code);
            }
            @Override GeodeticDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createGeodeticDatum(code);
            }
    };

    static final AuthorityFactoryProxy<Ellipsoid> ELLIPSOID =
        new AuthorityFactoryProxy<Ellipsoid>(Ellipsoid.class, AuthorityFactoryIdentifier.DATUM) {
            @Override Ellipsoid create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createEllipsoid(code);
            }
            @Override Ellipsoid createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createEllipsoid(code);
            }
    };

    static final AuthorityFactoryProxy<PrimeMeridian> PRIME_MERIDIAN =
        new AuthorityFactoryProxy<PrimeMeridian>(PrimeMeridian.class, AuthorityFactoryIdentifier.DATUM) {
            @Override PrimeMeridian create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createPrimeMeridian(code);
            }
            @Override PrimeMeridian createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createPrimeMeridian(code);
            }
    };

    static final AuthorityFactoryProxy<Extent> EXTENT =
        new AuthorityFactoryProxy<Extent>(Extent.class, AuthorityFactoryIdentifier.GEODETIC) {
            @Override Extent create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createExtent(code);
            }
            @Override Extent createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return geodeticFactory(factory).createExtent(code);
            }
    };

    static final AuthorityFactoryProxy<CoordinateSystem> COORDINATE_SYSTEM =
        new AuthorityFactoryProxy<CoordinateSystem>(CoordinateSystem.class, AuthorityFactoryIdentifier.CS) {
            @Override CoordinateSystem create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCoordinateSystem(code);
            }
            @Override CoordinateSystem createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createCoordinateSystem(code);
            }
    };

    static final AuthorityFactoryProxy<CartesianCS> CARTESIAN_CS =
        new AuthorityFactoryProxy<CartesianCS>(CartesianCS.class, AuthorityFactoryIdentifier.CS) {
            @Override CartesianCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCartesianCS(code);
            }
            @Override CartesianCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createCartesianCS(code);
            }
    };

    static final AuthorityFactoryProxy<PolarCS> POLAR_CS =
        new AuthorityFactoryProxy<PolarCS>(PolarCS.class, AuthorityFactoryIdentifier.CS) {
            @Override PolarCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createPolarCS(code);
            }
            @Override PolarCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createPolarCS(code);
            }
    };

    static final AuthorityFactoryProxy<CylindricalCS> CYLINDRICAL_CS =
        new AuthorityFactoryProxy<CylindricalCS>(CylindricalCS.class, AuthorityFactoryIdentifier.CS) {
            @Override CylindricalCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCylindricalCS(code);
            }
            @Override CylindricalCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createCylindricalCS(code);
            }
    };

    static final AuthorityFactoryProxy<SphericalCS> SPHERICAL_CS =
        new AuthorityFactoryProxy<SphericalCS>(SphericalCS.class, AuthorityFactoryIdentifier.CS) {
            @Override SphericalCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createSphericalCS(code);
            }
            @Override SphericalCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createSphericalCS(code);
            }
    };

    static final AuthorityFactoryProxy<EllipsoidalCS> ELLIPSOIDAL_CS =
        new AuthorityFactoryProxy<EllipsoidalCS>(EllipsoidalCS.class, AuthorityFactoryIdentifier.CS) {
            @Override EllipsoidalCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createEllipsoidalCS(code);
            }
            @Override EllipsoidalCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createEllipsoidalCS(code);
            }
    };

    static final AuthorityFactoryProxy<VerticalCS> VERTICAL_CS =
        new AuthorityFactoryProxy<VerticalCS>(VerticalCS.class, AuthorityFactoryIdentifier.CS) {
            @Override VerticalCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createVerticalCS(code);
            }
            @Override VerticalCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createVerticalCS(code);
            }
    };

    static final AuthorityFactoryProxy<TimeCS> TIME_CS =
        new AuthorityFactoryProxy<TimeCS>(TimeCS.class, AuthorityFactoryIdentifier.CS) {
            @Override TimeCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createTimeCS(code);
            }
            @Override TimeCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createTimeCS(code);
            }
    };

    static final AuthorityFactoryProxy<CoordinateSystemAxis> AXIS =
        new AuthorityFactoryProxy<CoordinateSystemAxis>(CoordinateSystemAxis.class, AuthorityFactoryIdentifier.CS) {
            @Override CoordinateSystemAxis create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCoordinateSystemAxis(code);
            }
            @Override CoordinateSystemAxis createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createCoordinateSystemAxis(code);
            }
    };

    @SuppressWarnings({"rawtypes","unchecked"})
    static final AuthorityFactoryProxy<Unit<?>> UNIT =
        new AuthorityFactoryProxy<Unit<?>>((Class) Unit.class, AuthorityFactoryIdentifier.CS) {
            @Override Unit<?> create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createUnit(code);
            }
            @Override Unit<?> createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createUnit(code);
            }
    };

    static final AuthorityFactoryProxy<CoordinateReferenceSystem> CRS =
        new AuthorityFactoryProxy<CoordinateReferenceSystem>(CoordinateReferenceSystem.class, AuthorityFactoryIdentifier.CRS) {
            @Override CoordinateReferenceSystem create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCoordinateReferenceSystem(code);
            }
            @Override CoordinateReferenceSystem createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createCoordinateReferenceSystem(code);
            }
    };

    static final AuthorityFactoryProxy<CompoundCRS> COMPOUND_CRS =
        new AuthorityFactoryProxy<CompoundCRS>(CompoundCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override CompoundCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCompoundCRS(code);
            }
            @Override CompoundCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createCompoundCRS(code);
            }
    };

    static final AuthorityFactoryProxy<DerivedCRS> DERIVED_CRS =
        new AuthorityFactoryProxy<DerivedCRS>(DerivedCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override DerivedCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createDerivedCRS(code);
            }
            @Override DerivedCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createDerivedCRS(code);
            }
    };

    static final AuthorityFactoryProxy<EngineeringCRS> ENGINEERING_CRS =
        new AuthorityFactoryProxy<EngineeringCRS>(EngineeringCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override EngineeringCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createEngineeringCRS(code);
            }
            @Override EngineeringCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createEngineeringCRS(code);
            }
    };

    static final AuthorityFactoryProxy<GeographicCRS> GEOGRAPHIC_CRS =
        new AuthorityFactoryProxy<GeographicCRS>(GeographicCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override GeographicCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createGeographicCRS(code);
            }
            @Override GeographicCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createGeographicCRS(code);
            }
    };

    static final AuthorityFactoryProxy<GeocentricCRS> GEOCENTRIC_CRS =
        new AuthorityFactoryProxy<GeocentricCRS>(GeocentricCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override GeocentricCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createGeocentricCRS(code);
            }
            @Override GeocentricCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createGeocentricCRS(code);
            }
    };

    static final AuthorityFactoryProxy<ImageCRS> IMAGE_CRS =
        new AuthorityFactoryProxy<ImageCRS>(ImageCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override ImageCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createImageCRS(code);
            }
            @Override ImageCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createImageCRS(code);
            }
    };

    static final AuthorityFactoryProxy<ProjectedCRS> PROJECTED_CRS =
        new AuthorityFactoryProxy<ProjectedCRS>(ProjectedCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override ProjectedCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createProjectedCRS(code);
            }
            @Override ProjectedCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createProjectedCRS(code);
            }
    };

    static final AuthorityFactoryProxy<TemporalCRS> TEMPORAL_CRS =
        new AuthorityFactoryProxy<TemporalCRS>(TemporalCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override TemporalCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createTemporalCRS(code);
            }
            @Override TemporalCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createTemporalCRS(code);
            }
    };

    static final AuthorityFactoryProxy<VerticalCRS> VERTICAL_CRS =
        new AuthorityFactoryProxy<VerticalCRS>(VerticalCRS.class, AuthorityFactoryIdentifier.CRS) {
            @Override VerticalCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createVerticalCRS(code);
            }
            @Override VerticalCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createVerticalCRS(code);
            }
    };

    @SuppressWarnings("rawtypes")
    static final AuthorityFactoryProxy<ParameterDescriptor> PARAMETER =
        new AuthorityFactoryProxy<ParameterDescriptor>(ParameterDescriptor.class, AuthorityFactoryIdentifier.GEODETIC) {
            @Override ParameterDescriptor<?> create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createParameterDescriptor(code);
            }
            @Override ParameterDescriptor createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return geodeticFactory(factory).createParameterDescriptor(code);
            }
    };

    static final AuthorityFactoryProxy<OperationMethod> METHOD =
        new AuthorityFactoryProxy<OperationMethod>(OperationMethod.class, AuthorityFactoryIdentifier.OPERATION) {
            @Override OperationMethod create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createOperationMethod(code);
            }
            @Override OperationMethod createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return opFactory(factory).createOperationMethod(code);
            }
    };

    static final AuthorityFactoryProxy<CoordinateOperation> OPERATION =
        new AuthorityFactoryProxy<CoordinateOperation>(CoordinateOperation.class, AuthorityFactoryIdentifier.OPERATION) {
            @Override CoordinateOperation create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCoordinateOperation(code);
            }
            @Override CoordinateOperation createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return opFactory(factory).createCoordinateOperation(code);
            }
    };

    /**
     * The list of all proxies. The most specific types must appear first in this array,
     * with a preference for those who are more likely to be requested.
     * This field can be declared only after all the above constants.
     */
    static final AuthorityFactoryProxy<?>[] PROXIES = new AuthorityFactoryProxy<?>[] {
        PROJECTED_CRS,      // Special kind of GeneralDerivedCRS.
        GEOGRAPHIC_CRS,     // Special kind of GeodeticCRS.
        GEOCENTRIC_CRS,     // Special kind of GeodeticCRS.
        VERTICAL_CRS,
        TEMPORAL_CRS,
        IMAGE_CRS,          // Can been seen as a special kind of EngineeringCRS (even if not shown in hierarchy).
        ENGINEERING_CRS,
        DERIVED_CRS,        // DerivedCRS can be also Vertical, Temporal or Engineering CRS. Give precedence to those.
        COMPOUND_CRS,
        CRS,
        GEODETIC_DATUM,
        VERTICAL_DATUM,
        TEMPORAL_DATUM,
        IMAGE_DATUM,        // Can been seen as a special kind of EngineeringDatum (even if not shown in hierarchy).
        ENGINEERING_DATUM,
        DATUM,
        ELLIPSOID,
        PRIME_MERIDIAN,
        CARTESIAN_CS,       // Special case of AffineCS.
        ELLIPSOIDAL_CS,
        SPHERICAL_CS,
        CYLINDRICAL_CS,
        POLAR_CS,
        VERTICAL_CS,
        TIME_CS,
        COORDINATE_SYSTEM,
        AXIS,
        OPERATION,
        METHOD,
        PARAMETER,
        UNIT,
        EXTENT,
        OBJECT,
        DESCRIPTION
    };

    /**
     * The proxy to use for a given type declared in a URN.
     * For example in the {@code "urn:ogc:def:crs:EPSG::4326"} URN, the proxy to use is {@link #CRS}.
     *
     * <p>Keys must be in lower case.</p>
     */
    private static final Map<String, AuthorityFactoryProxy<?>> BY_URN_TYPE;
    static {
        final Map<String, AuthorityFactoryProxy<?>> map = new HashMap<String, AuthorityFactoryProxy<?>>(14);
        map.put("crs",                  CRS);
        map.put("datum",                DATUM);
        map.put("ellipsoid",            ELLIPSOID);
        map.put("meridian",             PRIME_MERIDIAN);
        map.put("cs",                   COORDINATE_SYSTEM);
        map.put("axis",                 AXIS);
        map.put("coordinateoperation",  OPERATION);
        map.put("method",               METHOD);
        map.put("parameter",            PARAMETER);
        map.put("referencesystem",      CRS);
        map.put("uom",                  UNIT);
        BY_URN_TYPE = map;
    }

    /**
     * Returns the instance for the given type. The {@code type} argument can be a GeoAPI interface
     * or some implementation class like {@link org.apache.sis.referencing.crs.DefaultProjectedCRS}.
     * This method returns the most specific proxy for the given type.
     *
     * @param  type The GeoAPI or implementation class.
     * @return The most specific proxy for the given {@code type}.
     * @throws IllegalArgumentException if the type does not implement a valid interface.
     */
    @SuppressWarnings("unchecked")
    static <T> AuthorityFactoryProxy<? super T> getInstance(final Class<T> type)
            throws IllegalArgumentException
    {
        for (final AuthorityFactoryProxy<?> proxy : PROXIES) {
            if (proxy.type.isAssignableFrom(type)) {
                return (AuthorityFactoryProxy<? super T>) proxy;
            }
        }
        throw new IllegalArgumentException(Errors.format(
                Errors.Keys.IllegalClass_2, IdentifiedObject.class, type));
    }

    /**
     * The proxy to use for a given type declared in a URN.
     * For example in the {@code "urn:ogc:def:crs:EPSG::4326"} URN, the proxy to use is {@link #CRS}.
     *
     * @param  typeName The URN type.
     * @return The proxy for the given type, or {@code null} if the given type is illegal.
     */
    @SuppressWarnings("unchecked")
    AuthorityFactoryProxy<? extends T> specialize(final String typeName) {
        final AuthorityFactoryProxy<?> c = BY_URN_TYPE.get(typeName.toLowerCase(Locale.US));
        if (c != null) {
            if (c.type.isAssignableFrom(type)) {
                return this;
            }
            if (type.isAssignableFrom(c.type)) {
                return (AuthorityFactoryProxy<? extends T>) c;
            }
        }
        return null;
    }
}
