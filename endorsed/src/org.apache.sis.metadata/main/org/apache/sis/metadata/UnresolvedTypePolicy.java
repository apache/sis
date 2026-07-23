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
package org.apache.sis.metadata;


/**
 * Controls the behavior when a metadata type cannot be resolved.
 * {@link MetadataStandard} may throw an exception, return {@code null}
 * or return an arbitrary instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum UnresolvedTypePolicy {
    /**
     * Returns {@code null} if the metadata type cannot be identified or is ambiguous.
     */
    NULL,

    /**
     * Returns an arbitrary value if the metadata type is ambiguous.
     */
    ANY,

    /**
     * Throw an exception if the metadata type cannot be identified or is ambiguous.
     */
    THROW;

    /**
     * Returns a variant of this policy that does not throw exception.
     */
    UnresolvedTypePolicy lenient() {
        return (this != THROW) ? this : NULL;
    }
}
