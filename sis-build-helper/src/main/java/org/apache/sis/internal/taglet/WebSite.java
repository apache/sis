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


/**
 * The <code>@website</code> tag for inserting a URL to the web site.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 */
public final class WebSite extends InlineTaglet {
    /**
     * URL to the web site, <strong>without</strong> trailing slash.
     */
    static final String URL = "http://builds.apache.org/job/sis-trunk/site";

    /**
     * Register this taglet.
     *
     * @param tagletMap the map to register this tag to.
     */
    public static void register(final Map<String,Taglet> tagletMap) {
       final WebSite tag = new WebSite();
       tagletMap.put(tag.getName(), tag);
    }

    /**
     * Constructs a default <code>@website</code> taglet.
     */
    private WebSite() {
        super();
    }

    /**
     * Returns the name of this custom tag.
     *
     * @return The tag name.
     */
    @Override
    public String getName() {
        return "website";
    }

    /**
     * Given the <code>Tag</code> representation of this custom tag, return its string representation.
     *
     * @param  tag The tag to format.
     * @return A string representation of the given tag.
     */
    @Override
    public String toString(final Tag tag) {
        return URL;
    }
}
