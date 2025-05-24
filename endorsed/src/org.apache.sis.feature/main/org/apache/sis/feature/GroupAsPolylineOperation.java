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
package org.apache.sis.feature;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.EnumMap;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.util.privy.CollectionsExt;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.Operation;


/**
 * Creates a single (Multi){@code Polyline} instance from a sequence of points or polylines stored in another property.
 * This is the implementation of {@link FeatureOperations#groupAsPolyline FeatureOperations.groupAsPolyline(…)}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GroupAsPolylineOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1995248173704801739L;

    /**
     * The parameter descriptor for the "Group polylines" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = parameters("GroupAsPolyline");

    /**
     * Name of the property to follow in order to get the geometries to add to a polyline.
     * This property can be an attribute, operation or feature association,
     * usually with [0 … ∞] multiplicity.
     */
    private final String propertyName;

    /**
     * Whether the property giving components is an association to feature instances.
     */
    private final boolean isFeatureAssociation;

    /**
     * The geometry library.
     */
    private final Geometries<?> geometries;

    /**
     * The {@link #resultType} for each library, created when first needed.
     * Used for sharing the same instance for all operations using the same library.
     */
    private static final EnumMap<GeometryLibrary, DefaultAttributeType<?>> TYPES = new EnumMap<>(GeometryLibrary.class);

    /**
     * Returns an operation which will group into a single geometry all geometries contained in the specified property.
     *
     * @param  identification  the name of the operation, together with optional information.
     * @param  library         the library providing the implementations of geometry objects to read and write.
     * @param  components      attribute, association or operation providing the geometries to group as a polyline.
     */
    static AbstractOperation create(final Map<String,?> identification, final GeometryLibrary library, PropertyType components) {
        if (components instanceof LinkOperation) {
            components = ((LinkOperation) components).result;
        }
        final boolean isFeatureAssociation;
        if (components instanceof AttributeType<?>) {
            if (((AttributeType<?>) components).getMaximumOccurs() <= 1) {
                return new LinkOperation(identification, components);
            }
            isFeatureAssociation = false;
        } else {
            isFeatureAssociation = (components instanceof FeatureAssociationRole);
            if (!isFeatureAssociation) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.IllegalPropertyType_2,
                                                   components.getName(), components.getClass()));
            }
        }
        return new GroupAsPolylineOperation(identification, Geometries.factory(library), components, isFeatureAssociation);
    }

    /**
     * Creates an operation which will group into a single polyline all geometries contained in the specified property.
     * This constructor shall be invoked only after the {@code source} is known to contain collection, i.e. the maximum
     * number of occurrences of attribute values or feature instances is greater than 1.
     */
    private GroupAsPolylineOperation(final Map<String,?> identification, final Geometries<?> geometries,
                                     final PropertyType components, final boolean isFeatureAssociation)
    {
        super(identification);
        this.geometries = geometries;
        this.propertyName = components.getName().toString();
        this.isFeatureAssociation = isFeatureAssociation;
    }

    /**
     * Returns an empty parameter descriptor group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * Returns the names of feature properties that this operation needs for performing its task.
     */
    @Override
    public Set<String> getDependencies() {
        return Set.of(propertyName);
    }

    /**
     * Returns the same operation but using different properties as inputs.
     *
     * @param  dependencies  the new properties to use as operation inputs.
     * @return the new operation, or {@code this} if unchanged.
     */
    @Override
    public Operation updateDependencies(final Map<String, PropertyType> dependencies) {
        final PropertyType target = dependencies.get(propertyName);
        if (target != null) {
            final AbstractOperation op = create(inherit(), geometries.library, target);
            if (!equals(op)) {
                return FeatureOperations.POOL.unique(op);
            }
        }
        return this;
    }

    /**
     * Returns the expected result type.
     */
    @Override
    public final AttributeType<?> getResult() {
        synchronized (TYPES) {
            return TYPES.computeIfAbsent(geometries.library, (library) -> {
                var name = Map.of(AbstractIdentifiedType.NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY);
                var type = geometries.getGeometryClass(GeometryType.LINESTRING);
                return new DefaultAttributeType<>(name, type, 1, 1, null);
            });
        }
    }

    /**
     * Executes the operation on the specified feature.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final Property apply(Feature feature, ParameterValueGroup parameters) {
        return new Result<>(getResult(), feature);
    }


    /**
     * The attribute resulting from execution of the {@link GroupAsPolylineOperation}.
     * The value is computed when first requested, then cached for this {@code Result} instance only.
     * Note that the cache is not used when {@link #apply(Feature, ParameterValueGroup)} is invoked,
     * causing a new value to be computed again. The intent is to behave as if the operation has been
     * executed at {@code apply(…)} invocation time, even if we deferred the actual execution.
     *
     * @param <G> the root geometry class (implementation-dependent).
     */
    private final class Result<G> extends OperationResult<G> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 5558751012506417903L;

        /**
         * The result, computed when first needed.
         */
        private transient G geometry;

        /**
         * Creates a new result for an execution on the given feature.
         * The actual computation is deferred to the first call of {@link #getValue()}.
         */
        Result(final AttributeType<G> resultType, final Feature feature) {
            super(resultType, feature);
        }

        /**
         * Computes the geometry from all points or polylines found in the associated feature.
         *
         * @throws ClassCastException if a feature, a property value or a geometry is not of the expected class.
         */
        @Override
        public G getValue() {
            if (geometry == null) {
                geometry = compute();
            }
            return geometry;
        }

        /**
         * Computes the geometry when first needed.
         */
        private G compute() {
            /*
             * The property value is usually cast directly to `Collection` when the
             * constructor ensured that `Features.getMaximumOccurs(property) > 1`.
             */
            Iterator<?> paths = CollectionsExt.toCollection(feature.getPropertyValue(propertyName)).iterator();
            if (isFeatureAssociation) {
                final Iterator<?> it = paths;
                paths = new Iterator<Object>() {
                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public Object next() {
                        return ((Feature) it.next()).getPropertyValue(AttributeConvention.GEOMETRY);
                    }
                };
            }
            while (paths.hasNext()) {
                GeometryWrapper first = geometries.castOrWrap(paths.next());
                if (first != null) {
                    final Object geom = first.mergePolylines(paths);
                    return getType().getValueClass().cast(geom);
                }
            }
            return null;
        }
    }

    /**
     * Computes a hash-code value for this operation.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + propertyName.hashCode() + geometries.hashCode();
    }

    /**
     * Compares this operation with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (super.equals(obj)) {
            final var that = (GroupAsPolylineOperation) obj;
            return propertyName.equals(that.propertyName) &&
                   geometries.equals(that.geometries);
        }
        return false;
    }
}
