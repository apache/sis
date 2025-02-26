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
package org.apache.sis.storage.isobmff.gimi;

import java.util.Set;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.BoxRegistry;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GIMI implements BoxRegistry {

    private static final Set<String> BOXES = Set.of(
            TiledImageConfiguration.FCC
        );
    private static final Set<String> EXTENSIONS = Set.of(
            ModelTiePoint.UUID,
            ModelTransformation.UUID,
            ModelCRS.UUID
        );

    @Override
    public String getName() {
        return "GIMI";
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
        if (TiledImageConfiguration.FCC.equals(fourCC)) return new TiledImageConfiguration();
        throw new IllegalNameException();
    }

    @Override
    public Box createExtension(String uuid) throws IllegalNameException {
        if (ModelTiePoint.UUID.equals(uuid)) return new ModelTiePoint();
        else if (ModelTransformation.UUID.equals(uuid)) return new ModelTransformation();
        else if (ModelCRS.UUID.equals(uuid)) return new ModelCRS();
        throw new IllegalNameException();
    }

}
