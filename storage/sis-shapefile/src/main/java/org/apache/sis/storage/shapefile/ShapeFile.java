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
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.text.MessageFormat;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.Geometry;

import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.storage.DataStoreException;

// Branch-dependent imports
import org.opengis.feature.Feature;

/**
 * Provides a ShapeFile Reader.
 *
 * @author Travis L. Pinney
 * @since 0.5
 * @version 0.5
 * @module
 *
 * @see <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf">ESRI Shapefile Specification</a>
 * @see <a href="http://ulisse.elettra.trieste.it/services/doc/dbase/DBFstruct.htm">dBASE III File Structure</a>
 */
public class ShapeFile {
    /** Name of the Geometry field. */
    private static final String GEOMETRY_NAME = "geometry";

    /** File code. */
    public int FileCode; // big

    /** File length. */
    public int FileLength; // big // The value for file length is the total length of the file in 16-bit words

    /** File version. */
    public int Version; // little

    /** Shapefile type. */
    public ShapeTypeEnum ShapeType; // little

    /** X Min. */
    public double xmin; // little

    /** Y Min. */
    public double ymin; // little

    /** X Max. */
    public double xmax; // little

    /** Y Max. */
    public double ymax; // little

    /** Z Min. */
    public double zmin; // little

    /** Z Max. */
    public double zmax; // little

    /** M Min. */
    public double mmin; // little

    /** M Max. */
    public double mmax; // little

    /** Underlying databasefile content. */
    private Database dbf;

    /** Features existing in the shapefile. */
    public Map<Integer, Feature> FeatureMap = new HashMap<>();

    /**
     * Construct a Shapefile from a file.
     * @param shpfile file to read.
     * @throws IOException if the file cannot be opened.
     * @throws DataStoreException if the shapefile is not valid.
     */
    public ShapeFile(String shpfile) throws IOException, DataStoreException {
        Objects.requireNonNull(shpfile, "The shapefile to load cannot be null.");

        // Deduct database file name.
        StringBuilder b = new StringBuilder(shpfile);
        b.replace(shpfile.length() - 3, shpfile.length(), "dbf");

        dbf = new Database(b.toString());

        try (FileInputStream fis = new FileInputStream(shpfile); FileChannel fc = fis.getChannel();) {
            int fsize = (int) fc.size();
            MappedByteBuffer rf = fc.map(FileChannel.MapMode.READ_ONLY, 0, fsize);

            this.FileCode = rf.getInt();
            rf.getInt();
            rf.getInt();
            rf.getInt();
            rf.getInt();
            rf.getInt();
            this.FileLength = rf.getInt() * 2;

            rf.order(ByteOrder.LITTLE_ENDIAN);
            this.Version = rf.getInt();
            this.ShapeType = ShapeTypeEnum.get(rf.getInt());
            this.xmin = rf.getDouble();
            this.ymin = rf.getDouble();
            this.xmax = rf.getDouble();
            this.ymax = rf.getDouble();
            this.zmin = rf.getDouble();
            this.zmax = rf.getDouble();
            this.mmin = rf.getDouble();
            this.mmax = rf.getDouble();
            rf.order(ByteOrder.BIG_ENDIAN);

            dbf.loadDescriptor();
            final DefaultFeatureType featureType = getFeatureType(shpfile);

            dbf.getByteBuffer().get(); // should be 0d for field terminator
            loadFeatures(featureType, rf);
        } finally {
            dbf.close();
        }
    }

    /**
     * Returns the underlying database file.
     * @return Underlying database file.
     */
    public Database getDatabase() {
        return this.dbf;
    }

    /**
     * Returns the feature count of the shapefile.
     * @return Feature count.
     */
    public int getFeatureCount() {
        return this.dbf.getRecordCount();
    }

    /**
     * Load the features of a shapefile.
     * @param featureType Features descriptor.
     * @param rf byte buffer mapper.
     * @throws DataStoreException if a validation problem occurs.
     */
    private void loadFeatures(DefaultFeatureType featureType, MappedByteBuffer rf) throws DataStoreException {
        for (Integer i = 0; i < this.dbf.getRecordCount(); i++) {
            // insert points into some type of list
            int RecordNumber = rf.getInt();
            @SuppressWarnings("unused")
            int ContentLength = rf.getInt();

            rf.order(ByteOrder.LITTLE_ENDIAN);
            int iShapeType = rf.getInt();
            final Feature f = featureType.newInstance();

            ShapeTypeEnum type = ShapeTypeEnum.get(iShapeType);

            if (type == null)
                throw new DataStoreException(MessageFormat.format("The shapefile feature type {0} doesn''t match to any known feature type.", featureType));

            switch (type) {
            case Point:
                loadPointFeature(rf, f);
                break;

            case Polygon:
                loadPolygonFeature(rf, f);
                break;

            case PolyLine:
                loadPolylineFeature(rf, f);
                break;

            default:
                throw new DataStoreException("Unsupported shapefile type: " + iShapeType);
            }

            rf.order(ByteOrder.BIG_ENDIAN);
            // read in each Record and Populate the Feature

            dbf.loadRowIntoFeature(f);

            this.FeatureMap.put(RecordNumber, f);
        }
    }

    /**
     * Load point feature.
     * @param rf Byte buffer.
     * @param feature Feature to fill.
     */
    private void loadPointFeature(MappedByteBuffer rf, Feature feature) {
        double x = rf.getDouble();
        double y = rf.getDouble();
        Point pnt = new Point(x, y);
        feature.setPropertyValue(GEOMETRY_NAME, pnt);
    }

    /**
     * Load polygon feature.
     * @param rf Byte buffer.
     * @param feature Feature to fill.
     * @throws DataStoreException if the polygon cannot be handled.
     */
    private void loadPolygonFeature(MappedByteBuffer rf, Feature feature) throws DataStoreException {
        /* double xmin = */rf.getDouble();
        /* double ymin = */rf.getDouble();
        /* double xmax = */rf.getDouble();
        /* double ymax = */rf.getDouble();
        int NumParts = rf.getInt();
        int NumPoints = rf.getInt();

        if (NumParts > 1) {
            throw new DataStoreException("Polygons with multiple linear rings have not implemented yet.");
        }

        // read the one part
        @SuppressWarnings("unused")
        int Part = rf.getInt();
        Polygon poly = new Polygon();

        // create a line from the points
        double xpnt = rf.getDouble();
        double ypnt = rf.getDouble();
        // Point oldpnt = new Point(xpnt, ypnt);
        poly.startPath(xpnt, ypnt);

        for (int j = 0; j < NumPoints - 1; j++) {
            xpnt = rf.getDouble();
            ypnt = rf.getDouble();
            poly.lineTo(xpnt, ypnt);
        }

        feature.setPropertyValue(GEOMETRY_NAME, poly);
    }

    /**
     * Load polyline feature.
     * @param rf Byte buffer.
     * @param feature Feature to fill.
     */
    private void loadPolylineFeature(MappedByteBuffer rf, Feature feature) {
        /* double xmin = */rf.getDouble();
        /* double ymin = */rf.getDouble();
        /* double xmax = */rf.getDouble();
        /* double ymax = */rf.getDouble();

        int NumParts = rf.getInt();
        int NumPoints = rf.getInt();

        int[] NumPartArr = new int[NumParts + 1];

        for (int n = 0; n < NumParts; n++) {
            int idx = rf.getInt();
            NumPartArr[n] = idx;
        }
        NumPartArr[NumParts] = NumPoints;

        double xpnt, ypnt;
        Polyline ply = new Polyline();

        for (int m = 0; m < NumParts; m++) {
            xpnt = rf.getDouble();
            ypnt = rf.getDouble();
            ply.startPath(xpnt, ypnt);

            for (int j = NumPartArr[m]; j < NumPartArr[m + 1] - 1; j++) {
                xpnt = rf.getDouble();
                ypnt = rf.getDouble();
                ply.lineTo(xpnt, ypnt);
            }
        }

        feature.setPropertyValue(GEOMETRY_NAME, ply);
    }

    /**
     * Create a feature descriptor.
     * @param name Name of the field.
     * @return The feature type.
     */
    private DefaultFeatureType getFeatureType(final String name) {
        Objects.requireNonNull(name, "The feature name cannot be null.");

        final int n = dbf.getFieldsDescriptor().size();
        final DefaultAttributeType<?>[] attributes = new DefaultAttributeType<?>[n + 1];
        final Map<String, Object> properties = new HashMap<>(4);

        // Load data field.
        for (int i = 0; i < n; i++) {
            properties.put(DefaultAttributeType.NAME_KEY, dbf.getFieldsDescriptor().get(i).getName());
            attributes[i] = new DefaultAttributeType<>(properties, String.class, 1, 1, null);
        }

        // Add geometry field.
        properties.put(DefaultAttributeType.NAME_KEY, GEOMETRY_NAME);
        attributes[n] = new DefaultAttributeType<>(properties, Geometry.class, 1, 1, null);

        // Add name.
        properties.put(DefaultAttributeType.NAME_KEY, name);
        return new DefaultFeatureType(properties, false, null, attributes);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator", "\n");

        s.append("FileCode: ").append(FileCode).append(lineSeparator);
        s.append("FileLength: ").append(FileLength).append(lineSeparator);
        s.append("Version: ").append(Version).append(lineSeparator);
        s.append("ShapeType: ").append(ShapeType).append(lineSeparator);
        s.append("xmin: ").append(xmin).append(lineSeparator);
        s.append("ymin: ").append(ymin).append(lineSeparator);
        s.append("xmax: ").append(xmax).append(lineSeparator);
        s.append("ymax: ").append(ymax).append(lineSeparator);
        s.append("zmin: ").append(zmin).append(lineSeparator);
        s.append("zmax: ").append(zmax).append(lineSeparator);
        s.append("mmin: ").append(mmin).append(lineSeparator);
        s.append("mmax: ").append(mmax).append(lineSeparator);
        s.append("------------------------").append(lineSeparator);
        s.append(dbf.toString());

        return s.toString();
    }
}
