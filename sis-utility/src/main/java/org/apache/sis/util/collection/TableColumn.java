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
package org.apache.sis.util.collection;

import org.opengis.util.InternationalString;


/**
 * Identifies a column in {@link TreeTable.Node} instances.
 * Each {@code TableColumn} instance contains the column header and the type of values
 * for a particular column. {@code TableColumn}s are used for fetching values from nodes
 * as in the following example:
 *
 * {@preformat java
 *     class CityLocation {
 *         public static final TableColumn<String> CITY_NAME  = new MyColumn<>(String.class);
 *         public static final TableColumn<Float>  LATITUDE   = new MyColumn<>(Float .class);
 *         public static final TableColumn<Float>  LONGTITUDE = new MyColumn<>(Float .class);
 *
 *         private String city;
 *         private float  latitude;
 *         private float  longitude;
 *
 *         CityLocation(TreeTable.Node myNode) {
 *             city      = myNode.getValue(CITY_NAME);
 *             latitude  = myNode.getValue(LATITUDE );
 *             longitude = myNode.getValue(LONGITUDE);
 *         }
 *     }
 * }
 *
 * @param <V> Base type of all values in the column identified by this instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public interface TableColumn<V> extends CheckedContainer<V> {
    /**
     * Returns the text to display as column header.
     *
     * @return The text to display as column header.
     */
    InternationalString getHeader();

    /**
     * Returns the base type of all values in any column identified by this {@code TableColumn}
     * instance.
     */
    @Override
    Class<V> getElementType();
}
