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
package org.apache.sis.gui.map.style;


/**
 * Placeholder for {@code org.apache.sis.map.MapItem}.
 * We use this temporary class because {@code org.apache.sis.map.MapItem} is in incubator.
 *
 * @todo Replace by {@link org.apache.sis.map.MapItem}.
 */
public class MapItem {
    /**
     * A human-readable short description for labeling the map item in a tree view.
     * This title should be user friendly. It shall not be used as an identifier.
     */
    public final String title;

    /**
     * Creates a new map item with the given text.
     *
     * @param  title  a human-readable short description for labeling the map item in a tree view.
     */
    public MapItem(final String title) {
        this.title = title;
    }
}
