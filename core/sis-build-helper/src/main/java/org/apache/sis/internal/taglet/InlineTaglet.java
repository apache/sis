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
import java.util.Set;
import java.util.EnumSet;
import java.util.List;
import javax.tools.Diagnostic;
import javax.lang.model.element.Element;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.Taglet;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownInlineTagTree;


/**
 * Base class of inline taglets.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
public abstract class InlineTaglet implements Taglet {
    /**
     * Where to report warnings, or {@code null} if unknown.
     * This is initialized by {@link org.apache.sis.internal.doclet.Doclet#init Doclet.init(…)}.
     */
    public static volatile Reporter reporter;

    /**
     * Constructs a default inline taglet.
     */
    InlineTaglet() {
    }

    /**
     * Returns the set of locations in which this taglet may be used.
     * By default the taglet can be used everywhere.
     *
     * @return the set of locations in which this taglet may be used.
     */
    @Override
    public Set<Taglet.Location> getAllowedLocations() {
        return EnumSet.allOf(Taglet.Location.class);
    }

    /**
     * Returns {@code true} since this base class is all about inline tags.
     *
     * @return always {@code true}.
     */
    @Override
    public final boolean isInlineTag() {
        return true;
    }

    /**
     * Returns the text contained in the given inline tag.
     */
    static String text(final DocTree tag) {
        for (final DocTree node : ((UnknownInlineTagTree) tag).getContent()) {
            if (node.getKind() == DocTree.Kind.TEXT) {
                return ((TextTree) node).getBody().trim();
            }
        }
        return "";
    }

    /**
     * Returns the file that contains the given tag.
     */
    static File file(final DocTree tag) {
        throw new UnsupportedOperationException("We have not yet found how to get the file where a tag is contained."); // TODO
    }

    /**
     * Given a list of {@code DocTree}s representing this custom tag, returns its string representation.
     * This method will be invoked once for each instance of the tag in parsed Javadoc.
     *
     * @param  tags     the tags to format.
     * @param  element  the element to which the enclosing comment belongs.
     * @return a string representation of the given tags.
     */
    @Override
    public final String toString(final List<? extends DocTree> tags, final Element element) {
        final StringBuilder buffer = new StringBuilder(64);
        for (final DocTree tag : tags) {
            buffer.append(toString(tag));
        }
        return buffer.toString();
    }

    /**
     * Given a single {@code DocTree}s representing this custom tag, returns its string representation.
     * This method will be invoked once for each instance of the tag in parsed Javadoc.
     *
     * @param  tag  the tag to format.
     * @return a string representation of the given tag.
     */
    protected abstract String toString(DocTree tag);

    /**
     * Prints a warning message.
     */
    static void printWarning(final DocTree tag, final String message) {
        final Reporter reporter = InlineTaglet.reporter;
        if (reporter != null) {
            reporter.print(Diagnostic.Kind.WARNING, message);
        } else {
            System.err.println(message);
        }
    }

    /**
     * Prints an error message.
     */
    static void printError(final DocTree tag, final String message) {
        final Reporter reporter = InlineTaglet.reporter;
        if (reporter != null) {
            reporter.print(Diagnostic.Kind.ERROR, message);
        } else {
            System.err.println(message);
        }
    }
}
