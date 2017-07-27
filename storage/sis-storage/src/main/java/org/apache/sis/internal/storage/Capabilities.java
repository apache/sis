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
package org.apache.sis.internal.storage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Capabilities of a {@link org.apache.sis.storage.DataStore} or similar objects.
 * Some data stores can only read data while other can read and write.
 *
 * <p>This is not a committed API since the way to represent data store capabilities is likely to change.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Capabilities {
    /**
     * Indicates whether the data store created by the {@code open(…)} method can read and/or write data.
     *
     * @return information about whether the data store implementation can read and/or write data.
     */
    Capability[] value();
}
