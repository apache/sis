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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import org.opengis.util.GenericName;
import org.opengis.coverage.Coverage;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.util.ArgumentChecks;


/**
 * Represent the structure of features in the database.
 * The work done here is similar to reverse engineering.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class Database {
    /**
     * Possible value for the {@value Reflection#TABLE_TYPE} column in the {@link ResultSet}
     * returned by {@link DatabaseMetaData#getTables(String, String, String, String[])}.
     * Also a possible value for the last argument of above-cited method.
     */
    private static final String TABLE = "TABLE", VIEW = "VIEW";

    /**
     * Feature type used to mark types which are sub-types of others.
     */
    private static final FeatureType SUBTYPE;
    static {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder()
                .setName("SubType")
                .setAbstract(true);
        SUBTYPE = ftb.build();
    }

    private final SpatialFunctions functions;
    private final FeatureNaming<PrimaryKey> pkIndex;
    private final FeatureNaming<FeatureType> typeIndex;
    private final Map<String,Schema> schemas;

    public Database(final SQLStore store, final SpatialFunctions functions, final String schema, final String table,
            final List<String> addWarningsTo) throws SQLException, IllegalNameException
    {
        if (table != null) {
            ArgumentChecks.ensureNonEmpty("table", table);
        }
        this.functions = functions;
        pkIndex = new FeatureNaming<>();
        typeIndex = new FeatureNaming<>();
        schemas = new HashMap<>();
        analyze(store, schema, table, addWarningsTo);
    }

    private Collection<Schema> getSchemaMetaModels() {
        return schemas.values();
    }

    private Schema getSchemaMetaModel(String name) {
        return schemas.get(name);
    }

    private PrimaryKey getPrimaryKey(final SQLStore store, final String featureTypeName) throws IllegalNameException {
        return pkIndex.get(store, featureTypeName);
    }

    public FeatureType getFeatureType(final SQLStore store, final String typeName) throws IllegalNameException {
        return typeIndex.get(store, typeName);
    }

    /**
     * Explores all tables and views then recreate a complex feature model from relations.
     */
    private synchronized void analyze(final SQLStore store, final String schemaName, final String tableName, final List<String> addWarningsTo)
            throws SQLException, IllegalNameException
    {
        try (Connection cx = store.getDataSource().getConnection()) {
            final DatabaseMetaData metadata = cx.getMetaData();
            final Set<String> requieredSchemas = new HashSet<>();
            final Set<String> visitedSchemas = new HashSet<>();
            /*
             * Schema names available in the database:
             * 1. TABLE_SCHEM   : String  =>  schema name
             * 2. TABLE_CATALOG : String  =>  catalog name (may be null)
             */
            if (schemaName != null) {
                requieredSchemas.add(schemaName);
            } else try (ResultSet reflect = metadata.getSchemas()) {
                while (reflect.next()) {
                    requieredSchemas.add(reflect.getString(Reflection.TABLE_SCHEM));        // TODO: use schemas in getTables instead.
                }
            }
            /*
             * We need to analyze requiered schema references.
             */
            while (!requieredSchemas.isEmpty()) {
                final String sn = requieredSchemas.iterator().next();
                visitedSchemas.add(sn);
                requieredSchemas.remove(sn);
                // TODO: escape with metadata.getSearchStringEscape().
                final Schema schema = analyzeSchema(metadata, sn, tableName, requieredSchemas, visitedSchemas, addWarningsTo);
                schemas.put(schema.name, schema);
            }
            reverseSimpleFeatureTypes(metadata);
        }
        /*
         * Build indexes.
         */
        final Collection<Schema> candidates;
        if (schemaName == null) {
            candidates = getSchemaMetaModels();             // Take all schemas.
        } else {
            candidates = Collections.singleton(getSchemaMetaModel(schemaName));
        }
        for (Schema schema : candidates) {
           if (schema != null) {
                for (Table table : schema.getTables()) {

                    final FeatureTypeBuilder ft = table.getType(Table.View.SIMPLE_FEATURE_TYPE);
                    final GenericName name = ft.getName();
                    pkIndex.add(store, name, table.key);
                    if (table.isSubType()) {
                        // We don't show subtype, they are part of other feature types, add a flag to identify then
                        ft.setSuperTypes(SUBTYPE);
                    }
                    typeIndex.add(store, name, ft.build());
                 }
            } else {
                throw new SQLException("Specifed schema " + schemaName + " does not exist.");
            }
        }
    }

    /**
     * @param  schemaPattern  schema name with "%" and "_" interpreted as wildcards, or {@code null} for all schemas.
     */
    private Schema analyzeSchema(final DatabaseMetaData metadata, final String schemaPattern, final String tableNamePattern,
            final Set<String> requieredSchemas, final Set<String> visitedSchemas, final List<String> addWarningsTo)
            throws SQLException, IllegalNameException
    {
        final Schema schema = new Schema(schemaPattern);
        /*
         * Description of the tables available:
         * 1. TABLE_SCHEM : String  =>  table schema (may be null)
         * 2. TABLE_NAME  : String  =>  table name
         * 3. TABLE_TYPE  : String  =>  table type (typically "TABLE" or "VIEW").
         */
        try (ResultSet reflect = metadata.getTables(null, schemaPattern, tableNamePattern, new String[] {TABLE, VIEW})) {   // TODO: use metadata.getTableTypes()
            while (reflect.next()) {
                final Table table = analyzeTable(metadata, reflect, requieredSchemas, visitedSchemas, addWarningsTo);
                schema.tables.put(table.name, table);
            }
        }
        return schema;
    }

    private Table analyzeTable(final DatabaseMetaData metadata, final ResultSet tableSet,
            final Set<String> requieredSchemas, final Set<String> visitedSchemas, final List<String> addWarningsTo)
            throws SQLException, IllegalNameException
    {
        final String schemaName = tableSet.getString(Reflection.TABLE_SCHEM);
        final String tableName  = tableSet.getString(Reflection.TABLE_NAME);
        final String tableType  = tableSet.getString(Reflection.TABLE_TYPE);
        final Table table = new Table(tableName, tableType);
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        /*
         * Explore all columns.
         */
        try (ResultSet reflect = metadata.getColumns(null, schemaName, tableName, null)) {
            while (reflect.next()) {
                analyzeColumn(metadata, reflect, ftb.addAttribute(Object.class));
            }
        }
        /*
         * Find primary keys.
         */
        final List<Column> cols = new ArrayList<>();
        try (ResultSet rp = metadata.getPrimaryKeys(null, schemaName, tableName)) {
            while (rp.next()) {
                final String columnNamePattern = rp.getString(Reflection.COLUMN_NAME);
                // TODO: escape columnNamePattern with metadata.getSearchStringEscape().
                try (ResultSet reflect = metadata.getColumns(null, schemaName, tableName, columnNamePattern)) {
                    while (reflect.next()) {                                        // Should loop exactly once.
                        final int sqlType = reflect.getInt(Reflection.DATA_TYPE);
                        final String sqlTypeName = reflect.getString(Reflection.TYPE_NAME);
                        Class<?> columnType = functions.getJavaType(sqlType, sqlTypeName);
                        if (columnType == null) {
                            addWarningsTo.add("No class for SQL type " + sqlType);
                            columnType = Object.class;
                        }
                        Column col;
                        final Boolean b = SQLUtilities.parseBoolean(reflect.getString(Reflection.IS_AUTOINCREMENT));
                        if (b != null && b) {
                            col = new Column(schemaName, tableName, columnNamePattern, sqlType, sqlTypeName, columnType, Column.Type.AUTO, null);
                        } else {
                            // TODO: need to distinguish "NO" and empty string.
                            final String sequenceName = functions.getColumnSequence(metadata.getConnection(), schemaName, tableName, columnNamePattern);
                            if (sequenceName != null) {
                                col = new Column(schemaName, tableName, columnNamePattern, sqlType,
                                        sqlTypeName, columnType, Column.Type.SEQUENCED,sequenceName);
                            } else {
                                col = new Column(schemaName, tableName, columnNamePattern, sqlType,
                                        sqlTypeName, columnType, Column.Type.PROVIDED, null);
                            }
                        }
                        cols.add(col);
                    }
                }
            }
        }
        /*
         * Search indexes, they provide informations such as:
         * - Unique indexes may indicate 1:1 relations in complexe features
         * - Unique indexes can be used as primary key if no primary key are defined
         */
        final boolean pkEmpty = cols.isEmpty();
        final List<String> names = new ArrayList<>();
        final Map<String,List<String>> uniqueIndexes = new HashMap<>();
        String indexname = null;
        // We can't cache this one, seems to be a bug in the driver, it won't find anything for table name like '%'
        try (ResultSet reflect = metadata.getIndexInfo(null, schemaName, tableName, true, false)) {
            while (reflect.next()) {
                final String columnName = reflect.getString(Reflection.COLUMN_NAME);
                final String idxName = reflect.getString(Reflection.INDEX_NAME);
                List<String> lst = uniqueIndexes.get(idxName);
                if (lst == null) {
                    lst = new ArrayList<>();
                    uniqueIndexes.put(idxName, lst);
                }
                lst.add(columnName);
                if (pkEmpty) {
                    /*
                     * We use a single index columns set as primary key
                     * We must not mix with other potential indexes.
                     */
                    if (indexname == null) {
                        indexname = idxName;
                    } else if (!indexname.equals(idxName)) {
                        continue;
                    }
                    names.add(columnName);
                }
            }
        }
        /*
         * For each unique index composed of one column add a flag on the property descriptor.
         */
        for (Entry<String,List<String>> entry : uniqueIndexes.entrySet()) {
            final List<String> columns = entry.getValue();
            if (columns.size() == 1) {
                String columnName = columns.get(0);
                for (PropertyTypeBuilder desc : ftb.properties()) {
                    if (desc.getName().tip().toString().equals(columnName)) {
                        final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) desc;
                        atb.addCharacteristic(Column.JDBC_PROPERTY_UNIQUE).setDefaultValue(Boolean.TRUE);
                    }
                }
            }
        }
        if (pkEmpty && !names.isEmpty()) {
            /*
             * Build a primary key from unique index.
             */
            try (ResultSet reflect = metadata.getColumns(null, schemaName, tableName, null)) {
                while (reflect.next()) {
                    final String columnName = reflect.getString(Reflection.COLUMN_NAME);
                    if (names.contains(columnName)) {
                        final int sqlType = reflect.getInt(Reflection.DATA_TYPE);
                        final String sqlTypeName = reflect.getString(Reflection.TYPE_NAME);
                        final Class<?> columnType = functions.getJavaType(sqlType, sqlTypeName);
                        final Column col = new Column(schemaName, tableName, columnName,
                                sqlType, sqlTypeName, columnType, Column.Type.PROVIDED, null);
                        cols.add(col);
                        /*
                         * Set as identifier
                         */
                        for (PropertyTypeBuilder desc : ftb.properties()) {
                            if (desc.getName().tip().toString().equals(columnName)) {
                                final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) desc;
                                atb.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (cols.isEmpty()) {
            if (TABLE.equals(tableType)) {
                addWarningsTo.add("No primary key found for " + tableName);
            }
        }
        table.key = new PrimaryKey(tableName, cols);
        /*
         * Mark primary key columns.
         */
        for (PropertyTypeBuilder desc : ftb.properties()) {
            for (Column col : cols) {
                if (desc.getName().tip().toString().equals(col.name)) {
                    final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) desc;
                    atb.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                    break;
                }
            }
        }
        /*
         * Find imported keys.
         */
        try (ResultSet reflect = metadata.getImportedKeys(null, schemaName, tableName)) {
            while (reflect.next()) {
                String relationName = reflect.getString(Reflection.PK_NAME);
                if (relationName == null) relationName = reflect.getString(Reflection.FK_NAME);
                final String localColumn   = reflect.getString(Reflection.FKCOLUMN_NAME);
                final String refSchemaName = reflect.getString(Reflection.PKTABLE_SCHEM);
                final String refTableName  = reflect.getString(Reflection.PKTABLE_NAME);
                final String refColumnName = reflect.getString(Reflection.PKCOLUMN_NAME);
                final int deleteRule = reflect.getInt(Reflection.DELETE_RULE);
                final boolean deleteCascade = DatabaseMetaData.importedKeyCascade == deleteRule;
                final Relation relation = new Relation(relationName,localColumn,
                        refSchemaName, refTableName, refColumnName, true, deleteCascade);
                table.importedKeys.add(relation);
                if (refSchemaName!=null && !visitedSchemas.contains(refSchemaName)) requieredSchemas.add(refSchemaName);
                for (PropertyTypeBuilder desc : ftb.properties()) {
                    if (desc.getName().tip().toString().equals(localColumn)) {
                        final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) desc;
                        atb.addCharacteristic(Column.JDBC_PROPERTY_RELATION).setDefaultValue(relation);
                        break;
                    }
                }
            }
        }
        /*
         * Find exported keys.
         */
        try (ResultSet reflect = metadata.getExportedKeys(null, schemaName, tableName)) {
            while (reflect.next()) {
                String relationName = reflect.getString(Reflection.FKCOLUMN_NAME);
                if (relationName == null) relationName = reflect.getString(Reflection.FK_NAME);
                final String localColumn   = reflect.getString(Reflection.PKCOLUMN_NAME);
                final String refSchemaName = reflect.getString(Reflection.FKTABLE_SCHEM);
                final String refTableName  = reflect.getString(Reflection.FKTABLE_NAME);
                final String refColumnName = reflect.getString(Reflection.FKCOLUMN_NAME);
                final int deleteRule = reflect.getInt(Reflection.DELETE_RULE);
                final boolean deleteCascade = DatabaseMetaData.importedKeyCascade == deleteRule;
                table.exportedKeys.add(new Relation(relationName, localColumn,
                        refSchemaName, refTableName, refColumnName, false, deleteCascade));

                if (refSchemaName != null && !visitedSchemas.contains(refSchemaName)) {
                    requieredSchemas.add(refSchemaName);
                }
            }
        }
        ftb.setName(tableName);
        table.tableType = ftb;
        return table;
    }

    private AttributeType<?> analyzeColumn(final DatabaseMetaData metadata, final ResultSet columnSet, final AttributeTypeBuilder<?> atb) throws SQLException {
        final String schemaName     = columnSet.getString(Reflection.TABLE_SCHEM);
        final String tableName      = columnSet.getString(Reflection.TABLE_NAME);
        final String columnName     = columnSet.getString(Reflection.COLUMN_NAME);
        final int columnSize        = columnSet.getInt   (Reflection.COLUMN_SIZE);
        final int columnDataType    = columnSet.getInt   (Reflection.DATA_TYPE);
        final String columnTypeName = columnSet.getString(Reflection.TYPE_NAME);
        final String columnNullable = columnSet.getString(Reflection.IS_NULLABLE);
        atb.setName(columnName);
        atb.setMaximalLength(columnSize);
        functions.decodeColumnType(atb, metadata.getConnection(), columnTypeName, columnDataType, schemaName, tableName, columnName);
        // TODO: need to distinguish "YES" and empty string?
        final Boolean b = SQLUtilities.parseBoolean(columnNullable);
        atb.setMinimumOccurs(b != null && !b ? 1 : 0);
        atb.setMaximumOccurs(1);
        return atb.build();
    }

    /**
     * Analyze the metadata of the ResultSet to rebuild a feature type.
     */
    final FeatureType analyzeResult(final ResultSet result, final String name) throws SQLException, DataStoreException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName(name);
        final ResultSetMetaData metadata = result.getMetaData();
        final int nbcol = metadata.getColumnCount();
        for (int i=1; i <= nbcol; i++) {
            final String columnName = metadata.getColumnName(i);
            final String columnLabel = metadata.getColumnLabel(i);
            final String schemaName = metadata.getSchemaName(i);
            final String tableName = metadata.getTableName(i);
            final int sqlType = metadata.getColumnType(i);
            final String sqlTypeName = metadata.getColumnTypeName(i);

            // Search if we already have this property
            PropertyType desc = null;
            final Schema schema = getSchemaMetaModel(schemaName);
            if (schema != null) {
                Table table = schema.getTable(tableName);
                if (table != null) {
                    try {
                        desc = table.getType(Table.View.SIMPLE_FEATURE_TYPE).build().getProperty(columnName);
                    } catch (PropertyNotFoundException ex) {
                        // ok
                    }
                }
            }
            if (desc != null) {
                ftb.addProperty(desc);
            } else {
                // could not find the original type
                // this column must be calculated
                final AttributeTypeBuilder<?> atb = ftb.addAttribute(Object.class);
                final int nullable = metadata.isNullable(i);
                atb.setName(columnLabel);
                atb.setMinimumOccurs(nullable == ResultSetMetaData.columnNullable ? 0 : 1);
                atb.setMaximumOccurs(1);
                atb.setName(columnLabel);
                atb.setValueClass(functions.getJavaType(sqlType, sqlTypeName));
            }
        }
        return ftb.build();
    }

    /**
     * Rebuild simple feature types for each table.
     */
    private void reverseSimpleFeatureTypes(final DatabaseMetaData metadata) throws SQLException {
        for (final Schema schema : schemas.values()) {
            for (final Table table : schema.getTables()) {
                final FeatureTypeBuilder ftb = new FeatureTypeBuilder(table.tableType.build());
                final String featureName = ftb.getName().tip().toString();
                ftb.setName(featureName);
                final List<PropertyTypeBuilder> descs = ftb.properties();
                boolean defaultGeomSet = false;
                for (int i=0,n=descs.size(); i<n; i++) {
                    final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) descs.get(i);
                    final String name = atb.getName().tip().toString();
                    atb.setName(name);
                    /*
                     * Configure CRS if the column contains a geometry or a raster.
                     */
                    final Class<?> binding = atb.getValueClass();
                    final boolean isGeometry = Geometries.isKnownType(binding);
                    if (isGeometry || Coverage.class.isAssignableFrom(binding)) {
                        // TODO: escape columnNamePattern with metadata.getSearchStringEscape().
                        try (ResultSet reflect = metadata.getColumns(null, schema.name, table.name, name)) {
                            while (reflect.next()) {        // Should loop exactly once.
                                CoordinateReferenceSystem crs = functions.createGeometryCRS(reflect);
                                atb.setCRS(crs);
                                if (isGeometry & !defaultGeomSet) {
                                    atb.addRole(AttributeRole.DEFAULT_GEOMETRY);
                                    defaultGeomSet = true;
                                }
                            }
                        }
                    }
                }
                table.simpleFeatureType = ftb;
            }
        }
    }
}
