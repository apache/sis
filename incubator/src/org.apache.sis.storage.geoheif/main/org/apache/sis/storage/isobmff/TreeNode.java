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
package org.apache.sis.storage.isobmff;

import java.util.Iterator;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TreeNode {

    private TreeNode() {
    }

    /**
     * Returns a graphical representation of the specified objects. This representation can be
     * printed to the {@linkplain System#out standard output stream} (for example) if it uses
     * a monospaced font and supports unicode.
     *
     * @param  root  The root name of the tree to format.
     * @param  objects The objects to format as root children.
     * @return A string representation of the tree.
     */
    public static String toStringTree(String root, final Iterable<?> objects) {
        final StringBuilder sb = new StringBuilder();
        if (root != null) {
            sb.append(root);
        }
        if (objects != null) {
            final Iterator<?> ite = objects.iterator();
            while (ite.hasNext()) {
                sb.append('\n');
                final Object next = ite.next();
                final boolean last = !ite.hasNext();
                sb.append(last ? "\u2514\u2500 " : "\u251C\u2500 ");

                final String[] parts = String.valueOf(next).split("\n");
                sb.append(parts[0]);
                for (int k=1;k<parts.length;k++) {
                    sb.append('\n');
                    sb.append(last ? ' ' : '\u2502');
                    sb.append("  ");
                    sb.append(parts[k]);
                }
            }
        }
        return sb.toString();
    }
}
