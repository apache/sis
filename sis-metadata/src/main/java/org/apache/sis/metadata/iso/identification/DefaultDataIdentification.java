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
package org.apache.sis.metadata.iso.identification;

import java.util.Collection;
import java.util.Locale;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.Resolution;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.util.InternationalString;

public class DefaultDataIdentification extends AbstractIdentification implements DataIdentification {

    private Collection<SpatialRepresentationType> spatialRepresentationTypes;

    private Collection<Resolution> spatialResolutions;

    private Collection<Locale> languages;

    private Collection<CharacterSet> characterSets;

    private Collection<TopicCategory> topicCategories;

    private InternationalString environmentDescription;

    private Collection<Extent> extents;

    private InternationalString supplementalInformation;

    @Override
    public synchronized Collection<SpatialRepresentationType> getSpatialRepresentationTypes() {
        return spatialRepresentationTypes;
    }

    @Override
    public synchronized Collection<Resolution> getSpatialResolutions() {
        return spatialResolutions;
    }

    @Override
    public synchronized Collection<Locale> getLanguages() {
        return languages;
    }

    @Override
    public synchronized Collection<CharacterSet> getCharacterSets() {
        return characterSets;
    }

    @Override
    public synchronized Collection<TopicCategory> getTopicCategories() {
        return topicCategories;
    }

    @Override
    public synchronized InternationalString getEnvironmentDescription() {
        return environmentDescription;
    }

    @Override
    public synchronized Collection<Extent> getExtents() {
        return extents;
    }

    @Override
    public synchronized InternationalString getSupplementalInformation() {
        return supplementalInformation;
    }
}
