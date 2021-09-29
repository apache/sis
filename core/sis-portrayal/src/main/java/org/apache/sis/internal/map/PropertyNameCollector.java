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
package org.apache.sis.internal.map;

import java.util.Set;
import java.util.HashSet;
import org.opengis.filter.ValueReference;


/**
 * Collects all properties used in style elements.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class PropertyNameCollector extends SymbologyVisitor {
    /**
     * All value references found.
     *
     * @see ValueReference#getXPath()
     */
    final Set<String> references;

    /**
     * Creates a new collector.
     */
    PropertyNameCollector() {
        references = new HashSet<>();
    }

    /**
     * Invoked for each value reference found.
     */
    @Override
    protected void visitProperty(final ValueReference<?,?> expression) {
        if (expression != null) {
            references.add(expression.getXPath());
        }
    }
}
