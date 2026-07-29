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
 * This package contains a API for Discrete Global Grid Reference Systems (DGGRS).
 *
 * Based on specifications  :
 * - ISO-19170 : Geographic Information — Discrete Global Grid Systems Specifications — Core Reference System and Operations, and Equal Area Earth Reference System
 * - OGC Topic 21 - Discrete Global Grid Systems - Part 1 Core Reference system and Operations and Equal Area Earth Reference System : https://docs.ogc.org/as/20-040r3/20-040r3.html
 * - OGC DGGRS API : https://docs.ogc.org/DRAFTS/21-038r1.html
 *
 * ISO-19170:2020 and OGC Topic 21 - Discrete Global Grid Systems are actually the same document.
 *
 * Current DGGRS API do not follow ISO-19170 exactly.
 *
 * More precisely, ISO-19170 define a DGGRS as a sub-type of CRS (https://docs.ogc.org/as/20-040r3/20-040r3.html#tab-DGG_ReferenceSystem).
 * This choice would result a complete review of the CRS API to handle positions which are not numeric.
 *
 * As a matter of fact we have choosen to extend ReferingByIdentifiers (ISO-19112) instead.
 */
package org.apache.sis.referencing.dggs;
