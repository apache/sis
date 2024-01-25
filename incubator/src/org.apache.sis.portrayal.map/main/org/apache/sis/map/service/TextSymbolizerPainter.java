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
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.map.MapLayer;
import org.apache.sis.style.se1.Symbolizer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class TextSymbolizerPainter extends SymbolizerPainter {

    public TextSymbolizerPainter(Symbolizer<?> symbolizer) {
        super(symbolizer);
    }

    @Override
    public void paint(Graphics2D g, GridGeometry gridGeometry, MapLayer layer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
