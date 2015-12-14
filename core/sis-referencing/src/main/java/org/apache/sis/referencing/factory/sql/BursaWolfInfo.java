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
package org.apache.sis.referencing.factory.sql;


/**
 * Private structure for {@link EPSGFactory#createBursaWolfParameters} usage.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class BursaWolfInfo {
    /**
     * The value of {@code CO.COORD_OP_CODE}.
     */
    final int operation;

    /**
     * The value of {@code CO.COORD_OP_METHOD_CODE}.
     */
    final int method;

    /**
     * The value of {@code CRS1.DATUM_CODE}.
     */
    final String target;

    /**
     * Fills a structure with the specified values.
     */
    BursaWolfInfo(final int operation, final int method, final String target) {
        this.operation = operation;
        this.method    = method;
        this.target    = target;
    }

    /**
     * MUST returns the operation code. This is required by {@link EPSGFactory#sort(Object[])}.
     */
    @Override
    public String toString() {
        return String.valueOf(operation);
    }
}
