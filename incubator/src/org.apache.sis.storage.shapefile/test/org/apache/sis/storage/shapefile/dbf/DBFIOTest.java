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
package org.apache.sis.storage.shapefile.dbf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DBFIOTest {

    private ChannelDataInput openRead(String path) throws DataStoreException {
        final URL url = DBFIOTest.class.getResource(path);
        final StorageConnector cnx = new StorageConnector(url);
        final ChannelDataInput cdi = cnx.getStorageAs(ChannelDataInput.class);
        cnx.closeAllExcept(cdi);
        return cdi;
    }

    private ChannelDataOutput openWrite(Path path) throws DataStoreException, IOException {
        final StorageConnector cnx = new StorageConnector(path);
        cnx.setOption(OptionKey.OPEN_OPTIONS, new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING});
        final ChannelDataOutput cdo = cnx.getStorageAs(ChannelDataOutput.class);
        cnx.closeAllExcept(cdo);
        return cdo;
    }

    @Test
    public void readTest() throws DataStoreException, IOException, URISyntaxException {
        final String path = "/org/apache/sis/storage/shapefile/point.dbf";
        final ChannelDataInput cdi = openRead(path);

        try (DBFReader reader = new DBFReader(cdi, StandardCharsets.UTF_8, null, null)) {
            final DBFHeader header = reader.getHeader();
            assertEquals(123, header.lastUpdate.getYear()-1900);
            assertEquals(10, header.lastUpdate.getMonthValue());
            assertEquals(27, header.lastUpdate.getDayOfMonth());
            assertEquals(2, header.nbRecord);
            assertEquals(193, header.headerSize);
            assertEquals(120, header.recordSize);
            assertEquals(5, header.fields.length);
            assertEquals("id", header.fields[0].fieldName);
            assertEquals(78,  header.fields[0].fieldType);
            assertEquals(0,    header.fields[0].fieldAddress);
            assertEquals(10,   header.fields[0].fieldLength);
            assertEquals(0,    header.fields[0].fieldDecimals);
            assertEquals("text", header.fields[1].fieldName);
            assertEquals(67,  header.fields[1].fieldType);
            assertEquals(0,    header.fields[1].fieldAddress);
            assertEquals(80,   header.fields[1].fieldLength);
            assertEquals(0,    header.fields[1].fieldDecimals);
            assertEquals("integer", header.fields[2].fieldName);
            assertEquals(78,  header.fields[2].fieldType);
            assertEquals(0,    header.fields[2].fieldAddress);
            assertEquals(10,   header.fields[2].fieldLength);
            assertEquals(0,    header.fields[2].fieldDecimals);
            assertEquals("float", header.fields[3].fieldName);
            assertEquals(78,  header.fields[3].fieldType);
            assertEquals(0,    header.fields[3].fieldAddress);
            assertEquals(11,   header.fields[3].fieldLength);
            assertEquals(6,    header.fields[3].fieldDecimals);
            assertEquals("date", header.fields[4].fieldName);
            assertEquals(68,  header.fields[4].fieldType);
            assertEquals(0,    header.fields[4].fieldAddress);
            assertEquals(8,   header.fields[4].fieldLength);
            assertEquals(0,    header.fields[4].fieldDecimals);


            final Object[] record1 = reader.next();
            assertEquals(1L, record1[0]);
            assertEquals("text1", record1[1]);
            assertEquals(10L, record1[2]);
            assertEquals(20.0, record1[3]);
            assertEquals(LocalDate.of(2023, 10, 27), record1[4]);

            final Object[] record2 = reader.next();
            assertEquals(2L, record2[0]);
            assertEquals(40L, record2[2]);
            assertEquals(60.0, record2[3]);
            assertEquals(LocalDate.of(2023, 10, 28), record2[4]);

            //no more records
            assertNull(reader.next());
        }
    }

    @Test
    public void writeTest() throws DataStoreException, IOException, URISyntaxException {
        final String path = "/org/apache/sis/storage/shapefile/point.dbf";
        testReadAndWrite(path);
    }

    /**
     * Open given dbf, read it and write it to another file the compare them.
     * They must be identical.
     */
    private void testReadAndWrite(String path) throws DataStoreException, IOException, URISyntaxException {
        final ChannelDataInput cdi = openRead(path);

        final Path tempFile = Files.createTempFile("tmp", ".dbf");
        final ChannelDataOutput cdo = openWrite(tempFile);

        try {
            try (DBFReader reader = new DBFReader(cdi, StandardCharsets.US_ASCII, null, null);
                 DBFWriter writer = new DBFWriter(cdo)) {

                writer.writeHeader(reader.getHeader());

                for (Object[] record = reader.next(); record != null; record = reader.next()) {
                    writer.writeRecord(record);
                }
            }

            //compare files
            final byte[] expected = Files.readAllBytes(Paths.get(DBFIOTest.class.getResource(path).toURI()));
            final byte[] result = Files.readAllBytes(tempFile);
            assertArrayEquals(expected, result);

        } finally {
            Files.delete(tempFile);
        }
    }

    /**
     * Test reading only selected fields.
     */
    @Test
    public void readSelectionTest() throws DataStoreException, IOException {
        final String path = "/org/apache/sis/storage/shapefile/point.dbf";
        final ChannelDataInput cdi = openRead(path);

        try (DBFReader reader = new DBFReader(cdi, StandardCharsets.UTF_8, null, new int[] {1,3})) {
            final DBFHeader header = reader.getHeader();

            final Object[] record1 = reader.next();
            assertEquals("text1", record1[0]);
            assertEquals(20.0, record1[1]);

            final Object[] record2 = reader.next();
            assertEquals("text2", record2[0]);
            assertEquals(60.0, record2[1]);

            //no more records
            assertNull(reader.next());
        }
    }
}
