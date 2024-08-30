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
package org.apache.sis.storage.sql.feature;


/**
 * The encoding of Coordinate Reference Systems in a particular column, in preference order.
 * The Geopackage specification said that WKT 2 has precedence over WKT 1.
 *
 * <p><b>Note:</b> the distinction between version 1 and 2 of <abbr>WKT</abbr> formats should not have been needed,
 * because a decent parser should be able to differentiate those two versions automatically based on the fact that
 * they use different keywords. Unfortunately, the Geopackage format decided to use a separated column for WKT 2.
 * From Apache <abbr>SIS</abbr> perspective, this is more complication than help.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum CRSEncoding {
    /**
     * Well-Known Text 2 as defined by ISO 19162.
     */
    WKT2,

    /**
     * Well-KNown Text 1 as defined by OGC 01-009.
     */
    WKT1;
}
