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

import java.util.Objects;

import org.apache.sis.internal.metadata.sql.SQLBuilder;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * A column reference. Specify name of the column, and optionally an alias to use for public visibility.
 * By default, column has no alias. To create a column with an alias, use {@code ColumnRef myCol = new ColumnRef("colName).as("myAlias");}
 *
 * @author Alexis Manin (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
final class ColumnRef {
    private final String name;
    private final String alias;
    private final String attrName;

    ColumnRef(String name) {
        ensureNonNull("Column name", name);
        this.name = this.attrName = name;
        alias = null;
    }

    private ColumnRef(final String name, final String alias) {
        ensureNonNull("Column alias", alias);
        this.name = name;
        this.alias = this.attrName = alias;
    }

    public ColumnRef as(final String alias) {
        if (Objects.equals(alias, this.alias)) return this;
        else if (alias == null || alias.equals(name)) return new ColumnRef(name);
        return new ColumnRef(name, alias);
    }

    public SQLBuilder append(final SQLBuilder target) {
        target.appendIdentifier(name);
        if (alias != null) {
            target.append(" AS ").appendIdentifier(alias);
        }

        return target;
    }

    public String getColumnName() { return name; }
    public String getAttributeName() {
        return attrName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnRef)) return false;
        ColumnRef columnRef = (ColumnRef) o;
        return name.equals(columnRef.name) &&
                Objects.equals(alias, columnRef.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, alias);
    }
}
