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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.sql.SQLException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.AssociationRoleBuilder;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;

// Specific to the main branch:
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Defines an application schema inferred from an SQL database (query, table, etc.).
 * This is used by {@link Analyzer} for creating {@link Table} instances.
 * A view or a custom query is considered as a "virtual" table.
 *
 * <p>Instances of this class are created temporarily when starting the analysis
 * of a database structure, and discarded after the analysis is finished.</p>
 *
 * <h2>Side effects</h2>
 * Methods shall be invoked as below, in that order. The order is important because some
 * methods have values computed as side-effects and which are required by a subsequent method.
 * This is highly dependent of implementation details and may change in any future version.
 *
 * {@snippet lang="java" :
 *   importedKeys = spec.getForeignerKeys(Relation.Direction.IMPORT);
 *   exportedKeys = spec.getForeignerKeys(Relation.Direction.EXPORT);
 *   attributes   = spec.createAttributes();
 *   primaryKey   = spec.createAssociations(exportedKeys);
 *   featureType  = spec.buildFeatureType();
 *   }
 *
 * Methods not listed above can be invoked in any order.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class FeatureAnalyzer {
    /**
     * The parent analyzer for all tables.
     */
    final Analyzer analyzer;

    /**
     * The catalog, schema and table name of the table to analyze.
     * The catalog and schema parts are optional and can be null, but the table is mandatory.
     */
    final TableReference id;

    /**
     * The class of primary key values, or {@code null} if there is no primary key.
     * If the primary key use more than one column, then is the class of an array;
     * it may be an array of primitive type.
     *
     * <p>This field is computed as a side-effect of {@link #createAttributes()}.</p>
     *
     * @see PrimaryKey#valueClass
     */
    private Class<?> primaryKeyClass;

    /**
     * Whether the primary key can have null values.
     */
    private boolean primaryKeyNullable;

    /**
     * The columns that constitute the primary key, or an empty set if there is no primary key.
     */
    final Set<String> primaryKey;

    /**
     * Foreigner keys that are referencing primary keys of other tables ({@link Relation.Direction#IMPORT}).
     * Keys are column names and values are information about the relation (referenced table, <i>etc</i>).
     * For each value, the list should contain exactly 1 element. But more elements are allowed because the
     * same column could be used as a component of more than one foreigner key. The list may contain nulls.
     *
     * <p>This map is populated as a side-effect of {@code getForeignerKeys(Direction.IMPORT, …)} call.</p>
     */
    private final Map<String, List<Relation>> foreignerKeys;

    /**
     * The builder builder where to append attributes and associations.
     */
    final FeatureTypeBuilder feature;

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
     * Whether this table or view contains at least one raster column.
     *
     * @see Database#hasRaster
     */
    boolean hasRaster;

    /**
     * Creates a new analyzer.
     *
     * @param  id  the catalog, schema and table name of the table to analyze.
     */
    protected FeatureAnalyzer(final Analyzer analyzer, final TableReference id) {
        this.analyzer  = analyzer;
        this.id        = id;
        primaryKey     = new LinkedHashSet<>();
        foreignerKeys  = new HashMap<>();
        feature = new FeatureTypeBuilder(analyzer.nameFactory,
                analyzer.database.geomLibrary.library,
                analyzer.database.listeners.getLocale());
    }

    /**
     * Returns a list of associations between the table analyzed by this method and other tables.
     * The associations are defined by the foreigner keys referencing primary keys.
     *
     * <h4>Design note</h4>
     * The table relations can be defined in both ways: the foreigner keys of the analyzed table
     * may be referencing the primary keys of other tables ({@link Relation.Direction#IMPORT}),
     * or the primary keys of the analyzed table may be referenced by the foreigner keys of other tables
     * ({@link Relation.Direction#EXPORT}). However, in both cases, we will translate that into associations
     * from the analyzed table to the other tables. We cannot rely on {@code IMPORT} versus {@code EXPORT}
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
     * Declares that a foreigner key is referencing the primary key of another tables ({@link Relation.Direction#IMPORT}).
     * This is a helper method for {@link #getForeignerKeys(Relation.Direction)} implementations.
     */
    final void addForeignerKeys(Relation relation) {
        for (final String column : relation.getOwnerColumns()) {
            foreignerKeys.computeIfAbsent(column, (key) -> new ArrayList<>()).add(relation);
            relation = null;       // Only the first column will be associated.
        }
    }

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
     * @return the columns for attribute values (not including associations).
     * @throws SQLException if an error occurred while fetching information from the database.
     * @throws DataStoreException if a logical error occurred while analyzing the relations.
     * @throws Exception for WKB parsing error or other kinds of errors.
     */
    abstract Column[] createAttributes() throws Exception;

    /**
     * Creates a feature attribute for the given column.
     * This is a helper class for {@link #createAttributes()} implementations.
     *
     * @param  column  the column for which to create an attribute.
     * @return {@code true} if the column has been added as an attribute, or {@code false} if it was an association.
     */
    final boolean createAttribute(final Column column) throws Exception {
        final boolean isPrimaryKey = primaryKey.contains(column.name);
        final List<Relation> dependencies = foreignerKeys.get(column.name);
        updateCaseHeuristic(column.label);
        /*
         * Add the column as an attribute. Foreign keys are excluded (they will be replaced by associations),
         * except if the column is also a primary key. In the latter case we need to keep that column because
         * it is needed for building the feature identifier.
         */
        AttributeTypeBuilder<?> attribute = null;
        final boolean created = (isPrimaryKey || dependencies == null);
        if (created) {
            final ValueGetter<?> getter = analyzer.setValueGetterOf(column);
            attribute = column.createAttribute(feature);
            /*
             * Some columns have special purposes: components of primary keys will be used for creating
             * identifiers, some columns may contain a geometric object. Adding a role on those columns
             * may create synthetic columns, for example "sis:identifier".
             */
            if (isPrimaryKey) {
                attribute.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                primaryKeyNullable |= column.isNullable;
                primaryKeyClass = Classes.findCommonClass(primaryKeyClass, getter.valueType);
            }
            /*
             * If geometry columns are found, the first one will be defined as the default geometry.
             * Note: a future version may allow user to select which column should be the default.
             */
            if (!hasGeometry && Geometries.isKnownType(getter.valueType)) {
                hasGeometry = true;
                attribute.addRole(AttributeRole.DEFAULT_GEOMETRY);
            }
            if (!hasRaster) {
                hasRaster = GridCoverage.class.isAssignableFrom(getter.valueType);
            }
        }
        /*
         * If the column is a foreigner key, insert an association to another feature instead.
         * If the foreigner key uses more than one column, only one of those columns will become
         * an association and other columns will be omitted from the FeatureType (but there will
         * still be used in SQL queries). Note that columns may be used by more than one relation.
         */
        if (dependencies != null) {
            int count = 0;
            for (final Relation dependency : dependencies) {
                if (dependency != null && !dependency.excluded) {
                    final GenericName typeName = dependency.getName(analyzer);
                    final Table table = analyzer.table(dependency, typeName, id);
                    /*
                     * Use the column name as the association name, provided that the foreigner key
                     * uses only that column. If the foreigner key uses more than one column, then we
                     * do not know which column describes better the association (often there is none).
                     * In such case we use the foreigner key name as a fallback.
                     */
                    dependency.setPropertyName(column.getPropertyName(), count++);
                    final AssociationRoleBuilder association;
                    if (table != null) {
                        dependency.setSearchTable(analyzer, table, table.primaryKey, Relation.Direction.IMPORT);
                        association = feature.addAssociation(table.featureType);
                    } else {
                        association = feature.addAssociation(typeName);     // May happen in case of cyclic dependency.
                    }
                    association.setName(dependency.getPropertyName());
                    if (column.isNullable) {
                        association.setMinimumOccurs(0);
                    }
                    /*
                     * If the column is also used in the primary key, then we have a name clash.
                     * Rename the primary key column with the addition of a "pk:" scope. We rename
                     * the primary key column instead of this association because the primary key
                     * column should rarely be used directly.
                     */
                    if (attribute != null) {
                        attribute.setName(analyzer.nameFactory.createGenericName(null, "pk", column.getPropertyName()));
                        column.setPropertyName(attribute);
                        attribute = null;
                    }
                }
            }
        }
        return created;
    }

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
    final PrimaryKey createAssociations(final Relation[] exportedKeys) throws Exception {
        if (primaryKey.size() > 1) {
            if (!primaryKeyNullable) {
                primaryKeyClass = Numbers.wrapperToPrimitive(primaryKeyClass);
            }
            primaryKeyClass = Classes.changeArrayDimension(primaryKeyClass, 1);
        }
        final PrimaryKey pk = PrimaryKey.create(primaryKeyClass, primaryKey);
        int count = 0;
        for (final Relation dependency : exportedKeys) {
            if (dependency != null && !dependency.excluded) {
                final GenericName typeName = dependency.getName(analyzer);
                String propertyName = toHeuristicLabel(typeName.tip().toString());
                final String base = propertyName;
                while (feature.isNameUsed(propertyName)) {
                    propertyName = base + '-' + ++count;
                }
                dependency.setPropertyName(propertyName);
                final Table table = analyzer.table(dependency, typeName, id);
                final AssociationRoleBuilder association;
                if (table != null) {
                    dependency.setSearchTable(analyzer, table, pk, Relation.Direction.EXPORT);
                    association = feature.addAssociation(table.featureType);
                } else {
                    association = feature.addAssociation(typeName);     // May happen in case of cyclic dependency.
                }
                association.setName(propertyName)
                           .setMinimumOccurs(0)
                           .setMaximumOccurs(Integer.MAX_VALUE);
            }
        }
        return pk;
    }

    /**
     * Heuristic rule for determining if the column names starts with lower case or upper case.
     * Words that are all upper-case are ignored on the assumption that they are acronyms.
     *
     * @param  column  the column name.
     */
    private void updateCaseHeuristic(final String column) {
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
    private String toHeuristicLabel(String propertyName) {
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
     * Returns the exception to throw if a column is duplicated.
     */
    final DataStoreContentException duplicatedColumn(final Column column) {
        return new DataStoreContentException(analyzer.resources().getString(Resources.Keys.DuplicatedColumn_1, column.name));
    }

    /**
     * Returns an optional description of the application schema. This information is not used by computation,
     * but allows to give end-user global information about the schema (s)he is manipulating.
     */
    String getRemarks() throws SQLException {
        return id.freeText;
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
    final DefaultFeatureType buildFeatureType() throws DataStoreException, SQLException {
        String remarks = id.freeText;
        if (remarks != null) {
            feature.setDefinition(remarks);
        }
        feature.setName(id.getName(analyzer));
        final SchemaModifier customizer = analyzer.customizer;
        return (customizer != null) ? customizer.editFeatureType(id, feature) : feature.build();
    }
}
