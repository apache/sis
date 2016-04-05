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
package org.apache.sis.feature;

import java.util.Collections;
import org.apache.sis.util.Static;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;

/**
 * FeatureType operations utility methods.
 *
 * @author Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class FeatureOperations extends Static {

    /**
     * Create a link operation property type.
     *
     * @param name name of the property
     * @param linkAttributeType linked property type
     * @return Operation
     */
    public static Operation link(GenericName name, PropertyType linkAttributeType){
        return new LinkOperation(Collections.singletonMap("name", name), linkAttributeType);
    }

    /**
     * Create an aggregation operation property type.
     *
     * @param name name of the property
     * @param prefix prefix of the resulting aggregated string
     * @param suffix suffix of the resulting aggregated string
     * @param separator separator between each value
     * @param aggAttributeNames aggregated attribute values
     * @return Operation
     */
    public static Operation aggregate(GenericName name, String prefix, String suffix, String separator, GenericName ... aggAttributeNames){
        return new AggregateOperation(Collections.singletonMap("name", name), prefix, suffix, separator, aggAttributeNames);
    }

    /**
     * Create a calculate bounds operation type.
     *
     * @param name name of the property
     * @param baseCrs created envelope crs
     * @return Operation
     */
    public static Operation bounds(GenericName name, CoordinateReferenceSystem baseCrs){
        return new BoundsOperation(Collections.singletonMap("name", name), baseCrs);
    }

}
