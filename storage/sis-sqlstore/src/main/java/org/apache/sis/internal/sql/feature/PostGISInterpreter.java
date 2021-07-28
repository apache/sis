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
package org.apache.sis.internal.sql.feature;

// Branch-dependent imports
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.apache.sis.feature.Features;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.util.Constants;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.SpatialOperator;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.apache.sis.internal.sql.feature.PostGISSpatialFilterAdapter.LOGGER;


/**
 * Use PostGIS-specific syntax in SQL statements where appropriate.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class PostGISInterpreter extends ANSIInterpreter {

    PostGISInterpreter(final FeatureType target) {
        this(new PostGISSpatialFilterAdapter(init(target)));
    }

    PostGISInterpreter(UnaryOperator<Filter<Feature>> filterAdapter) {
        super(filterAdapter);
    }

    /**
     * Appends the SQL fragment to use for {@link SpatialOperatorName#BBOX} type of filter.
     * The filter encoding specification defines BBOX as a filter between envelopes.
     * The default ANSI interpreter performs a standard intersection between geometries,
     * which is not compliant. PostGIS has its own BBOX operator.
     *
     * @see <a href="https://postgis.net/docs/geometry_overlaps.html">Geometry overlapping</a>
     */
    @Override
    void bbox(final StringBuilder sb, final SpatialOperator<Feature> filter) {
        join(sb, filter, " && ");
    }

    /**
     * Creates a functor capable of searching the CRS associated to a given value reference. For now, the returned
     * function will only search in the target feature type. However, providing a functor allows to evolve it without
     * breaking user API.
     */
    private static Function<ValueReference<Feature, ?>, Optional<CoordinateReferenceSystem>> init(final FeatureType target) {
        if (target == null) return it -> Optional.empty();
        return it -> searchForPropertyCrs(target, it);
    }

    private static Optional<CoordinateReferenceSystem> searchForPropertyCrs(final FeatureType target, final ValueReference<Feature, ?> ref) {
        return searchReference(target, ref)
                .flatMap(Features::toAttribute)
                .map(attr -> attr.characteristics().get(AttributeConvention.CRS_CHARACTERISTIC.toString()))
                .map(it -> {
                    final Object value = it.getDefaultValue();
                    return (value instanceof CoordinateReferenceSystem) ? (CoordinateReferenceSystem) value : null;
                });
    }

    private static Optional<PropertyType> searchReference(final FeatureType target, final ValueReference<Feature, ?> ref) {
        final String xPath = ref.getXPath();
        if (xPath == null) return Optional.empty();
        final PropertyType brutSearch = searchProperty(target, xPath);
        if (brutSearch != null) return Optional.of(brutSearch);

        /* TODO: find a more robust strategy to find the property related to a value reference
         * Notes:
         *  - In the specific case of an SQL based property name, given XPath could be of the form
         *    <table>.<attribute>. We try to get it.
         *  - Another "tweak" is that it is a common pattern to get a name of the form <namespace>:<value>.
         *  - Other use-cases are not supported yet (Ex: a real xpath -> association1/association2/attribute).
         */
        return Stream.of('.', ':')
                .mapToInt(sep -> xPath.lastIndexOf(sep))
                .filter(it -> it >= 0 && it < xPath.length())
                .mapToObj(idx -> xPath.substring(idx+1))
                .map(name -> searchProperty(target, name))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static PropertyType searchProperty(FeatureType datatype, String propertyName) {
        try {
            return datatype.getProperty(propertyName);
        } catch (PropertyNotFoundException e) {
            LOGGER.log(Level.FINER, e, () -> String.format("No property %s in data type %s", propertyName, datatype.getName()));
            return null;
        }
    }
}
