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
package org.apache.sis.gui.coverage;

import org.apache.sis.internal.util.Strings;


/**
 * Information about how to render an image.
 * This is a combination of {@link ImageOperation} with {@link Stretching}.
 * Note that {@link Stretching} is a temporary enumeration to be deleted after SIS provides styling support.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class ImageDerivative {
    /**
     * The key when no operation is applied on the image.
     */
    static final ImageDerivative NONE = new ImageDerivative(ImageOperation.NONE, Stretching.NONE);

    /**
     * The operation applied on the image.
     */
    final ImageOperation operation;

    /**
     * Key of the currently selected alternative in {@link CoverageCanvas#resampledImages} map.
     */
    final Stretching styling;

    /**
     * Creates a new combination of operation and styling.
     */
    private ImageDerivative(final ImageOperation operation, final Stretching styling) {
        this.operation = operation;
        this.styling   = styling;
    }

    /**
     * Returns a key with the same styling than this key but a different operation.
     */
    final ImageDerivative setOperation(final ImageOperation selection) {
        return (selection != operation) ? new ImageDerivative(selection, styling) : this;
    }

    /**
     * Returns a key with the same operation than this key but a different styling.
     */
    final ImageDerivative setStyling(final Stretching selection) {
        return (selection != styling) ? new ImageDerivative(operation, selection) : this;
    }

    /**
     * Compares this key with given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ImageDerivative) {
            final ImageDerivative other = (ImageDerivative) obj;
            return (operation == other.operation) && (styling == other.styling);
        }
        return false;
    }

    /**
     * Returns a hash code value for this key.
     */
    @Override
    public int hashCode() {
        return operation.hashCode() + 11 * styling.hashCode();
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "operation", operation, "styling", styling);
    }
}
