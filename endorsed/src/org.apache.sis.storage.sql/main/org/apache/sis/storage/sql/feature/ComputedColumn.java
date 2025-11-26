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
package org.apache.sis.storage.sql.feature;

import java.sql.JDBCType;

// Specific to the main branch:
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.ValueReference;
import org.apache.sis.feature.AbstractFeature;


/**
 * A column which is the result of a computation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ComputedColumn extends Column implements ValueReference<AbstractFeature, Object> {
    /**
     * The <abbr>SQL</abbr> fragment to use for querying this column.
     */
    final String sql;

    /**
     * Creates a column of the given name and type.
     *
     * <h4>API design note</h4>
     * The {@code type} argument is restricted to the {@link JDBCType} enumeration instead of the more generic
     * {@link java.sql.SQLType} interface because we assumes that {@code getVendorTypeNumber()} are constants
     * from the {@link java.sql.Types} class.
     *
     * @param  type  type of the column.
     * @param  name  the column name, also used as the property name.
     * @param  sql   the <abbr>SQL</abbr> fragment to use for querying this column.
     */
    ComputedColumn(final Database<?> database, final JDBCType type, final String name, final String sql) {
        super(type.getVendorTypeNumber(), type.getName(), name);
        this.sql = sql;
        valueGetter = database.getDefaultMapping();
    }

    /**
     * Returns the type of object expected by this expression.
     */
    @Override
    public Class<AbstractFeature> getResourceClass() {
        return AbstractFeature.class;
    }

    /**
     * Returns the name of the property where the value will be stored.
     * This is a simple property name, not an XPath.
     */
    @Override
    public String getXPath() {
        return name;
    }

    /**
     * Fetches the property value from the given feature.
     */
    @Override
    public Object apply(final AbstractFeature instance) {
        return instance.getPropertyValue(name);
    }

    /**
     * Should not be invoked in the context of the <abbr>SQL</abbr> store.
     */
    @Override
    public <N> Expression<AbstractFeature, N> toValueType(final Class<N> type) {
        throw new ClassCastException();
    }
}
