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
package org.apache.sis.image.internal.shared;

import java.awt.Rectangle;
import org.apache.sis.io.TableAppender;


/**
 * Helper methods for producing messages in assertion failures.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AssertionMessages {
    /**
     * Do not allow instantiation of this class.
     */
    private AssertionMessages() {
    }

    /**
     * Produces an error message for a rectangle which was expected to be inside another rectangle, but is not.
     *
     * @param  outer  the outer rectangle.
     * @param  inner  the inner rectangle.
     * @return message to use as the assertion failure message.
     */
    public static String notContained(final Rectangle outer, final Rectangle inner) {
        final var message = new StringBuilder();
        if (outer.intersects(inner)) {
            if (inner.width > outer.width || inner.height > outer.height) {
                message.append("Subset is larger than the bounds. ");
            } else if (inner.x < outer.x || inner.y < outer.y) {
                message.append("Subset starts before the bounds. ");
            } else {
                message.append("Subset finishes after the bounds. ");
            }
        } else {
            message.append("Subset is fully outside the bounds. ");
        }
        message.append("Rectangles are:").append(System.lineSeparator());
        final var table = new TableAppender(" ");
        append(table, "outer", outer);
        append(table, "inner", inner);
        return message.append(table).toString();
    }

    /**
     * Appends the coordinates of the given rectangle in the given table.
     *
     * @param table   where to format the rectangle.
     * @param header  header to write in the left column.
     * @param bounds  rectangle to format.
     */
    private static void append(final TableAppender table, final String header, final Rectangle bounds) {
        table.append(header).append(':').nextColumn();
        for (int i=0; ; i++) {
            final char c;
            final int v;
            switch (i) {
                case 0: c='x'; v=bounds.x;      break;
                case 1: c='y'; v=bounds.y;      break;
                case 2: c='w'; v=bounds.width;  break;
                case 3: c='h'; v=bounds.height; break;
                default: table.nextLine(); return;
            }
            table.append(c).append('=').nextColumn();
            table.setCellAlignment(TableAppender.ALIGN_RIGHT);
            table.append(Integer.toString(v)).nextColumn();
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
        }
    }
}
