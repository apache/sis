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
package org.apache.sis.internal.shapefile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.internal.shapefile.jdbc.*;
import org.apache.sis.storage.shapefile.InvalidShapefileFormatException;
import org.apache.sis.storage.shapefile.ShapeTypeEnum;
import org.apache.sis.util.logging.Logging;
import org.opengis.feature.Feature;

import com.esri.core.geometry.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Reader of a Shapefile Binary content by the way of a {@link java.nio.MappedByteBuffer}
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class ShapefileByteReader extends CommonByteReader<InvalidShapefileFormatException, SQLShapefileNotFoundException> {
    /** Name of the Geometry field. */
    private static final String GEOMETRY_NAME = "geometry";

    /** Shapefile descriptor. */
    private ShapefileDescriptor shapefileDescriptor;

    /** Database Field descriptors. */
    private List<DBase3FieldDescriptor> databaseFieldsDescriptors;

    /** Type of the features contained in this shapefile. */
    private DefaultFeatureType featuresType;

    /**
     * Construct a shapefile byte reader.
     * @param shapefile Shapefile.
     * @param dbaseFile underlying database file name.
     * @throws InvalidShapefileFormatException if the shapefile format is invalid.
     * @throws SQLInvalidDbaseFileFormatException if the database file format is invalid.
     * @throws SQLShapefileNotFoundException if the shapefile has not been found.
     * @throws SQLDbaseFileNotFoundException if the database file has not been found.
     */
    public ShapefileByteReader(File shapefile, File dbaseFile) throws InvalidShapefileFormatException, SQLInvalidDbaseFileFormatException, SQLShapefileNotFoundException, SQLDbaseFileNotFoundException {
        super(shapefile, InvalidShapefileFormatException.class, SQLShapefileNotFoundException.class);
        loadDatabaseFieldDescriptors(dbaseFile);
        loadDescriptor();

        featuresType = getFeatureType(shapefile.getName());
    }

    /**
     * Returns the DBase 3 fields descriptors.
     * @return Fields descriptors.
     */
    public List<DBase3FieldDescriptor> getFieldsDescriptors() {
        return databaseFieldsDescriptors;
    }

    /**
     * Returns the shapefile descriptor.
     * @return Shapefile descriptor.
     */
    public ShapefileDescriptor getShapefileDescriptor() {
        return shapefileDescriptor;
    }

    /**
     * Returns the type of the features contained in this shapefile.
     * @return Features type.
     */
    public DefaultFeatureType getFeaturesType() {
        return featuresType;
    }

    /**
     * Create a feature descriptor.
     * @param name Name of the field.
     * @return The feature type.
     */
    private DefaultFeatureType getFeatureType(final String name) {
        Objects.requireNonNull(name, "The feature name cannot be null.");

        final int n = databaseFieldsDescriptors.size();
        final DefaultAttributeType<?>[] attributes = new DefaultAttributeType<?>[n + 1];
        final Map<String, Object> properties = new HashMap<String, Object>(4);

        // Load data field.
        for (int i = 0; i < n; i++) {
            properties.put(DefaultAttributeType.NAME_KEY, databaseFieldsDescriptors.get(i).getName());
            attributes[i] = new DefaultAttributeType<String>(properties, String.class, 1, 1, null);
        }

        // Add geometry field.
        properties.put(DefaultAttributeType.NAME_KEY, GEOMETRY_NAME);
        attributes[n] = new DefaultAttributeType<Geometry>(properties, Geometry.class, 1, 1, null);

        // Add name.
        properties.put(DefaultAttributeType.NAME_KEY, name);
        return new DefaultFeatureType(properties, false, null, attributes);
    }

    /**
     * Load shapefile descriptor.
     */
    private void loadDescriptor() {
        shapefileDescriptor = new ShapefileDescriptor(getByteBuffer());
    }

    /**
     * Load database field descriptors.
     * @param dbaseFile Database file.
     * @throws SQLInvalidDbaseFileFormatException if the database format is incorrect.
     * @throws SQLDbaseFileNotFoundException if the database file cannot be found.
     */
    private void loadDatabaseFieldDescriptors(File dbaseFile) throws SQLInvalidDbaseFileFormatException, SQLDbaseFileNotFoundException {
        MappedByteReader databaseReader = null;

        try {
            databaseReader = new MappedByteReader(dbaseFile, null);
            databaseFieldsDescriptors = databaseReader.getFieldsDescriptors();
        }
        finally {
            if (databaseReader != null) {
                try {
                    databaseReader.close();
                }
                catch(IOException e) {
                }
            }
        }
    }

    /**
     * Complete a feature with shapefile content.
     * @param feature Feature to complete.
     * @throws InvalidShapefileFormatException if a validation problem occurs.
     */
    public void completeFeature(Feature feature) throws InvalidShapefileFormatException {
        // insert points into some type of list
        int RecordNumber = getByteBuffer().getInt();
        @SuppressWarnings("unused")
        int ContentLength = getByteBuffer().getInt();

        getByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
        int iShapeType = getByteBuffer().getInt();

        ShapeTypeEnum type = ShapeTypeEnum.get(iShapeType);

        if (type == null)
            throw new InvalidShapefileFormatException(MessageFormat.format("The shapefile feature type {0} doesn''t match to any known feature type.", featuresType));

        switch (type) {
            case Point:
                loadPointFeature(feature);
                break;

            case Polygon:
                loadPolygonFeature(feature);
                break;

            case PolyLine:
                loadPolylineFeature(feature);
                break;

            default:
                throw new InvalidShapefileFormatException("Unsupported shapefile type: " + iShapeType);
        }

        getByteBuffer().order(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Load point feature.
     * @param feature Feature to fill.
     */
    private void loadPointFeature(Feature feature) {
        double x = getByteBuffer().getDouble();
        double y = getByteBuffer().getDouble();
        Point pnt = new Point(x, y);
        feature.setPropertyValue(GEOMETRY_NAME, pnt);
    }

    /**
     * Load polygon feature.
     * @param feature Feature to fill.
     */
    private void loadPolygonFeature(Feature feature) {
        /* double xmin = */getByteBuffer().getDouble();
        /* double ymin = */getByteBuffer().getDouble();
        /* double xmax = */getByteBuffer().getDouble();
        /* double ymax = */getByteBuffer().getDouble();
        int numParts = getByteBuffer().getInt();
        int numPoints = getByteBuffer().getInt();

        Polygon poly;

        // Handle multiple polygon parts.
        if (numParts > 1) {
            Logger log = Logging.getLogger(ShapefileByteReader.class.getName());
            
            if (log.isLoggable(Level.FINER)) {
                String format = "Polygon with multiple linear rings encountered at position {0,number} with {1,number} parts.";
                String message = MessageFormat.format(format, getByteBuffer().position(), numParts);
                log.finer(message);
            }
            
            poly = readMultiplePolygonParts(numParts, numPoints);
        }
        else {
            // Polygon with an unique part.
            poly = readUniquePolygonPart(numPoints);
        }
        
        feature.setPropertyValue(GEOMETRY_NAME, poly);
    }
    
    /**
     * Read a polygon that has a unique part.
     * @param numPoints Number of the points of the polygon.
     * @return Polygon.
     */
    @Deprecated // As soon as the readMultiplePolygonParts method proofs working well, this readUniquePolygonPart method can be removed and all calls be deferred to readMultiplePolygonParts.
    private Polygon readUniquePolygonPart(int numPoints) {
        int part = getByteBuffer().getInt();
        
        Polygon poly = new Polygon();

        // create a line from the points
        double xpnt = getByteBuffer().getDouble();
        double ypnt = getByteBuffer().getDouble();

        poly.startPath(xpnt, ypnt);

        for (int j = 0; j < numPoints - 1; j++) {
            xpnt = getByteBuffer().getDouble();
            ypnt = getByteBuffer().getDouble();
            poly.lineTo(xpnt, ypnt);
        }
        
        return poly;
    }
    
    /**
     * Read a polygon that has multiple parts.
     * @param numParts Number of parts of this polygon.
     * @param numPoints Total number of points of this polygon, all parts considered.
     * @return a multiple part polygon.
     */
    private Polygon readMultiplePolygonParts(int numParts, int numPoints) {
        /** 
         * From ESRI Specification : 
         * Parts : 0 5  (meaning : 0 designs the first v1, 5 designs the first v5 on the points list below).
         * Points : v1 v2 v3 v4 v1 v5 v8 v7 v6 v5
         *
         * POSITION  FIELD       VALUE      TYPE      NUMBER     ORDER
         * Byte 0    Shape Type  5          Integer   1          Little
         * Byte 4    Box         Box        Double    4          Little
         * Byte 36   NumParts    NumParts   Integer   1          Little
         * Byte 40   NumPoints   NumPoints  Integer   1          Little
         * Byte 44   Parts       Parts      Integer   NumParts   Little
         * Byte X    Points      Points     Point     NumPoints  Little        
         */
        int[] partsIndexes = new int[numParts];
        
        // Read all the parts indexes (starting at byte 44).
        for(int index=0; index < numParts; index ++) {
            partsIndexes[index] = getByteBuffer().getInt();
        }

        // Read all the points.
        double[] xPoints = new double[numPoints];
        double[] yPoints = new double[numPoints];

        for(int index=0; index < numPoints; index ++) {
            xPoints[index] = getByteBuffer().getDouble();
            yPoints[index] = getByteBuffer().getDouble();
        }
        
        // Create the polygon from the points.
        Polygon poly = new Polygon();

        // create a line from the points
        for(int index=0; index < numPoints; index ++) {
            // Check if this index is one that begins a new part.
            boolean newPolygon = false;

            for(int j=0; j < partsIndexes.length; j ++) {
                if (partsIndexes[j] == index) {
                    newPolygon = true;
                    break;
                }
            }
            
            if (newPolygon) {
                poly.startPath(xPoints[index], yPoints[index]);                
            }
            else {
                poly.lineTo(xPoints[index], yPoints[index]);                
            }
        }
        
        return poly;
    }
    
    /**
     * Load polyline feature.
     * @param feature Feature to fill.
     */
    private void loadPolylineFeature(Feature feature) {
        /* double xmin = */getByteBuffer().getDouble();
        /* double ymin = */getByteBuffer().getDouble();
        /* double xmax = */getByteBuffer().getDouble();
        /* double ymax = */getByteBuffer().getDouble();

        int NumParts = getByteBuffer().getInt();
        int NumPoints = getByteBuffer().getInt();

        int[] NumPartArr = new int[NumParts + 1];

        for (int n = 0; n < NumParts; n++) {
            int idx = getByteBuffer().getInt();
            NumPartArr[n] = idx;
        }
        NumPartArr[NumParts] = NumPoints;

        double xpnt, ypnt;
        Polyline ply = new Polyline();

        for (int m = 0; m < NumParts; m++) {
            xpnt = getByteBuffer().getDouble();
            ypnt = getByteBuffer().getDouble();
            ply.startPath(xpnt, ypnt);

            for (int j = NumPartArr[m]; j < NumPartArr[m + 1] - 1; j++) {
                xpnt = getByteBuffer().getDouble();
                ypnt = getByteBuffer().getDouble();
                ply.lineTo(xpnt, ypnt);
            }
        }

        feature.setPropertyValue(GEOMETRY_NAME, ply);
    }
}
