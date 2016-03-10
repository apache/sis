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
 * {@link org.apache.sis.storage.DataStore} implementation for Coma Separated Values (CSV) files.
 * This package implements the following specifications (more may be added in any future version):
 *
 * <ul>
 *   <li><a href="http://docs.opengeospatial.org/is/14-084r2/14-084r2.html">OGC® Moving Features Encoding Extension:
 *     Simple Comma Separated Values (CSV)</a> with some rules relaxed. The Apache SIS implementation allows the CSV
 *     file to have no date, thus allowing use of OGC 14-084 syntax for static features in addition to moving ones.</li>
 * </ul>
 *
 * The above extends the <a href="http://www.ietf.org/rfc/rfc4180.txt">Common Format and MIME Type for
 * Comma-Separated Values (CSV) Files</a> specification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
package org.apache.sis.internal.storage.csv;
