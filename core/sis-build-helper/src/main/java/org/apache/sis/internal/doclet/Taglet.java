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
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.tools.Diagnostic;
import javax.lang.model.element.Element;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.Doclet;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import jdk.javadoc.doclet.StandardDoclet;


/**
 * Base class of all taglets implemented in this package.
 * Taglets are assumed inline by default.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.3
 * @module
 */
abstract class Taglet implements jdk.javadoc.doclet.Taglet {
    /**
     * Where to report warnings, or {@code null} if unknown.
     */
    private Reporter reporter;

    /**
     * Utility methods for locating the path of elements, or {@code null} if unknown.
     */
    private DocTrees trees;

    /**
     * The current element, or {@code null} if none.
     */
    private Element element;

    /**
     * Constructs a default inline taglet.
     */
    Taglet() {
    }

    /**
     * Initializes this taglet with the given doclet environment and doclet.
     *
     * @param env     the environment in which the taglet is running.
     * @param doclet  the doclet that instantiated this taglet.
     */
    @Override
    public void init(final DocletEnvironment env, final Doclet doclet) {
        reporter = ((StandardDoclet) doclet).getReporter();
        trees = env.getDocTrees();
    }

    /**
     * Returns the set of locations in which this taglet may be used.
     * By default the taglet can be used everywhere.
     *
     * @return the set of locations in which this taglet may be used.
     */
    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.allOf(Location.class);
    }

    /**
     * Returns {@code true} by default since this base class is about inline tags.
     *
     * @return {@code true} if this tagle is an inline taglet.
     */
    @Override
    public boolean isInlineTag() {
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
     * Returns the file that contains the current tag, or {@code null} if the method
     * can not determine the file.
     *
     * @return file containing the current tag, or {@code null}.
     */
    protected final Path getCurrentFile() {
        if (trees != null && element != null) {
            final TreePath path = trees.getPath(element);
            if (path != null) {
                // Following methods do not document 'null' as a possible return value.
                return Paths.get(path.getCompilationUnit().getSourceFile().toUri());
            }
        }
        return null;
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
        try {
            this.element = element;
            for (final DocTree tag : tags) {
                format(tag, buffer);
            }
        } finally {
            this.element = null;
        }
        return buffer.toString();
    }

    /**
     * Given a single {@code DocTree}s representing this custom tag, returns its string representation.
     * This method will be invoked once for each instance of the tag in parsed Javadoc.
     *
     * @param  tag       the tag to format.
     * @param  appendTo  the buffer where to format the tag.
     */
    protected abstract void format(DocTree tag, StringBuilder appendTo);

    /**
     * Prints a warning message for the current tag.
     *
     * @param message  the warning message to print.
     */
    protected final void printWarning(final String message) {
        print(Diagnostic.Kind.WARNING, message);
    }

    /**
     * Prints an error message for the current tag.
     *
     * @param message  the error message to print.
     */
    protected final void printError(final String message) {
        print(Diagnostic.Kind.ERROR, message);
    }

    /**
     * Prints an error or warning message.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void print(final Diagnostic.Kind kind, final String message) {
        if (reporter != null) {
            if (element != null) {
                reporter.print(kind, element, message);
            } else {
                reporter.print(kind, message);
            }
        } else {
            System.err.println(message);
        }
    }
}
