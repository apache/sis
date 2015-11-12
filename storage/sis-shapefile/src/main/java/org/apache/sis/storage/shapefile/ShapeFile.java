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
import java.util.List;
import java.util.Objects;

import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.internal.shapefile.ShapefileDescriptor;
import org.apache.sis.internal.shapefile.jdbc.DBase3FieldDescriptor;

/**
 * Provides a ShapeFile Reader.
 *
 * @author  Travis L. Pinney
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf">ESRI Shapefile Specification</a>
 * @see <a href="http://ulisse.elettra.trieste.it/services/doc/dbase/DBFstruct.htm">dBASE III File Structure</a>
 */
public class ShapeFile {
    /** Shapefile. */
    private File shapeFile;

    /** Database file. */
    private File databaseFile;

    /** Type of the features contained in this shapefile. */
    private DefaultFeatureType featuresType;

    /** Shapefile descriptor. */
    private ShapefileDescriptor shapefileDescriptor;
    
    /** Database field descriptors. */
    private List<DBase3FieldDescriptor> databaseFieldsDescriptors;
    
    /**
     * Construct a Shapefile from a file.
     * @param shpfile file to read.
     */
    public ShapeFile(String shpfile) {
        Objects.requireNonNull(shpfile, "The shapefile to load cannot be null.");

        shapeFile = new File(shpfile);

        // Deduct database file name.
        StringBuilder dbfFileName = new StringBuilder(shpfile);
        dbfFileName.replace(shpfile.length() - 3, shpfile.length(), "dbf");
        databaseFile = new File(dbfFileName.toString());
    }

    /**
     * Return the default feature type.
     * @return Feature type.
     */
    public DefaultFeatureType getFeaturesType() {
        return this.featuresType;
    }
    
    /**
     * Returns the shapefile descriptor.
     * @return Shapefile descriptor.
     */
    public ShapefileDescriptor getShapefileDescriptor() {
        return this.shapefileDescriptor;
    }
    
    /** 
     * Returns the database fields descriptors.
     * @return List of fields descriptors. 
     */
    public List<DBase3FieldDescriptor> getDatabaseFieldsDescriptors() {
        return this.databaseFieldsDescriptors;
    }

    /**
     * Find features corresponding to an SQL request SELECT * FROM database.
     * @return Features
     * @throws DbaseFileNotFoundException if the database file has not been found.
     * @throws ShapefileNotFoundException if the shapefile has not been found.
     * @throws InvalidDbaseFileFormatException if the database file format is invalid.
     * @throws InvalidShapefileFormatException if the shapefile format is invalid.
     */
    public InputFeatureStream findAll() throws InvalidDbaseFileFormatException, ShapefileNotFoundException, DbaseFileNotFoundException, InvalidShapefileFormatException {
        InputFeatureStream is = new InputFeatureStream(shapeFile, databaseFile);
        this.featuresType = is.getFeaturesType();
        this.shapefileDescriptor = is.getShapefileDescriptor();
        this.databaseFieldsDescriptors = is.getDatabaseFieldsDescriptors();
        return is;
    }

    /**
     * Load shapefile descriptors : features types, shapefileDescriptor, database field descriptors :
     * this is also automatically done when executing a query on it, by findAll.
     * @throws DbaseFileNotFoundException if the database file has not been found.
     * @throws ShapefileNotFoundException if the shapefile has not been found.
     * @throws InvalidDbaseFileFormatException if the database file format is invalid.
     * @throws InvalidShapefileFormatException if the shapefile format is invalid.
     */
    public void loadDescriptors() throws InvalidDbaseFileFormatException, InvalidShapefileFormatException, ShapefileNotFoundException, DbaseFileNotFoundException {
        // Doing an simple query will init the internal descriptors.
        // It prepares a SELECT * FROM <DBase> but don't read a record by itself.
        try(InputFeatureStream is = findAll()) {
        }
    }
}
