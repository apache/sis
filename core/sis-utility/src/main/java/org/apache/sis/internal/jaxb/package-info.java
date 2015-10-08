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

/**
 * A set of helper classes for JAXB handling in the SIS implementation.
 *
 * <strong>Do not use!</strong>
 *
 * This package is for internal use by SIS only. Classes in this package
 * may change in incompatible ways in any future version without notice.
 *
 * <div class="section">Main content</div>
 * {@link org.apache.sis.internal.jaxb.IdentifierMapAdapter} is our internal implementation of
 * the public {@link org.apache.sis.xml.IdentifierMap} interface. The actual implementation is
 * usually the {@code ModifiableIdentifierMap} subclass.
 *
 * <p>{@link org.apache.sis.internal.jaxb.SpecializedIdentifier} wraps {@link org.apache.sis.xml.XLink},
 * {@link java.net.URI} and {@link java.util.UUID} as {@link org.opengis.metadata.Identifier} instances.
 * This is used for storing the value in a list of identifiers while preserving the original object.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
package org.apache.sis.internal.jaxb;
