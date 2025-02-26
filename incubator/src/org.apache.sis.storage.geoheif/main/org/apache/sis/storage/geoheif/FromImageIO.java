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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;


/**
 * A jpeg image as a GridCoverageResource.
 *
 * @author Johann Sorel (Geomatys)
 */
final class FromImageIO extends UncompressedImage {

    public static final String TYPE = "jpeg";

    public FromImageIO(GeoHeifStore store, Image item) throws DataStoreException {
        super(store, item);
    }

    @Override
    public GridCoverage read(GridGeometry gg, int... ints) throws DataStoreException {
        final byte[] data = item.getData(0,-1, null, 0);
        ImageInputStream iis;
        BufferedImage img;
        try {
            iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data));
            img = ImageIO.read(iis);
        } catch (IOException ex) {
            throw new DataStoreException(ex);
        }
        final GridGeometry gridGeometry = getGridGeometry();
        GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setDomain(gridGeometry);
        //gcb.setRanges(getSampleDimensions());
        //gcb.setValues(db, new Dimension((int)gridGeometry.getExtent().getSize(0), (int)gridGeometry.getExtent().getSize(1)));
        gcb.setValues(img);
        return gcb.build();
    }

}
