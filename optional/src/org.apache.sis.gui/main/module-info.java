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
 * JavaFX application for Apache SIS.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
module org.apache.sis.gui {
    requires            java.prefs;
    requires            java.logging;
    requires transitive java.desktop;
    requires            java.sql;
    requires            java.xml;
    requires            jakarta.xml.bind;
    requires transitive javafx.base;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires            javafx.web;
    requires transitive org.apache.sis.util;
    requires transitive org.apache.sis.metadata;
    requires transitive org.apache.sis.referencing;
    requires            org.apache.sis.referencing.gazetteer;
    requires transitive org.apache.sis.feature;
    requires transitive org.apache.sis.storage;
    requires            org.apache.sis.storage.xml;         // Those formats have special handling in `DataViewer`.
    requires transitive org.apache.sis.portrayal;

    provides org.apache.sis.setup.InstallationResources
        with org.apache.sis.gui.internal.io.OptionalDataDownloader;

    exports org.apache.sis.gui;
    exports org.apache.sis.gui.metadata;
    exports org.apache.sis.gui.referencing;
    exports org.apache.sis.gui.dataset;
    exports org.apache.sis.gui.coverage;
    exports org.apache.sis.gui.map;
}
