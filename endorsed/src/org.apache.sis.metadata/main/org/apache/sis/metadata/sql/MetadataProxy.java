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
package org.apache.sis.metadata.sql;

import java.io.ObjectStreamException;
import java.io.Serializable;


/**
 * Interface for metadata that are implemented by a proxy class.
 * Instances of this interface are created by calls to {@code Proxy.newProxyInstance(…)}
 * in {@link MetadataSource#lookup(Class, String)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
interface MetadataProxy extends Serializable {
    /**
     * Returns the identifier (primary key) of this metadata if it is using the given source,
     * or {@code null} otherwise.
     */
    String identifier(MetadataSource source);

    /**
     * Copies all proxy content to a serializable implementation.
     *
     * @return a serializable object with the same content as this proxy.
     * @throws ObjectStreamException if an error occurred while copying the content.
     */
    Object writeReplace() throws ObjectStreamException;
}
