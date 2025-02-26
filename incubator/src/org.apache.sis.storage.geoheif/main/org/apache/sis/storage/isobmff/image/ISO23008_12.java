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
package org.apache.sis.storage.isobmff.image;

import java.util.Set;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.BoxRegistry;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ISO23008_12 implements BoxRegistry {

    private static final Set<String> BOXES = Set.of(
            DerivedImageReference.FCC,
            ImagePyramid.FCC,
            ImageSpatialExtents.FCC,
            PixelInformation.FCC,
            UserDescription.FCC
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
        else if (ImagePyramid.FCC.equals(fourCC)) return new ImagePyramid();
        else if (ImageSpatialExtents.FCC.equals(fourCC)) return new ImageSpatialExtents();
        else if (PixelInformation.FCC.equals(fourCC)) return new PixelInformation();
        else if (UserDescription.FCC.equals(fourCC)) return new UserDescription();
        //TODO other box types
        throw new IllegalNameException();
    }

    @Override
    public Box createExtension(String uuid) throws IllegalNameException {
        throw new IllegalNameException();
    }

}
