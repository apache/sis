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
package org.apache.sis.internal.gpx;

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

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.AttributeType;
import org.opengis.feature.MultiValuedPropertyException;


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
 * @since   0.8
 * @version 0.8
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
    private static final AttributeType<Polyline> RESULT_TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY), Polyline.class, 1, 1, null);

    private final String[] path;

    /**
     *
     * @param identification operation identification parameters
     * @param attributePath names of the properties to group
     */
    GroupAsPolylineOperation(Map<String,?> identification, String... attributePath) {
        super(identification);
        this.path = attributePath;
    }

    /**
     * Returns an empty parameter descriptor group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AttributeType<Polyline> getResult() {
        return RESULT_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        return new GeomAtt(feature);
    }

    void addGeometry(Polyline geom, final Object propVal, final boolean first) {
        geom.add((Polyline) propVal, false);
    }

    final boolean explore(final Feature att, final int depth, Polyline geom, boolean first) {
        if (depth == path.length - 1) {
            // We are on the field that hold the geometry points
            for (final Object propVal : asCollection(att, path[depth])) {
                addGeometry(geom, propVal, first);
                first = false;
            }
        } else {
            // Explore children
            int d = depth + 1;
            for (final Object prop : asCollection(att, path[depth])) {
                final Feature child = (Feature) prop;
                first = explore(child, d, geom, first);
            }
        }
        return first;
    }

    private static Collection<?> asCollection(Feature att, String property) {
        final Object value = att.getPropertyValue(property);
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection<?>) {
            return (Collection<?>) value;
        }
        return Collections.singletonList(value);
    }


    /**
     * Operation attribute.
     * Value is calculated each time it is accessed.
     */
    private final class GeomAtt extends AbstractAttribute<Polyline> {

        private static final long serialVersionUID = -8872834506769732436L;

        private final Feature feature;

        GeomAtt(final Feature feature) {
            super(getResult());
            this.feature = feature;
        }

        @Override
        public Polyline getValue() throws MultiValuedPropertyException {
            final Polyline geom = new Polyline();
            explore(feature, 0, geom, true);
            return geom;
        }

        @Override
        public void setValue(Polyline value) {
            throw new UnsupportedOperationException("Operation attribute can not be set.");
        }
    }
}
