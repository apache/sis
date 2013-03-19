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
package org.apache.sis.metadata.iso.lineage;

import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.opengis.metadata.lineage.NominalResolution;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.metadata.lineage.Source;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;

public class DefaultSource extends ISOMetadata implements Source {

    private InternationalString description;

    private RepresentativeFraction scaleDenominator;

    private ReferenceSystem sourceReferenceSystem;

    private Citation sourceCitation;

    private Collection<Extent> sourceExtents;

    private Collection<ProcessStep> sourceSteps;

    private Identifier processedLevel;

    private NominalResolution resolution;

    @Override
    public synchronized InternationalString getDescription() {
        return description;
    }

    @Override
    public synchronized RepresentativeFraction getScaleDenominator() {
        return scaleDenominator;
    }

    @Override
    public synchronized ReferenceSystem getSourceReferenceSystem() {
        return sourceReferenceSystem;
    }

    @Override
    public synchronized Citation getSourceCitation() {
        return sourceCitation;
    }

    @Override
    public synchronized Collection<Extent> getSourceExtents() {
        return sourceExtents;
    }

    @Override
    public synchronized Collection<ProcessStep> getSourceSteps() {
        return sourceSteps;
    }

    @Override
    public synchronized Identifier getProcessedLevel() {
        return processedLevel;
    }

    @Override
    public synchronized NominalResolution getResolution() {
        return resolution;
    }
}
