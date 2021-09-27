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

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.system.DefaultFactories;

import static org.apache.sis.internal.sql.feature.Database.WILDCARD;


/**
 * Definition of a resource (table, view or query) to include in a {@link SQLStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ResourceDefinition {
    /**
     * The namespace for table names, created when first needed.
     * Used for specifying the name separator, which is {@code '.'}.
     */
    private static volatile NameSpace tableNS;

    /**
     * The table name or the query name.
     * This field has two meanings, depending on whether {@link #query} is null or not:
     *
     * <ul>
     *   <li>If {@link #query} is null, then this is the fully qualified name (including catalog and schema)
     *       of the table to include in the store. It may contain {@code LIKE} wildcard characters, in which
     *       case all tables matching the pattern will be included.</li>
     *   <li>If {@link #query} is non-null, then this is an arbitrary name to assign to the resource which will
     *       contain the query result.</li>
     * </ul>
     *
     * @see #getName()
     */
    private final GenericName name;

    /**
     * The SQL query to execute for the resource, or {@code null} if the resource is a table or view.
     *
     * @see #getQuery()
     */
    final String query;

    /**
     * Creates a new definition.
     */
    private ResourceDefinition(final GenericName name, final String query) {
        this.name  = name;
        this.query = query;
    }

    /**
     * Wraps the given table names and queries in an array of resource definitions.
     */
    static ResourceDefinition[] wrap(final GenericName[] tableNames, final Map<?,?> queries) {
        final int tableCount = (tableNames != null) ? tableNames.length : 0;
        final ResourceDefinition[] definitions = new ResourceDefinition[
                (queries != null) ? queries.size() + tableCount : tableCount];
        for (int i=0; i<tableCount; i++) {
            final GenericName name = tableNames[i];
            ArgumentChecks.ensureNonNullElement("tables", i, name);
            definitions[i] = new ResourceDefinition(name, null);
        }
        if (queries != null) {
            int i = 0;
            for (final Map.Entry<?,?> entry : queries.entrySet()) {
                /*
                 * Keys should be instances of `GenericName`, but strings are accepted as well
                 * as a convenience for local names.
                 */
                Object p = entry.getKey();
                ArgumentChecks.ensureNonNullElement("queries[#].key", i, p);
                final GenericName name = (p instanceof GenericName) ? (GenericName) p :
                        DefaultFactories.forBuildin(NameFactory.class).createLocalName(null, p.toString());
                /*
                 * Values shall be non-empty strings.
                 */
                p = entry.getValue();
                ArgumentChecks.ensureNonNullElement("queries[#].value", i, p);
                final String sql = p.toString();
                ArgumentChecks.ensureNonEmpty("sql", sql);
                definitions[tableCount + i] = new ResourceDefinition(name, sql);
            }
        }
        return definitions;
    }

    /**
     * Creates a resource definition for a table or a view in any catalog and schema of the database.
     * The table name can contain SQL wildcard characters:
     * {@code '_'} matches any single character and {@code '%'} matches any sequence of characters.
     *
     * @param  tablePattern   pattern (with {@code '_'} and {@code '%'} wildcards) of a table.
     * @return resource definition for the named table.
     */
    public static ResourceDefinition table(final String tablePattern) {
        return table(null, null, tablePattern);
    }

    /**
     * Creates a resource definition for a table or a view in the database.
     * The table name can be any of the followings:
     *
     * <ul>
     *   <li>{@code catalog.schemaPattern.tablePattern}</li>
     *   <li>{@code schemaPattern.tablePattern}</li>
     *   <li>{@code tablePattern}</li>
     * </ul>
     *
     * The schema and table names (but not the catalog) can contain SQL wildcard characters:
     * {@code '_'} matches any single character and {@code '%'} matches any sequence of characters.
     *
     * @param  catalog        name of a catalog as it is stored in the database, or {@code null} for any catalog.
     * @param  schemaPattern  pattern (with {@code '_'} and {@code '%'} wildcards) of a schema, or {@code null} for any schema.
     * @param  tablePattern   pattern (with {@code '_'} and {@code '%'} wildcards) of a table.
     * @return resource definition for the named table.
     */
    @SuppressWarnings("fallthrough")
    public static ResourceDefinition table(final String catalog, String schemaPattern, final String tablePattern) {
        ArgumentChecks.ensureNonNull("tablePattern", tablePattern);
        final int numParts;
        if (catalog != null) {
            numParts = 3;
            if (schemaPattern == null) {
                schemaPattern = WILDCARD;
            }
        } else if (schemaPattern != null && !schemaPattern.equals(WILDCARD)) {
            numParts = 2;
        } else {
            numParts = 1;
        }
        final String[] names = new String[numParts];
        int i = 0;
        switch (numParts) {
            default: throw new AssertionError(numParts);
            case 3: names[i++] = catalog;           // Fall through
            case 2: names[i++] = schemaPattern;     // Fall through
            case 1: names[i]   = tablePattern;
        }
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        NameSpace ns = tableNS;
        if (ns == null) {
            final Map<String,String> properties = new HashMap<>(4);     // TODO: use Map.of with JDK9.
            properties.put("separator",      ".");
            properties.put("separator.head", ":");
            tableNS = ns = factory.createNameSpace(factory.createLocalName(null, "database"), properties);
        }
        return new ResourceDefinition(factory.createGenericName(ns, names), null);
    }

    /**
     * Creates a resource definition for a SQL query.
     * Each column in the query should have a distinct name, using SQL {@code AS} keyword if needed.
     * It is caller's responsibility to ensure that the given query is not subject to SQL injection vulnerability.
     *
     * @param  name   name of the resource.
     * @param  query  the SQL query to execute.
     * @return resource definition for the given SQL query.
     */
    public static ResourceDefinition query(final String name, final String query) {
        ArgumentChecks.ensureNonEmpty("name",  name);
        ArgumentChecks.ensureNonEmpty("query", query);
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        return new ResourceDefinition(factory.createLocalName(null, name), query);
    }

    /**
     * Returns the name of the table, view or query to access as a resource.
     * There is small differences in the way it is used depending on whether
     * the resource is a table or a query:
     *
     * <ul>
     *   <li>If the resource is a table or a view, then this is the fully qualified name (including catalog and schema)
     *       of the table or view to include in the store. It may contain {@code LIKE} wildcard characters, in which
     *       case all tables matching the pattern will be included.</li>
     *   <li>If the resource is a query, then this is an arbitrary name to assign to the resource which will contain
     *       the query result.</li>
     * </ul>
     *
     * @return the name of the table, view or query.
     */
    public GenericName getName() {
        return name;
    }

    /**
     * Returns the SQL query to execute for the resource, or empty if the resource is a table or a view.
     *
     * @return the SQL query to execute for the resource.
     */
    public Optional<String> getQuery() {
        return Optional.ofNullable(query);
    }

    /**
     * Returns {@code true} if this definition is equal to the given object.
     *
     * @param  obj  another object, or {@code null}.
     * @return {@code true} if the other object is a resource definition equals to this one.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ResourceDefinition) {
            final ResourceDefinition other = (ResourceDefinition) obj;
            return name.equals(other.name) && Objects.equals(query, other.query);
        }
        return false;
    }

    /**
     * Returns a hash code value for this resource definition.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return name.hashCode() * 71 + Objects.hashCode(query);
    }

    /**
     * Returns a string representation of this resource definition.
     *
     * @return a string representation of this resource definition.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("Resource[\"").append(name).append('"');
        if (query != null) {
            b.append(" = ").append(query);
        }
        return b.append(']').toString();
    }
}
