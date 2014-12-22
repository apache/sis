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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;

import org.opengis.feature.Feature;

/**
 * Database byte reader contract. Used to allow refactoring of core byte management of a DBase file.
 * @author Marc LE BIHAN
 */
interface ByteReader {
    /**
     * Close the MappedByteReader.
     * @throws IOException if the close operation fails.
     */
    public void close() throws IOException;
    
    /**
     * Returns the charset.
     * @return Charset.
     */
    public Charset getCharset();
    
    /**
     * Returns the fields descriptors.
     * @return Fields descriptors.
     */
    public FieldsDescriptors getFieldsDescriptors();
    
    /**
     * Returns the database last update date.
     * @return Date of the last update.
     */
    public Date getDateOfLastUpdate();
    
    /**
     * Returns the record count.
     * @return Record count.
     */
    public int getRowCount();

    /**
     * Returns the current record number.
     * @return Current record number.
     */
    public int getRowNum();
    
    /**
     * Load a row into a feature.
     * @param feature Feature to fill.
     */
    public void loadRowIntoFeature(Feature feature);
    
    /**
     * Read the next row as a set of objects.
     * @return Map of field name / object value.
     */
    public HashMap<String, Object> readNextRowAsObjects();
}
