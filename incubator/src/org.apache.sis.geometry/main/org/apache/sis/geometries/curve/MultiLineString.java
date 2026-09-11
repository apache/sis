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
package org.apache.sis.geometries.curve;

import java.util.Set;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.internal.shared.DefaultMultiLineString;
import org.apache.sis.geometries.mesh.MeshPrimitive;


/**
 * A MultiLineString is a MultiCurve whose elements are LineStrings.
 *
 * @author Johann Sorel (Geomatys)
 */
public sealed interface MultiLineString extends MultiCurve<LineString>
        permits DefaultMultiLineString,
                MeshPrimitive.Lines
{

    static final String TYPE = "MULTILINESTRING";

    @Override
    default String getGeometryType() {
        return TYPE;
    }

    /**
     * Returns {@link GeometryType#LINE}: all the elements of this collection are line strings.
     *
     * @see ISO 19107:2019 - 6.4.31.2
     */
    @Override
    default Set<GeometryType> getElementType() {
        return Set.of(GeometryType.LINE);
    }

}
