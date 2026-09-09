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
 * Shapefile format DataStore implementation.
 *
 * <h2>Reading example</h2>
 *{@snippet class="org.apache.sis.storage.shapefile.Snippets" region="read"}
 *
 * <h2>Writing example</h2>
 *{@snippet class="org.apache.sis.storage.shapefile.Snippets" region="write"}
 *
 * <h2>Feature identifiers</h2>
 * The shapefile format does not store feature identifiers, they are derived from
 * the record number. To keep those identifiers stable, removing a feature does not
 * shift the following records : the record is flagged as deleted in the dbf file and
 * its slot is preserved, holding a null shape and blank fields. New features are always
 * appended after the last record number, an identifier is therefore never reused.
 * Use {@link org.apache.sis.storage.shapefile.ShapefileStore#compact()} to drop those
 * records and reduce the files size, at the cost of renumbering the remaining records.
 *
 * For raw access to DBF and SHP, use the related packages :
 * <ul>
 * <li>{@link org.apache.sis.storage.shapefile.shp}</li>
 * <li>{@link org.apache.sis.storage.shapefile.shx}</li>
 * <li>{@link org.apache.sis.storage.shapefile.dbf}</li>
 * <li>{@link org.apache.sis.storage.shapefile.cpg}</li>
 * </ul>
 * The shapefile datastore layer is very thin and the only performance overheap
 * is the mapping from DBFRecord/ShpRecord to Feature.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf">ESRI Shapefile Specification</a>
 */
package org.apache.sis.storage.shapefile;
