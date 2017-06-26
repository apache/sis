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

import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.feature.AbstractAttribute;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.FeatureUtilities;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;


/**
 * Creates a single (Multi){@code Polyline} instance from a sequence of points or polylines stored in another property.
 * This base class expects a sequence of {@code Point} or {@code Polyline} instances as input.
 * The single (Multi){@code Polyline} instance is re-computed every time this property is requested.
 *
 * <div class="note"><b>Examples:</b>
 * <p><i>Polylines created from points:</i>
 * a boat that record it's position every hour.
 * The list of all positions is stored in an attribute with [0 … ∞] cardinality.
 * This class will extract each position and create a line as a new attribute.
 * Any change applied to the positions will be visible on the line.</p>
 *
 * <p><i>Polylines created from other polylines:</i>
 * a boat that record track every hour.
 * The list of all tracks is stored in an attribute with [0 … ∞] cardinality.
 * This class will extract each track and create a polyline as a new attribute.
 * Any change applied to the tracks will be visible on the polyline.</p>
 * </div>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class GroupAsPolylineOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7898989085371304159L;

    /**
     * The parameter descriptor for the "Group polylines" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = FeatureUtilities.parameters("GroupPolylines");

    /**
     * Name of the property to follow in order to get the geometries to add to a polyline.
     * This property shall be a feature association, usually with [0 … ∞] cardinality.
     */
    private final String association;

    /**
     * The expected result type to be returned by {@link #getResult()}.
     */
    private final DefaultAttributeType<?> result;

    /**
     * Creates a new operation which will look for geometries in the given feature association.
     *
     * @param  identification  name and other information to be given to this operation.
     * @param  association     name of the property to follow in order to get the geometries to add to a polyline.
     * @param  result          the expected result type to be returned by {@link #getResult()}.
     */
    GroupAsPolylineOperation(final Map<String,?> identification, final String association, final DefaultAttributeType<?> result) {
        super(identification);
        this.association = association;
        this.result = result;
    }

    /**
     * Creates the {@code result} argument for the constructor. This creation is provided in a separated method
     * because the same instance will be shared by many {@code GroupAsPolylineOperation} instances.
     *
     * @param  geometries  accessor to the geometry implementation in use (Java2D, ESRI or JTS).
     */
    static DefaultAttributeType<?> getResult(final Geometries geometries) {
        return new DefaultAttributeType<>(Collections.singletonMap(NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY),
                geometries.polylineClass, 1, 1, null);
    }

    /**
     * Returns an empty parameter descriptor group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * Returns the expected result type.
     */
    @Override
    public final DefaultAttributeType<?> getResult() {
        return result;
    }

    /**
     * Executes the operation on the specified feature with the specified parameters.
     * If the geometries have changed since last time this method has been invoked,
     * the result will be recomputed.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final Object apply(AbstractFeature feature, ParameterValueGroup parameters) {
        return new Result(feature, association, result);
    }


    /**
     * The attribute resulting from execution if the {@link GroupAsPolylineOperation}.
     * The value is computed when first requested, then cached for this {@code Result} instance only.
     * Note that the cache is not used when {@code apply(Feature, ParameterValueGroup)} is invoked,
     * causing a new value to be computed again. The intend is to behave as if the operation has been
     * executed at {@code apply(…)} invocation time, even if we deferred the actual execution.
     *
     * @param  <G>  the root geometry class (implementation-dependent).
     */
    private static final class Result<G> extends AbstractAttribute<G> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -8872834506769732436L;

        /**
         * The feature on which to execute the operation.
         */
        private final AbstractFeature feature;

        /**
         * Name of the property to follow in order to get the geometries to add to a polyline.
         * This property shall be a feature association, usually with [0 … ∞] cardinality.
         */
        private final String association;

        /**
         * The result, computed when first needed.
         */
        private transient G geometry;

        /**
         * Creates a new result for an execution on the given feature.
         * The actual computation is deferred to the first call of {@link #getValue()}.
         */
        Result(final AbstractFeature feature, final String association, final DefaultAttributeType<G> result) {
            super(result);
            this.feature = feature;
            this.association = association;
        }

        /**
         * Computes the geometry from all points or polylines found in the associated feature.
         */
        @Override
        public G getValue() {
            if (geometry == null) {
                final Iterator<?> it = ((Collection<?>) feature.getPropertyValue(association)).iterator();
                final Object geom = Geometries.mergePolylines(new Iterator<Object>() {
                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public Object next() {
                        return ((AbstractFeature) it.next()).getPropertyValue("sis:geometry");
                    }

                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                });
                geometry = getType().getValueClass().cast(geom);
            }
            return geometry;
        }

        /**
         * Does not allow modification of this attribute.
         */
        @Override
        public void setValue(G value) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, AbstractAttribute.class));
        }
    }
}
