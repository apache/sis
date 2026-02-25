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
package org.apache.sis.geometries.scene;

import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Arrays;
import java.util.Objects;
import org.apache.sis.geometries.AttributesType;
import org.apache.sis.image.PixelIterator;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Texture {

    private RenderedImage image;
    private String texCoord = AttributesType.ATT_TEXCOORD_0;
    private Sampler sampler = new Sampler();

    /**
     * @return loaded image
     */
    public RenderedImage getImage() {
        return image;
    }

    /**
     * @param image loaded image
     */
    public void setImage(RenderedImage image) {
        this.image = image;
    }

    /**
     * @return The name texture's TEXCOORD attribute used for texture coordinate mapping.
     */
    public String getTexCoord() {
        return texCoord;
    }

    /**
     * @param texCoord The name texture's TEXCOORD attribute used for texture coordinate mapping.
     */
    public void setTexCoord(String texCoord) {
        this.texCoord = texCoord;
    }

    public Sampler getSampler() {
        return sampler;
    }

    public void setSampler(Sampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.image);
        hash = 97 * hash + Objects.hashCode(this.texCoord);
        hash = 97 * hash + Objects.hashCode(this.sampler);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Texture other = (Texture) obj;
        if (!Objects.equals(this.texCoord, other.texCoord)) {
            return false;
        }
        if (!( (this.image == other.image) || (this.image != null && compare(this.image, other.image)))) {
            return false;
        }
        if (!Objects.equals(this.sampler, other.sampler)) {
            return false;
        }
        return true;
    }

    private static boolean compare(RenderedImage expected, RenderedImage result) {
        if (expected == result) {
            return true;
        }
        final ColorModel expectedCm = expected.getColorModel();
        final ColorModel resultCm = result.getColorModel();
        if (!expectedCm.equals(resultCm)) {
            return false;
        }
        final SampleModel expectedSm = expected.getSampleModel();
        final SampleModel resultSm = result.getSampleModel();
        if (!expectedSm.equals(resultSm)) {
            return false;
        }

        final PixelIterator ite1 = PixelIterator.create(expected);
        final PixelIterator ite2 = PixelIterator.create(result);
        if (!ite1.getDomain().equals(ite2.getDomain())) {
            return false;
        }
        final double[] pixel1 = new double[ite1.getNumBands()];
        final double[] pixel2 = new double[ite2.getNumBands()];

        pixelLoop:
        while (ite1.next()) {
            final Point position = ite1.getPosition();
            ite2.moveTo(position.x, position.y);
            ite1.getPixel(pixel1);
            ite2.getPixel(pixel2);
            if (!Arrays.equals(pixel1, pixel2)) {
                return false;
            }
        }
        return true;
    }
}
