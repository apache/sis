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
package org.apache.sis.map.coverage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;
import org.apache.sis.image.internal.shared.TilePlaceholder;


/**
 * Workaround for the bug in calls to {@code Graphics2D.drawRenderedImage(…)}
 * when the image is tiled and some tiles are not writable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://bugs.openjdk.java.net/browse/JDK-8275345">JDK-8275345</a>
 */
public final class RenderingWorkaround implements RenderedImage {
    /**
     * Applies workaround for JDK-8275345 if needed.
     *
     * @param  image  the image on which to apply the workaround.
     * @return image that can be used for rendering purpose.
     */
    public static RenderedImage wrap(final RenderedImage image) {
        if (TilePlaceholder.PENDING_JDK_FIX) {
            if (!(image == null || image instanceof BufferedImage || image instanceof RenderingWorkaround)) {
                return new RenderingWorkaround(image);
            }
        }
        return image;
    }

    /**
     * The image to render.
     */
    private final RenderedImage image;

    /**
     * Creates a new wrapper for the given image.
     */
    private RenderingWorkaround(final RenderedImage image) {
        this.image = image;
    }

    @Override public Vector<RenderedImage> getSources()               {return image.getSources();}
    @Override public Object                getProperty(String name)   {return image.getProperty(name);}
    @Override public String[]              getPropertyNames()         {return image.getPropertyNames();}
    @Override public ColorModel            getColorModel()            {return image.getColorModel();}
    @Override public SampleModel           getSampleModel()           {return image.getSampleModel();}
    @Override public int                   getWidth()                 {return image.getWidth();}
    @Override public int                   getHeight()                {return image.getHeight();}
    @Override public int                   getMinX()                  {return image.getMinX();}
    @Override public int                   getMinY()                  {return image.getMinY();}
    @Override public int                   getNumXTiles()             {return image.getNumXTiles();}
    @Override public int                   getNumYTiles()             {return image.getNumYTiles();}
    @Override public int                   getMinTileX()              {return image.getMinTileX();}
    @Override public int                   getMinTileY()              {return image.getMinTileY();}
    @Override public int                   getTileWidth()             {return image.getTileWidth();}
    @Override public int                   getTileHeight()            {return image.getTileHeight();}
    @Override public int                   getTileGridXOffset()       {return image.getTileGridXOffset();}
    @Override public int                   getTileGridYOffset()       {return image.getTileGridYOffset();}
    @Override public Raster                getTile(int tx, int ty)    {return wrap(image.getTile(tx, ty));}
    @Override public Raster                getData()                  {return wrap(image.getData());}
    @Override public Raster                getData(Rectangle rect)    {return wrap(image.getData(rect));}
    @Override public WritableRaster        copyData(WritableRaster r) {return image.copyData(r);}

    /**
     * Returns the given raster as an instance of {@link WritableRaster}.
     * The underlying data buffer is shared, not copied.
     */
    private static Raster wrap(final Raster r) {
        if (r instanceof WritableRaster) {
            return r;
        } else {
            return Raster.createWritableRaster(r.getSampleModel(), r.getDataBuffer(), new Point(r.getMinX(), r.getMinY()));
        }
    }
}
