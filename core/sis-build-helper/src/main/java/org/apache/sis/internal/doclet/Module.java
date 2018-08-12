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
package org.apache.sis.internal.doclet;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import jdk.javadoc.doclet.Taglet;
import com.sun.source.doctree.DocTree;
import javax.lang.model.element.Element;


/**
 * The <code>@module</code> tag. This tag expects no argument.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final class Module implements Taglet {
    /**
     * The SIS module in which the <code>@module</code> taglet has been found.
     */
    private String module;

    /**
     * Constructs a <code>@module</code> taglet.
     */
    public Module() {
        super();
    }

    /**
     * Returns the name of this custom tag.
     *
     * @return "module".
     */
    @Override
    public String getName() {
        return "module";
    }

    /**
     * Returns the set of locations in which this taglet may be used.
     *
     * @return the set of locations in which this taglet may be used.
     */
    @Override
    public Set<Taglet.Location> getAllowedLocations() {
        return EnumSet.of(Taglet.Location.PACKAGE,
                          Taglet.Location.TYPE);
    }

    /**
     * Returns {@code false} since <code>@module</code> is not an inline tag.
     *
     * @return always {@code false}.
     */
    @Override
    public boolean isInlineTag() {
        return false;
    }

    /**
     * Given a list of {@code DocTree}s representing this custom tag, returns its string representation.
     *
     * @param  tags     the tags to format.
     * @param  element  the element to which the enclosing comment belongs.
     * @return a string representation of the given tags.
     */
    @Override
    public String toString(final List<? extends DocTree> tags, final Element element) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        final StringBuilder buffer = new StringBuilder(128);
        buffer.append("\n<p><font size=\"-1\">");
        for (final DocTree tag : tags) {
            File file = InlineTaglet.file(tag);
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
