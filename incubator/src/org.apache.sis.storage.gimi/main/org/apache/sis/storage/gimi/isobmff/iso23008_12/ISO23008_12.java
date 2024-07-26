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
package org.apache.sis.storage.gimi.isobmff.iso23008_12;

import java.util.Set;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.BoxRegistry;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ISO23008_12 implements BoxRegistry {

    private static final Set<String> BOXES = Set.of(
            DerivedImageReference.FCC,
            ImagePyramidEntityGroup.FCC,
            ImageSpatialExtents.FCC,
            PixelInformationProperty.FCC,
            UserDescriptionProperty.FCC
        );
    private static final Set<String> EXTENSIONS = Set.of();

    @Override
    public String getName() {
        return "ISO-23008-12";
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
        if (DerivedImageReference.FCC.equals(fourCC)) return new DerivedImageReference();
        else if (ImagePyramidEntityGroup.FCC.equals(fourCC)) return new ImagePyramidEntityGroup();
        else if (ImageSpatialExtents.FCC.equals(fourCC)) return new ImageSpatialExtents();
        else if (PixelInformationProperty.FCC.equals(fourCC)) return new PixelInformationProperty();
        else if (UserDescriptionProperty.FCC.equals(fourCC)) return new UserDescriptionProperty();
        //TODO other box types
        throw new IllegalNameException();
    }

    @Override
    public Box createExtension(String uuid) throws IllegalNameException {
        throw new IllegalNameException();
    }

}
