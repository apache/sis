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
package org.apache.sis.internal.feature;

import java.util.Set;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface FunctionRegister {
    /**
     *
     * Note : should it be the starting element of all function names to avoid any conflict ?
     *
     * @return factory unique name.
     */
    String getIdentifier();

    /**
     * Note : should we use GenericName ?
     *
     * @return set of supported function names.
     */
    Set<String> getNames();

    /**
     * Create a new function with given parameters.
     *
     * @param name function name, not null
     * @param parameters function parameters
     * @return
     * @throws IllegalArgumentException if function name is unknown or parameters do not match
     */
    Function create(String name, Expression... parameters) throws IllegalArgumentException;
}
