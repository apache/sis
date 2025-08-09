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
package org.apache.sis.referencing.datum;

import java.time.temporal.Temporal;
import org.opengis.referencing.datum.Datum;
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Placeholder for an interface that may be added in GeoAPI 3.1.
 */
interface DynamicReferenceFrame extends Datum {
    /**
     * {@return the epoch to which the coordinates of stations defining the dynamic datum are referenced}.
     */
    @UML(identifier="frameReferenceEpoch", obligation=MANDATORY, specification=ISO_19111)
    Temporal getFrameReferenceEpoch();
}
