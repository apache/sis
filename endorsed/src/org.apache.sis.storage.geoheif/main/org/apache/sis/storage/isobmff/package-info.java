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
 * Base classes for <abbr>ISO</abbr> base media file format (<abbr>ISOBMFF</abbr>).
 * This format is defined by a series of <abbr>ISO</abbr> 14496 standards divided in many parts.
 * Each part is defined in a sub-package, with {@code base} as the main sub-package.
 *
 * <h2>Basic file structure</h2>
 * A file is made of boxes, which are object-oriented building blocks defined by unique type identifiers.
 * All data is contained in boxes, there is no other data within the file. Box of unknown type should be ignored.
 * Some boxes contain index values, with index numbering starting at 1. Some boxes contain a version number,
 * with version 0 meaning that fields are 32-bits integers and version 1 meaning that fields are 64-bits integers.
 * Byte order is fixed to big-endian.
 *
 * <h2>References</h2>
 * The standards are divided in series, each series having many parts:
 * <ul>
 *   <li>ISO/IEC 14496 — Coding of audio-visual objects<ul>
 *     <li><a href="https://www.iso.org/standard/83102.html">Part 12: ISO base media file format</a></li>
 *     <li><a href="https://www.iso.org/standard/83529.html">Part 10: Advanced video coding</a></li>
 *   </ul></li>
 *   <li>ISO/IEC 23008 — High efficiency coding and media delivery in heterogeneous environments<ul>
 *     <li><a href="https://www.iso.org/standard/83650.html">Part 12: Image File Format</a></li>
 *   </ul></li>
 *   <li>ISO/IEC 23001 — MPEG systems technologies<ul>
 *     <li><a href="https://www.iso.org/standard/82528.html">Part 17: Carriage of uncompressed video and images in ISO base media file format</a></li>
 *   </ul></li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
package org.apache.sis.storage.isobmff;
