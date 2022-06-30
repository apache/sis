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
 * {@link org.apache.sis.storage.DataStore} implementation for XML files that can be (un)marshalled by the
 * {@link org.apache.sis.xml.XML} class. The kinds of objects recognized by this package is listed in the
 * {@link org.apache.sis.internal.storage.xml.Store} class.
 *
 * <p>This base package is designed for use with JAXB, which allows this package to be very small since most of the
 * XML (un)marshalling rules are specified in external classes designed for use with JAXB. However some classes can
 * also be used in other contexts. In particular, the {@link org.apache.sis.internal.storage.xml.stream} package in
 * the {@code sis-xmlstore} module extends this package with classes designed for use with StAX cursor API.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.4
 * @module
 */
package org.apache.sis.internal.storage.xml;
