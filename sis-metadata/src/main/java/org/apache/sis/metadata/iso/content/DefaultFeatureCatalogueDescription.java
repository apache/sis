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
package org.apache.sis.metadata.iso.content;

import java.util.Collection;
import java.util.Locale;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.content.FeatureCatalogueDescription;
import org.opengis.util.GenericName;

public class DefaultFeatureCatalogueDescription extends AbstractContentInformation
        implements FeatureCatalogueDescription
{

    private Boolean compliant;

    private Collection<Locale> languages;

    private boolean includedWithDataset;

    private Collection<GenericName> featureTypes;

    private Collection<Citation> featureCatalogueCitations;

    @Override
    public synchronized Boolean isCompliant() {
        return compliant;
    }

    @Override
    public synchronized Collection<Locale> getLanguages() {
        return languages;
    }

    @Override
    public synchronized boolean isIncludedWithDataset() {
        return includedWithDataset;
    }

    @Override
    public synchronized Collection<GenericName> getFeatureTypes() {
        return featureTypes;
    }

    @Override
    public synchronized Collection<Citation> getFeatureCatalogueCitations() {
        return featureCatalogueCitations;
    }
}
