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
import java.util.Collections;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.FeatureUtilities;

// Branch-dependent imports
import org.opengis.feature.AttributeType;


/**
 * Creates a single (Multi){@code Polyline} instance from a sequence of points stored in another property.
 * This class expects a sequence of {@link Point} as input.
 * The single (Multi){@code Polyline} instance is re-computed every time this property is requested.
 *
 * <div class="note"><b>Example:</b>
 * a boat that record it's position every hour.
 * The list of all positions is stored in an attribute with [0 … ∞] cardinality.
 * This class will extract each position and create a line as a new attribute.
 * Any change applied to the positions will be visible on the line.
 * </div>
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class GroupPointsAsPolylineOperation extends GroupAsPolylineOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5169104838093353092L;

    /**
     * The parameter descriptor for the "Group points" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = FeatureUtilities.parameters("GroupPoints");

    /**
     * The type of the values computed by this operation. The name of this type presumes
     * that the result will be assigned to the "geometry" attribute of the feature type.
     */
    private static final AttributeType<Polyline> RESULT_TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, AttributeConvention.ENVELOPE_PROPERTY), Polyline.class, 1, 1, null);

    /**
     *
     * @param identification operation identification parameters
     * @param attributePath names of the properties to group
     */
    GroupPointsAsPolylineOperation(Map<String,?> identification, String... attributePath) {
        super(identification, attributePath);
    }

    /**
     * Returns an empty parameter descriptor group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeType<Polyline> getResult() {
        return RESULT_TYPE;
    }

    @Override
    void addGeometry(Polyline geom, final Object propVal, final boolean first) {
        if (first) {
            geom.startPath(((Point) propVal));
        } else {
            geom.lineTo(((Point) propVal));
        }
    }
}
