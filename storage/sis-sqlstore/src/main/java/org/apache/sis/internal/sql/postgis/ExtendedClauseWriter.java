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
package org.apache.sis.internal.sql.postgis;

import org.apache.sis.internal.sql.feature.SelectionClauseWriter;

// Branch-dependent imports
import org.opengis.filter.SpatialOperatorName;


/**
 * Converter from filters/expressions to the {@code WHERE} part of SQL statement
 * with PostGIS-specific syntax where useful.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ExtendedClauseWriter extends SelectionClauseWriter {
    /**
     * The unique instance.
     */
    static final ExtendedClauseWriter INSTANCE = new ExtendedClauseWriter();

    /**
     * Creates a new converter from filters/expressions to SQL.
     */
    private ExtendedClauseWriter() {
        super(DEFAULT);
        setFilterHandler(SpatialOperatorName.BBOX, (f,sql) -> {
            writeBinaryOperator(sql, f, " && ");
        });
    }

    /**
     * Creates a new converter initialized to the same handlers than the specified converter.
     *
     * @param  source  the converter from which to copy the handlers.
     */
    private ExtendedClauseWriter(ExtendedClauseWriter source) {
        super(source);
    }

    /**
     * Creates a new converter of the same class than {@code this} and initialized with the same data.
     *
     * @return a converter initialized to a copy of {@code this}.
     */
    @Override
    protected SelectionClauseWriter duplicate() {
        return new ExtendedClauseWriter(this);
    }
}
