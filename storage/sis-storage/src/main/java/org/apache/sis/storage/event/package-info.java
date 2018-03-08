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
 * Provides interfaces and classes for dealing with different types of events fired by resources.
 * The different types of events are differentiated by the {@link ChangeEvent} subclasses.
 * There is different subclasses for structural changes or changes in resource content.
 * It is possible to register a listener for only some specific types of events.
 *
 * <p>Note that warnings that may occur during exploitation of a resource are handled by an interface
 * defined in another package, {@link org.apache.sis.util.logging.WarningListener}.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @since   1.0
 * @version 1.0
 * @module
 */
package org.apache.sis.storage.event;
