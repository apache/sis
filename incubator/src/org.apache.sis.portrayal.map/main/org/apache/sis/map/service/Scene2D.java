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
package org.apache.sis.map.service;

import java.awt.Graphics2D;
import java.util.logging.Logger;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.util.ArgumentChecks;


/**
 * Holds the rendering properties.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Scene2D {

    public static final Logger LOGGER = Logger.getLogger("org.apache.sis.internal.renderer");

    /**
     * The rendering grid geometry.
     */
    public final GridGeometry grid;
    /**
     * Graphics to render into.
     * When modified by renderers, it must be reset accordingly.
     */
    public final Graphics2D graphics;

    public Scene2D(GridGeometry grid, Graphics2D graphics) {
        ArgumentChecks.ensureNonNull("grid", grid);
        ArgumentChecks.ensureNonNull("graphics", graphics);
        this.grid = grid;
        this.graphics = graphics;
    }

    public Graphics2D getGraphics() {
        return graphics;
    }

}
