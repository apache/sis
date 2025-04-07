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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class Snippets {

    public void read() throws IllegalArgumentException, DataStoreException, IOException{
        // @start region="read"
        //open a channel
        StorageConnector cnx = new StorageConnector(Paths.get("/path/to/file.dbf"));
        ChannelDataInput channel = cnx.getStorageAs(ChannelDataInput.class);
        try (var reader = new DBFReader(channel, StandardCharsets.UTF_8, null, null)) {

            //print the DBase fields
            DBFHeader header = reader.getHeader();
            for (DBFField field : header.fields) {
                System.out.println(field);
            }

            //iterate over records
            for (Object[] record = reader.next(); record != null; record = reader.next()){

                if (record == DBFReader.DELETED_RECORD) {
                    //a deleted record, those should be ignored
                    continue;
                }

                //print record values
                for (int i = 0; i < header.fields.length; i++) {
                    System.out.println(header.fields[i].fieldName + " : " + record[i]);
                }
            }
        }
        // @end
    }

    public void write() throws IllegalArgumentException, DataStoreException, IOException{
        // @start region="write"
        //open a channel
        StorageConnector cnx = new StorageConnector(Paths.get("/path/to/file.dbf"));
        cnx.setOption(OptionKey.OPEN_OPTIONS, new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING});
        ChannelDataOutput channel = cnx.getStorageAs(ChannelDataOutput.class);

        //define the header
        Charset charset = StandardCharsets.UTF_8;
        DBFHeader header = new DBFHeader();
        header.lastUpdate = LocalDate.now();
        header.fields = new DBFField[] {
            new DBFField("id", DBFField.TYPE_NUMBER, 0, 8, 0, charset, null),
            new DBFField("desc", DBFField.TYPE_CHAR, 0, 255, 0, charset, null),
            new DBFField("value", DBFField.TYPE_NUMBER, 0, 11, 6, charset, null)
        };

        //write records
        try (DBFWriter writer = new DBFWriter(channel)) {
            writer.writeHeader(header);
            writer.writeRecord(1, "A short description", 3.14);
            writer.writeRecord(2, "Another short description", 123.456);
            // ... more records
        }
        // @end
    }

}
