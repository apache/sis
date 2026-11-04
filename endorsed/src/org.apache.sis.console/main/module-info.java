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
 * Command line interface for Apache <abbr>SIS</abbr>.
 * An introduction is available in the <a href="https://sis.apache.org/command-line.html">Apache SIS web site</a>.
 *
 * <h2>Usage</h2>
 * The main method is launched by the {@code sis} executable (a bash script).
 * Invoking {@code sis} without argument shows a summary of available commands and all options.
 * For executing a command, the syntax is:
 *
 * <blockquote><pre>sis <command> [options] [files]</pre></blockquote>
 *
 * For example, {@code sis crs EPSG:6676} prints the definition of the <abbr>CRS</abbr> identified by
 * the <abbr>EPSG</abbr> code 6676. Other commands allow to verify the validity of a <abbr>CRS</abbr>,
 * perform coordinate operations or show <abbr>ISO</abbr> 19115 metadata.
 * See above-cited web site for more details.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since SIS 0.3
 */
module org.apache.sis.console {
    requires            java.sql;
    requires            java.rmi;
    requires            java.management;
    requires            jakarta.xml.bind;
    requires transitive org.apache.sis.util;            // Transitive because of `ResourcesDownloader`.
    requires            org.apache.sis.metadata;
    requires            org.apache.sis.referencing;
    requires            org.apache.sis.feature;
    requires            org.apache.sis.storage;
    requires            org.apache.sis.storage.xml;     // Because of GPX in `OutputFormat`.

    provides org.apache.sis.setup.InstallationResources
        with org.apache.sis.console.ResourcesDownloader;

    exports org.apache.sis.console;
}
