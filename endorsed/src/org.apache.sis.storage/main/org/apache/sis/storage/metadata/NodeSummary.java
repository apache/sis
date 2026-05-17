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
package org.apache.sis.storage.metadata;

import org.apache.sis.util.SimpleInternationalString;


/**
 * Value or a {@code TreeTable.Node} which is used only for summarizing the children of the node.
 * Since this text is redundant with the children, a <abbr>GUI</abbr> can show this text when the
 * node is collapsed and hide this text when the node is expanded.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.metadata.TitleProperty
 */
public final class NodeSummary extends SimpleInternationalString {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5768405462866237705L;

    /**
     * Creates a new instance from the given string.
     *
     * @param text the string for all locales.
     */
    private NodeSummary(final String text) {
        super(text);
    }

    /**
     * Returns the given text as a {@code NodeSummary} instance.
     *
     * @param  text  the text to wrap, or {@code null}.
     * @return the wrapped text, or {@code null} if the given text was null.
     */
    public static NodeSummary of(final CharSequence text) {
        if (text == null || text instanceof NodeSummary) {
            return (NodeSummary) text;
        } else {
            return new NodeSummary(text.toString());
        }
    }
}
