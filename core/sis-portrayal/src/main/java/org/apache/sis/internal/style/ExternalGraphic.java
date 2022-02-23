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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.style.StyleVisitor;

/**
 * Mutable implementation of {@link org.opengis.style.ExternalGraphic}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ExternalGraphic implements org.opengis.style.ExternalGraphic, GraphicalSymbol {

    private OnlineResource onlineResource;
    private Icon inlineContent;
    private String format;
    private Collection<ColorReplacement> colorReplacements;

    public ExternalGraphic() {
    }

    public ExternalGraphic(OnlineResource onlineResource, Icon inlineContent, String format, Collection<ColorReplacement> colorReplacements) {
        this.onlineResource = onlineResource;
        this.inlineContent = inlineContent;
        this.format = format;
        this.colorReplacements = colorReplacements;
    }

    @Override
    public OnlineResource getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(OnlineResource onlineResource) {
        this.onlineResource = onlineResource;
    }

    @Override
    public Icon getInlineContent() {
        return inlineContent;
    }

    public void setInlineContent(Icon inlineContent) {
        this.inlineContent = inlineContent;
    }

    @Override
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public Collection<org.opengis.style.ColorReplacement> getColorReplacements() {
        return (Collection) colorReplacements;
    }

    public void setColorReplacements(Collection<ColorReplacement> colorReplacements) {
        this.colorReplacements = colorReplacements;
    }

    @Override
    public Object accept(StyleVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(onlineResource, inlineContent, format, colorReplacements);
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
        final ExternalGraphic other = (ExternalGraphic) obj;
        return Objects.equals(this.format, other.format)
            && Objects.equals(this.onlineResource, other.onlineResource)
            && Objects.equals(this.inlineContent, other.inlineContent)
            && Objects.equals(this.colorReplacements, other.colorReplacements);
    }

    /**
     * Cast or copy to an SIS implementation.
     *
     * @param candidate to copy, can be null.
     * @return cast or copied object.
     */
    public static ExternalGraphic castOrCopy(org.opengis.style.ExternalGraphic candidate) {
        if (candidate == null) {
            return null;
        } else if (candidate instanceof ExternalGraphic) {
            return (ExternalGraphic) candidate;
        }

        final List<ColorReplacement> cs = new ArrayList<>();
        for (org.opengis.style.ColorReplacement cr : candidate.getColorReplacements()) {
            cs.add(ColorReplacement.castOrCopy(cr));
        }

        return new ExternalGraphic(
                candidate.getOnlineResource(),
                candidate.getInlineContent(),
                candidate.getFormat(),
                cs);
    }
}
