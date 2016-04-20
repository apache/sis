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

import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.sis.feature.AbstractAttribute;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.MultiValuedPropertyException;
import org.opengis.feature.Property;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;


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
 * @since   0.7
 * @version 0.7
 * @module
 */
final class GroupPointsAsPolylineOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5169104838093353092L;

    private static final AttributeType<Polyline> TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, "Polyline"),Polyline.class,1,1,null);

    /**
     * An empty parameter value group for this operation.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS =
            new DefaultParameterDescriptorGroup(Collections.singletonMap("name", "noargs"), 0, 1);

    private final GenericName[] path;

    /**
     *
     * @param identification operation identification parameters
     * @param attributePath names of the properties to group
     */
    GroupPointsAsPolylineOperation(Map<String,?> identification, GenericName... attributePath) {
        super(identification);
        this.path = attributePath;
    }

    /**
     * Returns an empty parameter descriptor group.
     * @return Empty parameter descriptor
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdentifiedType getResult() {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        return new GeomAtt(feature);
    }

    private boolean explore(final Feature att, final int depth, Polyline geom, boolean first) {
        if (depth == path.length - 1) {
            // We are on the field that hold the geometry points
            for (final Object propVal : asCollection(att, path[depth])) {
                if (first) {
                    geom.startPath(((Point) propVal));
                    first = false;
                } else {
                    geom.lineTo(((Point) propVal));
                }
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

    private static Collection<?> asCollection(Feature att, GenericName property) {
        final Object value = att.getPropertyValue(property.toString());
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
            super(TYPE);
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
