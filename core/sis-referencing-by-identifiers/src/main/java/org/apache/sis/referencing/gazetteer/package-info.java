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
 * Mapping between geographic identifiers (addresses, grid indexes…) and locations (descriptions, coordinates…).
 * This package provides base classes implementing the <cite>Spatial referencing by geographic identifiers</cite>
 * (ISO 19112) standard. Those base classes are
 * {@link org.apache.sis.referencing.gazetteer.AbstractLocation},
 * {@link org.apache.sis.referencing.gazetteer.ModifiableLocationType} and
 * {@link org.apache.sis.referencing.gazetteer.ReferencingByIdentifiers}, completed by the
 * {@link org.apache.sis.referencing.gazetteer.LocationFormat} utility class.
 *
 * <p>This package provides also implementations on top of above base classes.
 * Some implementation classes are {@link org.apache.sis.referencing.gazetteer.MilitaryGridReferenceSystem}
 * (also for civilian use) and {@link org.apache.sis.referencing.gazetteer.GeohashReferenceSystem}.</p>
 *
 * @author  Chris Mattmann (JPL)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.8
 * @module
 */
package org.apache.sis.referencing.gazetteer;
