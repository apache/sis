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

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.filter.FilterGeometryUtils;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.Expression;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
public abstract class AbstractGeomConstructor extends AbstractSpatialFunction {

    public AbstractGeomConstructor(Expression[] parameters) {
        super(parameters);
    }

    @Override
    protected int getMinParams() {
        return 1;
    }

    @Override
    protected int getMaxParams() {
        return 2;
    }

    protected abstract Class getExpectedClass();

    public String getSyntax() {
        return getName() + "( wkt [,srid] ) or " + getName() + "( wkb [,srid] )";
    }

    @Override
    public Object evaluate(Object candidate) {
        final Geometry geom = FilterGeometryUtils.toGeometry(candidate, parameters.get(0));

        if (parameters.size() == 2) {
            geom.setSRID(((Number) parameters.get(1).evaluate(candidate)).intValue());
        } else if (parameters.size() > 2) {
            warning(new Exception("Unexpected number of arguments : " + parameters.size()));
            return null;
        }

        if (!getExpectedClass().isInstance(geom)) {
            warning(new Exception("Geometry is not of expected type : " + getExpectedClass().getSimpleName()));
            return null;
        }

        return geom;
    }

    @Override
    public PropertyTypeBuilder expectedType(FeatureType valueType, FeatureTypeBuilder addTo) {
        return addTo.addAttribute(getExpectedClass()).setName(getName());
    }
}
