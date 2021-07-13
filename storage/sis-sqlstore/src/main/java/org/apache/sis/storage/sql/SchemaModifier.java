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
package org.apache.sis.storage.sql;

import org.apache.sis.feature.builder.FeatureTypeBuilder;


/**
 * Modifies the feature types inferred from database analysis.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public interface SchemaModifier {
    /**
     * Invoked after analysis of a table.
     * The given builder is initialized with all properties inferred from the table.
     * Implementation of this method can add, remove or modify properties.
     *
     * @param  builder  a feature type builder initialized with all properties inferred by the analysis of a table.
     */
    void editFeatureType(FeatureTypeBuilder builder);
}
