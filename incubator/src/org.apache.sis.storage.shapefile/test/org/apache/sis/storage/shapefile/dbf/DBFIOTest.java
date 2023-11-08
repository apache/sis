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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.junit.Test;
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

    @Test
    public void readTest() throws DataStoreException, IOException {
        final String path = "/org/apache/sis/storage/shapefile/point.dbf";
        final ChannelDataInput cdi = openRead(path);

        try (DBFReader reader = new DBFReader(cdi, StandardCharsets.UTF_8, null)) {
            final DBFHeader header = reader.getHeader();
            assertEquals(123, header.year);
            assertEquals(10, header.month);
            assertEquals(27, header.day);
            assertEquals(2, header.nbRecord);
            assertEquals(193, header.headerSize);
            assertEquals(120, header.recordSize);
            assertEquals(5, header.fields.length);
            assertEquals("id", header.fields[0].fieldName);
            assertEquals(110,  header.fields[0].fieldType);
            assertEquals(0,    header.fields[0].fieldAddress);
            assertEquals(10,   header.fields[0].fieldLength);
            assertEquals(0,    header.fields[0].fieldLDecimals);
            assertEquals("text", header.fields[1].fieldName);
            assertEquals(99,  header.fields[1].fieldType);
            assertEquals(0,    header.fields[1].fieldAddress);
            assertEquals(80,   header.fields[1].fieldLength);
            assertEquals(0,    header.fields[1].fieldLDecimals);
            assertEquals("integer", header.fields[2].fieldName);
            assertEquals(110,  header.fields[2].fieldType);
            assertEquals(0,    header.fields[2].fieldAddress);
            assertEquals(10,   header.fields[2].fieldLength);
            assertEquals(0,    header.fields[2].fieldLDecimals);
            assertEquals("float", header.fields[3].fieldName);
            assertEquals(110,  header.fields[3].fieldType);
            assertEquals(0,    header.fields[3].fieldAddress);
            assertEquals(11,   header.fields[3].fieldLength);
            assertEquals(6,    header.fields[3].fieldLDecimals);
            assertEquals("date", header.fields[4].fieldName);
            assertEquals(100,  header.fields[4].fieldType);
            assertEquals(0,    header.fields[4].fieldAddress);
            assertEquals(8,   header.fields[4].fieldLength);
            assertEquals(0,    header.fields[4].fieldLDecimals);


            final DBFRecord record1 = reader.next();
            assertEquals(1L, record1.fields[0]);
            assertEquals("text1", record1.fields[1]);
            assertEquals(10L, record1.fields[2]);
            assertEquals(20.0, record1.fields[3]);
            assertEquals(LocalDate.of(2023, 10, 27), record1.fields[4]);

            final DBFRecord record2 = reader.next();
            assertEquals(2L, record2.fields[0]);
            assertEquals("text2", record2.fields[1]);
            assertEquals(40L, record2.fields[2]);
            assertEquals(60.0, record2.fields[3]);
            assertEquals(LocalDate.of(2023, 10, 28), record2.fields[4]);

            //no more records
            assertNull(reader.next());
        }
    }

    /**
     * Test reading only selected fields.
     */
    @Test
    public void readSelectionTest() throws DataStoreException, IOException {
        final String path = "/org/apache/sis/storage/shapefile/point.dbf";
        final ChannelDataInput cdi = openRead(path);

        try (DBFReader reader = new DBFReader(cdi, StandardCharsets.UTF_8, new int[]{1,3})) {
            final DBFHeader header = reader.getHeader();

            final DBFRecord record1 = reader.next();
            assertEquals("text1", record1.fields[0]);
            assertEquals(20.0, record1.fields[1]);

            final DBFRecord record2 = reader.next();
            assertEquals("text2", record2.fields[0]);
            assertEquals(60.0, record2.fields[1]);

            //no more records
            assertNull(reader.next());
        }
    }
}
