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

import org.locationtech.jts.geom.LineString;
import java.text.ParseException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;

/**
 * SQL/MM, ISO/IEC 13249-3:2011, ST_IsRing. <br>
 * Test if an ST_Curve value is a ring, ignoring z and m coordinate values in the calculations.
 *
 * This class should be for all curve types, but JTS only has LineString.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
final class ST_IsRing extends AbstractAccessorSpatialFunction<LineString> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7067326611766377264L;

    public static final String NAME = "ST_IsRing";

    public ST_IsRing(Expression... parameters) {
        super(parameters);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Class<LineString> getExpectedClass() {
        return LineString.class;
    }

    @Override
    public Object execute(LineString geom, Object... params) throws ParseException {
        return geom.isRing();
    }

    @Override
    public PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo) {
        return addTo.addAttribute(Boolean.class).setName(NAME);
    }

}
