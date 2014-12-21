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
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;

import org.opengis.feature.Feature;

/**
 * Reader of a Database Binary content by the way of a {@link java.nio.MappedByteBuffer}
 * @author Marc LE BIHAN
 */
class MappedByteReader extends AbstractByteReader {
    /** Input Stream on the DBF. */
    private FileInputStream fis;

    /** File channel on the file. */
    private FileChannel fc;

    /** Buffer reader. */
    private MappedByteBuffer df;

    /**
     * Construct a mapped byte reader on a file.
     * @param fileName File name.
     * @throws FileNotFoundException the file name cannot be null.
     * @throws InvalidDbaseFileFormatException if the database seems to be invalid.
     */
    public MappedByteReader(String fileName) throws FileNotFoundException, InvalidDbaseFileFormatException {
        Objects.requireNonNull(fileName, "The filename cannot be null.");
        
        dbfFile = fileName;
        fis = new FileInputStream(fileName);
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
    }

    /**
     * Load a row into a feature.
     * @param feature Feature to fill.
     */
    @Override public void loadRowIntoFeature(Feature feature) {
        // TODO: ignore deleted records
        df.get(); // denotes whether deleted or current
        // read first part of record

        for (FieldDescriptor fd : fieldsDescriptors) {
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
     * Read the next row as a set of objects.
     * @return Map of field name / object value.
     */
    @Override public HashMap<String, Object> readNextRowAsObjects() {
        // TODO: ignore deleted records
        byte isDeleted = df.get(); // denotes whether deleted or current
        // read first part of record

        HashMap<String, Object> fieldsValues = new HashMap<>();

        for (FieldDescriptor fd : fieldsDescriptors) {
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
    
            this.DbaseVersion = df.get();
            df.get(this.DbaseLastUpdate);
    
            df.order(ByteOrder.LITTLE_ENDIAN);
            this.recordCount = df.getInt();
            this.DbaseHeaderBytes = df.getShort();
            this.DbaseRecordBytes = df.getShort();
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
    
            while(df.position() < this.DbaseHeaderBytes - 1) {
                FieldDescriptor fd = toFieldDescriptor();
                this.fieldsDescriptors.add(fd);
                // loop until you hit the 0Dh field terminator
            }
            
            this.descriptorTerminator = df.get();

            // If the last character read after the field descriptor isn't 0x0D, the expected mark has not been found and the DBF is corrupted.
            if (descriptorTerminator != 0x0D) {
                String message = format(Level.SEVERE, "excp.filedescriptor_problem", new File(dbfFile).getAbsolutePath(), "Character marking the end of the fields descriptors (0x0D) has not been found.");
                throw new InvalidDbaseFileFormatException(message);
            }
        }
        catch(IOException e) {
            // This exception doesn't denote a trouble of file opening because the file has been checked before 
            // the calling of this private function.
            // Therefore, an internal structure problem cause maybe a premature End of file or anything else, but the only thing
            // we can conclude is : we are not before a device trouble, but a file format trouble.
            String message = format(Level.SEVERE, "excp.filedescriptor_problem", new File(dbfFile).getAbsolutePath(), e.getMessage());
            throw new InvalidDbaseFileFormatException(message);
        }
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

        return fd;
    }
}
