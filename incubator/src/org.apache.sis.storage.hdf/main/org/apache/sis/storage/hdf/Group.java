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
package org.apache.sis.storage.hdf;

import java.util.List;
import java.util.Map;
import org.apache.sis.storage.Aggregate;


/**
 * A group of an <abbr>HDF5</abbr> file, containing other groups and datasets.
 * The whole tree of groups is read when the {@link HDF5Store} is opened.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface Group extends Node, Aggregate {
    /**
     * Returns the groups and datasets which are hard-linked from this group.
     *
     * @return the children of this group, never {@code null}.
     */
    @Override
    List<Node> components();

    /**
     * Returns the node at the given path.
     *
     * @param  path  {@code '/'} separated path, relative to this group or absolute if starting with {@code '/'}.
     * @return the node at the given path, or {@code null} if none.
     */
    Node resolve(String path);

    /**
     * Returns the soft links of this group by link name.
     * Values are the paths of the targets, which are not resolved.
     *
     * @return the soft links of this group by link name, never {@code null}.
     */
    Map<String,String> getSoftLinks();
}
