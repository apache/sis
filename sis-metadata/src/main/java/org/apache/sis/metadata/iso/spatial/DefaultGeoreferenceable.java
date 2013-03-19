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
package org.apache.sis.metadata.iso.spatial;

import java.util.Collection;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.spatial.GeolocationInformation;
import org.opengis.metadata.spatial.Georeferenceable;
import org.opengis.util.InternationalString;
import org.opengis.util.Record;

public class DefaultGeoreferenceable extends DefaultGridSpatialRepresentation implements Georeferenceable {

    private boolean controlPointAvailable;

    private boolean orientationParameterAvailable;

    private InternationalString orientationParameterDescription;

    private Record georeferencedParameters;

    private Collection<Citation> parameterCitations;

    private Collection<GeolocationInformation> geolocationInformation;

    @Override
    public synchronized boolean isControlPointAvailable() {
        return controlPointAvailable;
    }

    @Override
    public synchronized boolean isOrientationParameterAvailable() {
        return orientationParameterAvailable;
    }

    @Override
    public synchronized InternationalString getOrientationParameterDescription() {
        return orientationParameterDescription;
    }

    @Override
    public synchronized Record getGeoreferencedParameters() {
        return georeferencedParameters;
    }

    @Override
    public synchronized Collection<Citation> getParameterCitations() {
        return parameterCitations;
    }

    @Override
    public synchronized Collection<GeolocationInformation> getGeolocationInformation() {
        return geolocationInformation;
    }
}
