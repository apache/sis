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

import java.sql.SQLException;
import org.apache.sis.util.CharSequences;
import org.apache.sis.storage.DataStoreException;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * Defines an application schema inferred from an SQL database (query, table, etc.).
 * This is used by {@link Analyzer} for creating {@link Table} instances.
 * A view or a custom query is considered as a "virtual" table.
 *
 * <h2>Side effects</h2>
 * Methods shall be invoked as below, in that order. The order is important because some
 * methods have values computed as side-effects and which are required by a subsequent method.
 * This is highly dependent of implementation details and may change in any future version.
 *
 * {@preformat java
 *   importedKeys = spec.getForeignerKeys(Relation.Direction.IMPORT);
 *   exportedKeys = spec.getForeignerKeys(Relation.Direction.EXPORT);
 *   attributes   = spec.createAttributes();
 *   primaryKey   = spec.createAssociations(exportedKeys);
 *   featureType  = spec.buildFeatureType();
 * }
 *
 * Methods not listed above can be invoked in any order.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class TableAnalyzer {
    /**
     * The catalog, schema and table name of the table to analyze.
     * The catalog and schema parts are optional and can be null, but the table is mandatory.
     */
    final TableReference id;

    /**
     * Count of the number of columns having a name starting with lower cases.
     */
    private int countLowerCaseStarts;

    /**
     * Whether this table or view contains at least one geometry column.
     *
     * @see Database#hasGeometry
     */
    boolean hasGeometry;

    /**
     * Creates a new analyzer.
     *
     * @param  id  the catalog, schema and table name of the table to analyze.
     */
    protected TableAnalyzer(final TableReference id) {
        this.id = id;
    }

    /**
     * Returns a list of associations between the table analyzed by this method and other tables.
     * The associations are defined by the foreigner keys referencing primary keys.
     *
     * <h4>Design note</h4>
     * The table relations can be defined in both ways: the foreigner keys of the analyzed table
     * may be referencing the primary keys of other tables ({@link Relation.Direction#IMPORT}),
     * or the primary keys of the analyzed table may be referenced by the foreigner keys of other tables
     * ({@link Relation.Direction#EXPORT}). However in both cases, we will translate that into associations
     * from the analyzed table to the other tables. We can not rely on {@code IMPORT} versus {@code EXPORT}
     * direction for determining the association navigability because the database designer's choice may be
     * driven by the need to support multi-occurrences.
     *
     * @param  direction   direction of the foreigner key for which to return components.
     * @return components of the foreigner key for the requested direction.
     * @throws SQLException if an error occurred while fetching information from the database.
     * @throws DataStoreException if a logical error occurred while analyzing the relations.
     */
    abstract Relation[] getForeignerKeys(Relation.Direction direction) throws SQLException, DataStoreException;

    /**
     * Configures the feature builder with attributes and "simple" associations inferred from the analyzed table.
     * The ordinary attributes and the "simple" associations (inferred from foreigner keys) are handled together
     * in order to have properties listed in the same order as the columns in the database table.
     *
     * <p>For each column in the table that is not a foreigner key, this method creates an {@code AttributeType}
     * of the same name. The Java type is inferred from the SQL type, and the attribute multiplicity in inferred
     * from the SQL nullability. Attribute names column names are usually the same, except when a column is used
     * both as a primary key and as foreigner key.</p>
     *
     * <p>This method handles only "ordinary" columns and {@link Relation.Direction#IMPORT} foreigner keys.
     * The values of those properties are singletons. By contrast, the associations in {@code EXPORT} direction
     * are multi-valued.</p>
     *
     * @param  feature  the builder where to add attributes and associations.
     * @return the columns for attribute values (not including associations).
     * @throws SQLException if an error occurred while fetching information from the database.
     * @throws DataStoreException if a logical error occurred while analyzing the relations.
     * @throws Exception for WKB parsing error or other kinds of errors.
     */
    abstract Column[] createAttributes() throws Exception;

    /**
     * Completes the configuration of feature builder with remaining associations.
     * This method appends the potentially multi-valued associations inferred from
     * {@link Relation.Direction#EXPORT} foreigner keys.
     *
     * <p>This analysis step computes the primary key (if available) as a side-effect.
     * That information is not available before this step because of type analysis done
     * for providing the {@link PrimaryKey#valueClass} information.</p>
     *
     * @param  exportedKeys  value of {@code getForeignerKeys(EXPORT)}.
     * @return primary key columns, or {@code null} if there is no primary key.
     * @throws SQLException if an error occurred while fetching information from the database.
     * @throws DataStoreException if a logical error occurred while analyzing the relations.
     * @throws Exception for WKB parsing error or other kinds of errors.
     */
    abstract PrimaryKey createAssociations(Relation[] exportedKeys) throws Exception;

    /**
     * Heuristic rule for determining if the column names starts with lower case or upper case.
     * Words that are all upper-case are ignored on the assumption that they are acronyms.
     *
     * @param  column  the column name.
     */
    final void updateCaseHeuristic(final String column) {
        if (!column.isEmpty()) {
            final int firstLetter = column.codePointAt(0);
            if (Character.isLowerCase(firstLetter)) {
                countLowerCaseStarts++;
            } else if (Character.isUpperCase(firstLetter) && !CharSequences.isUpperCase(column)) {
                countLowerCaseStarts--;
            }
        }
    }

    /**
     * Rewrites the given property name in a more human-readable form using heuristic rules.
     *
     * @param  propertyName  name of the property to rewrite.
     * @return proposed human-readable property name.
     */
    final String toHeuristicLabel(String propertyName) {
        if (countLowerCaseStarts > 0) {
            final CharSequence words = CharSequences.camelCaseToWords(propertyName, true);
            final int first = Character.codePointAt(words, 0);
            propertyName = new StringBuilder(words.length())
                    .appendCodePoint(Character.toLowerCase(first))
                    .append(words, Character.charCount(first), words.length())
                    .toString();
        }
        return propertyName;
    }

    /**
     * Completes the creation of the feature type. This method sets global information about the feature type:
     *
     * <ul>
     *   <li>The feature name, which is derived from the table name when possible.</li>
     *   <li>An optional description of the application schema. This information is not used by computation,
     *       but allows to give end-user global information about the schema (s)he is manipulating.</li>
     * </ul>
     *
     * The remarks can be opportunistically fetched from {@code id.freeText} if known by the caller.
     *
     * @return the feature type.
     */
    abstract FeatureType buildFeatureType() throws DataStoreException, SQLException;
}
