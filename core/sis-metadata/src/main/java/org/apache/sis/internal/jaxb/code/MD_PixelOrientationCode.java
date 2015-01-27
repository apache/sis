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
package org.apache.sis.internal.jaxb.code;

import javax.xml.bind.annotation.XmlElement;
import org.opengis.metadata.spatial.PixelOrientation;
import org.apache.sis.internal.jaxb.gmd.EnumAdapter;


/**
 * JAXB adapter for {@link PixelOrientation}, in order to integrate the value in an element
 * complying with ISO-19139 standard. See package documentation for more information about
 * the handling of {@code CodeList} in ISO-19139.
 *
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
public final class MD_PixelOrientationCode extends EnumAdapter<MD_PixelOrientationCode, PixelOrientation> {
    /**
     * The enumeration value.
     */
    @XmlElement(name = "MD_PixelOrientationCode")
    private String value;

    /**
     * Empty constructor for JAXB only.
     */
    public MD_PixelOrientationCode() {
    }

    /**
     * Returns the wrapped value.
     *
     * @param wrapper The wrapper.
     * @return The wrapped value.
     */
    @Override
    public final PixelOrientation unmarshal(final MD_PixelOrientationCode wrapper) {
        return PixelOrientation.valueOf(name(wrapper.value));
    }

    /**
     * Wraps the given value.
     *
     * @param  e The value to wrap.
     * @return The wrapped value.
     */
    @Override
    public final MD_PixelOrientationCode marshal(final PixelOrientation e) {
        if (e == null) {
            return null;
        }
        final MD_PixelOrientationCode wrapper = new MD_PixelOrientationCode();
        wrapper.value = value(e);
        return wrapper;
    }
}
