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

import java.net.URI;
import java.nio.file.AccessDeniedException;


/**
 * Indication of whether access to a file or <abbr>URL</abbr> is granted or denied.
 * A file may be specified in a {@code xlink:href} attribute of an <abbr>XML</abbr> document,
 * or as a parameter in the definition of a coordinate operation (e.g. a datum shift grid file).
 * By default, Apache <abbr>SIS</abbr> opens these files only if they are relative to a directory
 * inferred by the context (for example {@code $SIS_DATA/DatumChanges} for datum shift grid files),
 * of if they are in the same directory or a sub-directory of the document referencing the file,
 * This enumeration can be used for replacing the default policy by an user-specified policy.
 * See the following methods for more information:
 *
 * <ul>
 *   <li>{@link org.apache.sis.xml.ReferenceResolver#accessControl(URI)} —
 *       for {@code xlink:href} attributes in <abbr>GML</abbr> document,</li>
 *   <li>{@link org.apache.sis.referencing.operation.transform.MathTransformBuilder#getAccessControl()} —
 *       for datum shift grids or other files used by coordinate operations.</li>
 * </ul>
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
     * The access is granted if the reference is an <abbr>URL</abbr> local to the <abbr>JSON</abbr> or
     * <abbr>GML</abbr> document, or if the <abbr>URL</abbr> is a file in the same directory or in a
     * sub-directory of the <abbr>JSON</abbr>, <abbr>GML</abbr>, <abbr>WKT</abbr>, <abbr>netCDF</abbr>,
     * <i>etc.</i> document referencing the file.
     * In addition, some other special directories are accepted depending on the context:
     *
     * <ul>
     *   <li>For coordinate operations using datum shift grids,
     *       grant access to files inside the {@code $SIS_DATA/DatumChanges} directory.</li>
     * </ul>
     */
    DEFAULT
}
