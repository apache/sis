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
package org.apache.sis.metadata;

import java.util.Locale;
import java.util.TimeZone;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTableFormat;
import org.apache.sis.internal.system.LocalizedStaticObject;
import org.apache.sis.io.TableAppender;


/**
 * Default format for {@link AbstractMetadata} objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@SuppressWarnings({"CloneableImplementsClone", "serial"})       // Not intended to be cloned or serialized.
final class MetadataFormat extends TreeTableFormat {
    /**
     * The shared instance to use for the {@link TreeTableView#toString()} method implementation.
     * Would need to be reset to {@code null} on locale or timezone changes, but we do not yet have
     * any listener for such information.
     */
    @LocalizedStaticObject
    static final MetadataFormat INSTANCE = new MetadataFormat();

    /**
     * Creates a new format.
     */
    private MetadataFormat() {
        super(Locale.getDefault(Locale.Category.FORMAT), TimeZone.getDefault());
        setColumns(TableColumn.NAME, TableColumn.VALUE, TableColumn.REMARKS);
    }

    /**
     * Override the default behavior for <strong>not</strong> moving to next column before writing remarks.
     * Doing so put too many spaces for large metadata tree. Instead we add spaces in the current column.
     */
    @Override
    protected void writeColumnSeparator(final int nextColumn, final TableAppender out) {
        if (nextColumn == 1) {
            super.writeColumnSeparator(nextColumn, out);
        } else {
            out.append("    ! ");
        }
    }
}
