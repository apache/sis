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
package org.apache.sis.internal.system;

import java.util.EnumSet;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.sis.setup.About;
import org.apache.sis.util.collection.TreeTable;


/**
 * Provides information about the state of a running Apache SIS instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public interface SupervisorMBean {
    /**
     * Returns information about the current configuration.
     * This method tries to focus on the information that are the most relevant to SIS.
     * Those information are grouped in sections: a "Versions" section containing the
     * Apache SIS version, Java version and operation system version; a "Classpath"
     * section containing bootstrap, extension and user classpath, <i>etc</i>.
     *
     * @param  sections The section for which information are desired.
     * @param  locale   The locale to use for formatting the texts in the tree, or {@code null} for the default.
     * @param  timezone The timezone to use for formatting the dates, or {@code null} for the default.
     * @return Configuration information, as a tree for grouping some configuration by sections.
     */
    TreeTable configuration(EnumSet<About> sections, Locale locale, TimeZone timezone);

    /**
     * If there is something wrong with the current Apache SIS status,
     * returns descriptions of the problems. Otherwise returns {@code null}.
     *
     * @param  locale The locale to use for reporting messages, or {@code null} for the default.
     * @return A description of a problems in the library, or {@code null} if none.
     */
    String[] warnings(Locale locale);
}
