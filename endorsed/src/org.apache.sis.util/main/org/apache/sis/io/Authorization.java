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
package org.apache.sis.io;

import java.nio.file.AccessDeniedException;


/**
 * Indication of whether access to a file or <abbr>URL</abbr> is granted or denied.
 * A file may be specified in a {@code xlink:href} attribute of an <abbr>XML</abbr> document,
 * or as a parameter in the definition of a coordinate operation (e.g. a datum shift grid file).
 * By default, Apache <abbr>SIS</abbr> opens these files only if they are in dedicated directories.
 * This enumeration is used when the default behavior is replaced by user policy.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 * @since   1.7
 */
public enum Authorization {
    /**
     * Access to the file or <abbr>URL</abbr> is authorized.
     * The file may be opened and its content read.
     */
    GRANTED,

    /**
     * Access to the file or <abbr>URL</abbr> is denied.
     * It may result in an {@link AccessDeniedException} to be thrown.
     */
    DENIED,

    /**
     * Access to the file or <abbr>URL</abbr> is determined by Apache <abbr>SIS</abbr> default policy.
     * These defaults depend on the type of document containing references by <abbr>URL</abbr>s.
     * Examples:
     *
     * <ul>
     *   <li>In a <abbr>GML</abbr> document, follow {@code xlink:href} only if the reference is local to the document.</li>
     *   <li>In coordinate operations defined in <abbr>JSON</abbr>, <abbr>GML</abbr> or <abbr>WKT</abbr> documents,
     *       read datum shift grid file only if inside the {@code $SIS_DATA/DatumChanges} directory or in the same
     *       directory or server as the document.</li>
     * </ul>
     */
    DEFAULT
}
