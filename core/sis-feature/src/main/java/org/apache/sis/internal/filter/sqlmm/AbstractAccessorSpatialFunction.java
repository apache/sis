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
import org.apache.sis.internal.filter.FilterGeometryUtils;
import org.opengis.filter.expression.Expression;

/**
 * Spatial methods accessing properties of a geometry.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
public abstract class AbstractAccessorSpatialFunction<T extends Geometry> extends AbstractSpatialFunction {

    public AbstractAccessorSpatialFunction(Expression[] parameters) {
        super(parameters);
    }

    protected abstract Class<T> getExpectedClass();

    @Override
    public Object evaluate(Object candidate) {
        Geometry geometry = FilterGeometryUtils.toGeometry(candidate, parameters.get(0));

        if (!getExpectedClass().isInstance(geometry)) {
            return null;
        }

        final Object[] params = new Object[parameters.size()];
        params[0] = geometry;
        for (int i = 1, n = parameters.size(); i < n; i++) {
            params[i] = parameters.get(i).evaluate(candidate);
        }

        try {
            return execute((T) geometry, params);
        } catch (ParseException ex) {
            warning(ex);
        }
        return null;
    }

    public abstract Object execute(T geom, Object... params) throws ParseException;

}
