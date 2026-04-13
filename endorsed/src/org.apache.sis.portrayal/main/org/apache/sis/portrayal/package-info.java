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
 * Symbology and map representations, together with a rendering engine for display.
 * This package is currently in early draft stage.
 *
 * <h2>Thread safety</h2>
 * Unless otherwise specified, classes in this package are not thread-safe.
 * A single thread should be used for interactions with all instances of
 * {@link org.apache.sis.portrayal.Canvas} that are linked together by
 * {@link org.apache.sis.portrayal.CanvasFollower} or other listeners.
 * External synchronization is generally not sufficient because listeners may create a graph of canvases,
 * and it is difficult to ensure that a lock is kept during all the graph traversal.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.7
 * @since   1.1
 */
package org.apache.sis.portrayal;
