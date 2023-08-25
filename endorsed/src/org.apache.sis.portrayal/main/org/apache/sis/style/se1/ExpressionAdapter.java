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
package org.apache.sis.style.se1;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

// Specific to the main branch:
import org.apache.sis.filter.Expression;


/**
 * Adapter for expression in style.
 * This is a place-holder for future work.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
final class ExpressionAdapter<R> extends XmlAdapter<String, Expression<R,?>> {
    /**
     * Creates an adapter.
     */
    public ExpressionAdapter() {
    }

    @Override
    public String marshal(Expression<R,?> value) {
        return null;
    }

    @Override
    public Expression<R,?> unmarshal(String value) {
        return null;
    }
}
