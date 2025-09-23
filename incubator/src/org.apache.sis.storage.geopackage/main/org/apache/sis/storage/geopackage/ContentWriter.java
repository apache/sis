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
package org.apache.sis.storage.geopackage;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Locale;
import java.time.Instant;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.sql.DataAccess;
import org.apache.sis.metadata.sql.internal.shared.SQLBuilder;


/**
 * Helper methods for writing a new {@link Content} in the Geopackage content table.
 * This is provided in a separated class for reducing class loading in read-only stores.
 * Each instance should be used only once.
 *
 * @param  <R>  the result of the write operation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class ContentWriter<R> {
    /**
     * The <abbr>SQL</abbr> statement being prepared.
     */
    private final StringBuilder sql;

    /**
     * Number of parameters.
     */
    private int parameterCount;

    /**
     * {@code false} for an insert operation, or {@code true} for a delete operation.
     */
    private final boolean delete;

    /**
     * Creates a new writer for an {@code INSERT INTO} or {@code DELETE FROM} operation.
     *
     * @param  delete  {@code false} for an insert operation, or {@code true} for a delete operation.
     */
    ContentWriter(final boolean delete) {
        this.delete = delete;
        sql = new StringBuilder(delete ? SQLBuilder.DELETE : SQLBuilder.INSERT).append(Content.TABLE_NAME);
    }

    /**
     * Executes the insert or delete operation on the Geopackage content table.
     * Null values will be left unspecified in <abbr>SQL</abbr> statement for
     * allowing the use of whatever default is specified in the database schema.
     *
     * @param  connection  the connection to use for inserting or deleting a row.
     * @param  content the row to insert or delete.
     * @throws SQLException if an error occurred while inserting the row.
     */
    private void updateContentTable(final Connection connection, final Content content) throws SQLException {
        if (delete) {
            sql.append(" WHERE " + Content.PRIMARY_KEY + "=?");
            try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                stmt.setString(1, content.tableName());
                stmt.execute();
            }
            return;
        }
        parameterCount = 2;
        sql.append(" ("  + Content.PRIMARY_KEY + ", " + Content.DATA_TYPE);      // "INSERT INTO" statement.
        final String      identifier  = appendColumn(content.identifier(),  "identifier");
        final String      description = appendColumn(content.description(), "description");
        final Instant     lastChange  = appendColumn(content.lastChange(),  "last_change");
        final Envelope    bounds      = appendColumn(content.bounds(),      "min_x, max_x, min_y, max_y");
        final OptionalInt srid        = content.srsId();
        if (bounds != null) {
            parameterCount += 3;
        }
        if (srid.isPresent()) {
            sql.append(", srs_id");
            parameterCount++;
        }
        sql.append(") VALUES (");
        while (--parameterCount >= 0) {
            sql.append('?').append((parameterCount != 0) ? ',' : ')');
        }
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int column = 0;
            stmt.setString(++column, content.tableName());
            stmt.setString(++column, content.dataType());
            if (identifier  != null) stmt.setString(++column, identifier);
            if (description != null) stmt.setString(++column, description);
            if (lastChange  != null) stmt.setString(++column, lastChange.toString());
            if (srid.isPresent())    stmt.setInt   (++column, srid.getAsInt());
            stmt.execute();
        }
    }

    /**
     * Helper method for adding an optional column in a prepared <abbr>SQL</abbr> statement.
     *
     * @param  <V>     type of value to add.
     * @param  value   the value to add.
     * @param  sql     the <abbr>SQL</abbr> statement to prepare.
     * @param  column  name of the column to add if the value is present.
     * @return the value, or {@code null} if none.
     */
    private <V> V appendColumn(final Optional<V> value, final String column) {
        if (value.isEmpty()) {
            return null;
        }
        parameterCount++;
        sql.append(", ").append(column);
        return value.get();
    }

    /**
     * Writes or delete the resource.
     * Shall return a non-null value on success, or {@code null} if this method cannot do its work.
     *
     * @param  dao  the data access object for writing in the database.
     * @return the content of the resource added or deleted, or {@code null} if the work cannot be done.
     * @throws Exception if an error occurred while writing or deleting the resource.
     */
    abstract Content updateResource(final DataAccess dao) throws Exception;

    /**
     * Executes the {@code updateResource()} method, then add or remove the associated row in the content table.
     *
     * @param  <R>      type of value returned by the action.
     * @param  store    the Geopackage store where to add or remove a content.
     * @param  action   the action to execute for adding or removing the content.
     * @return the result of the write action, which may be {@code null}.
     * @throws DataStoreException if an error occurred while updating the database.
     */
    final R execute(final GpkgStore store) throws DataStoreException {
        R result = null;
        try (DataAccess dao = store.newDataAccess(true)) {
            final Connection cnx = dao.getConnection();
            cnx.setAutoCommit(false);
            Content content = null;
            boolean success = false;
            try {
                content = updateResource(dao);
                if (content != null) {
                    updateContentTable(cnx, content);
                    success = true;
                }
            } finally {
                if (success) {
                    cnx.commit();
                } else {
                    cnx.rollback();
                }
                cnx.setAutoCommit(true);
            }
            if (content != null) {
                result = result(content, dao);
            }
        } catch (Exception e) {
            throw GpkgStore.cannotExecute(null, e);
        }
        return result;
    }

    /**
     * Returns the final result of the write operation.
     * This method should not modify the database, as the changes have already been commited.
     *
     * @param  content  the content that has been added.
     * @param  dao      the data access object for reading the database.
     * @return the result, or {@code null} of failure.
     * @throws Exception if an error occurred.
     */
    abstract R result(Content content, DataAccess dao) throws Exception;

    /**
     * Returns the first identifier or title from the given metadata which is different than the table name.
     *
     * @param  metadata    the metadata, or {@code null}.
     * @param  tableName   the table name to exclude.
     * @param  identifier  the identifier to exclude, or {@code null} if the caller is searching for an identifier.
     * @param  locale      the locale to use for fetching strings from metadata, or {@code null} for the default.
     * @return first non-excluded title or identifier.
     */
    static String firstDistinctString(final Metadata metadata, final String tableName, final String identifier, final Locale locale) {
        if (metadata != null) {
            for (Identification info : metadata.getIdentificationInfo()) {
                Citation citation = info.getCitation();
                if (citation != null) {
                    if (identifier != null) {
                        final var i18n = citation.getTitle();
                        if (i18n != null) {
                            final String t = i18n.toString(locale);
                            if (!(t.isBlank() || t.equalsIgnoreCase(tableName) || t.equalsIgnoreCase(identifier))) {
                                return t;
                            }
                        }
                    } else {
                        for (Identifier id : citation.getIdentifiers()) {
                            final String t = id.getCode();
                            if (!(t.isBlank() || tableName.equalsIgnoreCase(t))) {
                                return t;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
