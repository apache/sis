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
package org.apache.sis.style.xml;

import javax.swing.ImageIcon;


/**
 * An icon read from the {@code <InlineContent>} element of a style.
 * This icon keeps a reference to the encoded image, for writing it back without re-encoding.
 *
 * @author  Johann Sorel (Geomatys)
 */
@SuppressWarnings("serial")             // Not intended to be serialized.
final class InlineImage extends ImageIcon {
    /**
     * The encoded image, as read from the XML document. This array shall not be modified.
     */
    final byte[] data;

    /**
     * Creates a new icon for the given encoded image.
     *
     * @param  data  the encoded image, for example in PNG format.
     */
    InlineImage(final byte[] data) {
        super(data);
        this.data = data;
    }
}
