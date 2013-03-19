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
package org.apache.sis.metadata.iso.maintenance;

import java.util.Set;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.apache.sis.metadata.iso.ISOMetadata;

public class DefaultScopeDescription extends ISOMetadata implements ScopeDescription {

    private Set<AttributeType> attributes;

    private Set<FeatureType> features;

    private Set<FeatureType> featureInstances;

    private Set<AttributeType> attributeInstances;

    private String dataset;

    private String other;

    @Override
    public synchronized Set<AttributeType> getAttributes() {
        return attributes;
    }

    @Override
    public synchronized Set<FeatureType> getFeatures() {
        return features;
    }

    @Override
    public synchronized Set<FeatureType> getFeatureInstances() {
        return featureInstances;
    }

    @Override
    public synchronized Set<AttributeType> getAttributeInstances() {
        return attributeInstances;
    }

    @Override
    public synchronized String getDataset() {
        return dataset;
    }

    @Override
    public synchronized String getOther() {
        return other;
    }
}
