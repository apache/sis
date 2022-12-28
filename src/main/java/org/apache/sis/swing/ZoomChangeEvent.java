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
package org.apache.sis.swing;

import java.util.EventObject;
import java.awt.geom.AffineTransform;


/**
 * An event which indicates that a zoom occurred in a component.
 * This event is fired by {@link ZoomPane}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public class ZoomChangeEvent extends EventObject {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5063317286699888858L;

    /**
     * An affine transform indicating the zoom change. If {@code oldZoom} and {@code newZoom}
     * are the affine transforms before and after the change respectively, then the following
     * relation must hold (within the limits of rounding error):
     *
     * {@snippet lang="java" :
     *     newZoom = oldZoom.concatenate(change)
     *     }
     */
    private final AffineTransform change;

    /**
     * Constructs a new event. If {@code oldZoom} and {@code newZoom} are the affine transforms
     * before and after the change respectively, then the following relation must hold (within
     * the limits of rounding error):
     *
     * {@snippet lang="java" :
     *     newZoom = oldZoom.concatenate(change)
     *     }
     *
     * @param  source  the event source.
     * @param  change  an affine transform indicating the zoom change.
     */
    public ZoomChangeEvent(final ZoomPane source, final AffineTransform change) {
        super(source);
        this.change = change;
    }

    /**
     * Returns the affine transform indicating the zoom change.
     * <strong>Note:</strong> for performance reasons, this method does not clone
     * the returned transform. Do not change!
     *
     * @return the zoom change as an affine transform (<strong>not</strong> cloned).
     */
    public AffineTransform getChange() {
        return change;
    }
}
