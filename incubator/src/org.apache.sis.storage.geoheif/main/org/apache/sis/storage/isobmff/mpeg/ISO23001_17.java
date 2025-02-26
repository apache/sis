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
package org.apache.sis.storage.isobmff.mpeg;

import java.util.Set;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.BoxRegistry;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ISO23001_17 implements BoxRegistry {

    private static final Set<String> BOXES = Set.of(
            ComponentDefinition.FCC,
            UncompressedFrameConfig.FCC,
            ComponentPalette.FCC,
            ComponentPatternDefinition.FCC,
            ComponentReferenceLevel.FCC,
            PolarizationPatternDefinition.FCC,
            SensorNonUniformityCorrection.FCC,
            SensorBadPixelsMap.FCC,
            ChromaLocation.FCC,
            FramePackingInformation.FCC,
            DisparityInformation.FCC,
            DepthMappingInformation.FCC,
            FieldInterlaceType.FCC,
            FieldInterlace.FCC,
            //TAIClockInfo.FCC, //TODO : find box structure, it seems to have a variable size
            TAITimeStamp.FCC
        );
    private static final Set<String> EXTENSIONS = Set.of();

    @Override
    public String getName() {
        return "ISO-23001-17";
    }

    @Override
    public Set<String> getBoxesFourCC() {
        return BOXES;
    }

    @Override
    public Set<String> getExtensionUUIDs() {
        return EXTENSIONS;
    }

    @Override
    public Box create(String fourCC) throws IllegalNameException {
        //TODO replace by String switch when SIS minimum java is updated
        if (ComponentDefinition.FCC.equals(fourCC)) return new ComponentDefinition();
        else if (UncompressedFrameConfig.FCC.equals(fourCC)) return new UncompressedFrameConfig();
        else if (ComponentPalette.FCC.equals(fourCC)) return new ComponentPalette();
        else if (ComponentPatternDefinition.FCC.equals(fourCC)) return new ComponentPatternDefinition();
        else if (ComponentReferenceLevel.FCC.equals(fourCC)) return new ComponentReferenceLevel();
        else if (PolarizationPatternDefinition.FCC.equals(fourCC)) return new PolarizationPatternDefinition();
        else if (SensorNonUniformityCorrection.FCC.equals(fourCC)) return new SensorNonUniformityCorrection();
        else if (SensorBadPixelsMap.FCC.equals(fourCC)) return new SensorBadPixelsMap();
        else if (ChromaLocation.FCC.equals(fourCC)) return new ChromaLocation();
        else if (FramePackingInformation.FCC.equals(fourCC)) return new FramePackingInformation();
        else if (DisparityInformation.FCC.equals(fourCC)) return new DisparityInformation();
        else if (DepthMappingInformation.FCC.equals(fourCC)) return new DepthMappingInformation();
        else if (FieldInterlaceType.FCC.equals(fourCC)) return new FieldInterlaceType();
        else if (FieldInterlace.FCC.equals(fourCC)) return new FieldInterlace();
        else if (TAIClockInfo.FCC.equals(fourCC)) return new TAIClockInfo();
        else if (TAITimeStamp.FCC.equals(fourCC)) return new TAITimeStamp();
        throw new IllegalNameException();
    }

    @Override
    public Box createExtension(String uuid) throws IllegalNameException {
        throw new IllegalNameException();
    }

}
