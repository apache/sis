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
package org.apache.sis.internal.style;

import java.util.Objects;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.ChannelSelection}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ChannelSelection implements org.opengis.style.ChannelSelection {

    private SelectedChannelType gray;
    private SelectedChannelType red;
    private SelectedChannelType green;
    private SelectedChannelType blue;

    public ChannelSelection() {
    }

    public ChannelSelection(SelectedChannelType gray) {
        this.gray = gray;
    }

    public ChannelSelection(SelectedChannelType red, SelectedChannelType green, SelectedChannelType blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    private ChannelSelection(org.opengis.style.SelectedChannelType gray, org.opengis.style.SelectedChannelType[] rgb) {
        this.gray = SelectedChannelType.castOrCopy(gray);
        if (rgb != null) {
            this.red = SelectedChannelType.castOrCopy(rgb[0]);
            this.green = SelectedChannelType.castOrCopy(rgb[1]);
            this.blue = SelectedChannelType.castOrCopy(rgb[2]);
        }
    }

    @Override
    public SelectedChannelType[] getRGBChannels() {
        return gray != null ? null :
                red != null ? new SelectedChannelType[]{red, green, blue} : null;
    }

    public void setRGBChannels(SelectedChannelType[] rgb) {
        if (rgb == null) {
            this.red = null;
            this.green = null;
            this.blue = null;
        } else {
            this.red = rgb[0];
            this.green = rgb[1];
            this.blue = rgb[2];
        }
    }

    @Override
    public SelectedChannelType getGrayChannel() {
        return gray;
    }

    public void setGray(SelectedChannelType gray) {
        this.gray = gray;
    }


    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gray, red, green, blue);
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
        final ChannelSelection other = (ChannelSelection) obj;
        return Objects.equals(this.gray, other.gray)
            && Objects.equals(this.red, other.red)
            && Objects.equals(this.green, other.green)
            && Objects.equals(this.blue, other.blue);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static ChannelSelection castOrCopy(org.opengis.style.ChannelSelection candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof ChannelSelection) {
            return (ChannelSelection) candidate;
        }
        return new ChannelSelection(candidate.getGrayChannel(), candidate.getRGBChannels());
    }
}
