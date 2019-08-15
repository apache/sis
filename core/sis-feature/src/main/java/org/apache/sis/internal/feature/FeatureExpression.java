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
package org.apache.sis.internal.feature;

// Branch-dependent imports
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;


/**
 * OGC expressions or other functions operating on feature instances.
 * This interface adds an additional method, {@link #expectedType(FeatureType)},
 * for fetching in advance the expected type of expression results.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface FeatureExpression {
    /**
     * Returns the expected type of values produced by this expression when a feature of the given
     * type is evaluated. The resulting type shall describe a "static" property, i.e. it can be an
     * {@link org.opengis.feature.AttributeType} or a {@link org.opengis.feature.FeatureAssociationRole}
     * but not an {@link org.opengis.feature.Operation}.
     *
     * @param  valueType  the type of features on which to apply this expression.
     * @return expected expression result type.
     * @throws IllegalArgumentException if this method can not determine the property type for the given feature type.
     */
    PropertyType expectedType(FeatureType valueType);
}
