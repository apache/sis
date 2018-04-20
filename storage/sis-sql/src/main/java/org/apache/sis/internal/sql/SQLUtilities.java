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
package org.apache.sis.internal.sql;

import java.util.Iterator;
import org.apache.sis.util.CharSequences;


/**
 * Miscellaneous utility methods.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class SQLUtilities {
    /**
     * Do not allow instantiation of this class.
     */
    private SQLUtilities(){
    }

    /**
     * Returns a graphical representation of the specified objects. This representation can be
     * printed to the {@linkplain System#out standard output stream} (for example) if it uses
     * a monospaced font and supports Unicode.
     *
     * @param  root     the root name of the tree to format, or {@code null} if none.
     * @param  objects  the objects to format as root children, or {@code null} if none.
     * @return a string representation of the tree.
     */
    public static String toTreeString(String root, final Iterable<?> objects) {
        final StringBuilder sb = new StringBuilder(100);
        if (root != null) {
            sb.append(root);
        }
        if (objects != null) {
            final String lineSeparator = System.lineSeparator();
            final Iterator<?> it = objects.iterator();
            boolean hasNext;
            if (it.hasNext()) do {
                sb.append(lineSeparator);
                final Object next = it.next();
                hasNext = it.hasNext();
                sb.append(hasNext ? "├─ " : "└─ ");

                final CharSequence[] parts = CharSequences.splitOnEOL(String.valueOf(next));
                sb.append(parts[0]);
                for (int k=1; k < parts.length; k++) {
                    sb.append(lineSeparator)
                      .append(hasNext ? '│' : ' ')
                      .append("  ")
                      .append(parts[k]);
                }
            } while (hasNext);
        }
        return sb.toString();
    }
}
