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

import java.io.*;
import java.util.Map;
import com.sun.javadoc.Tag;
import com.sun.javadoc.SourcePosition;
import com.sun.tools.doclets.Taglet;


/**
 * The <code>@include</code> tag for inserting a HTML fragment defined in an external file.
 * This tag is used mostly for formulas written in MathML. Those formulas are provided in
 * separated files for easier edition and for avoiding errors with compilers that verify the
 * HTML tags (e.g. JDK 8 javac). Fragment of the external files are inserted in the Javadoc
 * by the following tag:
 *
 * <blockquote><pre>{@include formulas.html#<var>title</var>}</pre></blockquote>
 *
 * where <var>title</var> is the text inside <code>&lt;h2&gt;…&lt;/h2&gt;</code> elements
 * just before the parts to copy in the javadoc.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class Include extends InlineTaglet {
    /**
     * The beginning and the end of the anchor texts. Must be on the same line.
     */
    private static final String ANCHOR_START = "<h2>", ANCHOR_END = "</h2>";

    /**
     * End of document.
     */
    private static final String DOCUMENT_END = "</body>";

    /**
     * Register this taglet.
     *
     * @param tagletMap the map to register this tag to.
     */
    public static void register(final Map<String,Taglet> tagletMap) {
       final Include tag = new Include();
       tagletMap.put(tag.getName(), tag);
    }

    /**
     * Constructs a default <code>@include</code> taglet.
     */
    private Include() {
        super();
    }

    /**
     * Returns the name of this custom tag.
     *
     * @return The tag name.
     */
    @Override
    public String getName() {
        return "include";
    }

    /**
     * Given the <code>Tag</code> representation of this custom tag, return its string representation.
     *
     * @param tag The tag to format.
     * @return A string representation of the given tag.
     */
    @Override
    public String toString(final Tag tag) {
        final SourcePosition position = tag.position();
        final String reference = tag.text();
        final int sep = reference.indexOf('#');
        if (sep < 0) {
            printWarning(position, "@include expected a reference like \"filename#anchor\" but got \"" + reference + "\".");
            return reference;
        }
        File file = position.file();
        file = new File(file.getParentFile(), reference.substring(0, sep));
        final String anchor = reference.substring(sep + 1);
        final StringBuilder buffer = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            /*
             * Search the anchor.
             */
            String line;
            int start, end;
            do if ((line = in.readLine()) == null) {
                printWarning(position, "Header \"" + anchor + "\" not found in file " + file);
                return reference;
            }
            while ((start = line.indexOf(ANCHOR_START)) < 0 ||
                     (end = line.lastIndexOf(ANCHOR_END)) < start ||
                     !line.substring(start + ANCHOR_START.length(), end).trim().equals(anchor));
            /*
             * At this point, the anchor has been found. Now copy everything up to the next anchor,
             * or to the </body> HTML tag.
             */
            while (true) {
                line = in.readLine();
                if (line == null) {
                    printWarning(position, "Unexpected end of file in " + file);
                    return reference;
                }
                start = line.indexOf(ANCHOR_START);
                if (start >= 0 && line.lastIndexOf(ANCHOR_END) >= start) {
                    break; // Found the next section - stop this one.
                }
                if (line.contains(DOCUMENT_END)) {
                    break;
                }
                buffer.append(line).append('\n');
            }
        } catch (IOException e) {
            printError(position, "Error reading " + file + ":\n" + e);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                printError(position, "Error reading " + file + ":\n" + e);
            }
        }
        return buffer.toString();
    }
}
