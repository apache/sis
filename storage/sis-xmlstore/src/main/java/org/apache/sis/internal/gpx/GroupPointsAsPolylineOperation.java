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
import org.apache.sis.feature.DefaultAttributeType;

// Branch-dependent imports
import org.opengis.feature.AttributeType;


/**
 * A calculated attribute that define a Polyline geometry calculated
 * from other attributes of the feature.
 *
 * For example : a boat that record it's position every hour.
 * each record is available in a 0-N associate attribute.
 * This class while extract each position and create a line as a new attribute.
 * Any change applied to the positions will be visible on the line.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class GroupPointsAsPolylineOperation extends GroupPolylinesOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5169104838093353092L;

    private static final AttributeType<Polyline> TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, "Polyline"), Polyline.class, 1, 1, null);

    /**
     *
     * @param identification operation identification parameters
     * @param attributePath names of the properties to group
     */
    GroupPointsAsPolylineOperation(Map<String,?> identification, String... attributePath) {
        super(identification, attributePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeType<Polyline> getResult() {
        return TYPE;
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
