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
 * Functions that associates positions within a bounded space (its domain) to values (its range).
 * This package makes no assumption on the domain geometry; it is not necessarily a grid.
 * For the most common case — a coverage backed by a regular grid (also called "raster") — see
 * {@link org.apache.sis.coverage.grid}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.0
 */
package org.apache.sis.coverage;
