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
import com.sun.javadoc.Doc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;


/**
 * The <code>@section</code> tag for inserting a new section in a javadoc comment.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 */
public final class Section implements Taglet {
    /**
     * Register this taglet.
     *
     * @param tagletMap the map to register this tag to.
     */
    public static void register(final Map<String,Taglet> tagletMap) {
       final Section tag = new Section();
       tagletMap.put(tag.getName(), tag);
    }

    /**
     * Constructs a default <code>@section</code> taglet.
     */
    private Section() {
        super();
    }

    /**
     * Returns the name of this custom tag.
     *
     * @return The tag name.
     */
    @Override
    public String getName() {
        return "section";
    }

    /**
     * Returns {@code false} since <code>@section</code> can not be used in overview.
     *
     * @return Always {@code false}.
     */
    @Override
    public boolean inOverview() {
        return false;
    }

    /**
     * Returns {@code true} since <code>@section</code> can be used in package documentation.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean inPackage() {
        return true;
    }

    /**
     * Returns {@code true} since <code>@section</code> can be used in type documentation
     * (classes or interfaces).
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean inType() {
        return true;
    }

    /**
     * Returns {@code true} since <code>@section</code> can be used in constructor
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean inConstructor() {
        return true;
    }

    /**
     * Returns {@code true} since <code>@section</code> can be used in method documentation.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean inMethod() {
        return true;
    }

    /**
     * Returns {@code true} since <code>@section</code> can be used in field documentation.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean inField() {
        return true;
    }

    /**
     * Returns {@code true} since <code>@section</code> is an inline tag.
     *
     * @return Always {@code true}.
     */
    @Override
    public boolean isInlineTag() {
        return true;
    }

    /**
     * Given the <code>Tag</code> representation of this custom tag, return its string representation.
     *
     * @param tag The tag to format.
     * @return A string representation of the given tag.
     */
    @Override
    public String toString(final Tag tag) {
        final Doc holder = tag.holder();
        final boolean small = holder == null || holder.isField() || holder.isMethod() || holder.isConstructor();
        final StringBuilder buffer = new StringBuilder("<p>");
        if (!small) {
            buffer.append("<br>");
        }
        buffer.append("\n<b><u>");
        if (!small) {
            buffer.append("<font size=+1>");
        }
        buffer.append(tag.text());
        if (!small) {
            buffer.append("</font>");
        }
        return buffer.append("</u></b><br>").toString();
    }

    /**
     * Given an array of {@code Tag}s representing this custom tag, return its string
     * representation. This method should not be called since arrays of inline tags do
     * not exist. However we define it as a matter of principle.
     *
     * @param tags The tags to format.
     * @return A string representation of the given tags.
     */
    @Override
    public String toString(final Tag[] tags) {
        final StringBuilder buffer = new StringBuilder();
        for (int i=0; i<tags.length; i++) {
            buffer.append(toString(tags[i]));
        }
        return buffer.toString();
    }
}
