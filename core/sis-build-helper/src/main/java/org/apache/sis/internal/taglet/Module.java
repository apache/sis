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

import java.io.File;
import java.util.Map;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;


/**
 * The <code>@module</code> tag. This tag expects no argument.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class Module implements Taglet {
    /**
     * Register this taglet.
     *
     * @param tagletMap the map to register this tag to.
     */
    public static void register(final Map<String,Taglet> tagletMap) {
       final Module tag = new Module();
       tagletMap.put(tag.getName(), tag);
    }

    /**
     * The SIS module in which the <code>@module</code> taglet has been found.
     */
    private String module;

    /**
     * Constructs a default <code>@module</code> taglet.
     */
    private Module() {
        super();
    }

    /**
     * Returns the name of this custom tag.
     *
     * @return The tag name.
     */
    @Override
    public String getName() {
        return "module";
    }

    /**
     * Returns {@code false} since <code>@module</code> can not be used in overview.
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean inOverview() {
        return false;
    }

    /**
     * Returns {@code true} since <code>@module</code> can be used in package documentation.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean inPackage() {
        return true;
    }

    /**
     * Returns {@code true} since <code>@module</code> can be used in type documentation
     * (classes or interfaces). This is actually its main target.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean inType() {
        return true;
    }

    /**
     * Returns {@code false} since <code>@module</code> can not be used in constructor.
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean inConstructor() {
        return false;
    }

    /**
     * Returns {@code false} since <code>@module</code> can not be used in method documentation.
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean inMethod() {
        return false;
    }

    /**
     * Returns {@code false} since <code>@module</code> can not be used in field documentation.
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean inField() {
        return false;
    }

    /**
     * Returns {@code false} since <code>@module</code> is not an inline tag.
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean isInlineTag() {
        return false;
    }

    /**
     * Given the <code>Tag</code> representation of this custom tag, return its string representation.
     * The default implementation invokes the array variant of this method.
     *
     * @param tag The tag to format.
     * @return A string representation of the given tag.
     */
    @Override
    public String toString(final Tag tag) {
        return toString(new Tag[] {tag});
    }

    /**
     * Given an array of {@code Tag}s representing this custom tag, return its string
     * representation.
     *
     * @param tags The tags to format.
     * @return A string representation of the given tags.
     */
    @Override
    public String toString(final Tag[] tags) {
        if (tags == null || tags.length == 0) {
            return "";
        }
        final StringBuilder buffer = new StringBuilder(128);
        buffer.append("\n<p><font size=\"-1\">");
        for (final Tag tag : tags) {
            File file = tag.position().file();
            module = file.getName();
            while ((file = file.getParentFile()) != null) {
                if (file.getName().equals("src")) {
                    file = file.getParentFile();
                    if (file != null) {
                        module = file.getName();
                    }
                    break;
                }
            }
            /*
             * Appends the module link.
             */
            buffer.append("Defined in the <code>").append(module).append("</code> module");
        }
        return buffer.append("</font></p>\n").toString();
    }
}
