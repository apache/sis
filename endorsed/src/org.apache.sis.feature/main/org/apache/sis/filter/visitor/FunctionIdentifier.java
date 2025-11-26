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
package org.apache.sis.filter.visitor;

import java.sql.Types;


/**
 * Identification of a function. Instances of this interface are enumeration values or
 * code list values used by factories for identifying the filters or expressions to create.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public interface FunctionIdentifier {
    /**
     * Returns the name of the function.
     *
     * @return the function name.
     */
    String name();

    /**
     * Returns the types of arguments and the type of the return value of this function.
     * The {@code dataTypes[0]} array element is the data type of the function's return value.
     * All other array elements are the data types of the function's parameters, in order.
     *
     * <p>The values of all array elements are constants of the {@link Types} class.
     * Permitted values are: {@link Types#DOUBLE}, {@link Types#BOOLEAN}.</p>
     *
     * <p>This method is invoked for checking if a filter or expression in Java code can be replaced by
     * a <abbr>SQL</abbr> function. If this method returns {@code null}, then the function signature will
     * not be verified.</p>
     *
     * @todo We may change the return type to {@code int[][]} in a future version if we want
     *       to allow function overloading (same function name but with different arguments).
     *
     * @return  dataTypes  data type of the return value followed by parameters, as {@link java.sql.Types} constants.
     */
    default int[] getSignature() {
        return null;
    }
}
