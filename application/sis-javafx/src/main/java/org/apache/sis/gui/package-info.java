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
 * JavaFX application for Apache SIS.
 * See the <a href="https://sis.apache.org/javafx.html">JavaFX application</a> page
 * on the Apache SIS web site for more information.
 *
 * <h2>File size limit</h2>
 * There is usually no size limit when viewing only the metadata, because only the file headers are read at that time.
 * When viewing the data, there is no size limit if the data are pyramided and tiled with tiles of reasonable size,
 * because the application loads only the tiles needed for the area being displayed.
 * An example of file format supporting tiling is GeoTIFF.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
package org.apache.sis.gui;
