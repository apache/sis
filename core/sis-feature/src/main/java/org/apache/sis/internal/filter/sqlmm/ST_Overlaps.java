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
package org.apache.sis.internal.filter.sqlmm;

import org.locationtech.jts.geom.Geometry;
import java.text.ParseException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;

/**
 * SQL/MM, ISO/IEC 13249-3:2011, ST_Overlaps. <br>
 * Test if an ST_Geometry value spatially 2D overlaps another ST_Geometry value, ignoring z and m coordinate
 * values in the calculations.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
final class ST_Overlaps extends AbstractBinarySpatialFunction {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3322606303065731156L;

    public static final String NAME = "ST_Overlaps";

    public ST_Overlaps(Expression... parameters) {
        super(parameters);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object execute(Geometry left, Geometry right, Object... params) throws ParseException {
        return left.overlaps(right);
    }

    @Override
    public PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo) {
        return addTo.addAttribute(Boolean.class).setName(NAME);
    }

}
