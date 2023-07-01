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
 * Symbology for styling map data independently of their source.
 * This is a placeholder for future evolution.
 * The following works in progress can be sources for the definition of a future style API:
 *
 * <ul>
 *   <li>ISO 19117:2012 — Portrayal (under review as of June 2023)</li>
 *   <li>OGC Styles and Symbology Encoding standard working group</li>
 *   <li>OGC 3D Portrayal standard working group</li>
 *   <li>OGC Portrayal discussion working group</li>
 *   <li>OGC API — Styles standard working group</li>
 * </ul>
 *
 * As of June 2023 we have not yet determined how to consolidate above works in a Java API.
 * The {@link org.apache.sis.style.se1} package, which is derived from SE 1.1 standard,
 * is used an an interim API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
package org.apache.sis.style;
