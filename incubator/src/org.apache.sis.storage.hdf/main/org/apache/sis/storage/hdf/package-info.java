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
 * Data store for files in the Hierarchical Data Format version 5 (<abbr>HDF5</abbr>).
 *
 * <p>Specification: <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html">HDF5 File Format Specification Version 3.0</a></p>
 *
 * <p>Note-1 : The specification contains bugs(differences between what it says and what the hdf5 lib does) and unsaid informations.
 *        As a result we have no other choice but to check the code from HDF5 library
 *        and sometimes to transpose it's logic without understand it to be able to read HDF-5 files.
 *        This is the unfortunate result of a specification build above a specific library.
 *        Which is not uncommon (like the Draco compression), but leads to lower quality specifications.</p>
 * <p>Note-2 : As a result of note 1, HDF-5 should not be used as a reliable exchange format because what dictates
 *        a valid HDF-5 file is what produces HDFLib, not what the specification dictates. Such behavior may lead
 *        to unreadable files in the futur.</p>
 */
package org.apache.sis.storage.hdf;
