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

import org.apache.sis.util.collection.TreeTable;


/**
 * An extension of the {@code TreeTable} interface for use with the <abbr>GUI</abbr> module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public interface TreeTableForGUI extends TreeTable {
    /**
     * Returns whether the given value produces by the given node is a title.
     * Title are a short description of the node, typically copied from one of the children.
     * For example for the code of a {@code Citation} object, this is the {@code title} property.
     *
     * <p>This information is used by <abbr>GUI</abbr> for showing the value when the node is collapsed,
     * and hiding the value when the node is expanded for avoiding redundancy with the child that really
     * provides the value.</p>
     *
     * @param  node   the node where the value come from.
     * @param  value  the value provided by the node.
     * @return whether the given value is a title.
     */
    boolean isNodeTitle(Node node, Object value);
}
