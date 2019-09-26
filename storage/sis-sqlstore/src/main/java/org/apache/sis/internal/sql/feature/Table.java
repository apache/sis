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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.util.GenericName;

import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.WeakValueHashMap;

// Branch-dependent imports


/**
 * Description of a table in the database, including columns, primary keys and foreigner keys.
 * This class contains a {@link FeatureType} inferred from the table structure. The {@link FeatureType}
 * contains an {@link AttributeType} for each table column, except foreigner keys which are represented
 * by {@link FeatureAssociationRole}s.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Table extends AbstractFeatureSet {
    /**
     * Provider of (pooled) connections to the database.
     */
    final DataSource source;

    /**
     * The structure of this table represented as a feature. Each feature attribute is a table column,
     * except synthetic attributes like "sis:identifier". The feature may also contain associations
     * inferred from foreigner keys that are not immediately apparent in the table.
     */
    final FeatureType featureType;

    /**
     * The name in the database of this {@code Table} object, together with its schema and catalog.
     */
    final TableReference name;

    /**
     * Name of all columns to fetch from database, optionally amended with an alias. Alias is used for feature type
     * attributes which have been renamed to avoid name collisions. In any case, a call to {@link ColumnRef#getAttributeName()}}
     * will return the name available in target feature type.
     */
    final List<ColumnRef> attributes;

    /**
     * The columns that constitute the primary key, or {@code null} if there is no primary key.
     */
    private final String[] primaryKeys;

    /**
     * The primary keys of other tables that are referenced by this table foreign key columns.
     * They are 0:1 relations. May be {@code null} if there is no imported keys.
     */
    final Relation[] importedKeys;

    /**
     * The foreign keys of other tables that reference this table primary key columns.
     * They are 0:N relations. May be {@code null} if there is no exported keys.
     */
    final Relation[] exportedKeys;

    /**
     * The class of primary key values, or {@code null} if there is no primary keys.
     * If the primary keys use more than one column, then this field is the class of
     * an array; it may be an array of primitive type.
     */
    final Class<?> primaryKeyClass;

    private final SQLTypeSpecification specification;

    /**
     * Feature instances already created for given primary keys. This map is used only when requesting feature
     * instances by identifiers (not for iterating over all features) and those identifiers are primary keys.
     * We create this map only for tables referenced by foreigner keys of other tables as enumerated by the
     * {@link Relation.Direction#IMPORT} and {@link Relation.Direction#EXPORT} cases; not for arbitrary
     * cross-reference cases. Values are usually {@code Feature} instances, but may also be {@code Collection<Feature>}.
     *
     * @see #instanceForPrimaryKeys()
     */
    private WeakValueHashMap<?,Object> instanceForPrimaryKeys;

    /**
     * {@code true} if this table contains at least one geometry column.
     */
    final boolean hasGeometry;

    /**
     * Keep a reference of target database metadata, to ease creation of {@link SQLBuilder}.
     */
    final DatabaseMetaData dbMeta;

    /**
     * An SQL builder whose sole purpose is to allow creation of new builders without metadata analysis. It allows to
     * reduce error eventuality, and re-use already  computed information.
     */
    private final SQLBuilder sqlTemplate;

    private final FeatureAdapter adapter;

    /**
     * Creates a description of the table of the given name.
     * The table is identified by {@code id}, which contains a (catalog, schema, name) tuple.
     * The catalog and schema parts are optional and can be null, but the table is mandatory.
     *
     * @param  analyzer    helper functions, e.g. for converting SQL types to Java types.
     * @param  id          the catalog, schema and table name of the table to analyze.
     * @param  importedBy  if this table is imported by the foreigner keys of another table,
     *                     the parent table. Otherwise {@code null}.
     */
    Table(final Analyzer analyzer, final TableReference id, final TableReference importedBy)
            throws SQLException, DataStoreException
    {
        super(analyzer.listeners);
        this.dbMeta = analyzer.metadata;
        this.sqlTemplate = new SQLBuilder(this.dbMeta, true);
        this.source = analyzer.source;
        this.name   = id;
        /*
         * Creates a list of associations between the table read by this method and other tables.
         * The associations are defined by the foreigner keys referencing primary keys. Note that
         * the table relations can be defined in both ways:  the foreigner keys of this table may
         * be referencing the primary keys of other tables (Direction.IMPORT) or the primary keys
         * of this table may be referenced by the foreigner keys of other tables (Direction.EXPORT).
         * However in both case, we will translate that into associations from this table to the
         * other tables. We can not rely on IMPORT versus EXPORT for determining the association
         * navigability because the database designer's choice may be driven by the need to support
         * multi-occurrences.
         */
        /*
         * For each column in the table that is not a foreigner key, create an AttributeType of the same name.
         * The Java type is inferred from the SQL type, and the attribute multiplicity in inferred from the SQL
         * nullability. Attribute names are added in the 'attributeNames' and 'attributeColumns' list. Those
         * names are usually the same, except when a column is used both as a primary key and as foreigner key.
         */

        /*
         * If the primary keys uses more than one column, we will need an array to store it.
         * If all columns are non-null numbers, use primitive arrays instead than array of wrappers.
         */
        this.specification = analyzer.create(id, importedBy);
        primaryKeys = (String[]) specification.getPK()
                .map(PrimaryKey::getColumns)
                .orElse(Collections.EMPTY_LIST)
                .toArray(new String[0]);
        this.adapter          = analyzer.buildAdapter(specification);
        this.featureType      = adapter.type;
        this.importedKeys     = toArray(specification.getImports());
        this.exportedKeys     = toArray(specification.getExports());
        this.primaryKeyClass  = primaryKeys.length < 2 ? Object.class : Object[].class;
        this.hasGeometry      = specification.getPrimaryGeometryColumn().isPresent();
        this.attributes       = Collections.unmodifiableList(
                specification.getColumns().stream()
                        .map(SQLColumn::getName)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
        if (!(query instanceof SimpleQuery)) return super.subset(query);
        boolean remainingQuery = true;
        final SimpleQuery q = (SimpleQuery) query;
        FeatureSet subset = this;
        final List<SimpleQuery.Column> cols = q.getColumns();

        /**
         * Once filter has been taken care of, we will be able to check columns to filter. Note that all filters
         * managed by database engine can use non-returned columns, but it is not the case of remaining ones, which
         * are applied after feature creation, therefore with only filtered columns accessible.
         */
        if (cols != null && !cols.isEmpty()) {

        }

        return remainingQuery ? subset.subset(q) : subset;
    }

    /**
     * Returns the given relations as an array, or {@code null} if none.
     */
    private static Relation[] toArray(final Collection<Relation> relations) {
        final int size = relations.size();
        return (size != 0) ? relations.toArray(new Relation[size]) : null;
    }

    /**
     * Sets the search tables on all {@link Relation} instances for which this operation has been deferred.
     * This happen when a table could not be obtained because of circular dependency. This method is invoked
     * after all tables have been created in order to fill such holes.
     *
     * @param  tables  all tables created.
     */
    final void setDeferredSearchTables(final Analyzer analyzer, final Map<GenericName,Table> tables) throws DataStoreException {
        for (final Relation.Direction direction : Relation.Direction.values()) {
            final Relation[] relations;
            switch (direction) {
                case IMPORT: relations = importedKeys; break;
                case EXPORT: relations = exportedKeys; break;
                default: continue;
            }
            if (relations != null) {
                for (final Relation relation : relations) {
                    if (!relation.isSearchTableDefined()) {
                        // A ClassCastException below would be a bug since 'relation.propertyName' shall be for an association.
                        final PropertyType property = featureType.getProperty(relation.propertyName.toString());
                        if (!(property instanceof FeatureAssociationRole)) {
                            throw new IllegalStateException(String.format(
                                    "We expect a feature association for %s relation %s. Duplicate key ?",
                                    direction.name(), relation.propertyName
                            ));
                        }
                        FeatureAssociationRole association = (FeatureAssociationRole) property;
                        final Table table = tables.get(association.getValueType().getName());
                        if (table == null) {
                            throw new InternalDataStoreException(association.toString());
                        }
                        final String[] referenced;
                        switch (direction) {
                            case IMPORT: referenced = table.primaryKeys; break;
                            case EXPORT: referenced =  this.primaryKeys; break;
                            default: throw new AssertionError(direction);
                        }
                        relation.setSearchTable(analyzer, table, referenced, direction);
                    }
                }
            }
        }
    }


    // ────────────────────────────────────────────────────────────────────────────────────────
    //     End of table construction. Next methods are for visualizing the table structure.
    // ────────────────────────────────────────────────────────────────────────────────────────


    /**
     * Appends all children to the given parent. The children are added under the given node.
     * If the children array is null, then this method does nothing.
     *
     * @param  parent    the node where to add children.
     * @param  children  the children to add, or {@code null} if none.
     * @param  arrow     the symbol to use for relating the columns of two tables in a foreigner key.
     */
    @Debug
    private static void appendAll(final TreeTable.Node parent, final Relation[] children, final String arrow) {
        if (children != null) {
            for (final Relation child : children) {
                child.appendTo(parent, arrow);
            }
        }
    }

    /**
     * Creates a tree representation of this table for debugging purpose.
     *
     * @param  parent  the parent node where to add the tree representation.
     */
    @Debug
    final void appendTo(TreeTable.Node parent) {
        parent = Relation.newChild(parent, featureType.getName().toString());
        for (final ColumnRef attribute : attributes) {
            TableReference.newChild(parent, attribute.getAttributeName());
        }
        appendAll(parent, importedKeys, " → ");
        appendAll(parent, exportedKeys, " ← ");
    }

    /**
     * Formats a graphical representation of this table for debugging purpose. This representation
     * can be printed to the {@linkplain System#out standard output stream} (for example) if the
     * output device uses a monospaced font and supports Unicode.
     */
    @Override
    public String toString() {
        return TableReference.toString(this, (n) -> appendTo(n));
    }


    // ────────────────────────────────────────────────────────────────────────────────────────
    //     End of table structure visualization. Next methods are for fetching features.
    // ────────────────────────────────────────────────────────────────────────────────────────


    /**
     * Returns the table identifier composed of catalog, schema and table name.
     */
    @Override
    public final Optional<GenericName> getIdentifier() {
        return Optional.of(featureType.getName().toFullyQualifiedName());
    }

    /**
     * Returns the feature type inferred from the database structure analysis.
     */
    @Override
    public final FeatureType getType() {
        return featureType;
    }

    /**
     * If this table imports the inverse of the given relation, returns the imported relation.
     * Otherwise returns {@code null}. This method is used for preventing infinite recursivity.
     *
     * @param  exported       the relation exported by another table.
     * @param  exportedOwner  {@code exported.owner.name}: table that contains the {@code exported} relation.
     * @return the inverse of the given relation, or {@code null} if none.
     */
    final Relation getInverseOf(final Relation exported, final TableReference exportedOwner) {
        if (importedKeys != null && name.equals(exported)) {
            for (final Relation relation : importedKeys) {
                if (relation.equals(exportedOwner) && relation.isInverseOf(exported)) {
                    return relation;
                }
            }
        }
        return null;
    }

    /**
     * Returns a cache for fetching feature instances by identifier. The map is created when this method is
     * first invoked. Keys are primary key values, typically as {@code String} or {@code Integer} instances
     * or arrays of those if the keys use more than one column. Values are usually {@code Feature} instances,
     * but may also be {@code Collection<Feature>}.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final synchronized WeakValueHashMap<?,Object> instanceForPrimaryKeys() {
        if (instanceForPrimaryKeys == null) {
            instanceForPrimaryKeys = new WeakValueHashMap<>(primaryKeyClass);
        }
        return instanceForPrimaryKeys;
    }

    /**
     * Returns the number of rows, or -1 if unknown. Note that some database drivers returns 0,
     * so it is better to consider 0 as "unknown" too. We do not cache this count because it may
     * change at any time.
     *
     * @param  metadata     information about the database.
     * @param  approximate  whether approximate or outdated values are acceptable.
     * @return number of rows (may be approximate), or -1 if unknown.
     */
    final long countRows(final DatabaseMetaData metadata, final boolean approximate) throws SQLException {
        long count = -1;
        final String[] names = TableReference.splitName(featureType.getName());
        try (ResultSet reflect = metadata.getIndexInfo(names[2], names[1], names[0], false, approximate)) {
            while (reflect.next()) {
                final long n = reflect.getLong(Reflection.CARDINALITY);
                if (!reflect.wasNull()) {
                    if (reflect.getShort(Reflection.TYPE) == DatabaseMetaData.tableIndexStatistic) {
                        return n;       // "Index statistic" type provides the number of rows in the table.
                    }
                    if (n > count) {    // Other index types may be inaccurate.
                        count = n;
                    }
                }
            }
        }
        return count;
    }

    public SQLBuilder createStatement() {
        return new SQLBuilder(sqlTemplate);
    }

    /**
     * Returns a stream of all features contained in this dataset.
     *
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return all features contained in this dataset.
     * @throws DataStoreException if an error occurred while creating the stream.
     */
    @Override
    public Stream<Feature> features(final boolean parallel) throws DataStoreException {
        return new StreamSQL(this, parallel);
    }

    /**
     * Returns an iterator over the features.
     *
     * @param connection  connection to the database.
     * @param following   the relations that we are following. Used for avoiding never ending loop.
     * @param noFollow    relation to not follow, or {@code null} if none.
     */
    final Features features(final Connection connection, final List<Relation> following, final Relation noFollow)
            throws SQLException, InternalDataStoreException
    {
        return new Features(this, connection, attributes, following, noFollow, false, -1, -1);
    }
}
