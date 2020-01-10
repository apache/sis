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

import java.util.Map;
import java.util.Optional;
import org.apache.sis.feature.Features;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.internal.filter.NamedFunction;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
public abstract class AbstractSpatialFunction extends NamedFunction implements FeatureExpression {

    public AbstractSpatialFunction(Expression[] parameters) {
        super(parameters);
    }

    protected int getMinParams() {
        return 1;
    }

    protected int getMaxParams() {
        return 1;
    }

    public String getSyntax() {
        final int minparams = getMinParams();
        final int maxparams = getMaxParams();
        final StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append('(');
        for (int i = 0; i < minparams; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("param");
            sb.append(i + 1);
        }
        if (minparams != maxparams) {
            sb.append('[');
            for (int i = minparams; i < maxparams; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append("param");
                sb.append(i + 1);
            }
            sb.append(']');
        }
        sb.append(')');

        return sb.toString();
    }

    protected static CoordinateReferenceSystem expectedCrs(FeatureType type, Expression exp) {
        final PropertyType expressionType = FeatureExpression.expectedType(exp, type, new FeatureTypeBuilder()).build();
        final Optional<AttributeType<?>> attr = Features.toAttribute(expressionType);
        if (attr.isPresent()) {
            final AttributeType<CoordinateReferenceSystem> crsCharacteristic = Features.cast(
                    attr.get().characteristics().get(AttributeConvention.CRS_CHARACTERISTIC),
                    CoordinateReferenceSystem.class);
            return crsCharacteristic == null ? null : crsCharacteristic.getDefaultValue();
        }
        return null;
    }

    protected static void copyCrs(Geometry source, Geometry target) {
        if (source == null || target == null) return;

        final Object userData = source.getUserData();
        CoordinateReferenceSystem crs = null;
        if (userData instanceof CoordinateReferenceSystem) {
            crs = (CoordinateReferenceSystem) userData;
        } else if (userData instanceof Map<?,?>) {
            final Map<?,?> map = (Map<?,?>) userData;
            final Object value = map.get(JTS.CRS_KEY);
            if (value instanceof CoordinateReferenceSystem) {
                crs = (CoordinateReferenceSystem) value;
            }
        }
        target.setUserData(crs);
        target.setSRID(source.getSRID());
    }
}
