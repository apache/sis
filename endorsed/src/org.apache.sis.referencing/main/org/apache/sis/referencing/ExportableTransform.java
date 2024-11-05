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
package org.apache.sis.referencing;

/**
 * Experimental API to export transforms to other syntaxes.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface ExportableTransform {

    /**
     * The produced code should transform one value only.
     * Input array is expected to be named src.
     * Ouput array is expected to be named dat.
     *
     * Example of expected produced code :
     * <pre>
     * {@code
     * {
     *   _CONSTANT_1: 123.456,
     *   _CONSTANT_2: 234.567,
     *
     *   transform(src) {
     *     //complex math using _CONSTANT_1 and _internalFunction1
     *     return dst;
     *   },
     *
     *   _internalFunction1(val1, val2) {
     *     //does something
     *   }
     * }
     * }
     * </pre>
     *
     *
     * @return ECMAScript code
     * @throws UnsupportedOperationException if export is not possible or not available
     */
    String toECMAScript() throws UnsupportedOperationException;
}
