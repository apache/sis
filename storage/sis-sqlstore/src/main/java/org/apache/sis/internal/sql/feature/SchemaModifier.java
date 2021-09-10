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
package org.apache.sis.internal.sql.feature;

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.setup.OptionKey;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * Modifies the feature types inferred from database analysis.
 *
 * @todo May move to public API (in revised form) in a future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public interface SchemaModifier {
    /**
     * Invoked after analysis of a table for allowing modifications of the inferred feature type.
     * The given builder is initialized with all properties inferred from the table definition.
     * Implementation of this method can add, remove or modify properties.
     *
     * <p>The default implementation returns {@code feature.build()} without making any change.</p>
     *
     * @param  table    the catalog (if present), schema (if present) and table name.
     * @param  feature  a feature type builder initialized with all properties inferred by the analysis of a table.
     *                  This builder can be modified in-place.
     * @throws DataStoreException if an error occurred while modifying the feature type.
     * @return the feature type to use for the specified table.
     */
    default FeatureType editFeatureType(TableReference table, FeatureTypeBuilder feature) throws DataStoreException {
        return feature.build();
    }

    /**
     * The option for declaring a schema modifier at {@link org.apache.sis.storage.sql.SQLStore} creation time.
     *
     * @todo if we move this key in public API in the future, then it would be a
     *       value in existing {@link org.apache.sis.storage.DataOptionKey} class.
     */
    OptionKey<SchemaModifier> OPTION = new OptionKey<SchemaModifier>("SCHEMA_MODIFIER", SchemaModifier.class) {
    };
}
