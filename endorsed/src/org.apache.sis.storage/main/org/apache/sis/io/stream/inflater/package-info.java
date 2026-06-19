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
 * Utility classes for the implementation of data stores that need to decompress block of data.
 *
 * <STRONG>Do not use!</STRONG>
 *
 * This package is for internal use by <abbr>SIS</abbr> only.
 * Classes in this package may change in incompatible ways in any future version without notice.
 *
 * <p>More implementations of the base classes in this package are provided in other modules such as GeoTIFF.
 * Some of these implementations may move to this package in the future if there is a need to share compression
 * algorithms.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
package org.apache.sis.io.stream.inflater;
