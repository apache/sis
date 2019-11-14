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
import java.sql.SQLException;
import java.util.stream.Stream;

import org.opengis.feature.Feature;

import org.apache.sis.storage.DataStoreException;

/**
 * Simple abstraction to describe a component capable of loading data from an SQL connection. Used for subsetting SQL
 * related feature sets.
 */
interface Connector {
    /**
     * Triggers Loading of data through an existing connection.
     *
     * @param connection The database connection to use for data loading. Note its the caller responsability to close
     *                   the connection, and it should not be done before stream terminal operation is over.
     * @return Features loaded from input connection. It is recommended to implement lazy solutions, however it's an
     * implementation dependant choice.
     * @throws SQLException If an error occurs while exchanging information with the database.
     * @throws DataStoreException If a data model dependant error occurs.
     */
    Stream<Feature> connect(Connection connection) throws SQLException, DataStoreException;

    /**
     * Provides an approximate query to resume data loaded.
     *
     * @param count If the query estimation is needed for a count operation, in which case the returned query should be
     *              a count query.
     * @return SQL query describing the way this component load data. Never null. However, implementations are free to
     * throw {@link UnsupportedOperationException} if they do not support such operation.
     */
    String estimateStatement(final boolean count);
}
