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
 * French extensions defined by the <cite>Association Française de Normalisation</cite> (AFNOR).
 *
 * <div class="section">Historical note</div>
 * The French profile also added two properties to the ISO 19115:2003 standard.
 * Equivalent properties have been added to the 2013 revision of ISO 19115,
 * so the French profile should not be needed anymore except for compatibility with oldest specifications.
 * The following table lists the French extentions and their replacement in the newer ISO standard:
 *
 * <table class="sis">
 *   <caption>AFNOR to ISO mapping</caption>
 *   <tr><th>French profile</th> <th>ISO 19115:2014 equivalent</th></tr>
 *   <tr><td>{@code FRA_DataIdentification.relatedCitation}</td> <td>{@code MD_Identification.additionalDocumentation}</td></tr>
 *   <tr><td>{@code FRA_Constraints.citation}</td> <td>{@code MD_Constraints.reference}</td></tr>
 * </table>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 0.4
 * @since   0.4
 * @module
 */
package org.apache.sis.profile.france;
