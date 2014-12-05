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
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

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
    
    /** Reserved (dBASE IV) Filled with 00h. */
    private byte[] reservedFiller1 = new byte[2];
    
    /** 
     * Reserved : Incomplete transaction (dBASE IV).
     * 00h : Transaction ended (or rolled back).
     * 01h : Transaction started. 
     */
    private byte reservedIncompleteTransaction;

    /**
     * Reserved : Encryption flag (dBASE IV).
     * 00h : Not encrypted. 
     * 01h : Data encrypted.
     */
    private byte reservedEncryptionFlag;
    
    /** Reserved : Free record thread (for LAN only). */
    private byte[] reservedFreeRecordThread = new byte[4];
    
    /** Reserved : For multi-user (DBase 3+). */
    private byte[] reservedMultiUser = new byte[8];

    /** Reserved : MDX flag (dBASE IV). */
    private byte reservedMDXFlag;
    
    /** Binary code page value. */
    private byte codePage;
    
    /** Database charset. */
    private Charset charset;
    
    /** Reserved (dBASE IV) Filled with 00h. */
    private byte[] reservedFiller2 = new byte[2];
    
    /** Marks the end of the descriptor : must be 0x0D. */
    private byte descriptorTerminator;

    /** Fields descriptor. */
    public FieldsDescriptors fieldsDescriptors = new FieldsDescriptors();

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
     * @param file Database file.
     * @throws FileNotFoundException if the database file cannot be found.
     * @throws InvalidDbaseFileFormatException if the database seems to be invalid.
     */
    public Database(String file) throws FileNotFoundException, InvalidDbaseFileFormatException {
        Objects.requireNonNull(file, "The database file to load cannot be null.");
        dbfFile = file;

        fis = new FileInputStream(file);
        fc = fis.getChannel();
        rowNum = 0;
        isClosed = false;
        
        loadDescriptor();
    }

    /**
     * Returns the database charset, converted from its internal code page.
     * @return Charset.
     */
    public Charset getCharset() {
        return this.charset;
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
     * Read the next row as a set of objects.
     * @return Map of field name / object value.
     */
    public HashMap<String, Object> readNextRowAsObjects() {
        // TODO: ignore deleted records
        byte isDeleted = df.get(); // denotes whether deleted or current
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

        return fd;
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

     * Return a date from a byte array.
     * @param yymmdd byte[3] with byte[0] = year (2 digits), [1] = month, [2] = day. 
     * @return Date.
     */
    private Date toDate(byte[] yymmdd) {
        Objects.requireNonNull(yymmdd, "the yymmdd bytes cannot be null");
        
        if (yymmdd.length != 3)
            throw new IllegalArgumentException(MessageFormat.format("Database:toDate() works only on a 3 bytes YY MM DD date. this array has {0} length", yymmdd.length));
        
        Objects.requireNonNull(yymmdd[0], "the year byte cannot be null");
        Objects.requireNonNull(yymmdd[1], "the month byte cannot be null");
        Objects.requireNonNull(yymmdd[2], "the day byte cannot be null");
        
        int year = yymmdd[0] < 70 ? 100 + yymmdd[0] : yymmdd[0];
        int month = yymmdd[1];
        int day = yymmdd[2];
        
        @SuppressWarnings("deprecation") // But everything is deprecated in DBF files... 
        Date date = new Date(year, month, day);
        return date;
    }

    /**
     * Convert the binary code page value of the Dbase 3 file to a recent Charset.
     * @param codePageBinaryValue page code binary value.
     * @return Charset.
     * @throws InvalidDbaseFileFormatException if the binary value is not one of the standard values that the DBF file should carry : the Dbase 3
     * file might be corrupted.
     * @throws UnsupportedCharsetException if the code page as no representation in recents Charset (legacy DOS or macintosh charsets).
     */
    private Charset toCharset(byte codePageBinaryValue) throws InvalidDbaseFileFormatException, UnsupportedCharsetException {
        // Attempt to find a known conversion.
        String dbfCodePage = toCodePage(codePageBinaryValue);
        
        // If no conversion has been found, decide if the cause is an unsupported value or an illegal value to choose the good exception to return.
        if (dbfCodePage == null) {
            switch(Byte.toUnsignedInt(codePageBinaryValue)) {
                case 0x04: dbfCodePage = "unsupported"; break;
                case 0x68: dbfCodePage = "unsupported"; break; // Kamenicky (Czech) MS-DOS
                case 0x69: dbfCodePage = "unsupported"; break; // Mazovia (Polish) MS-DOS
                case 0x96: dbfCodePage = "unsupported"; break; // russian mac
                case 0x97: dbfCodePage = "unsupported"; break; // eastern european macintosh
                case 0x98: dbfCodePage = "unsupported"; break; // greek macintosh
                case 0xC8: dbfCodePage = "unsupported"; break; // windows ee
                default: dbfCodePage = "invalid"; break;
            }
        }
        
        assert dbfCodePage != null;
        
        // If the code page is invalid, the database itself has chances to be invalid too.
        if (dbfCodePage.equals("invalid")) {
            String message = format(Level.SEVERE, "excp.illegal_codepage", codePageBinaryValue, new File(dbfFile).getAbsolutePath());
            throw new InvalidDbaseFileFormatException(message);
        }
        
        // If the code page cannot find a match for a more recent Charset, we wont be able to handle this DBF.
        if (dbfCodePage.equals("unsupported")) {
            String message = format(Level.SEVERE, "excp.unsupported_codepage", dbfCodePage, new File(dbfFile).getAbsolutePath());
            throw new UnsupportedCharsetException(message);
        }
        
        try {
            return Charset.forName(dbfCodePage);
        }
        catch(IllegalArgumentException e) {
            // If this happens here, it means that we have selected a wrong charset. We have a bug.
            String message = format(Level.SEVERE, "assert.wrong_charset_selection", dbfCodePage, new File(dbfFile).getAbsolutePath());
            throw new RuntimeException(message);
        }
    }

    /**
     * Return a Charset code page from a binary code page value.
     * @param pageCodeBinaryValue binary code page value.
     * @return Page code.
     */
    private String toCodePage(byte pageCodeBinaryValue) {
        // From http://trac.osgeo.org/gdal/ticket/2864
        HashMap<Integer, String> knownConversions = new HashMap<>();
        knownConversions.put(0x01, "cp437"); //  U.S. MS–DOS
        knownConversions.put(0x02, "cp850"); // International MS–DOS
        knownConversions.put(0x03, "cp1252"); // Windows ANSI
        knownConversions.put(0x08, "cp865"); //  Danish OEM
        knownConversions.put(0x09, "cp437"); //  Dutch OEM
        knownConversions.put(0x0a, "cp850"); //  Dutch OEM*
        knownConversions.put(0x0b, "cp437"); //  Finnish OEM
        knownConversions.put(0x0d, "cp437"); //  French OEM
        knownConversions.put(0x0e, "cp850"); //  French OEM*
        knownConversions.put(0x0f, "cp437"); //  German OEM
        knownConversions.put(0x10, "cp850"); //  German OEM*
        knownConversions.put(0x11, "cp437"); //  Italian OEM
        knownConversions.put(0x12, "cp850"); //  Italian OEM*
        knownConversions.put(0x13, "cp932"); //  Japanese Shift-JIS
        knownConversions.put(0x14, "cp850"); //  Spanish OEM*
        knownConversions.put(0x15, "cp437"); //  Swedish OEM
        knownConversions.put(0x16, "cp850"); //  Swedish OEM*
        knownConversions.put(0x17, "cp865"); //  Norwegian OEM
        knownConversions.put(0x18, "cp437"); //  Spanish OEM
        knownConversions.put(0x19, "cp437"); //  English OEM (Britain)
        knownConversions.put(0x1a, "cp850"); //  English OEM (Britain)*
        knownConversions.put(0x1b, "cp437"); //  English OEM (U.S.)
        knownConversions.put(0x1c, "cp863"); //  French OEM (Canada)
        knownConversions.put(0x1d, "cp850"); //  French OEM*
        knownConversions.put(0x1f, "cp852"); //  Czech OEM
        knownConversions.put(0x22, "cp852"); //  Hungarian OEM
        knownConversions.put(0x23, "cp852"); //  Polish OEM
        knownConversions.put(0x24, "cp860"); //  Portuguese OEM
        knownConversions.put(0x25, "cp850"); //  Portuguese OEM*
        knownConversions.put(0x26, "cp866"); //  Russian OEM
        knownConversions.put(0x37, "cp850"); //  English OEM (U.S.)*
        knownConversions.put(0x40, "cp852"); //  Romanian OEM
        knownConversions.put(0x4d, "cp936"); //  Chinese GBK (PRC)
        knownConversions.put(0x4e, "cp949"); //  Korean (ANSI/OEM)
        knownConversions.put(0x4f, "cp950"); //  Chinese Big5 (Taiwan)
        knownConversions.put(0x50, "cp874"); //  Thai (ANSI/OEM)
        knownConversions.put(0x57, "cp1252"); // ANSI
        knownConversions.put(0x58, "cp1252"); // Western European ANSI
        knownConversions.put(0x59, "cp1252"); // Spanish ANSI
        knownConversions.put(0x64, "cp852"); //  Eastern European MS–DOS
        knownConversions.put(0x65, "cp866"); //  Russian MS–DOS
        knownConversions.put(0x66, "cp865"); //  Nordic MS–DOS
        knownConversions.put(0x67, "cp861"); //  Icelandic MS–DOS
        knownConversions.put(0x6a, "cp737"); //  Greek MS–DOS (437G)
        knownConversions.put(0x6b, "cp857"); //  Turkish MS–DOS
        knownConversions.put(0x6c, "cp863"); //  French–Canadian MS–DOS
        knownConversions.put(0x78, "cp950"); //  Taiwan Big 5
        knownConversions.put(0x79, "cp949"); //  Hangul (Wansung)
        knownConversions.put(0x7a, "cp936"); //  PRC GBK
        knownConversions.put(0x7b, "cp932"); //  Japanese Shift-JIS
        knownConversions.put(0x7c, "cp874"); //  Thai Windows/MS–DOS
        knownConversions.put(0x86, "cp737"); //  Greek OEM
        knownConversions.put(0x87, "cp852"); //  Slovenian OEM
        knownConversions.put(0x88, "cp857"); //  Turkish OEM
        knownConversions.put(0xc8, "cp1250"); // Eastern European Windows
        knownConversions.put(0xc9, "cp1251"); // Russian Windows
        knownConversions.put(0xca, "cp1254"); // Turkish Windows
        knownConversions.put(0xcb, "cp1253"); // Greek Windows
        knownConversions.put(0xcc, "cp1257"); // Baltic Windows
        
        return(knownConversions.get(Byte.toUnsignedInt(pageCodeBinaryValue)));
    }

    /**
     * Return the database as a {@link java.io.File}.
     * @return File.
     */
    public File getFile() {
        return(new File(dbfFile));
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return format("toString", System.getProperty("line.separator", "\n"),
               DbaseVersion, toDate(DbaseLastUpdate), recordCount, fieldsDescriptors, DbaseHeaderBytes, DbaseRecordBytes, charset);
        
        /*StringBuilder s = new StringBuilder();
        s.append("DbasePlusLanReserved: ").append(DbasePlusLanReserved).append(lineSeparator);
        */
    }
}
