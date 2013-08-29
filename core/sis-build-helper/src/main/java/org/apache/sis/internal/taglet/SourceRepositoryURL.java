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
package org.apache.sis.internal.taglet;

import java.util.Map;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;
import com.sun.tools.doclets.formats.html.ConfigurationImpl;


/**
 * The <code>@scmUrl</code> tag for inserting a URL to a file in the source code repository.
 * This tag shall contain a keyword, for example <code>{@scmUrl gmd-data}</code>.
 * Valid keywords are:
 *
 * <table class="sis>
 *   <tr><th>Keyword</th>   <th>path</th></tr>
 *   <tr><td>gmd-data</td>  <td>core/sis-metadata/src/test/resources/org/apache/sis/xml</td></tr>
 * </table>
 *
 * The URL never contain trailing <code>'/'</code> character.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from GeoAPI 3.1)
 * @version 0.3
 * @module
 */
public final class SourceRepositoryURL extends InlineTaglet {
    /**
     * Registers this taglet.
     *
     * @param tagletMap the map to register this tag to.
     */
    public static void register(final Map<String,Taglet> tagletMap) {
       final SourceRepositoryURL tag = new SourceRepositoryURL();
       tagletMap.put(tag.getName(), tag);
    }

    /**
     * Constructs a default <code>@scmUrl</code> taglet.
     */
    private SourceRepositoryURL() {
        super();
    }

    /**
     * Returns the name of this custom tag.
     *
     * @return The tag name.
     */
    @Override
    public String getName() {
        return "scmUrl";
    }

    /**
     * Given the <code>Tag</code> representation of this custom tag, return its string representation.
     *
     * @param  tag The tag to format.
     * @return A string representation of the given tag.
     */
    @Override
    public String toString(final Tag tag) {
        final StringBuilder url = new StringBuilder("http://svn.apache.org/repos/asf/sis/branches/JDK7");
        final String keyword = tag.text();
        switch (keyword) {
            case "gmd-data": {
                url.append("/core/sis-metadata/src/test/resources/org/apache/sis/xml");
                break;
            }
            default: {
                ConfigurationImpl.getInstance().root.printWarning(tag.position(), "Unknown keyword: " + keyword);
                break;
            }
        }
        return url.toString();
    }
}
