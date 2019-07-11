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
package org.apache.sis.filter;

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * TODO implement all SQL/MM specification functions.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class ST_Transform extends AbstractFunction implements FeatureExpression {

    public static final String NAME = "ST_Transform";

    private final CoordinateReferenceSystem outCrs;

    public ST_Transform(Expression[] parameters) {
        super(NAME, parameters, null);
        if (parameters.length != 2) throw new IllegalArgumentException("Reproject function expect 2 parameters, an expression for the geometry and a literal for the target CRS.");
        if (!(parameters[0] instanceof FeatureExpression)) {
            throw new IllegalArgumentException("First expression must be a FeatureExpression");
        }
        if (!(parameters[1] instanceof Literal)) {
            throw new IllegalArgumentException("Second expression must be a Literal");
        }
        final Object crsObj = parameters[1].evaluate(null);
        if (crsObj instanceof CoordinateReferenceSystem) {
            outCrs = (CoordinateReferenceSystem) crsObj;
        } else if (crsObj instanceof Number) {
            try {
                this.outCrs = CRS.forCode("EPSG:" + crsObj);
            } catch (FactoryException ex) {
                throw new IllegalArgumentException("Requested CRS" + crsObj + "is undefined.\n"+ex.getMessage(), ex);
            }
        } else if (crsObj instanceof String) {
            try {
                this.outCrs = CRS.forCode((String) crsObj);
            } catch (FactoryException ex) {
                throw new IllegalArgumentException("Requested CRS" + crsObj + "is undefined.\n"+ex.getMessage(), ex);
            }
        } else {
            throw new IllegalArgumentException("Second expression must be a Literal with a CRS, Number or String value");
        }
    }

    @Override
    public Object evaluate(Object object) {
        final Expression inGeometry = parameters.get(0);
        Geometry geometry = inGeometry.evaluate(object, Geometry.class);
        if (geometry == null) return null;

        try {
            return JTS.transform(geometry, outCrs);
        } catch (TransformException ex) {
            warning(ex);
        } catch (FactoryException ex) {
            warning(ex);
        }
        return null;
    }

    @Override
    public PropertyType expectedType(FeatureType type) {
        final FeatureExpression inGeometry = (FeatureExpression) parameters.get(0);

        PropertyType expectedType = inGeometry.expectedType(type);
        if (!(expectedType instanceof AttributeType)) {
            throw new IllegalArgumentException("First expression must result in a Geometric attribute");
        }
        AttributeType att = (AttributeType) expectedType;
        if (!Geometry.class.isAssignableFrom(att.getValueClass())) {
            throw new IllegalArgumentException("First expression must result in a Geometric attribute");
        }
        return new FeatureTypeBuilder().addAttribute(att).setCRS(outCrs).build();
    }


}
