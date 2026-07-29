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
package org.apache.sis.storage.coverage;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.sis.coverage.grid.GridCoverage;
import static org.apache.sis.feature.AbstractIdentifiedType.NAME_KEY;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociation;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.ResourceId;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * NOTE : merge with Apache SIS 'org.apache.sis.feature.Features' class.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class FeatureExt {

    public static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.coverage");

    /**
     * TODO remove when AttributeConvention.CRS will exist
     */
    public static final String CRS = "sis:crs";

    /**
     * A test to know if a given property is an SIS convention or not. Return true if
     * the property is NOT marked as an SIS convention, false otherwise.
     */
    public static final Predicate<IdentifiedType> IS_NOT_CONVENTION = p -> !AttributeConvention.contains(p.getName());

    /**
     * Extract the coordinate reference system associated to the primary geometry
     * of input data type.
     *
     * @implNote
     * Primary geometry is determined using {@link #getDefaultGeometry(org.opengis.feature.FeatureType) }.
     *
     * @param type The data type to extract reference system from.
     * @return The CRS associated to the default geometry of this data type, or
     * a null value if we cannot determine what is the primary geometry of the
     * data type. Note that a null value is also returned if a geometry property
     * is found, but no CRS characteristics is associated with it.
     */
    public static CoordinateReferenceSystem getCRS(FeatureType type){
        try {
            return getCRS(getDefaultGeometry(type));
        } catch (IllegalArgumentException|IllegalStateException ex) {
            LOGGER.log(Level.FINE, "Cannot extract CRS from type, cause no default geometry is available", ex);
            //no default geometry property
            return null;
        }
    }

    /**
     * Extract CRS characteristic if it exist.
     *
     * @param type
     * @return CoordinateReferenceSystem or null
     */
    public static CoordinateReferenceSystem getCRS(PropertyType type){
        return getCharacteristicValue(type, CRS, null);
    }

    /**
     * Extract characteristic value if it exist.
     *
     * @param <T> expected value class
     * @param type base type to search in
     * @param charName characteristic name
     * @param defaulValue default value if characteristic is missing or null.
     * @return characteristic value or default value is not found
     */
    public static <T> T getCharacteristicValue(PropertyType type, String charName, T defaulValue){
        while(type instanceof Operation){
            type = (PropertyType) ((Operation)type).getResult();
        }
        if(type instanceof AttributeType){
            final AttributeType at = (AttributeType) ((AttributeType)type).characteristics().get(charName);
            if(at!=null){
                T val = (T) at.getDefaultValue();
                return val==null ? defaulValue : val;
            }
        }
        return defaulValue;
    }

    /**
     * Search for the main geometric property in the given type. We'll search
     * for an SIS convention first (see
     * {@link AttributeConvention#GEOMETRY_PROPERTY}. If no convention is set on
     * the input type, we'll check if it contains a single geometric property.
     * If it's the case, we return it. if multiple geometries are found we throw
     * an exception.
     *
     * @param type The data type to search into.
     * @return The main geometric property we've found.
     * @throws IllegalStateException If we've found more than one geometry.
     */
    public static Optional<PropertyType> getDefaultGeometrySafe(final FeatureType type) throws IllegalStateException {
        PropertyType geometry = null;
        try {
            geometry = getDefaultGeometry(type);
        } catch (PropertyNotFoundException e) {
            // We rely on exception instead of `FeatureType.hasProperty(String)`
            // because `getDefaultGeometry(type)` tests many alternatives.
        }
        return Optional.ofNullable(geometry);
    }

    /**
     * Search for the main geometric property in the given type. We'll search
     * for an SIS convention first (see
     * {@link AttributeConvention#GEOMETRY_PROPERTY}. If no convention is set on
     * the input type, we'll check if it contains a single geometric property.
     * If it's the case, we return it. Otherwise (no or multiple geometries), we
     * throw an exception.
     *
     * @param type The data type to search into.
     * @return The main geometric property we've found.
     * @throws PropertyNotFoundException If no geometric property is available
     * in the given type.
     * @throws IllegalStateException If no convention is set (see
     * {@link AttributeConvention#GEOMETRY_PROPERTY}), and we've found more than
     * one geometry.
     */
    public static PropertyType getDefaultGeometry(final FeatureType type) throws PropertyNotFoundException, IllegalStateException {
        PropertyType geometry;
        try {
            geometry = type.getProperty(AttributeConvention.GEOMETRY);
        } catch (PropertyNotFoundException e) {
            try {
                geometry = searchForGeometry(type);
            } catch (RuntimeException e2) {
                e2.addSuppressed(e);
                throw e2;
            }
        }
        return geometry;
    }

    /**
     * Search for a geometric attribute outside SIS conventions. More accurately,
     * we expect the given type to have a single geometry attribute. If many are
     * found, an exception is thrown.
     *
     * @param type The data type to search into.
     * @return The only geometric property we've found.
     * @throws PropertyNotFoundException If no geometric property is available in
     * the given type.
     * @throws IllegalStateException If we've found more than one geometry.
     */
    private static PropertyType searchForGeometry(final FeatureType type) throws PropertyNotFoundException, IllegalStateException {
        final List<? extends PropertyType> geometries = type.getProperties(true).stream()
                .filter(IS_NOT_CONVENTION)
                .filter(AttributeConvention::isGeometryAttribute)
                .collect(Collectors.toList());

        if (geometries.size() < 1) {
            throw new PropertyNotFoundException("No geometric property can be found outside of sis convention.");
        } else if (geometries.size() > 1) {
            throw new IllegalStateException("Multiple geometries found. We don't know which one to select.");
        } else {
            return geometries.get(0);
        }
    }

    public static Optional<Object> getDefaultGeometryValueSafe(Feature input) throws IllegalStateException {
        try {
            return getDefaultGeometryValue(input);
        } catch (PropertyNotFoundException ex) {}
        return Optional.empty();
    }

    /**
     * Get main geometry property value. The ways this method determines default
     * geometry property are the same as {@link #getDefaultGeometry(org.opengis.feature.FeatureType) }.
     *
     * @param input the feature to extract geometry from.
     * @return Value of the main geometric property of the given feature. The returned
     * optional will be empty only if the feature defines a geometric property, but has
     * no value for it.
     * @throws PropertyNotFoundException If no geometric property is available in
     * the given feature.
     * @throws IllegalStateException If we've found more than one geometry.
     */
    public static Optional<Object> getDefaultGeometryValue(Feature input) throws PropertyNotFoundException, IllegalStateException {
        PropertyType geomType = null;
        Object geometry;
        try{
            geometry = input.getPropertyValue(AttributeConvention.GEOMETRY);
        } catch(PropertyNotFoundException ex) {
            try {
                geomType = FeatureExt.getDefaultGeometry(input.getType());
                geometry = input.getPropertyValue(geomType.getName().toString());
            } catch (RuntimeException e) {
                e.addSuppressed(ex);
                throw e;
            }
        }

        if (geometry instanceof Geometry) {
            //fix for bad readers who do not have crs set on geometries
            Geometry g = (Geometry) geometry;
            CoordinateReferenceSystem crs = Geometries.wrap(geometry).get().getCoordinateReferenceSystem();
            if (crs == null) {
                if (geomType == null) {
                    crs = getCRS(input.getType().getProperty(AttributeConvention.GEOMETRY));
                } else {
                    crs = getCRS(geomType);
                }
                if (crs != null) {
                    g.setUserData(crs);
                }
            }
        }

        return Optional.ofNullable(geometry);
    }

}
