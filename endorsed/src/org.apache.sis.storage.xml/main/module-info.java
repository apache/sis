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

/**
 * XML files store.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.8
 */
module org.apache.sis.storage.xml {
    requires jakarta.xml.bind;
    requires transitive org.apache.sis.storage;

    provides org.apache.sis.storage.DataStoreProvider
        with org.apache.sis.internal.storage.gpx.StoreProvider;

    exports org.apache.sis.storage.gps;

    exports org.apache.sis.internal.storage.gpx to
            org.apache.sis.console,
            org.apache.sis.gui;                     // In the "optional" sub-project.

    /*
     * Allow JAXB to use reflection for marshalling and
     * unmarshalling Apache SIS objects in XML documents.
     */
    opens org.apache.sis.internal.storage.gpx to jakarta.xml.bind;
}
