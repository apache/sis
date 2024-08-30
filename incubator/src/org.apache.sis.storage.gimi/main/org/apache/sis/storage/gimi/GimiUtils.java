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
package org.apache.sis.storage.gimi;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.ISOBMFFReader;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class GimiUtils {

    public static void printAll(Path path) throws IllegalArgumentException, DataStoreException, IOException {

        final StorageConnector cnx = new StorageConnector(path);
        final ChannelDataInput cdi = cnx.getStorageAs(ChannelDataInput.class);
        final ISOBMFFReader reader = new ISOBMFFReader(cdi);

        try {
            while(true) {
                final Box box = reader.readBox();
                System.out.println(box);
                cdi.seek(box.boxOffset + box.size);

            }
        } catch (EOFException ex) {
            //do nothing
        }
    }

}
