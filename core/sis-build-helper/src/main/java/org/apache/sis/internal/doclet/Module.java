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

import java.util.Set;
import java.util.EnumSet;
import java.nio.file.Path;
import com.sun.source.doctree.DocTree;


/**
 * The <code>@module</code> tag. This tag expects no argument.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final class Module extends Taglet {
    /**
     * Constructs a <code>@module</code> taglet.
     */
    public Module() {
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
    public Set<Location> getAllowedLocations() {
        return EnumSet.of(Location.PACKAGE, Location.TYPE);
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
     * Given a {@code DocTree}s representing this custom tag, appends its string representation.
     *
     * @param  tag     the tag to format.
     * @param  buffer  the buffer where to format the tag.
     */
    @Override
    protected void format(final DocTree tag, final StringBuilder buffer) {
        buffer.append("\n<p><font size=\"-1\">");
        Path file = getCurrentFile();
        if (file != null) {
            String module = file.getFileName().toString();
            while ((file = file.getParent()) != null) {
                if (file.getFileName().toString().equals("src")) {
                    file = file.getParent();
                    if (file != null) {
                        module = file.getFileName().toString();
                    }
                    break;
                }
            }
            /*
             * Appends the module link.
             */
            buffer.append("Defined in the <code>").append(module).append("</code> module").append("</font></p>\n");
        }
    }
}
