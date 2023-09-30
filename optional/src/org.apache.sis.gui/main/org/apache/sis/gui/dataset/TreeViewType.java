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
package org.apache.sis.gui.dataset;

import org.apache.sis.storage.folder.UnstructuredAggregate;


/**
 * The different views (aggregation, etc.) which may be associated to a resource item.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum TreeViewType {
    /**
     * The original resource. Associated value shall never be {@code null}.
     */
    SOURCE,

    /**
     * The result of {@link UnstructuredAggregate#getStructuredView()}.
     */
    AGGREGATION
}
