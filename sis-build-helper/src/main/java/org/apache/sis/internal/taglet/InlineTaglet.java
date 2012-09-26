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

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;


/**
 * Base class of inline taglets.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 */
abstract class InlineTaglet implements Taglet {
    /**
     * Constructs a default inline taglet.
     */
    InlineTaglet() {
    }

    /**
     * Returns {@code true} since SIS taglets can be used in overview.
     *
     * @return Default to {@code true}.
     */
    @Override
    public boolean inOverview() {
        return true;
    }

    /**
     * Returns {@code true} since SIS taglets can be used in package documentation.
     *
     * @return Default to {@code true}.
     */
    @Override
    public boolean inPackage() {
        return true;
    }

    /**
     * Returns {@code true} since SIS taglets can be used in type documentation
     * (classes or interfaces).
     *
     * @return Default to {@code true}.
     */
    @Override
    public boolean inType() {
        return true;
    }

    /**
     * Returns {@code true} since SIS taglets can be used in constructor
     *
     * @return Default to {@code true}.
     */
    @Override
    public boolean inConstructor() {
        return true;
    }

    /**
     * Returns {@code true} since SIS taglets can be used in method documentation.
     *
     * @return Default to {@code true}.
     */
    @Override
    public boolean inMethod() {
        return true;
    }

    /**
     * Returns {@code true} since SIS taglets can be used in field documentation.
     *
     * @return Default to {@code true}.
     */
    @Override
    public boolean inField() {
        return true;
    }

    /**
     * Returns {@code true} since this base class is all about inline tags.
     *
     * @return Always {@code true}.
     */
    @Override
    public final boolean isInlineTag() {
        return true;
    }

    /**
     * Given an array of {@code Tag}s representing this custom tag, return its string
     * representation. This method should not be called since arrays of inline tags do
     * not exist. However we define it as a matter of principle.
     *
     * @param  tags The tags to format.
     * @return A string representation of the given tags.
     */
    @Override
    public final String toString(final Tag[] tags) {
        final StringBuilder buffer = new StringBuilder(64);
        for (int i=0; i<tags.length; i++) {
            buffer.append(toString(tags[i]));
        }
        return buffer.toString();
    }
}
