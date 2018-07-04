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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
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
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.util.logging.WarningListeners;


/**
 * Represent the structure of the database.
 * The work done here is similar to reverse engineering.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class DataBaseModel {

    private static final String TYPE_TABLE = "TABLE";
    private static final String TYPE_VIEW  = "VIEW";
    private static final String VALUE_YES = "YES";
    private static final String VALUE_NO = "NO";

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

    private final SQLStore store;
    private final Dialect dialect;
    private final String databaseSchema;
    private final String databaseTable;

    private FeatureNaming<PrimaryKey> pkIndex = new FeatureNaming<>();
    private Set<GenericName> typeNames = new HashSet<>();
    private FeatureNaming<FeatureType> typeIndex = new FeatureNaming<>();
    private Map<String,SchemaMetaModel> schemas;
    private Set<GenericName> nameCache;

    //various cache while analyzing model
    private DatabaseMetaData metadata;
    private CachedResultSet cacheTables;
    private CachedResultSet cacheColumns;
    private CachedResultSet cachePrimaryKeys;
    private CachedResultSet cacheImportedKeys;
    private CachedResultSet cacheExportedKeys;
    private CachedResultSet cacheIndexInfos;
    //this set contains schema names which are needed to rebuild relations
    private Set<String> visitedSchemas;
    private Set<String> requieredSchemas;

    private final WarningListeners<DataStore> listeners;

    public DataBaseModel(final SQLStore store, final Dialect dialect, final String schema, final String table, final WarningListeners<DataStore> listeners) {
        this.store          = store;
        this.dialect        = dialect;
        this.databaseSchema = schema;
        this.databaseTable  = table;
        this.listeners      = listeners;
    }

    private Collection<SchemaMetaModel> getSchemaMetaModels() throws DataStoreException {
        if (schemas == null) {
            analyze();
        }
        return schemas.values();
    }

    private SchemaMetaModel getSchemaMetaModel(String name) throws DataStoreException{
        if (schemas == null) {
            analyze();
        }
        return schemas.get(name);
    }

    /**
     * Clear the model cache. A new database analyze will be made the next time it is needed.
     */
    private synchronized void clearCache() {
        pkIndex   = new FeatureNaming<>();
        typeIndex = new FeatureNaming<>();
        typeNames = new HashSet<>();
        nameCache = null;
        schemas   = null;
    }

    private PrimaryKey getPrimaryKey(final String featureTypeName) throws DataStoreException{
        if (schemas == null) {
            analyze();
        }
        return pkIndex.get(store, featureTypeName);
    }

    private synchronized Set<GenericName> getNames() throws DataStoreException {
        Set<GenericName> ref = nameCache;
        if (ref == null) {
            analyze();
            final Set<GenericName> names = new HashSet<>();
            for (GenericName name : typeNames) {
                final FeatureType type = typeIndex.get(store, name.toString());
                if (SUBTYPE.isAssignableFrom(type)) continue;
                if (dialect.isTableIgnored(name.tip().toString())) continue;
                names.add(name);
            }
            ref = Collections.unmodifiableSet(names);
            nameCache = ref;
        }
        return ref;
    }

    public FeatureType getFeatureType(final String typeName) throws DataStoreException {
        if (schemas == null) {
            analyze();
        }
        return typeIndex.get(store, typeName);
    }

    /**
     * Explores all tables and views then recreate a complex feature model from relations.
     */
    private synchronized void analyze() throws DataStoreException{
        if (schemas != null) {
            // Already analyzed
            return;
        }
        clearCache();
        schemas = new HashMap<>();
        visitedSchemas = new HashSet<>();
        requieredSchemas = new HashSet<>();

        try (Connection cx = store.getDataSource().getConnection()) {
            metadata = cx.getMetaData();
            /*
             * Schema names available in the database:
             * 1. TABLE_SCHEM   : String  =>  schema name
             * 2. TABLE_CATALOG : String  =>  catalog name (may be null)
             */
            final CachedResultSet cacheSchemas = new CachedResultSet(metadata.getSchemas(),
                    Reflection.TABLE_SCHEM);
            /*
             * Description of the tables available:
             * 1. TABLE_SCHEM : String  =>  table schema (may be null)
             * 2. TABLE_NAME  : String  =>  table name
             * 3. TABLE_TYPE  : String  =>  table type (typically "TABLE" or "VIEW").
             */
            cacheTables = new CachedResultSet(
                    metadata.getTables(null, null, null, new String[] {TYPE_TABLE, TYPE_VIEW}),   // TODO: use metadata.getTableTypes()
                    Reflection.TABLE_SCHEM,
                    Reflection.TABLE_NAME,
                    Reflection.TABLE_TYPE);
            cacheColumns = new CachedResultSet(metadata.getColumns(null, null, null, null),
                    Reflection.TABLE_SCHEM,
                    Reflection.TABLE_NAME,
                    Reflection.COLUMN_NAME,
                    Reflection.COLUMN_SIZE,
                    Reflection.DATA_TYPE,
                    Reflection.TYPE_NAME,
                    Reflection.IS_NULLABLE,
                    Reflection.IS_AUTOINCREMENT,
                    Reflection.REMARKS);
            cachePrimaryKeys = new CachedResultSet(metadata.getPrimaryKeys(null, null, null),
                    Reflection.TABLE_SCHEM,
                    Reflection.TABLE_NAME,
                    Reflection.COLUMN_NAME);
            cacheImportedKeys = new CachedResultSet(metadata.getImportedKeys(null, null, null),
                    Reflection.PK_NAME,
                    Reflection.FK_NAME,
                    Reflection.FKTABLE_SCHEM,
                    Reflection.FKTABLE_NAME,
                    Reflection.FKCOLUMN_NAME,
                    Reflection.PKTABLE_SCHEM,
                    Reflection.PKTABLE_NAME,
                    Reflection.PKCOLUMN_NAME,
                    Reflection.DELETE_RULE);
            cacheExportedKeys = new CachedResultSet(metadata.getExportedKeys(null, null, null),
                    Reflection.PK_NAME,
                    Reflection.FK_NAME,
                    Reflection.PKTABLE_SCHEM,
                    Reflection.PKTABLE_NAME,
                    Reflection.PKCOLUMN_NAME,
                    Reflection.FKTABLE_SCHEM,
                    Reflection.FKTABLE_NAME,
                    Reflection.FKCOLUMN_NAME,
                    Reflection.DELETE_RULE);

            ////////////////////////////////////////////////////////////////////////////////

            if (databaseSchema != null) {
                requieredSchemas.add(databaseSchema);
            } else {
                final Iterator<Map<String,Object>> ite = cacheSchemas.records.iterator();
                while (ite.hasNext()) {
                    requieredSchemas.add((String) ite.next().get(Reflection.TABLE_SCHEM));
                }
            }

            // We need to analyze requiered schema references
            while (!requieredSchemas.isEmpty()) {
                final String sn = requieredSchemas.iterator().next();
                visitedSchemas.add(sn);
                requieredSchemas.remove(sn);
                final SchemaMetaModel schema = analyzeSchema(sn,cx);
                schemas.put(schema.name, schema);
            }

            reverseSimpleFeatureTypes(cx);

        } catch (SQLException e) {
            throw new DataStoreException("Error occurred analyzing database model.\n" + e.getMessage(), e);
        } finally {
            cacheTables = null;
            cacheColumns = null;
            cachePrimaryKeys = null;
            cacheImportedKeys = null;
            cacheExportedKeys = null;
            cacheIndexInfos = null;
            metadata = null;
            visitedSchemas = null;
            requieredSchemas = null;
        }


        // Build indexes---------------------------------------------------------
        final String baseSchemaName = databaseSchema;

        final Collection<SchemaMetaModel> candidates;
        if (baseSchemaName == null) {
            //take all schemas
            candidates = getSchemaMetaModels();
        } else {
            candidates = Collections.singleton(getSchemaMetaModel(baseSchemaName));
        }

        for (SchemaMetaModel schema : candidates) {
           if (schema != null) {
                for (TableMetaModel table : schema.getTables()) {

                    final FeatureTypeBuilder ft = table.getType(TableMetaModel.View.SIMPLE_FEATURE_TYPE);
                    final GenericName name = ft.getName();
                    pkIndex.add(store, name, table.key);
                    if (table.isSubType()) {
                        // We don't show subtype, they are part of other feature types, add a flag to identify then
                        ft.setSuperTypes(SUBTYPE);
                    }
                    typeNames.add(name);
                    typeIndex.add(store, name, ft.build());
                 }
            } else {
                throw new DataStoreException("Specifed schema " + baseSchemaName + " does not exist.");
             }
         }

    }

    private SchemaMetaModel analyzeSchema(final String schemaName, final Connection cx) throws SQLException {
        final SchemaMetaModel schema = new SchemaMetaModel(schemaName);
        for (Map<String,Object> info : cacheTables.records) {
            if (!Objects.equals(info.get(Reflection.TABLE_SCHEM), schemaName)) continue;
            if (databaseTable != null && !databaseTable.isEmpty() && !Objects.equals(info.get(Reflection.TABLE_NAME), databaseTable)) continue;
            final TableMetaModel table = analyzeTable(info, cx);
            schema.tables.put(table.name, table);
        }
        return schema;
    }

    private TableMetaModel analyzeTable(final Map<String,Object> tableSet, final Connection cx) throws SQLException {
        final String schemaName = (String) tableSet.get(Reflection.TABLE_SCHEM);
        final String tableName  = (String) tableSet.get(Reflection.TABLE_NAME);
        final String tableType  = (String) tableSet.get(Reflection.TABLE_TYPE);
        final TableMetaModel table = new TableMetaModel(tableName,tableType);
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        // Explore all columns ----------------------------------------------
        final Predicate<Map<String,Object>> tableFilter = (Map<String,Object> info) -> {
            return Objects.equals(info.get(Reflection.TABLE_SCHEM), schemaName)
                && Objects.equals(info.get(Reflection.TABLE_NAME), tableName);
        };

        final Iterator<Map<String,Object>> ite1 = cacheColumns.records.stream().filter(tableFilter).iterator();
        while (ite1.hasNext()) {
            analyzeColumn(ite1.next(), cx, ftb.addAttribute(Object.class));
        }

        // Find primary key -------------------------------------------------
        final List<ColumnMetaModel> cols = new ArrayList<>();
        final Iterator<Map<String,Object>> pkIte = cachePrimaryKeys.records.stream().filter(tableFilter).iterator();
        while (pkIte.hasNext()) {
            final Map<String,Object> result = pkIte.next();
            final String columnName = (String) result.get(Reflection.COLUMN_NAME);

            final Predicate<Map<String,Object>> colFilter = (Map<String,Object> info) -> {
                return Objects.equals(info.get(Reflection.COLUMN_NAME), columnName);
            };
            final Iterator<Map<String,Object>> cite = cacheColumns.records.stream().filter(tableFilter.and(colFilter)).iterator();
            final Map<String,Object> column = cite.next();

            final int sqlType = ((Number) column.get(Reflection.DATA_TYPE)).intValue();
            final String sqlTypeName = (String) column.get(Reflection.TYPE_NAME);
            Class<?> columnType = dialect.getJavaType(sqlType, sqlTypeName);

            if (columnType == null) {
                listeners.warning("No class for SQL type " + sqlType, null);
                columnType = Object.class;
            }

            ColumnMetaModel col = null;

            final String str = (String) column.get(Reflection.IS_AUTOINCREMENT);
            if (VALUE_YES.equalsIgnoreCase(str)) {
                col = new ColumnMetaModel(schemaName, tableName, columnName, sqlType, sqlTypeName, columnType, ColumnMetaModel.Type.AUTO, null);
            } else {
                // TODO: need to distinguish "NO" and empty string.
                final String sequenceName = dialect.getColumnSequence(cx,schemaName, tableName, columnName);
                if (sequenceName != null) {
                    col = new ColumnMetaModel(schemaName, tableName, columnName, sqlType,
                            sqlTypeName, columnType, ColumnMetaModel.Type.SEQUENCED,sequenceName);
                } else {
                    col = new ColumnMetaModel(schemaName, tableName, columnName, sqlType,
                            sqlTypeName, columnType, ColumnMetaModel.Type.PROVIDED, null);
                }
            }
            cols.add(col);
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
        cacheIndexInfos = new CachedResultSet(metadata.getIndexInfo(null, schemaName, tableName, true, false),
                Reflection.TABLE_SCHEM,
                Reflection.TABLE_NAME,
                Reflection.COLUMN_NAME,
                Reflection.INDEX_NAME);
        final Iterator<Map<String,Object>> indexIte = cacheIndexInfos.records.stream().filter(tableFilter).iterator();
        while (indexIte.hasNext()) {
            final Map<String,Object> result = indexIte.next();
            final String columnName = (String) result.get(Reflection.COLUMN_NAME);
            final String idxName = (String) result.get(Reflection.INDEX_NAME);

            List<String> lst = uniqueIndexes.get(idxName);
            if (lst == null) {
                lst = new ArrayList<>();
                uniqueIndexes.put(idxName, lst);
            }
            lst.add(columnName);

            if (pkEmpty) {
                // We use a single index columns set as primary key
                // We must not mix with other potential indexes.
                if (indexname == null) {
                    indexname = idxName;
                } else if (!indexname.equals(idxName)) {
                    continue;
                }
                names.add(columnName);
            }
        }

        // For each unique index composed of one column add a flag on the property descriptor
        for (Entry<String,List<String>> entry : uniqueIndexes.entrySet()) {
            final List<String> columns = entry.getValue();
            if (columns.size() == 1) {
                String columnName = columns.get(0);
                for (PropertyTypeBuilder desc : ftb.properties()) {
                    if (desc.getName().tip().toString().equals(columnName)) {
                        final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) desc;
                        atb.addCharacteristic(ColumnMetaModel.JDBC_PROPERTY_UNIQUE).setDefaultValue(Boolean.TRUE);
                    }
                }
            }
        }

        if (pkEmpty && !names.isEmpty()) {
            // Build a primary key from unique index
            final Iterator<Map<String,Object>> ite = cacheColumns.records.stream().filter(tableFilter).iterator();
            while (ite.hasNext()) {
                final Map<String,Object> result = ite.next();
                final String columnName = (String) result.get(Reflection.COLUMN_NAME);
                if (!names.contains(columnName)) {
                    continue;
                }

                final int sqlType = ((Number) result.get(Reflection.DATA_TYPE)).intValue();
                final String sqlTypeName = (String) result.get(Reflection.TYPE_NAME);
                final Class<?> columnType = dialect.getJavaType(sqlType, sqlTypeName);
                final ColumnMetaModel col = new ColumnMetaModel(schemaName, tableName, columnName,
                        sqlType, sqlTypeName, columnType, ColumnMetaModel.Type.PROVIDED, null);
                cols.add(col);

                // Set as identifier
                for (PropertyTypeBuilder desc : ftb.properties()) {
                    if (desc.getName().tip().toString().equals(columnName)) {
                        final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) desc;
                        atb.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                        break;
                    }
                }
            }
        }


        if (cols.isEmpty()) {
            if (TYPE_TABLE.equals(tableType)) {
                listeners.warning("No primary key found for " + tableName, null);
            }
        }
        table.key = new PrimaryKey(tableName, cols);

        // Mark primary key columns
        for (PropertyTypeBuilder desc : ftb.properties()) {
            for (ColumnMetaModel col : cols) {
                if (desc.getName().tip().toString().equals(col.name)) {
                    final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) desc;
                    atb.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                    break;
                }
            }
        }


        // Find imported keys -----------------------------------------------
        final Predicate<Map<String,Object>> fkFilter = (Map<String,Object> info) -> {
            return Objects.equals(info.get(Reflection.FKTABLE_SCHEM), schemaName)
                && Objects.equals(info.get(Reflection.FKTABLE_NAME), tableName);
        };
        Iterator<Map<String,Object>> ite = cacheImportedKeys.records.stream().filter(fkFilter).iterator();
        while (ite.hasNext()) {
            final Map<String,Object> result = ite.next();
            String relationName = (String) result.get(Reflection.PK_NAME);
            if (relationName == null) relationName = (String) result.get(Reflection.FK_NAME);
            final String localColumn = (String) result.get(Reflection.FKCOLUMN_NAME);
            final String refSchemaName = (String) result.get(Reflection.PKTABLE_SCHEM);
            final String refTableName = (String) result.get(Reflection.PKTABLE_NAME);
            final String refColumnName = (String) result.get(Reflection.PKCOLUMN_NAME);
            final int deleteRule = ((Number) result.get(Reflection.DELETE_RULE)).intValue();
            final boolean deleteCascade = DatabaseMetaData.importedKeyCascade == deleteRule;
            final RelationMetaModel relation = new RelationMetaModel(relationName,localColumn,
                    refSchemaName, refTableName, refColumnName, true, deleteCascade);
            table.importedKeys.add(relation);

            if (refSchemaName!=null && !visitedSchemas.contains(refSchemaName)) requieredSchemas.add(refSchemaName);

            // Set the information
            for (PropertyTypeBuilder desc : ftb.properties()) {
                if (desc.getName().tip().toString().equals(localColumn)) {
                    final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) desc;
                    atb.addCharacteristic(ColumnMetaModel.JDBC_PROPERTY_RELATION).setDefaultValue(relation);
                    break;
                }
            }
        }

        // Find exported keys -----------------------------------------------
        final Predicate<Map<String,Object>> ekFilter = (Map<String,Object> info) -> {
            return Objects.equals(info.get(Reflection.PKTABLE_SCHEM), schemaName)
                && Objects.equals(info.get(Reflection.PKTABLE_NAME), tableName);
        };
        ite = cacheExportedKeys.records.stream().filter(ekFilter).iterator();
        while (ite.hasNext()) {
            final Map<String,Object> result = ite.next();
            String relationName = (String) result.get(Reflection.FKCOLUMN_NAME);
            if (relationName == null) relationName = (String) result.get(Reflection.FK_NAME);
            final String localColumn = (String) result.get(Reflection.PKCOLUMN_NAME);
            final String refSchemaName = (String) result.get(Reflection.FKTABLE_SCHEM);
            final String refTableName = (String) result.get(Reflection.FKTABLE_NAME);
            final String refColumnName = (String) result.get(Reflection.FKCOLUMN_NAME);
            final int deleteRule = ((Number) result.get(Reflection.DELETE_RULE)).intValue();
            final boolean deleteCascade = DatabaseMetaData.importedKeyCascade == deleteRule;
            table.exportedKeys.add(new RelationMetaModel(relationName, localColumn,
                    refSchemaName, refTableName, refColumnName, false, deleteCascade));

            if (refSchemaName != null && !visitedSchemas.contains(refSchemaName)) requieredSchemas.add(refSchemaName);
        }

        // Find parent table if any -----------------------------------------
//        if (handleSuperTableMetadata == null || handleSuperTableMetadata) {
//            try {
//                result = metadata.getSuperTables(null, schemaName, tableName);
//                while (result.next()) {
//                    final String parentTable = result.getString(SuperTable.SUPERTABLE_NAME);
//                    table.parents.add(parentTable);
//                }
//            } catch (SQLException ex) {
//                //not implemented by database
//                handleSuperTableMetadata = Boolean.FALSE;
//                store.getLogger().log(Level.INFO, "Database does not handle getSuperTable, feature type hierarchy will be ignored.");
//            } finally {
//                closeSafe(store.getLogger(),result);
//            }
//        }

        ftb.setName(tableName);
        table.tableType = ftb;
        return table;
    }

    private AttributeType<?> analyzeColumn(final Map<String,Object> columnSet, final Connection cx, final AttributeTypeBuilder<?> atb)
            throws SQLException
    {
        final String schemaName     = (String)  columnSet.get(Reflection.TABLE_SCHEM);
        final String tableName      = (String)  columnSet.get(Reflection.TABLE_NAME);
        final String columnName     = (String)  columnSet.get(Reflection.COLUMN_NAME);
        final int columnSize        = ((Number) columnSet.get(Reflection.COLUMN_SIZE)).intValue();
        final int columnDataType    = ((Number) columnSet.get(Reflection.DATA_TYPE)).intValue();
        final String columnTypeName = (String)  columnSet.get(Reflection.TYPE_NAME);
        final String columnNullable = (String)  columnSet.get(Reflection.IS_NULLABLE);
        atb.setName(columnName);
        atb.setMaximalLength(columnSize);
        dialect.decodeColumnType(atb, cx, columnTypeName, columnDataType, schemaName, tableName, columnName);
        // TODO: need to distinguish "YES" and empty string?
        atb.setMinimumOccurs(VALUE_NO.equalsIgnoreCase(columnNullable) ? 1 : 0);
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
            final String typeName = metadata.getColumnTypeName(i);
            final String schemaName = metadata.getSchemaName(i);
            final String tableName = metadata.getTableName(i);
            final int sqlType = metadata.getColumnType(i);
            final String sqlTypeName = metadata.getColumnTypeName(i);

            // Search if we already have this property
            PropertyType desc = null;
            final SchemaMetaModel schema = getSchemaMetaModel(schemaName);
            if (schema != null) {
                TableMetaModel table = schema.getTable(tableName);
                if (table != null) {
                    try {
                        desc = table.getType(TableMetaModel.View.SIMPLE_FEATURE_TYPE).build().getProperty(columnName);
                    } catch (PropertyNotFoundException ex) {
                        //ok
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

                try (Connection cx = store.getDataSource().getConnection()) {
                    final Class<?> type = dialect.getJavaType(sqlType, sqlTypeName);
                    //TODO : avoid jts, global geometry interface common to all ?
//                    if (type.equals(Geometry.class)) {
//                        // try to determine the real geometric type
//                        dialect.decodeGeometryColumnType(atb, cx, result, i, true);
//                    } else {
                        atb.setValueClass(type);
//                    }
                } catch (SQLException e) {
                    throw new DataStoreException("Error occurred analyzing column : " + columnName, e);
                }
            }
        }
        return ftb.build();
    }

    /**
     * Rebuild simple feature types for each table.
     */
    private void reverseSimpleFeatureTypes(final Connection cx) throws SQLException {
        for (final SchemaMetaModel schema : schemas.values()) {
            for (final TableMetaModel table : schema.getTables()) {
                final String tableName = table.name;

                final FeatureTypeBuilder ftb = new FeatureTypeBuilder(table.tableType.build());
                final String featureName = ftb.getName().tip().toString();
                ftb.setName(featureName);

                final List<PropertyTypeBuilder> descs = ftb.properties();

                boolean defaultGeomSet = false;
                for (int i=0,n=descs.size(); i<n; i++) {
                    final AttributeTypeBuilder<?> atb = (AttributeTypeBuilder) descs.get(i);
                    final String name = atb.getName().tip().toString();

                    atb.setName(name);

                    //Configure CRS if it is a geometry
                    final Class<?> binding = atb.getValueClass();
                    if (Geometries.isKnownType(binding)) {

                        final Predicate<Map<?,?>> colFilter = (Map<?,?> info) -> {
                            return Objects.equals(info.get(Reflection.TABLE_SCHEM), schema.name)
                                && Objects.equals(info.get(Reflection.TABLE_NAME), tableName)
                                && Objects.equals(info.get(Reflection.COLUMN_NAME), name);
                        };
                        final Map<String,Object> metas = cacheColumns.records.stream().filter(colFilter).findFirst().get();

                        CoordinateReferenceSystem crs = null;
                        Integer srid = dialect.getGeometrySRID(databaseSchema, tableName, name, metas, cx);
                        if (srid != null) {
                            crs = dialect.createCRS(srid, cx);
                        }
                        atb.setCRS(crs);
                        if (srid != null) {
                            atb.addCharacteristic(ColumnMetaModel.JDBC_PROPERTY_SRID).setDefaultValue(srid);
                            if (!defaultGeomSet) {
                                atb.addRole(AttributeRole.DEFAULT_GEOMETRY);
                                defaultGeomSet = true;
                            }
                        }
                    } else if (Coverage.class.isAssignableFrom(binding)) {
                        final Predicate<Map<String,Object>> colFilter = (Map<String,Object> info) -> {
                            return Objects.equals(info.get(Reflection.TABLE_SCHEM), schema.name)
                                && Objects.equals(info.get(Reflection.TABLE_NAME), tableName)
                                && Objects.equals(info.get(Reflection.COLUMN_NAME), name);
                        };
                        final Map<String,Object> metas = cacheColumns.records.stream().filter(colFilter).findFirst().get();

                        // Add the attribute as a geometry, try to figure out its srid first
                        CoordinateReferenceSystem crs = null;
                        Integer srid = dialect.getGeometrySRID(databaseSchema, tableName, name, metas, cx);
                        if (srid != null) {
                            crs = dialect.createCRS(srid, cx);
                        }
                        atb.setCRS(crs);
                        if (srid != null) {
                            atb.addCharacteristic(ColumnMetaModel.JDBC_PROPERTY_SRID).setDefaultValue(srid);
                            //not working yet, SIS FeatureTypeBuilder do not reconize Coverage as a geometry type.
//                            if (!defaultGeomSet) {
//                                //create a computed geometry from coverage envelope
//                                PropertyTypeBuilder geomProp = ftb.addProperty(new CoverageGeometryOperation(AttributeConvention.GEOMETRY_PROPERTY, atb.getName().toString()));
//                                try {
//                                    ftb.addProperty(FeatureOperations.envelope(
//                                        Collections.singletonMap(AbstractOperation.NAME_KEY,AttributeConvention.ENVELOPE_PROPERTY), null, geomProp.build()));
//                                } catch (FactoryException e) {
//                                    throw new IllegalStateException(e);
//                                }
//                                defaultGeomSet = true;
//                            }
                        }
                    }
                }
                table.simpleFeatureType = ftb;
            }
        }
    }
}
