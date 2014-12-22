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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.apache.sis.util.logging.AbstractAutoChecker;
import org.opengis.feature.Feature;

/**
 * Load a whole DBF file.
 *
 * @author  Travis L. Pinney
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see <a href="http://ulisse.elettra.trieste.it/services/doc/dbase/DBFstruct.htm" >Database structure - 1</a>
 * @see <a href="https://www.cs.cmu.edu/~varun/cs315p/xbase.txt">Databse structure - 2</a>
 */
public class Database extends AbstractAutoChecker implements AutoCloseable {
    /** Database filename. */
    private String dbfFile;

    /** Indicates is the database file is closed or not. */
    private boolean isClosed;
    
    /** Database binary reader. */
    private ByteReader binaryReader; 
    
    /**
     * Load a database file.
     * @param file Database file.
     * @throws FileNotFoundException if the database file cannot be found.
     * @throws InvalidDbaseFileFormatException if the database seems to be invalid.
     */
    public Database(String file) throws FileNotFoundException, InvalidDbaseFileFormatException {
        Objects.requireNonNull(file, "The database file to load cannot be null.");
        dbfFile = file;
        binaryReader = new MappedByteReader(file);
        
        isClosed = false;
    }

    /**
     * @see java.lang.AutoCloseable#close()
     * @deprecated Will be removed in next API versions.
     */
    @Override @Deprecated
    public void close() throws IOException {
        binaryReader.close();
        isClosed = true;
    }

    /**
     * Returns the database charset, converted from its internal code page.
     * @return Charset.
     */
    public Charset getCharset() {
        return binaryReader.getCharset();
    }
    
    /**
     * Return the record count of the database file.
     * @return Record count.
     */
    public int getRecordCount() {
        return binaryReader.getRecordCount();
    }

    /**
     * Return the fields descriptor.
     * @return Field descriptor.
     */
    public List<FieldDescriptor> getFieldsDescriptor() {
        return binaryReader.getFieldsDescriptors();
    }

    /**
     * Return the database as a {@link java.io.File}.
     * @return File.
     */
    public File getFile() {
        return(new File(dbfFile));
    }

    /**
     * Return the current row number red.
     * @return Row number (zero based) or -1 if reading has not started.
     */
    public int getRowNum() {
        return binaryReader.getRowNum();
    }

    /**
     * Determines if the database is closed.
     * @return true if it is closed.
     * @deprecated Will be removed in next API versions.
     */
    @Deprecated
    public boolean isClosed() {
        return isClosed;
    }
    
    /**
     * Load a row into a feature.
     * @param feature Feature to fill.
     * @deprecated Will be removed in next API versions.
     */
    @Deprecated 
    public void loadRowIntoFeature(Feature feature) {
        binaryReader.loadRowIntoFeature(feature);
    }
    
    /**
     * Read the next row as a set of objects.
     * @return Map of field name / object value.
     * @deprecated Will be removed in next API versions.
     */
    @Deprecated 
    public HashMap<String, Object> readNextRowAsObjects() {
        return binaryReader.readNextRowAsObjects();
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return format("toString", System.getProperty("line.separator", "\n"), 
                binaryReader.getDateOfLastUpdate(), binaryReader.getFieldsDescriptors(), binaryReader.getCharset());
    }
}
