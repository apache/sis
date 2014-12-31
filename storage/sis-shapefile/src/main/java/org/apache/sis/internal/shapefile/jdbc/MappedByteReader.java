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
package org.apache.sis.internal.shapefile.jdbc;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Level;

import org.apache.sis.internal.shapefile.jdbc.resultset.SQLIllegalColumnIndexException;
import org.apache.sis.internal.shapefile.jdbc.resultset.SQLNoSuchFieldException;
import org.opengis.feature.Feature;

/**
 * Reader of a Database Binary content by the way of a {@link java.nio.MappedByteBuffer}
 * @author Marc LE BIHAN
 */
class MappedByteReader extends AbstractByteReader {
    /** The DBF file. */
    private File file;
    
    /** Input Stream on the DBF. */
    private FileInputStream fis;

    /** File channel on the file. */
    private FileChannel fc;

    /** Buffer reader. */
    private MappedByteBuffer df;
    
    /** Indicates if the byte buffer is closed. */
    private boolean isClosed = false;
    
    /** List of field descriptors. */
    private List<FieldDescriptor> m_fieldsDescriptors = new ArrayList<>();
    
    /**
     * Construct a mapped byte reader on a file.
     * @param dbase3File File.
     * @throws FileNotFoundException the file name cannot be null.
     * @throws InvalidDbaseFileFormatException if the database seems to be invalid.
     */
    public MappedByteReader(File dbase3File) throws FileNotFoundException, InvalidDbaseFileFormatException {
        Objects.requireNonNull(dbase3File, "The file cannot be null.");
        
        this.file = dbase3File;
        fis = new FileInputStream(dbase3File);
        fc = fis.getChannel();
        loadDescriptor();
    }

    /**
     * Close the MappedByteReader.
     * @throws IOException if the close operation fails.
     */
    @Override public void close() throws IOException {
        if (fc != null)
            fc.close();

        if (fis != null)
            fis.close();
        
        isClosed = true;
    }

    /**
     * @see org.apache.sis.internal.shapefile.jdbc.ByteReader#isClosed()
     */
    @Override public boolean isClosed() {
        return isClosed;
    }

    /**
     * Load a row into a feature.
     * @param feature Feature to fill.
     */
    @Override public void loadRowIntoFeature(Feature feature) {
        // TODO: ignore deleted records
        df.get(); // denotes whether deleted or current
        // read first part of record

        for (FieldDescriptor fd : m_fieldsDescriptors) {
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
     * Checks if a next row is available. Warning : it may be a deleted one.
     * @return true if a next row is available.
     */
    @Override 
    public boolean nextRowAvailable() {
        return df.hasRemaining();
    }

    /**
     * Read the next row as a set of objects.
     * @return Map of field name / object value.
     */
    @Override 
    public Map<String, Object> readNextRowAsObjects() {
        // TODO: ignore deleted records
        byte isDeleted = df.get(); // denotes whether deleted or current
        // read first part of record

        HashMap<String, Object> fieldsValues = new HashMap<>();

        for (FieldDescriptor fd : m_fieldsDescriptors) {
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
     * Loading the database file content from binary .dbf file.
     * @throws InvalidDbaseFileFormatException if descriptor is not readable. 
     */
    private void loadDescriptor() throws InvalidDbaseFileFormatException {
        try {
            int fsize = (int) fc.size();
            df = fc.map(FileChannel.MapMode.READ_ONLY, 0, fsize);
    
            this.dbaseVersion = df.get();
            df.get(this.dbaseLastUpdate);
    
            df.order(ByteOrder.LITTLE_ENDIAN);
            this.rowCount = df.getInt();
            this.dbaseHeaderBytes = df.getShort();
            this.dbaseRecordBytes = df.getShort();
            df.order(ByteOrder.BIG_ENDIAN);
            
            df.get(reservedFiller1);
            this.reservedIncompleteTransaction = df.get();
            this.reservedEncryptionFlag = df.get();
            df.get(reservedFreeRecordThread);
            df.get(reservedMultiUser);
            reservedMDXFlag = df.get();
            
            // Translate code page value to a known charset.
            this.codePage = df.get();
            this.charset = toCharset(this.codePage);             
            
            df.get(reservedFiller2); 
    
            while(df.position() < this.dbaseHeaderBytes - 1) {
                FieldDescriptor fd = new FieldDescriptor(df); 
                this.m_fieldsDescriptors.add(fd);
                // loop until you hit the 0Dh field terminator
            }
            
            this.descriptorTerminator = df.get();

            // If the last character read after the field descriptor isn't 0x0D, the expected mark has not been found and the DBF is corrupted.
            if (descriptorTerminator != 0x0D) {
                String message = format(Level.WARNING, "excp.filedescriptor_problem", file.getAbsolutePath(), "Character marking the end of the fields descriptors (0x0D) has not been found.");
                throw new InvalidDbaseFileFormatException(message);
            }
        }
        catch(IOException e) {
            // This exception doesn't denote a trouble of file opening because the file has been checked before 
            // the calling of this private function.
            // Therefore, an internal structure problem cause maybe a premature End of file or anything else, but the only thing
            // we can conclude is : we are not before a device trouble, but a file format trouble.
            String message = format(Level.WARNING, "excp.filedescriptor_problem", file.getAbsolutePath(), e.getMessage());
            throw new InvalidDbaseFileFormatException(message);
        }
    }
    
    /**
     * Returns the fields descriptors in their binary format.
     * @return Fields descriptors.
     */
    @Override 
    public List<FieldDescriptor> getFieldsDescriptors() {
        return m_fieldsDescriptors;
    }

    /**
     * Return a field name.
     * @param columnIndex Column index.
     * @param sql For information, the SQL statement that is attempted.
     * @return Field Name.
     * @throws SQLIllegalColumnIndexException if the index is out of bounds.
     */
    @Override 
    public String getFieldName(int columnIndex, String sql) throws SQLIllegalColumnIndexException {
        return getField(columnIndex, sql).getName();
    }

    /**
     * @see org.apache.sis.internal.shapefile.jdbc.ByteReader#getColumnCount()
     */
    @Override 
    public int getColumnCount() {
        return m_fieldsDescriptors.size();
    }
    
    /**
     * Returns the column index for the given column name.
     * The default implementation of all methods expecting a column label will invoke this method.
     * @param columnLabel The name of the column.
     * @param sql For information, the SQL statement that is attempted.
     * @return The index of the given column name : first column is 1.
     * @throws SQLNoSuchFieldException if there is no field with this name in the query.
     */
    @Override 
    public int findColumn(String columnLabel, String sql) throws SQLNoSuchFieldException {
        // If the column name is null, no search is needed.
        if (columnLabel == null) {
            String message = format(Level.WARNING, "excp.no_such_column_in_resultset", columnLabel, sql, file.getName());
            throw new SQLNoSuchFieldException(message, sql, file, columnLabel);
        }
        
        // Search the field among the fields descriptors.
        for(int index=0; index < m_fieldsDescriptors.size(); index ++) {
            if (m_fieldsDescriptors.get(index).getName().equals(columnLabel)) {
                return index + 1;
            }
        }

        // If we are here, we haven't found our field. Throw an exception.
        String message = format(Level.WARNING, "excp.no_such_column_in_resultset", columnLabel, sql, file.getName());
        throw new SQLNoSuchFieldException(message, sql, file, columnLabel);
    }
    
    /**
     * Returns the field descriptor of a given ResultSet column index.
     * @param columnIndex Column index, first column is 1, second is 2, etc.
     * @param sql For information, the SQL statement that is attempted.
     * @return Field Descriptor.
     * @throws SQLIllegalColumnIndexException if the index is out of bounds.
     */
    private FieldDescriptor getField(int columnIndex, String sql) throws SQLIllegalColumnIndexException {
        if (columnIndex < 1 || columnIndex > getColumnCount()) {
            String message = format(Level.WARNING, "excp.illegal_column_index", columnIndex, getColumnCount());
            throw new SQLIllegalColumnIndexException(message, sql, file, columnIndex);
        }
        
        return m_fieldsDescriptors.get(columnIndex-1);
    }
}
