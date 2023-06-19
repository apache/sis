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


/**
 * Object having a graphical representation as small picture.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public interface GraphicalElement {
    /**
     * Returns the graphic.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this fill, and conversely.
     *
     * @return the picture.
     */
    Graphic getGraphic();

    /**
     * Sets the graphic.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is a {@linkplain Graphic#Graphic() default instance}.
     *
     * <p>The interpretation of {@code null} argument value depends on whether the graphic is mandatory or optional.
     * If mandatory, then a {@code null} argument value resets the {@linkplain Graphic#Graphic() default graphic}.
     * If optional, then a {@code null} argument value specifies to plot nothing.
     *
     * @param  value  new picture, or {@code null} for none or for resetting the default value.
     */
    void setGraphic(Graphic value);
}
