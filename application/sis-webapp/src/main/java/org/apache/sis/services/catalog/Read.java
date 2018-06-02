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
package org.apache.sis.services.catalog;

import java.io.File;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.metadata.sql.MetadataWriter;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.opengis.metadata.Metadata;
import org.postgresql.ds.PGSimpleDataSource;



/**
 *
 * @author haonguyen
 */
public class Read {
    public static void main(String[] args) throws DataStoreException, MetadataStoreException {
//        Connector a = new Connector();
//        PGSimpleDataSource dataSource = a.Connect();

        // Example using PostgreSQL data source (org.postgresql.ds.PGSimpleDataSource)
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setServerName("localhost");
        source.setDatabaseName("csw");
        source.setUser("postgres");
        source.setPassword("1234");
        MetadataWriter writer = new MetadataWriter(MetadataStandard.ISO_19115, source, "metadata", null);
       DataStore ds = DataStores.open(new File("/home/haonguyen/data/LC081260462017073101T1-SC20170831010609/LC08_L1TP_126046_20170731_20170811_01_T1_MTL.txt")) ;
//        
//        AbstractMetadata
//            Metadata md = ds.getMetadata();
////            System.out.println(ds.getMetadata());
////            String a = b.search(md);
////            System.out.println(a);
//            String id = writer.add(md);
//
//        Metadata metadata = writer.lookup(Metadata.class, id);
        // Registration assuming that a JNDI implementation is available
//        Context env = (Context) InitialContext.doLookup("java:comp/env");
//        env.bind("jdbc/SpatialMetadata", ds);
        

    }
}
