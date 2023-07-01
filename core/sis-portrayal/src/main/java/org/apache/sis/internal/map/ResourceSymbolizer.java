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

import org.apache.sis.style.se1.StyleFactory;
import org.apache.sis.style.se1.Symbolizer;


/**
 * Resource symbolizers act on a resource as a whole, not on individual features.
 * Such symbolizers are not defined by the Symbology Encoding specification but are
 * often required to produce uncommon presentations.
 *
 * <p>
 * NOTE: this class is a first draft subject to modifications.
 * </p>
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
public abstract class ResourceSymbolizer<R> extends Symbolizer<R> {
    /**
     * Constructs a new symbolozer.
     *
     * @param  context  context (features or coverages) in which this style element will be used.
     */
    public ResourceSymbolizer(final StyleFactory<R> context) {
        super(context);
    }
}
