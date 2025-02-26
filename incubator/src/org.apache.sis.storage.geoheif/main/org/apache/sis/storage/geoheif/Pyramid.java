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
package org.apache.sis.storage.geoheif;

import org.opengis.util.GenericName;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.isobmff.image.ImagePyramid;


/**
 * A pyramid of images.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 *
 * @todo Not yet implemented. Not to integrate with {@code TiledGridCoverage}.
 *       It will require completion of the work for making {@code TiledGridCoverage}.
 *       an implementation of {@code TileMatrixSet}.
 */
final class Pyramid extends AbstractResource {
    /**
     * Name of this pyramid.
     */
    private final GenericName name;

    /**
     * Creates a new pyramid.
     *
     * @param store       the parent of this pyramid.
     * @param name        the name of this pyramid.
     * @param components  the child resources.
     */
    Pyramid(final GeoHeifStore store, final GenericName name, final ImagePyramid pyramid, final GridCoverageResource[] components) {
        super(store);
        this.name = name;
    }
}
