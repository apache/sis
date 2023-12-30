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
package org.apache.sis.storage.shapefile.shp;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
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
        StorageConnector cnx = new StorageConnector(Paths.get("/path/to/file.shp"));
        ChannelDataInput channel = cnx.getStorageAs(ChannelDataInput.class);
        try (ShapeReader reader = new ShapeReader(channel, null)) {

            //print the DBase fields
            ShapeHeader header = reader.getHeader();
            System.out.println(header.shapeType);

            //iterate over records
            for (ShapeRecord record = reader.next(); record != null; record = reader.next()){
                System.out.println(record.recordNumber);
                System.out.println(record.bbox);
                System.out.println(record.geometry.toText());
            }
        }
        // @end
    }

    public void write() throws IllegalArgumentException, DataStoreException, IOException{
        // @start region="write"
        //open a channel
        StorageConnector cnx = new StorageConnector(Paths.get("/path/to/file.shp"));
        cnx.setOption(OptionKey.OPEN_OPTIONS, new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING});
        ChannelDataOutput channel = cnx.getStorageAs(ChannelDataOutput.class);

        //define the header
        ShapeHeader header = new ShapeHeader();
        header.shapeType = ShapeType.POINT;

        //write records
        GeometryFactory gf = new GeometryFactory();
        try (ShapeWriter writer = new ShapeWriter(channel)) {
            writer.writeHeader(header);
            int recordNumber = 1;
            writer.writeRecord(recordNumber++, gf.createPoint(new Coordinate(10,20)));
            writer.writeRecord(recordNumber++, gf.createPoint(new Coordinate(-7, 45)));
            // ... more records
        }
        // @end
    }

}
