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
package org.apache.sis.metadata.sql;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Collection;
import java.util.logging.Level;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.FactoryException;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.TypeValuePolicy;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.sql.privy.SQLBuilder;
import org.apache.sis.metadata.sql.privy.Reflection;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.xml.IdentifiedObject;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.ControlledVocabulary;


/**
 * A connection to a metadata database with write capabilities. The database must have a schema of the given name,
 * which can be initially empty. Tables and columns are created as needed when the {@link #add(Object)} method is
 * invoked.
 *
 * <p>No more than one instance of {@code MetadataWriter} should be used for the same database.
 * However, multiple instances of {@code MetadataSource} can be used concurrently with a single
 * {@code MetadataWriter} instance on the same database.</p>
 *
 * <h2>Properties</h2>
 * The constructor expects three Java arguments (the {@linkplain MetadataStandard metadata standard},
 * the {@linkplain DataSource data source} and the database schema) completed by an arbitrary amount
 * of optional arguments given as a map of properties.
 * The following keys are recognized by {@code MetadataSource} and all other entries are ignored:
 *
 * <table class="sis">
 *   <caption>Optional properties at construction time</caption>
 *   <tr>
 *     <th>Key</th>
 *     <th>Value type</th>
 *     <th>Description</th>
 *   </tr><tr>
 *     <td>{@code "catalog"}</td>
 *     <td>{@link String}</td>
 *     <td>The database catalog where the metadata schema is stored.</td>
 *   </tr><tr>
 *     <td>{@code "classloader"}</td>
 *     <td>{@link ClassLoader}</td>
 *     <td>The class loader to use for creating {@link java.lang.reflect.Proxy} instances.</td>
 *   </tr><tr>
 *     <td>{@code "maxStatements"}</td>
 *     <td>{@link Integer}</td>
 *     <td>Maximal number of {@link java.sql.PreparedStatement}s that can be kept simultaneously open.</td>
 *   </tr><tr>
 *     <td>{@code "maximumIdentifierLength"}</td>
 *     <td>{@link Integer}</td>
 *     <td>The maximal number of characters allowed for primary keys.
 *         This is the value given to the {@code VARCHAR} type when creating new {@code "ID"} columns.</td>
 *   </tr><tr>
 *     <td>{@code "maximumValueLength"}</td>
 *     <td>{@link Integer}</td>
 *     <td>Maximal number of characters allowed in text columns. This is the parameter given to the {@code VARCHAR}
 *         type when creating new columns. Attempts to insert a text longer than this limit will typically throws
 *         a {@link SQLException}, but the exact behavior is database-dependent.</td>
 *   </tr><tr>
 *       <td>{@code "columnCreationPolicy"}</td>
 *       <td>{@link ValueExistencePolicy}</td>
 *       <td>Whether columns should be created only for non-empty attributes ({@link ValueExistencePolicy#NON_EMPTY
 *           NON_EMPTY}, the default) or for all attributes ({@link ValueExistencePolicy#ALL ALL})</td>
 *   </tr>
 * </table>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 */
public class MetadataWriter extends MetadataSource {
    /**
     * The name of the column for code list.
     */
    private static final String CODE_COLUMN = "CODE";

    /**
     * Minimum value allowed for {@link #maximumIdentifierLength}.
     */
    private static final int MINIMAL_LIMIT = 5;

    /**
     * Maximal length for the identifier. This applies also to code list values.
     */
    private final int maximumIdentifierLength;

    /**
     * Maximal length of values.
     */
    private final int maximumValueLength;

    /**
     * Whether the tables should contain a column for every attribute, or only for non-null
     * and non-empty attributes. The default is {@link ValueExistencePolicy#NON_EMPTY NON-EMPTY}.
     */
    private final ValueExistencePolicy columnCreationPolicy;

    /**
     * Creates a new metadata writer.
     *
     * @param  standard    the metadata standard to implement.
     * @param  dataSource  the source for getting a connection to the database.
     * @param  schema      the database schema were metadata tables are stored, or {@code null} if none.
     * @param  properties  additional options, or {@code null} if none. See class javadoc for a description.
     */
    public MetadataWriter(final MetadataStandard standard, final DataSource dataSource, final String schema,
            final Map<String,?> properties)
    {
        super(standard, dataSource, schema, properties);
        Integer maximumIdentifierLength           = Containers.property(properties, "maximumIdentifierLength", Integer.class);
        Integer maximumValueLength                = Containers.property(properties, "maximumValueLength",      Integer.class);
        ValueExistencePolicy columnCreationPolicy = Containers.property(properties, "columnCreationPolicy",    ValueExistencePolicy.class);
        if (maximumIdentifierLength != null) {
            ArgumentChecks.ensureBetween("maximumIdentifierLength", MINIMAL_LIMIT, 100, maximumIdentifierLength);
            this.maximumIdentifierLength = maximumIdentifierLength;
        } else {
            this.maximumIdentifierLength = 24;
        }
        if (maximumValueLength != null) {
            ArgumentChecks.ensureBetween("maximumValueLength", MINIMAL_LIMIT, Short.MAX_VALUE, maximumValueLength);
            this.maximumValueLength = maximumValueLength;
        } else {
            this.maximumValueLength = 1000;
        }
        this.columnCreationPolicy = (columnCreationPolicy != null) ? columnCreationPolicy : ValueExistencePolicy.NON_EMPTY;
    }

    /**
     * Adds the given metadata object to the database, if it does not already exists.
     * If the database already contains a metadata equals to the given one, then the
     * database is left unchanged and the identifier of the existing metadata is returned.
     *
     * @param  metadata  the metadata object to add.
     * @return the identifier (primary key) of the metadata just added,
     *         or the identifier of the existing metadata is one exists.
     * @throws MetadataStoreException if the metadata object does not implement a metadata interface
     *         of the expected package, if an exception occurred while reading or writing the database.
     *         In such case, the database content is left unchanged
     *         (i.e. this method is a <i>all or nothing</i> operation).
     */
    public String add(final Object metadata) throws MetadataStoreException {
        String identifier = proxy(metadata);
        if (identifier == null) try {
            synchronized (this) {
                final Connection connection = connection();
                connection.setAutoCommit(false);
                boolean success = false;
                try {
                    try (Statement stmt = connection.createStatement()) {
                        if (metadata instanceof ControlledVocabulary) {
                            identifier = addCode(stmt, (ControlledVocabulary) metadata);
                        } else {
                            identifier = add(stmt, metadata, new IdentityHashMap<>(), null);
                        }
                    }
                    success = true;
                } finally {
                    if (success) {
                        connection.commit();
                    } else {
                        connection.rollback();
                    }
                    connection.setAutoCommit(true);
                }
            }
        } catch (ClassCastException e) {
            throw new MetadataStoreException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_2, "metadata", metadata.getClass()));
        } catch (SQLException e) {
            /*
             * Derby sometimes wraps SQLException into another SQLException.  For making the stack strace a
             * little bit simpler, keep only the root cause provided that the exception type is compatible.
             */
            throw new MetadataStoreException(e.getLocalizedMessage(), Exceptions.unwrap(e));
        } catch (FactoryException e) {
            throw new MetadataStoreException(e.getLocalizedMessage(), e);
        }
        return identifier;
    }

    /**
     * Implementation of the {@link #add(Object)} method. This method invokes itself recursively,
     * and maintains a map of metadata inserted up to date in order to avoid infinite recursion.
     *
     * @param  stmt      the statement to use for inserting data.
     * @param  metadata  the metadata object to add.
     * @param  done      the metadata objects already added, mapped to their primary keys.
     * @param  parent    the primary key of the parent, or {@code null} if there is no parent.
     *                   This identifier shall not contain {@linkplain #isReservedChar(int) reserved characters}.
     * @return the identifier (primary key) of the metadata just added.
     * @throws SQLException if an exception occurred while reading or writing the database.
     * @throws ClassCastException if the metadata object does not implement a metadata interface
     *         of the expected package.
     */
    private String add(final Statement stmt, final Object metadata, final Map<Object,String> done,
            final String parent) throws ClassCastException, SQLException, FactoryException
    {
        final SQLBuilder helper = helper();
        /*
         * Take a snapshot of the metadata content. We do that in order to protect ourself against
         * concurrent changes in the metadata object. This protection is needed because we need to
         * perform multiple passes on the same metadata.
         */
        final Map<String,Object> asValueMap = asValueMap(metadata);
        final Map<String,Object> asSingletons = new LinkedHashMap<>();
        for (final Map.Entry<String,Object> entry : asValueMap.entrySet()) {
            asSingletons.put(entry.getKey(), extractFromCollection(entry.getValue()));
        }
        /*
         * Search the database for an existing metadata.
         */
        final Class<?> implementationType = metadata.getClass();
        final Class<?> interfaceType = standard.getInterface(implementationType);
        final String table = getTableName(interfaceType);
        final Set<String> columns = getExistingColumns(table);
        String identifier = search(table, columns, asSingletons, stmt, helper);
        if (identifier != null) {
            if (done.put(metadata, identifier) != null) {
                throw new AssertionError(metadata);
            }
            return identifier;
        }
        /*
         * Trim the null values or empty collections. We perform this operation only after the check
         * for existing entries, in order to take in account null values when checking existing entries.
         */
        if (columnCreationPolicy != ValueExistencePolicy.ALL) {
            for (final Iterator<Object> it = asSingletons.values().iterator(); it.hasNext();) {
                if (it.next() == null) {
                    it.remove();
                }
            }
        }
        /*
         * Process to the table creation if it does not already exists. If the table has parents, they will be
         * created first. The latter will work only for database supporting table inheritance, like PostgreSQL.
         * For other kind of database engine, we cannot store metadata having parent interfaces.
         */
        Boolean isChildTable = createTable(stmt, interfaceType, table, columns);
        if (isChildTable == null) {
            isChildTable = isChildTable(interfaceType);
        }
        /*
         * Add missing columns if there is any. If columns are added, we will keep trace of foreigner keys in
         * this process but will not create the constraints now because the foreigner tables may not exist yet.
         * They will be created later by recursive calls to this method a little bit below.
         */
        Map<String,Class<?>> colTypes = null, colTables = null;
        final Map<String,FKey> foreigners = new LinkedHashMap<>();
        for (final String column : asSingletons.keySet()) {
            if (!columns.contains(column)) {
                if (colTypes == null) {
                    colTypes  = standard.asTypeMap(implementationType, NAME_POLICY, TypeValuePolicy.ELEMENT_TYPE);
                    colTables = standard.asTypeMap(implementationType, NAME_POLICY, TypeValuePolicy.DECLARING_INTERFACE);
                }
                /*
                 * We have found a column to add. Check if the column actually needs to be added to the parent table
                 * (if such parent exists). In most case, the answer is "no" and `addTo` is equal to `table`.
                 */
                String addTo = table;
                if (helper.dialect.supportsTableInheritance()) {
                    @SuppressWarnings("null")     // `colTables` is initialized in same time as `colTypes`.
                    final Class<?> declaring = colTables.get(column);
                    if (!interfaceType.isAssignableFrom(declaring)) {
                        addTo = getTableName(declaring);
                    }
                }
                /*
                 * Determine the column data type. We infer that type from the method return value, not from
                 * actual value of given metadata object, since the value type for the same property may be
                 * different in future calls to this method.
                 */
                int maxLength = maximumValueLength;
                Class<?> rt = colTypes.get(column);
                final boolean isCodeList = ControlledVocabulary.class.isAssignableFrom(rt);     // Accept also some enums.
                if (isCodeList || standard.isMetadata(rt)) {
                    /*
                     * Found a reference to another metadata. Remind that column for creating a foreign key
                     * constraint later, except if the return type is an abstract CodeList or Enum (in which
                     * case the reference could be to any CodeList or Enum table). Abstract CodeList or Enum
                     * may happen when the concrete class is not yet available in the GeoAPI version that we
                     * are using.
                     */
                    if (!isCodeList || !Modifier.isAbstract(rt.getModifiers())) {
                        if (foreigners.put(column, new FKey(addTo, rt, null)) != null) {
                            throw new AssertionError(column);                           // Should never happen.
                        }
                    }
                    rt = null;                                                          // For forcing VARCHAR type.
                    maxLength = maximumIdentifierLength;
                } else if (rt.isEnum()) {
                    maxLength = maximumIdentifierLength;
                }
                stmt.executeUpdate(helper.createColumn(schema(), addTo, column, rt, maxLength));
                columns.add(column);
            }
        }
        /*
         * Get the identifier for the new metadata. If no identifier is proposed, we will try to recycle
         * the identifier of the parent.  For example, in ISO 19115, Contact (which contains phone number,
         * etc.) is associated only to Responsibility. So it make sense to use the Responsibility ID for
         * the contact info.
         */
        identifier = Strings.trimOrNull(removeReservedChars(suggestIdentifier(metadata, asValueMap), null));
        if (identifier == null) {
            identifier = parent;
            if (identifier == null) {
                /*
                 * Arbitrarily pickup the first non-metadata attribute.
                 * Fallback on "unknown" if none are found.
                 */
                identifier = "unknown";
                for (final Object value : asSingletons.values()) {
                    if (value != null && !standard.isMetadata(value.getClass())) {
                        identifier = abbreviation(value.toString());
                        break;
                    }
                }
            }
        }
        /*
         * If the record to add is located in a child table, we need to prepend the child table name
         * in the identifier in order to allow MetadataSource to locate the right table to query.
         */
        final int minimalIdentifierLength;
        if (isChildTable) {
            identifier = TableHierarchy.encode(table, identifier);
            minimalIdentifierLength = table.length() + 2;
        } else {
            minimalIdentifierLength = 0;
        }
        /*
         * Check for key collision. We will add a suffix if there is one. Note that the final identifier must be
         * found before we put its value in the map, otherwise cyclic references (if any) will use the wrong value.
         *
         * First, we trim the identifier (primary key) to the maximal length. Then, the loop removes at most four
         * additional characters if the identifier is still too long. After that point, if the identifier still too
         * long, we will let the database driver produces its own SQLException.
         */
        try (IdentifierGenerator idCheck = new IdentifierGenerator(this, schema(), table, ID_COLUMN, helper)) {
            for (int i=0; i<MINIMAL_LIMIT-1; i++) {
                final int maxLength = maximumIdentifierLength - i;
                if (maxLength < minimalIdentifierLength) break;
                if (identifier.length() > maxLength) {
                    identifier = identifier.substring(0, maxLength);
                }
                identifier = idCheck.identifier(identifier);
                if (identifier.length() <= maximumIdentifierLength) {
                    break;
                }
            }
        }
        if (done.put(metadata, identifier) != null) {
            throw new AssertionError(metadata);
        }
        /*
         * Process all dependencies now. This block may invoke this method recursively.
         * Once a dependency has been added to the database, the corresponding value in
         * the `asMap` HashMap is replaced by the identifier of the dependency we just added.
         */
        Map<String,FKey> referencedTables = null;
        for (final Map.Entry<String,Object> entry : asSingletons.entrySet()) {
            Object value = entry.getValue();
            final Class<?> type = value.getClass();
            if (ControlledVocabulary.class.isAssignableFrom(type)) {
                value = addCode(stmt, (ControlledVocabulary) value);
            } else if (type.isEnum()) {
                value = ((Enum<?>) value).name();
            } else if (standard.isMetadata(type)) {
                String dependency = proxy(value);
                if (dependency == null) {
                    dependency = done.get(value);
                    if (dependency == null) {
                        dependency = add(stmt, value, done, identifier);
                        assert done.get(value) == dependency;                       // Really identity comparison.
                        if (!helper.dialect.supportsIndexInheritance()) {
                            /*
                             * In a classical object-oriented model, the foreigner key constraints declared in the
                             * parent table would take in account the records in the child table and we would have
                             * nothing special to do. However, PostgreSQL 9.1 does not yet inherit index. So if we
                             * detect that a column references some records in two different tables, then we must
                             * suppress the foreigner key constraint.
                             */
                            final String column = entry.getKey();
                            final Class<?> targetType = standard.getInterface(value.getClass());
                            FKey fkey = foreigners.get(column);
                            if (fkey != null && !targetType.isAssignableFrom(fkey.tableType)) {
                                /*
                                 * The foreigner key constraint does not yet exist, so we can
                                 * change the target table. Set the target to the child table.
                                 */
                                fkey.tableType = targetType;
                            }
                            if (fkey == null) {
                                /*
                                 * The foreigner key constraint may already exist. Get a list of all foreigner keys for
                                 * the current table, then verify if the existing constraint references the right table.
                                 */
                                if (referencedTables == null) {
                                    referencedTables = new HashMap<>();
                                    try (ResultSet rs = stmt.getConnection().getMetaData().getImportedKeys(catalog, schema(), table)) {
                                        while (rs.next()) {
                                            if ((schema() == null || schema().equals(rs.getString(Reflection.PKTABLE_SCHEM))) &&
                                                (catalog  == null || catalog.equals(rs.getString(Reflection.PKTABLE_CAT))))
                                            {
                                                referencedTables.put(rs.getString(Reflection.FKCOLUMN_NAME),
                                                            new FKey(rs.getString(Reflection.PKTABLE_NAME), null,
                                                                     rs.getString(Reflection.FK_NAME)));
                                            }
                                        }
                                    }
                                }
                                fkey = referencedTables.remove(column);
                                if (fkey != null && !fkey.tableName.equals(getTableName(targetType))) {
                                    /*
                                     * The existing foreigner key constraint doesn't reference the right table.
                                     * We have no other choice than removing it...
                                     */
                                    stmt.executeUpdate(helper.clear().append("ALTER TABLE ")
                                            .appendIdentifier(schema(), table).append(" DROP CONSTRAINT ")
                                            .appendIdentifier(fkey.keyName).toString());
                                    warning(MetadataWriter.class, "add", Messages.forLocale(null)
                                            .createLogRecord(Level.WARNING, Messages.Keys.DroppedForeignerKey_1,
                                            table + '.' + column + " ⇒ " + fkey.tableName + '.' + ID_COLUMN));
                                }
                            }
                        }
                    }
                }
                value = dependency;
            }
            entry.setValue(value);
        }
        /*
         * Now that all dependencies have been inserted in the database, we can setup the foreigner key constraints
         * if there is any. Note that we deferred the foreigner key creations not because of the missing rows,
         * but because of missing tables (since new tables may be created in the process of inserting dependencies).
         */
        if (!foreigners.isEmpty()) {
            for (final Map.Entry<String,FKey> entry : foreigners.entrySet()) {
                final FKey fkey = entry.getValue();
                Class<?> rt = fkey.tableType;
                final boolean isCodeList = ControlledVocabulary.class.isAssignableFrom(rt);
                final String primaryKey;
                if (isCodeList) {
                    primaryKey = CODE_COLUMN;
                } else {
                    primaryKey = ID_COLUMN;
                    rt = standard.getInterface(rt);
                }
                final String column = entry.getKey();
                final String target = getTableName(rt);
                stmt.executeUpdate(helper.createForeignKey(
                        schema(), fkey.tableName, column,       // Source (schema.table.column)
                        target, primaryKey,                     // Target (table.column)
                        !isCodeList));                          // CASCADE if metadata, RESTRICT if CodeList or Enum.
                /*
                 * In a classical object-oriented model, the constraint would be inherited by child tables.
                 * However, this is not yet supported as of PostgreSQL 9.6. If inheritance is not supported,
                 * then we have to repeat the constraint creation in child tables.
                 */
                if (!helper.dialect.supportsIndexInheritance() && !table.equals(fkey.tableName)) {
                    stmt.executeUpdate(helper.createForeignKey(schema(), table, column, target, primaryKey, !isCodeList));
                }
            }
        }
        /*
         * Create the SQL statement which will insert the data.
         */
        helper.clear().append(SQLBuilder.INSERT).appendIdentifier(schema(), table).append(" (").appendIdentifier(ID_COLUMN);
        for (final String column : asSingletons.keySet()) {
            helper.append(", ").appendIdentifier(column);
        }
        helper.append(") VALUES (").appendValue(identifier);
        for (final Object value : asSingletons.values()) {
            helper.append(", ").appendValue(toStorableValue(value));
        }
        final String sql = helper.append(')').toString();
        if (stmt.executeUpdate(sql) != 1) {
            throw new SQLException(Errors.format(Errors.Keys.DatabaseUpdateFailure_3, 0, table, identifier));
        }
        return identifier;
    }

    /**
     * Information about the source and the target of a foreigner key. This class stores only the table names
     * (indirectly in the case of {@link #tableType}, since the name is derived from the type).
     * The column name are known by other way: either as the map key in the case of the source,
     * or fixed to {@value MetadataWriter#ID_COLUMN} in the case of the target.
     */
    private static final class FKey {
        final String tableName;                 // May be source or target, depending on the context.
        Class<?>     tableType;                 // Always the target table.
        final String keyName;

        FKey(final String tableName, final Class<?> tableType, final String keyName) {
            this.tableName = tableName;
            this.tableType = tableType;
            this.keyName   = keyName;
        }
    }

    /**
     * Returns the parent of the given type. Normally, {@code type} is an interface, in which case the parent types are
     * other interfaces that the given type extends. But in some cases (e.g. when Apache SIS implements a new ISO 19115
     * type not yet defined in GeoAPI), the given type is a class. In such cases we ignore its interface (it usually do
     * not implement any) and look for its parent class.
     */
    private static Class<?>[] getParentTypes(final Class<?> type) {
        return type.isInterface() ? type.getInterfaces() : new Class<?>[] {type.getSuperclass()};
    }

    /**
     * Returns {@code true} if the given metadata type is a subtype of another metadata.
     * If true, then we will need to prefix the identifier by the metadata subtype.
     *
     * @return whether the given metadata is a subtype of another metadata. This method never return {@code null}, but
     *         the result is nevertheless given as a {@code Boolean} wrapper for consistency with {@code createTable(…)}.
     */
    private Boolean isChildTable(final Class<?> type) {
        for (final Class<?> candidate : getParentTypes(type)) {
            if (standard.isMetadata(candidate)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Creates a table for the given type, if the table does not already exists.
     * This method may call itself recursively for creating parent tables, if they do not exist neither.
     * This method opportunistically computes the same return value as {@link #isChildTable(Class)}.
     *
     * @param  stmt     the statement to use for creating tables.
     * @param  type     the interface class.
     * @param  table    the name of the table (should be consistent with the type).
     * @param  columns  the existing columns, as an empty set if the table does not exist yet.
     * @return the value that {@code isChildTable(type)} would return, or {@code null} if undetermined.
     * @throws SQLException if an error occurred while creating the table.
     */
    private Boolean createTable(final Statement stmt, final Class<?> type, final String table, final Set<String> columns)
            throws SQLException
    {
        Boolean isChildTable = null;
        if (columns.isEmpty()) {
            isChildTable = Boolean.FALSE;
            StringBuilder inherits = null;
            for (final Class<?> candidate : getParentTypes(type)) {
                if (standard.isMetadata(candidate)) {
                    isChildTable = Boolean.TRUE;
                    final SQLBuilder helper = helper();
                    if (helper.dialect.supportsTableInheritance()) {
                        final String parent = getTableName(candidate);
                        createTable(stmt, candidate, parent, getExistingColumns(parent));
                        if (inherits == null) {
                            helper.clear().append("CREATE TABLE ").appendIdentifier(schema(), table);
                            if (!helper.dialect.supportsIndexInheritance()) {
                                /*
                                 * In a classical object-oriented model, the new child table would inherit the index from
                                 * its parent table. However, this is not yet the case as of PostgreSQL 9.6. If the index is
                                 * not inherited, then we have to repeat the primary key creation in every child tables.
                                 */
                                helper.append("(CONSTRAINT ").appendIdentifier(table + "_pkey")
                                      .append(" PRIMARY KEY (").appendIdentifier(ID_COLUMN).append(")) ");
                            }
                            inherits = new StringBuilder(helper.append(" INHERITS (").toString());
                        } else {
                            inherits.append(", ");
                        }
                        inherits.append(helper.clear().appendIdentifier(schema(), parent));
                    }
                }
            }
            final String sql;
            if (inherits != null) {
                sql = inherits.append(')').toString();
            } else {
                sql = createTable(table, ID_COLUMN);
            }
            stmt.executeUpdate(sql);
            columns.add(ID_COLUMN);
        }
        return isChildTable;
    }

    /**
     * Returns the SQL statement for creating the given table with the given primary key.
     * This method returns a string of the following form:
     *
     * {@snippet lang="sql" :
     *     CREATE TABLE "schema"."table" (primaryKey VARCHAR(20) NOT NULL PRIMARY KEY)
     *     }
     */
    private String createTable(final String table, final String primaryKey) throws SQLException {
        return helper().clear().append("CREATE TABLE ").appendIdentifier(schema(), table)
                .append(" (").appendIdentifier(primaryKey).append(" VARCHAR(").append(maximumIdentifierLength)
                .append(") NOT NULL PRIMARY KEY)").toString();
    }

    /**
     * Adds a code list if it is not already present. This is used only in order to enforce
     * foreigner key constraints in the database. The value of CodeList tables are not used
     * at parsing time. Enumerations are handled as if they were CodeLists; we do not use
     * the native SQL {@code ENUM} type for making easier to add new values when a standard
     * is updated.
     */
    private String addCode(final Statement stmt, final ControlledVocabulary code) throws SQLException {
        assert Thread.holdsLock(this);
        final String table = getTableName(code.getClass());
        final Set<String> columns = getExistingColumns(table);
        if (columns.isEmpty()) {
            stmt.executeUpdate(createTable(table, CODE_COLUMN));
            columns.add(CODE_COLUMN);
        }
        final String identifier = Types.getCodeName(code);
        final String query = helper().clear().append(SQLBuilder.SELECT).appendIdentifier(CODE_COLUMN)
                .append(" FROM ").appendIdentifier(schema(), table).append(" WHERE ")
                .appendIdentifier(CODE_COLUMN).appendEqualsValue(identifier).toString();
        final boolean exists;
        try (ResultSet rs = stmt.executeQuery(query)) {
            exists = rs.next();
        }
        if (!exists) {
            final String sql = helper().clear().append(SQLBuilder.INSERT).appendIdentifier(schema(), table)
                    .append(" (").appendIdentifier(CODE_COLUMN).append(") VALUES (").appendValue(identifier)
                    .append(')').toString();
            if (stmt.executeUpdate(sql) != 1) {
                throw new SQLException(Errors.format(Errors.Keys.DatabaseUpdateFailure_3, 0, table, identifier));
            }
        }
        return identifier;
    }

    /**
     * Suggests an identifier (primary key) to be used for the given metadata. This method is invoked automatically
     * when a new metadata is about to be inserted in the database. The default implementation uses heuristic rules
     * for a few "well known" metadata like {@link Identifier} and {@link Citation}. Subclasses can override this
     * method for implementing their own heuristic.
     *
     * <p>This method does not need to care about key collision.
     * The caller will adds some suffix if this is necessary for differentiating otherwise identical identifiers.</p>
     *
     * @param  metadata    the metadata instance for which to suggests an identifier.
     * @param  asValueMap  a view of all metadata properties as a map.
     *                     Keys are {@linkplain KeyNamePolicy#UML_IDENTIFIER UML identifiers}.
     * @return the proposed identifier, or {@code null} if this method does not have any suggestion.
     * @throws SQLException if an access to the database was desired but failed.
     */
    protected String suggestIdentifier(final Object metadata, final Map<String,Object> asValueMap) throws SQLException {
        String identifier = null;
        final Collection<? extends Identifier> identifiers;
        if (metadata instanceof Identifier) {
            identifiers = Set.of((Identifier) metadata);
        } else if (metadata instanceof IdentifiedObject) {
            identifiers = ((IdentifiedObject) metadata).getIdentifiers();
        } else {
            identifiers = Set.of();
        }
        for (final Identifier id : identifiers) {
            identifier = Strings.trimOrNull(id.getCode());
            if (identifier != null) {
                final String cs = Strings.trimOrNull(id.getCodeSpace());
                if (cs != null) {
                    identifier = cs + Constants.DEFAULT_SEPARATOR + identifier;
                }
                break;
            }
        }
        if (identifier == null && metadata instanceof Citation) {
            identifier = Strings.trimOrNull(Citations.toCodeSpace((Citation) metadata));
        }
        if (identifier == null) {
            final TitleProperty tp = metadata.getClass().getAnnotation(TitleProperty.class);
            if (tp != null) {
                final Object value = asValueMap.get(Strings.trimOrNull(tp.name()));
                if (value != null) {
                    identifier = Strings.trimOrNull(value.toString());
                }
            }
        }
        /*
         * At this point we got a suggested identifier, but it may be quite long.
         * For example, it may be a citation title. Try to make an abbreviation.
         */
        if (identifier != null && identifier.length() >= 8) {                   // Arbitrary threshold.
            identifier = abbreviation(identifier);
        }
        return identifier;
    }

    /**
     * Returns an abbreviation of the given identifier, if one is found.
     * The returned identifier is guaranteed to not contain {@linkplain #isReservedChar(int) reserved characters}.
     */
    private static String abbreviation(final String identifier) {
        final var buffer = new StringBuilder();
        for (final String token : identifier.split("\\s+")) {
            final int c = token.codePointAt(0);
            if (!isReservedChar(c)) {
                buffer.appendCodePoint(c);
            }
        }
        /*
         * If there is not enough characters in the abbreviation, take the given
         * identifier as-is except for the reserved characters which are removed.
         */
        if (buffer.length() >= 3) {
            return buffer.toString();
        }
        buffer.setLength(0);
        return removeReservedChars(identifier, buffer.append(identifier));
    }

    /**
     * Removes the reserved characters in the given identifier.
     * If the given buffer is non-null, then it shall contain a copy of {@code identifier}.
     */
    private static String removeReservedChars(final String identifier, StringBuilder buffer) {
        if (identifier != null) {
            boolean modified = false;
            for (int i=identifier.length(); --i >= 0;) {
                final char c = identifier.charAt(i);
                if (isReservedChar(c)) {
                    if (buffer == null) {
                        buffer = new StringBuilder(identifier);
                    }
                    buffer.deleteCharAt(i);
                    modified = true;
                }
            }
            if (modified) {
                return buffer.toString();
            }
        }
        return identifier;
    }

    /**
     * Returns {@code true} if the given code point is a reserved character.
     */
    private static boolean isReservedChar(final int c) {
        return (c == TableHierarchy.TYPE_OPEN) || (c == TableHierarchy.TYPE_CLOSE);
    }
}
