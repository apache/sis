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
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Spatial methods involving two geometries.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since 2.0
 * @module
 */
public abstract class AbstractBinarySpatialFunction extends AbstractSpatialFunction {

    public AbstractBinarySpatialFunction(Expression[] parameters) {
        super(parameters);
    }

    @Override
    protected int getMinParams() {
        return 2;
    }

    @Override
    protected int getMaxParams() {
        return 2;
    }

    @Override
    public Object evaluate(Object candidate) {
        Geometry left = FilterGeometryUtils.toGeometry(candidate, parameters.get(0));
        Geometry right = FilterGeometryUtils.toGeometry(candidate, parameters.get(1));
        try {
            final Geometry[] geoms = FilterGeometryUtils.toSameCRS(left, right);
            left = geoms[0];
            right = geoms[1];
        } catch (FactoryException ex) {
            return null;
        } catch (TransformException ex) {
            return null;
        }

        if (left == null || right == null) {
            return null;
        }

        final Object[] params = new Object[parameters.size()];
        params[0] = left;
        params[1] = right;
        for (int i = 2, n = parameters.size(); i < n; i++) {
            params[i] = parameters.get(i).evaluate(candidate);
        }

        try {
            return execute(left, right);
        } catch (ParseException ex) {
            warning(ex);
        }
        return null;
    }

    public abstract Object execute(Geometry left, Geometry right, Object... params) throws ParseException;

}
