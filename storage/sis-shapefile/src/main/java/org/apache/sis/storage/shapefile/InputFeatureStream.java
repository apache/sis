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
package org.apache.sis.storage.shapefile;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLFeatureNotSupportedException;
import java.text.*;

import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.internal.shapefile.InvalidShapefileFormatException;
import org.apache.sis.internal.shapefile.ShapefileByteReader;
import org.apache.sis.internal.shapefile.ShapefileNotFoundException;
import org.apache.sis.internal.shapefile.jdbc.*;
import org.apache.sis.internal.shapefile.jdbc.connection.DBFConnection;
import org.apache.sis.internal.shapefile.jdbc.metadata.DBFDatabaseMetaData;
import org.apache.sis.internal.shapefile.jdbc.resultset.*;
import org.apache.sis.internal.shapefile.jdbc.sql.SQLIllegalParameterException;
import org.apache.sis.internal.shapefile.jdbc.sql.SQLInvalidStatementException;
import org.apache.sis.internal.shapefile.jdbc.sql.SQLUnsupportedParsingFeatureException;
import org.apache.sis.internal.shapefile.jdbc.statement.DBFStatement;
import org.opengis.feature.Feature;

/**
 * Input Stream of features.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class InputFeatureStream extends InputStream {
    /** Dedicated connection to DBF. */
    private DBFConnection m_connection;
    
    /** Statement. */
    private DBFStatement m_stmt;
    
    /** ResultSet. */
    private DBFRecordBasedResultSet m_rs;
    
    /** SQL Statement executed. */
    private String m_sql;
    
    /** Marks the end of file. */
    private boolean m_endOfFile;
    
    /** Shapefile. */
    private File m_shapefile;
    
    /** Database file. */
    private File m_databaseFile;

    /** Type of the features contained in this shapefile. */
    private DefaultFeatureType m_featuresType;
    
    /** Shapefile reader. */
    private ShapefileByteReader m_shapefileReader;

    /**
     * Create an input stream of features over a connection.
     * @param shapefile Shapefile.
     * @param dbaseFile Database file.
     * @throws SQLInvalidStatementException if the given SQL Statement is invalid.
     * @throws InvalidShapefileFormatException if the shapefile format is invalid. 
     * @throws InvalidDbaseFileFormatException if the Dbase file format is invalid.
     * @throws ShapefileNotFoundException if the shapefile has not been found.
     * @throws DbaseFileNotFoundException if the database file has not been found.
     */
    public InputFeatureStream(File shapefile, File dbaseFile) throws SQLInvalidStatementException, InvalidDbaseFileFormatException, InvalidShapefileFormatException, ShapefileNotFoundException, DbaseFileNotFoundException {
        m_connection = (DBFConnection)new DBFDriver().connect(dbaseFile.getAbsolutePath(), null);
        m_sql = MessageFormat.format("SELECT * FROM {0}", dbaseFile.getName());
        m_shapefile = shapefile;
        m_databaseFile = dbaseFile;
        
        m_shapefileReader = new ShapefileByteReader(m_shapefile, m_databaseFile);
        m_featuresType = m_shapefileReader.getFeaturesType(); 
        
        try {
            executeQuery();
        }
        catch(SQLConnectionClosedException e) {
            // This would be an internal trouble because in this function (at least) it should be open.
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    /**
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() {
        throw new UnsupportedOperationException("InputFeatureStream doesn't allow the use of read(). Use readFeature() instead.");
    }

    /**
     * @see java.io.InputStream#available()
     */
    @Override 
    public int available() {
        throw new UnsupportedOperationException("InputFeatureStream doesn't allow the use of available(). Use readFeature() will return null when feature are no more available.");
    }
    
    /**
     * @see java.io.InputStream#close()
     */
    @Override 
    public void close() {
        m_rs.close();
        m_stmt.close();
        m_connection.close();
    }
    
    /**
     * Read next feature responding to the SQL request.
     * @return Feature, null if no more feature is available.
     * @throws SQLNotNumericException if a field expected numeric isn't. 
     * @throws SQLNotDateException if a field expected of date kind, isn't.
     * @throws SQLNoSuchFieldException if a field doesn't exist.
     * @throws SQLIllegalParameterException if a parameter is illegal in the query.
     * @throws SQLInvalidStatementException if the SQL statement is invalid.
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLUnsupportedParsingFeatureException if a SQL ability is not currently available through this driver.
     * @throws SQLIllegalColumnIndexException if a column index is illegal.
     * @throws SQLFeatureNotSupportedException if a SQL ability is not currently available through this driver.
     * @throws InvalidShapefileFormatException if the shapefile format is invalid.
     */
    public Feature readFeature() throws SQLConnectionClosedException, SQLInvalidStatementException, SQLIllegalParameterException, SQLNoSuchFieldException, SQLUnsupportedParsingFeatureException, SQLNotNumericException, SQLNotDateException, SQLFeatureNotSupportedException, SQLIllegalColumnIndexException, InvalidShapefileFormatException {
        try {
            if (m_endOfFile) {
                return null;
            }
            
            if (m_rs.next() == false) {
                m_endOfFile = true;
                return null;
            }
            
            Feature feature = m_featuresType.newInstance();
            m_shapefileReader.completeFeature(feature);
            DBFDatabaseMetaData metadata = (DBFDatabaseMetaData)m_connection.getMetaData();
            
            try(DBFBuiltInMemoryResultSetForColumnsListing rsDatabase = (DBFBuiltInMemoryResultSetForColumnsListing)metadata.getColumns(null, null, null, null)) {
                while(rsDatabase.next()) {
                    String fieldName = rsDatabase.getString("COLUMN_NAME");
                    Object fieldValue = m_rs.getObject(fieldName);
                    
                    // FIXME To allow features to be filled again, the values are converted to String again : feature should allow any kind of data.
                    String stringValue;
                    
                    if (fieldValue == null) {
                        stringValue = null;
                    }
                    else {
                        if (fieldValue instanceof Integer || fieldValue instanceof Long) {
                            stringValue = MessageFormat.format("{0,number,#0}", fieldValue); // Avoid thousand separator.
                        }
                        else {
                            if (fieldValue instanceof Double || fieldValue instanceof Float) {
                                // Avoid thousand separator.
                                DecimalFormat df = new DecimalFormat();
                                df.setGroupingUsed(false);
                                stringValue = df.format(fieldValue);                                
                            }
                            else
                                stringValue = fieldValue.toString();
                        }
                    }
                    
                    feature.setPropertyValue(fieldName, stringValue);
                }
                
                return feature;
            }
            catch(SQLNoResultException e) {
                // This an internal trouble, if it occurs.
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        catch(SQLNoResultException e) {
            // We are trying to prevent this. If it occurs, we have an internal problem.
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Execute the wished SQL query.
     * @throws SQLConnectionClosedException if the connection is closed. 
     * @throws SQLInvalidStatementException if the given SQL Statement is invalid.
     */
    private void executeQuery() throws SQLConnectionClosedException, SQLInvalidStatementException {
        m_stmt = (DBFStatement)m_connection.createStatement(); 
        m_rs = (DBFRecordBasedResultSet)m_stmt.executeQuery(m_sql);
    }
}
