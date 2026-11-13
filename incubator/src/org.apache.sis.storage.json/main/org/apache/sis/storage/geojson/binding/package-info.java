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
 * Jackson bindings of GeoJSON (<a href="https://datatracker.ietf.org/doc/html/rfc7946">RFC 7946</a>) objects.
 *
 * <p>The GeoJSON bindings have been extended with:
 * <ul>
 *   <li><a href="https://ogcapi.ogc.org/features/">OGC API - Features</a> members,</li>
 *   <li><a href="https://docs.ogc.org/is/21-045r1/21-045r1.html">OGC JSON-FG 1.0.0 (21-045r1)</a> members and geometry types.</li>
 * </ul>
 *
 * <h2>ISO/TR 19115-4 metadata</h2>
 * The {@code JSONMD} classes are bindings of the ISO/TR 19115-4 {@code mdj.json} and {@code dqc.json} schema types,
 * used as {@code properties} of metadata records.
 * Each binding is created from a SIS metadata object with a constructor taking a {@link JSONMDEncoder},
 * and converted back with a {@code toMetadata} method taking a {@link JSONMDDecoder}.
 * Property names are the ISO 19115-1 and ISO 19157-1 UML names.
 * Unknown JSON members are kept in each binding but not converted.
 *
 * <p>In addition to the ISO 19115-4 Core profile, the following properties are encoded with the same rules:
 * {@code MD_Metadata.referenceSystemInfo}, {@code MD_DataIdentification.graphicOverview}, {@code spatialResolution}
 * and {@code resourceSpecificUsage}, {@code CI_Organisation.logo}, {@code EX_Extent.temporalElement} and {@code verticalElement}.
 *
 * <p>Limitations:
 * <ul>
 *   <li>only one {@code identificationInfo} is encoded,</li>
 *   <li>only one date per date type ({@code CI_Date}) and one number per telephone type ({@code CI_Telephone}) are encoded,</li>
 *   <li>texts are encoded in a single locale,</li>
 *   <li>{@code MD_ReferenceSystem.coordinateEpoch}, {@code QuantitativeResult.valueRecordType},
 *       {@code QualityEvaluationReportInformation.elementReport} and {@code qualityEvaluationReportDetails},
 *       and the evaluation method {@code name} are not supported,</li>
 *   <li>the reference system type is lost when the reference system cannot be resolved from its URI,</li>
 *   <li>other SIS metadata properties (e.g. {@code contentInfo}, {@code spatialRepresentationInfo}) are not encoded.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json</a>
 * @see <a href="https://schemas.isotc211.org/json/19157/-1/dqc/1.0.0/dqc.json">dqc.json</a>
 */
package org.apache.sis.storage.geojson.binding;
