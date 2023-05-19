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
    // Dependencies visible in public API.
    requires transitive java.desktop;
    requires transitive javafx.controls;
    requires transitive org.apache.sis.portrayal;

    // Dependencies for implementation only.
    requires java.prefs;
    requires java.logging;
    requires java.sql;
    requires java.xml;
    requires jakarta.xml.bind;
    requires javafx.web;
    requires org.apache.sis.storage.xml;
    requires org.apache.sis.referencing.gazetteer;

    provides org.apache.sis.setup.InstallationResources
        with org.apache.sis.internal.gui.OptionalDataDownloader;

    exports org.apache.sis.gui;
    exports org.apache.sis.gui.metadata;
    exports org.apache.sis.gui.referencing;
    exports org.apache.sis.gui.dataset;
    exports org.apache.sis.gui.coverage;
    exports org.apache.sis.gui.map;
}
