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
package org.apache.sis.geometries;

import org.opengis.referencing.crs.SingleCRS;
import org.apache.sis.measure.NumberRange;


/**
 * A prism is defined by a base shape (e.g. Polygon or Circle) that is then extruded from some optional lower limit to an upper limit.
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#prism
 */
public interface Prism extends Geometry {

    public static final String TYPE = "PRISM";

    @Override
    public default String getGeometryType() {
        return TYPE;
    }

    @Override
    public default AttributesType getAttributesType() {
        return getBase().getAttributesType();
    }

    /**
     * @return base shape of the prism.
     */
    Geometry getBase();

    /**
     * @return lower and upper limits of the extrusion
     */
    NumberRange<?> getExtrusionRange();

    /**
     * @return the extrusion crs
     */
    SingleCRS getExtrusionCrs();

}
