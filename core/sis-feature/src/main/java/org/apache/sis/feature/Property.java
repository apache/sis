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
package org.apache.sis.feature;

import org.opengis.util.GenericName;


/**
 * Place-holder for an interface not available in GeoAPI 3.0.
 * This place-holder will be removed after we upgrade to a later GeoAPI version.
 *
 * <p><strong>Do not put this type in public API</strong>. We need to prevent users from using
 * this type in order to reduce compatibility breaks when we will upgrade the GeoAPI version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
abstract class Property {
    public abstract GenericName getName();

    public abstract Object getValue();
}
