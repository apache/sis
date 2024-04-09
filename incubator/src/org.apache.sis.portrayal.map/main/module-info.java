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

/**
 * Raster imagery and geometry features.
 *
 * @author  Johann Sorel (Geomatys)
 */
module org.apache.sis.portrayal.map {
    requires transitive org.apache.sis.portrayal;
    requires static org.locationtech.jts;

    exports org.apache.sis.map;
    exports org.apache.sis.map.service;

    uses org.apache.sis.map.service.StylePainter;
    uses org.apache.sis.map.service.se1.SymbolizerToScene2D.Spi;

    provides org.apache.sis.map.service.StylePainter
            with org.apache.sis.map.service.se1.SEPainter;
    provides org.apache.sis.map.service.se1.SymbolizerToScene2D.Spi
            with org.apache.sis.map.service.se1.PointToScene2D.Spi,
                 org.apache.sis.map.service.se1.LineToScene2D.Spi,
                 org.apache.sis.map.service.se1.PolygonToScene2D.Spi,
                 org.apache.sis.map.service.se1.TextToScene2D.Spi,
                 org.apache.sis.map.service.se1.RasterToScene2D.Spi;
}