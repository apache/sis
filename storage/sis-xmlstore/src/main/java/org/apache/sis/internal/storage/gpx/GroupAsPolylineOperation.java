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
import java.util.Collection;
import java.util.Collections;
import com.esri.core.geometry.Polyline;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.feature.AbstractAttribute;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.FeatureUtilities;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;


/**
 * Creates a single (Multi){@code Polyline} instance from a sequence of points or polylines stored in another property.
 * This base class expects a sequence of {@link Polyline} as input, but subclass will expect other kind of geometries.
 * The single (Multi){@code Polyline} instance is re-computed every time this property is requested.
 *
 * <div class="note"><b>Example:</b>
 * a boat that record track every hour.
 * The list of all tracks is stored in an attribute with [0 … ∞] cardinality.
 * This class will extract each track and create a polyline as a new attribute.
 * Any change applied to the tracks will be visible on the polyline.
 * </div>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
class GroupAsPolylineOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7898989085371304159L;

    /**
     * The parameter descriptor for the "Group polylines" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = FeatureUtilities.parameters("GroupPolylines");

    /**
     * The type of the values computed by this operation. The name of this type presumes
     * that the result will be assigned to the "geometry" attribute of the feature type.
     */
    static final DefaultAttributeType<Polyline> RESULT_TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY), Polyline.class, 1, 1, null);

    /**
     * Name of the property to follow in order to get the geometries to add to a polyline.
     * This property shall be a feature association, usually with [0 … ∞] cardinality.
     */
    final String association;

    /**
     * Creates a new operation which will look for geometries in the given feature association.
     *
     * @param  identification  name and other information to be given to this operation.
     * @param  association     name of the property to follow in order to get the geometries to add to a polyline.
     */
    GroupAsPolylineOperation(final Map<String,?> identification, final String association) {
        super(identification);
        this.association = association;
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
    public final DefaultAttributeType<Polyline> getResult() {
        return RESULT_TYPE;
    }

    /**
     * Executes the operation on the specified feature with the specified parameters.
     * If the geometries have changed since last time this method has been invoked,
     * the result will be recomputed.
     */
    @Override
    public final Object apply(AbstractFeature feature, ParameterValueGroup parameters) {
        return new Result(feature);
    }

    /**
     * Invoked for every geometric objects to put in a single polyline.
     *
     * @param addTo     where to add the geometry object.
     * @param geometry  the point or polyline to add to {@code addTo}.
     * @param isFirst   whether {@code geometry} is the first object added to the given polyline.
     */
    void addGeometry(final Polyline addTo, final Object geometry, final boolean isFirst) {
        addTo.add((Polyline) geometry, false);
    }


    /**
     * The attribute resulting from execution if the {@link GroupAsPolylineOperation}.
     * The value is computed when first requested, then cached for this {@code Result} instance only.
     * Note that the cache is not used when {@link #apply(Feature, ParameterValueGroup)} is invoked,
     * causing a new value to be computed again. The intend is to behave as if the operation has been
     * executed at {@code apply(…)} invocation time, even if we deferred the actual execution.
     */
    private final class Result extends AbstractAttribute<Polyline> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -8872834506769732436L;

        /**
         * The feature on which to execute the operation.
         */
        private final AbstractFeature feature;

        /**
         * The result, computed when first needed.
         */
        private transient Polyline geometry;

        /**
         * Creates a new result for an execution on the given feature.
         * The actual computation is deferred to the first call of {@link #getValue()}.
         */
        Result(final AbstractFeature feature) {
            super(RESULT_TYPE);
            this.feature = feature;
        }

        /**
         * Computes the geometry from all points of polylines found in the associated feature.
         */
        @Override
        public Polyline getValue() {
            if (geometry == null) {
                boolean isFirst = true;
                geometry = new Polyline();
                for (final Object child : (Collection<?>) feature.getPropertyValue(association)) {
                    addGeometry(geometry, ((AbstractFeature) child).getPropertyValue("@geometry"), isFirst);
                    isFirst = false;
                }
            }
            return geometry;
        }

        /**
         * Does not allow modification of this attribute.
         */
        @Override
        public void setValue(Polyline value) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, AbstractAttribute.class));
        }
    }
}
