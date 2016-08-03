/*
 * Copyright 2016 haonguyen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geotiff;


import java.io.File;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.StorageConnector;
import org.opengis.metadata.Metadata;





/**
 *
 * @author haonguyen
 */
public class NewClass {
    
    public static void main(String[] args) throws Exception {
        try (final GeoTiffStore store = new GeoTiffStore(new StorageConnector(new File("/home/haonguyen/Downloads/sample.tif")))) {
           //DefaultMetadata md = new DefaultMetadata(store.getMetadata());
            System.out.println(store.getMetadata());
        }
    }
}


