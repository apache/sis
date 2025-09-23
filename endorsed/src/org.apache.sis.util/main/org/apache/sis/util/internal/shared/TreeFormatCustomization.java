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
package org.apache.sis.util.internal.shared;

import java.util.function.Predicate;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TreeTableFormat;


/**
 * Customization of {@link TreeTable} formatting on a per-instance basis. Methods in this interface
 * are invoked by {@link TreeTableFormat#format(TreeTable, Appendable)} before to format the tree.
 * Non-null return values are merged with the {@code TreeTableFormat} configuration.
 *
 * <h2>Design note</h2>
 * methods in this class are invoked for configuring the formatter before to write the tree.
 * We do not use this interface as callbacks invoked for individual rows during formatting.
 * The reason is that functions provided by this interface may need to manage a state
 * (for example {@linkplain #filter() filtering} may depend on previous rows) but we do not want
 * to force implementations to store such state in {@code TreeFormatCustomization} instances
 * since objects implementing this interface may be immutable.
 *
 * <p>This class is not yet in public API. We are waiting for more experience before to decide if it should be
 * committed API.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public interface TreeFormatCustomization {
    /**
     * Returns the tree node filter to use when formatting instances of the {@code TreeTable}.
     * If non-null, then the filter is combined with {@link TreeTableFormat#getNodeFilter()}
     * by a "and" operation.
     *
     * @return the tree node filter to use for the {@code TreeTable} instance being formatted.
     */
    Predicate<TreeTable.Node> filter();
}
