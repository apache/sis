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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import org.opengis.feature.*;

/**
 * Load a whole DBF file.
 *
 * @author  Travis L. Pinney
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see <a href="http://ulisse.elettra.trieste.it/services/doc/dbase/DBFstruct.htm" >Database structure</a>
 */
public class Database implements AutoCloseable {
    /** Database filename. */
    private String m_dbfFile;

    /** Indicates is the database file is closed or not. */
    private boolean isClosed;

    /** Valid dBASE III PLUS table file (03h without a memo .DBT file; 83h with a memo). */
    public byte DbaseVersion;

    /** Date of last update; in YYMMDD format. */
    public byte[] DbaseLastUpdate = new byte[3];

    /** Number of records in the table. */
    public int recordCount;

    /** Number of bytes in the header. */
    public short DbaseHeaderBytes;

    /** Number of bytes in the record. */
    public short DbaseRecordBytes;

    /** Reserved bytes. */
    public byte[] DbasePlusLanReserved = new byte[13];

    /** Fields descriptor. */
    public ArrayList<FieldDescriptor> fieldsDescriptors = new ArrayList<>();

    /** Input Stream on the DBF. */
    private FileInputStream fis;

    /** File channel on the file. */
    private FileChannel fc;

    /** Buffer reader. */
    private MappedByteBuffer df;

    /** Current row rumber. */
    private int rowNum;

    /**
     * Load a database file.
     * @param dbfFile Database file.
     * @throws FileNotFoundException if the database file cannot be found.
     */
    public Database(String dbfFile) throws FileNotFoundException {
        Objects.requireNonNull(dbfFile, "The database file to load cannot be null.");
        m_dbfFile = dbfFile;

        fis = new FileInputStream(m_dbfFile);
        fc = fis.getChannel();
        rowNum = 0;
        isClosed = false;
    }

    /**
     * Return the record count of the database file.
     * @return Record count.
     */
    public int getRecordCount() {
        return this.recordCount;
    }

    /**
     * Return the fields descriptor.
     * @return Field descriptor.
     */
    public ArrayList<FieldDescriptor> getFieldsDescriptor() {
        return this.fieldsDescriptors;
    }

    /**
     * Return the mapped byte buffer currently used to read that database file.
     * @return Database file.
     */
    public MappedByteBuffer getByteBuffer() {
        return this.df;
    }

    /**
     * Loading the database file content from binary .dbf file.
     * @throws IOException if the file cannot be opened.
     */
    public void loadDescriptor() throws IOException {
        int fsize = (int) fc.size();
        df = fc.map(FileChannel.MapMode.READ_ONLY, 0, fsize);

        this.DbaseVersion = df.get();
        df.get(this.DbaseLastUpdate);

        df.order(ByteOrder.LITTLE_ENDIAN);
        this.recordCount = df.getInt();
        this.DbaseHeaderBytes = df.getShort();
        this.DbaseRecordBytes = df.getShort();
        df.order(ByteOrder.BIG_ENDIAN);
        df.getShort(); // reserved
        df.get(); // reserved
        df.get(DbasePlusLanReserved);
        df.getInt();

        while (df.position() < this.DbaseHeaderBytes - 1) {
            FieldDescriptor fd = toFieldDescriptor();
            this.fieldsDescriptors.add(fd);
            // loop until you hit the 0Dh field terminator
        }
    }

    /**
     * Read the next row as a set of objects.
     * @return Map of field name / object value.
     */
    public HashMap<String, Object> readNextRowAsObjects() {
        // TODO: ignore deleted records
        df.get(); // denotes whether deleted or current
        // read first part of record

        HashMap<String, Object> fieldsValues = new HashMap<>();

        for (FieldDescriptor fd : this.fieldsDescriptors) {
            byte[] data = new byte[fd.getLength()];
            df.get(data);

            int length = data.length;
            while (length != 0 && data[length - 1] <= ' ') {
                length--;
            }

            String value = new String(data, 0, length);
            fieldsValues.put(fd.getName(), value);
        }

        rowNum ++;
        return fieldsValues;
    }

    /**
     * Return the current row number red.
     * @return Row number (zero based) or -1 if reading has not started.
     */
    public int getRowNum() {
        return rowNum;
    }

    /**
     * Load a row into a feature.
     * @param feature Feature to fill.
     */
    public void loadRowIntoFeature(Feature feature) {
        // TODO: ignore deleted records
        df.get(); // denotes whether deleted or current
        // read first part of record

        for (FieldDescriptor fd : this.fieldsDescriptors) {
            byte[] data = new byte[fd.getLength()];
            df.get(data);

            int length = data.length;
            while (length != 0 && data[length - 1] <= ' ') {
                length--;
            }

            String value = new String(data, 0, length);
            feature.setPropertyValue(fd.getName(), value);
        }

        rowNum ++;
    }

    /**
     * Create a field descriptor from the current position of the binary stream.
     * @return FieldDescriptor or null if there is no more available.
     */
    private FieldDescriptor toFieldDescriptor() {
        // If there is no more field description available, return null.
        if (df.position() >= this.DbaseHeaderBytes - 1)
            return null;

        FieldDescriptor fd = new FieldDescriptor();

        // Field name.
        df.get(fd.FieldName);

        // Field type.
        char dt = (char) df.get();
        fd.FieldType = DataType.valueOfDataType(dt);

        // Field address.
        df.get(fd.FieldAddress);

        // Length and scale.
        fd.FieldLength = df.get();
        fd.FieldDecimalCount = df.get();

        df.getShort(); // reserved

        df.get(fd.DbasePlusLanReserved2);

        // Work area id.
        fd.WorkAreaID = df.get();

        df.get(fd.DbasePlusLanReserved3);

        // Fields.
        fd.SetFields = df.get();

        byte[] data = new byte[6];
        df.get(data); // reserved

        return (fd);
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws IOException {
        if (fc != null)
            fc.close();

        if (fis != null)
            fis.close();

        isClosed = true;
    }

    /**
     * Determines if the database is closed.
     * @return true if it is closed.
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator", "\n");

        s.append("DbaseVersion: ").append(DbaseVersion).append(lineSeparator);
        s.append("DbaseLastUpdate: ").append(new String(DbaseLastUpdate)).append(lineSeparator);
        s.append("FeatureCount: ").append(recordCount).append(lineSeparator);
        s.append("DbaseHeaderBytes: ").append(DbaseHeaderBytes).append(lineSeparator);
        s.append("DbaseRecordBytes: ").append(DbaseRecordBytes).append(lineSeparator);
        s.append("DbasePlusLanReserved: ").append(DbasePlusLanReserved).append(lineSeparator);

        return s.toString();
    }
}
