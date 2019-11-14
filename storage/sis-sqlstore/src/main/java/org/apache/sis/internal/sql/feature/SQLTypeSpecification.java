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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.opengis.util.GenericName;

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.storage.DataStoreContentException;

/**
 * Defines an application schema inferred from an SQL database (query, table, etc.). Implementations will be used by
 * {@link Analyzer} to create an {@link FeatureAdapter adaptation layer to the feature model}. Default implementations
 * can be retrieved for tables and queries respectively through {@link Analyzer#create(TableReference, TableReference)}
 * and {@link Analyzer#create(PreparedStatement, String, GenericName)} methods.
 */
interface SQLTypeSpecification {
    /**
     * Identifying name for the application schema. It is strongly recommended to be present, for SIS engine to be
     * capable to create insightful models. However, in corner cases where no proper names could be provided, an empty
     * value is allowed.
     *
     * @implNote SIS {@link FeatureTypeBuilder feature type builder} <em>requires</em> a name, and current
     * {@link Analyzer#buildAdapter(SQLTypeSpecification) analysis implementation} will create a random UUID if
     * necessary.
     *
     * @return Name for the feature type to build.
     * @throws SQLException If an error occurs while retrieving information from database.
     */
    Optional<GenericName> getName() throws SQLException;

    /**
     * Gives an optional description of the application schema.This information is not necessary for any kind of
     * computation, but allows to give end-user global information about the schema (s)he's manipulating.
     *
     * @return A brief description of the data source.
     * @throws SQLException If an error occurs while retrieving information from database.
     */
    Optional<String> getDefinition() throws SQLException;

    /**
     * Primary key definition of source schema. Can be empty if no primary key is defined (Example: query definition).
     *
     * @return Primary key definition if any, otherwise an empty shell.
     * @throws SQLException If an error occurs while exchanging information with underlying database.
     */
    Optional<PrimaryKey> getPK() throws SQLException;

    /**
     *
     * @return Ordered list of columns in application schema. Order is important, and will be relied upon to retrieve
     *  {@link ResultSet#getObject(int) result values by index}.
     */
    List<SQLColumn> getColumns();

    /**
     *
     * @return All identified relations based on a foreign key in <em>current</em> application schema (1..1 or n..1).
     * Corresponds to {@link Relation.Direction#IMPORT}. Can be empty but not null.
     *
     * @throws SQLException If an error occurs while exchanging information with underlying database.
     */
    List<Relation> getImports() throws SQLException;

    /**
     *
     * @return All identified relations based on foreign key located in <em>another</em> application schema (1..n).
     * Corresponds to {@link Relation.Direction#EXPORT}. Can be empty but not null.
     * @throws SQLException If an error occurs while exchanging information with underlying database.
     * @throws DataStoreContentException If a schema problem is encountered.
     */
    List<Relation> getExports() throws SQLException, DataStoreContentException;

    /**
     * In case target schema contains geographic information, this serves to identify without ambiguity which column
     * contains what could be considered main geolocation (as stated by {@link AttributeConvention#GEOMETRY_PROPERTY}).
     * This is a very important information in case application schema contains multiple geometric fields.
     *
     * @return The name of the column/attribute to be considered as main geometry information, or an empty shell if
     * unknown.
     */
    default Optional<ColumnRef> getPrimaryGeometryColumn() {return Optional.empty();}
}
