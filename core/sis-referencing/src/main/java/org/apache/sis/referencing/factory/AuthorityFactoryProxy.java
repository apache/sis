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
import org.apache.sis.util.resources.Errors;


/**
 * Delegates object creations to one of the {@code create} methods in a backing {@code AuthorityFactory}.
 * It is possible to use the generic {@link AuthorityFactory#createObject(String)} method instead of this class,
 * but some factories are more efficient when we use the most specific {@code create} method.
 * For example when using a {@linkplain org.apache.sis.referencing.factory.epsg.EPSGFactory},
 * invoking {@link CRSAuthorityFactory#createProjectedCRS(String)} instead of
 * {@code AuthorityFactory.createObject(String)} method reduce the amount of tables to be queried.
 *
 * <p>This class is useful when the same {@code create} method need to be invoked often, but is unknown at compile time.
 * It may also be used as a workaround for authority factories that do not implement the {@code createObject(String)}
 * method.</p>
 *
 * <div class="note"><b>Example:</b>
 * the following code creates a proxy which will delegates its work to the
 * {@link CRSAuthorityFactory#createGeographicCRS createGeographicCRS} method.
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
     * Creates a new proxy for objects of the given type.
     */
    AuthorityFactoryProxy(final Class<T> type) {
        this.type = type;
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
    abstract T create(GeodeticAuthorityFactory factory, String code) throws FactoryException;

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
     * The proxy for the {@link GeodeticAuthorityFactory#createObject} method.
     */
    static final AuthorityFactoryProxy<IdentifiedObject> OBJECT =
        new AuthorityFactoryProxy<IdentifiedObject>(IdentifiedObject.class) {
            @Override IdentifiedObject create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createObject(code);
            }
            @Override IdentifiedObject createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return factory.createObject(code);
            }
    };

    static final AuthorityFactoryProxy<Datum> DATUM =
        new AuthorityFactoryProxy<Datum>(Datum.class) {
            @Override Datum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createDatum(code);
            }
            @Override Datum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createDatum(code);
            }
    };

    static final AuthorityFactoryProxy<EngineeringDatum> ENGINEERING_DATUM =
        new AuthorityFactoryProxy<EngineeringDatum>(EngineeringDatum.class) {
            @Override EngineeringDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createEngineeringDatum(code);
            }
            @Override EngineeringDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createEngineeringDatum(code);
            }
    };

    static final AuthorityFactoryProxy<ImageDatum> IMAGE_DATUM =
        new AuthorityFactoryProxy<ImageDatum>(ImageDatum.class) {
            @Override ImageDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createImageDatum(code);
            }
            @Override ImageDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createImageDatum(code);
            }
    };

    static final AuthorityFactoryProxy<VerticalDatum> VERTICAL_DATUM =
        new AuthorityFactoryProxy<VerticalDatum>(VerticalDatum.class) {
            @Override VerticalDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createVerticalDatum(code);
            }
            @Override VerticalDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createVerticalDatum(code);
            }
    };

    static final AuthorityFactoryProxy<TemporalDatum> TEMPORAL_DATUM =
        new AuthorityFactoryProxy<TemporalDatum>(TemporalDatum.class) {
            @Override TemporalDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createTemporalDatum(code);
            }
            @Override TemporalDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createTemporalDatum(code);
            }
    };

    static final AuthorityFactoryProxy<GeodeticDatum> GEODETIC_DATUM =
        new AuthorityFactoryProxy<GeodeticDatum>(GeodeticDatum.class) {
            @Override GeodeticDatum create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createGeodeticDatum(code);
            }
            @Override GeodeticDatum createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createGeodeticDatum(code);
            }
    };

    static final AuthorityFactoryProxy<Ellipsoid> ELLIPSOID =
        new AuthorityFactoryProxy<Ellipsoid>(Ellipsoid.class) {
            @Override Ellipsoid create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createEllipsoid(code);
            }
            @Override Ellipsoid createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createEllipsoid(code);
            }
    };

    static final AuthorityFactoryProxy<PrimeMeridian> PRIME_MERIDIAN =
        new AuthorityFactoryProxy<PrimeMeridian>(PrimeMeridian.class) {
            @Override PrimeMeridian create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createPrimeMeridian(code);
            }
            @Override PrimeMeridian createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return datumFactory(factory).createPrimeMeridian(code);
            }
    };

    static final AuthorityFactoryProxy<Extent> EXTENT =
        new AuthorityFactoryProxy<Extent>(Extent.class) {
            @Override Extent create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createExtent(code);
            }
            @Override Extent createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return geodeticFactory(factory).createExtent(code);
            }
    };

    static final AuthorityFactoryProxy<CoordinateSystem> COORDINATE_SYSTEM =
        new AuthorityFactoryProxy<CoordinateSystem>(CoordinateSystem.class) {
            @Override CoordinateSystem create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCoordinateSystem(code);
            }
            @Override CoordinateSystem createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createCoordinateSystem(code);
            }
    };

    static final AuthorityFactoryProxy<CartesianCS> CARTESIAN_CS =
        new AuthorityFactoryProxy<CartesianCS>(CartesianCS.class) {
            @Override CartesianCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCartesianCS(code);
            }
            @Override CartesianCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createCartesianCS(code);
            }
    };

    static final AuthorityFactoryProxy<PolarCS> POLAR_CS =
        new AuthorityFactoryProxy<PolarCS>(PolarCS.class) {
            @Override PolarCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createPolarCS(code);
            }
            @Override PolarCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createPolarCS(code);
            }
    };

    static final AuthorityFactoryProxy<CylindricalCS> CYLINDRICAL_CS =
        new AuthorityFactoryProxy<CylindricalCS>(CylindricalCS.class) {
            @Override CylindricalCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCylindricalCS(code);
            }
            @Override CylindricalCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createCylindricalCS(code);
            }
    };

    static final AuthorityFactoryProxy<SphericalCS> SPHERICAL_CS =
        new AuthorityFactoryProxy<SphericalCS>(SphericalCS.class) {
            @Override SphericalCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createSphericalCS(code);
            }
            @Override SphericalCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createSphericalCS(code);
            }
    };

    static final AuthorityFactoryProxy<EllipsoidalCS> ELLIPSOIDAL_CS =
        new AuthorityFactoryProxy<EllipsoidalCS>(EllipsoidalCS.class) {
            @Override EllipsoidalCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createEllipsoidalCS(code);
            }
            @Override EllipsoidalCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createEllipsoidalCS(code);
            }
    };

    static final AuthorityFactoryProxy<VerticalCS> VERTICAL_CS =
        new AuthorityFactoryProxy<VerticalCS>(VerticalCS.class) {
            @Override VerticalCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createVerticalCS(code);
            }
            @Override VerticalCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createVerticalCS(code);
            }
    };

    static final AuthorityFactoryProxy<TimeCS> TIME_CS =
        new AuthorityFactoryProxy<TimeCS>(TimeCS.class) {
            @Override TimeCS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createTimeCS(code);
            }
            @Override TimeCS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createTimeCS(code);
            }
    };

    static final AuthorityFactoryProxy<CoordinateSystemAxis> AXIS =
        new AuthorityFactoryProxy<CoordinateSystemAxis>(CoordinateSystemAxis.class) {
            @Override CoordinateSystemAxis create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCoordinateSystemAxis(code);
            }
            @Override CoordinateSystemAxis createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createCoordinateSystemAxis(code);
            }
    };

    @SuppressWarnings({"rawtypes","unchecked"})
    static final AuthorityFactoryProxy<Unit<?>> UNIT =
        new AuthorityFactoryProxy<Unit<?>>((Class) Unit.class) {
            @Override Unit<?> create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createUnit(code);
            }
            @Override Unit<?> createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return csFactory(factory).createUnit(code);
            }
    };

    static final AuthorityFactoryProxy<CoordinateReferenceSystem> CRS =
        new AuthorityFactoryProxy<CoordinateReferenceSystem>(CoordinateReferenceSystem.class) {
            @Override CoordinateReferenceSystem create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCoordinateReferenceSystem(code);
            }
            @Override CoordinateReferenceSystem createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createCoordinateReferenceSystem(code);
            }
    };

    static final AuthorityFactoryProxy<CompoundCRS> COMPOUND_CRS =
        new AuthorityFactoryProxy<CompoundCRS>(CompoundCRS.class) {
            @Override CompoundCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCompoundCRS(code);
            }
            @Override CompoundCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createCompoundCRS(code);
            }
    };

    static final AuthorityFactoryProxy<DerivedCRS> DERIVED_CRS =
        new AuthorityFactoryProxy<DerivedCRS>(DerivedCRS.class) {
            @Override DerivedCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createDerivedCRS(code);
            }
            @Override DerivedCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createDerivedCRS(code);
            }
    };

    static final AuthorityFactoryProxy<EngineeringCRS> ENGINEERING_CRS =
        new AuthorityFactoryProxy<EngineeringCRS>(EngineeringCRS.class) {
            @Override EngineeringCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createEngineeringCRS(code);
            }
            @Override EngineeringCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createEngineeringCRS(code);
            }
    };

    static final AuthorityFactoryProxy<GeographicCRS> GEOGRAPHIC_CRS =
        new AuthorityFactoryProxy<GeographicCRS>(GeographicCRS.class) {
            @Override GeographicCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createGeographicCRS(code);
            }
            @Override GeographicCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createGeographicCRS(code);
            }
    };

    static final AuthorityFactoryProxy<GeocentricCRS> GEOCENTRIC_CRS =
        new AuthorityFactoryProxy<GeocentricCRS>(GeocentricCRS.class) {
            @Override GeocentricCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createGeocentricCRS(code);
            }
            @Override GeocentricCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createGeocentricCRS(code);
            }
    };

    static final AuthorityFactoryProxy<ImageCRS> IMAGE_CRS =
        new AuthorityFactoryProxy<ImageCRS>(ImageCRS.class) {
            @Override ImageCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createImageCRS(code);
            }
            @Override ImageCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createImageCRS(code);
            }
    };

    static final AuthorityFactoryProxy<ProjectedCRS> PROJECTED_CRS =
        new AuthorityFactoryProxy<ProjectedCRS>(ProjectedCRS.class) {
            @Override ProjectedCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createProjectedCRS(code);
            }
            @Override ProjectedCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createProjectedCRS(code);
            }
    };

    static final AuthorityFactoryProxy<TemporalCRS> TEMPORAL_CRS =
        new AuthorityFactoryProxy<TemporalCRS>(TemporalCRS.class) {
            @Override TemporalCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createTemporalCRS(code);
            }
            @Override TemporalCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createTemporalCRS(code);
            }
    };

    static final AuthorityFactoryProxy<VerticalCRS> VERTICAL_CRS =
        new AuthorityFactoryProxy<VerticalCRS>(VerticalCRS.class) {
            @Override VerticalCRS create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createVerticalCRS(code);
            }
            @Override VerticalCRS createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return crsFactory(factory).createVerticalCRS(code);
            }
    };

    @SuppressWarnings("rawtypes")
    static final AuthorityFactoryProxy<ParameterDescriptor> PARAMETER =
        new AuthorityFactoryProxy<ParameterDescriptor>(ParameterDescriptor.class) {
            @Override ParameterDescriptor<?> create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createParameterDescriptor(code);
            }
            @Override ParameterDescriptor createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return geodeticFactory(factory).createParameterDescriptor(code);
            }
    };

    static final AuthorityFactoryProxy<OperationMethod> METHOD =
        new AuthorityFactoryProxy<OperationMethod>(OperationMethod.class) {
            @Override OperationMethod create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createOperationMethod(code);
            }
            @Override OperationMethod createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return opFactory(factory).createOperationMethod(code);
            }
    };

    static final AuthorityFactoryProxy<CoordinateOperation> OPERATION =
        new AuthorityFactoryProxy<CoordinateOperation>(CoordinateOperation.class) {
            @Override CoordinateOperation create(GeodeticAuthorityFactory factory, String code) throws FactoryException {
                return factory.createCoordinateOperation(code);
            }
            @Override CoordinateOperation createFromAPI(AuthorityFactory factory, String code) throws FactoryException {
                return opFactory(factory).createCoordinateOperation(code);
            }
    };

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
     * The types of proxies. The most specific types must appear first in this list.
     * This field must be declared after all the above constants.
     */
    static final AuthorityFactoryProxy<?>[] PROXIES = new AuthorityFactoryProxy<?>[] {
        OPERATION,
        METHOD,
        PARAMETER,
        PROJECTED_CRS,
        GEOGRAPHIC_CRS,
        GEOCENTRIC_CRS,
        IMAGE_CRS,
        DERIVED_CRS,
        VERTICAL_CRS,
        TEMPORAL_CRS,
        ENGINEERING_CRS,
        COMPOUND_CRS,
        CRS,
        AXIS,
        CARTESIAN_CS,
        ELLIPSOIDAL_CS,
        SPHERICAL_CS,
        CYLINDRICAL_CS,
        POLAR_CS,
        VERTICAL_CS,
        TIME_CS,
        COORDINATE_SYSTEM,
        PRIME_MERIDIAN,
        ELLIPSOID,
        GEODETIC_DATUM,
        IMAGE_DATUM,
        VERTICAL_DATUM,
        TEMPORAL_DATUM,
        ENGINEERING_DATUM,
        DATUM,
        EXTENT,
        UNIT,
        OBJECT
    };
}
