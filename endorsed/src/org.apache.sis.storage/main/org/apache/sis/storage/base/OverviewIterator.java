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
package org.apache.sis.storage.base;

import java.awt.image.RenderedImage;
import org.apache.sis.storage.DataStoreException;


/**
 * Provider of overviews for writing a pyramided image.
 * Overviews are returned from finest resolution to coarsest resolution.
 * They may be computed on the fly or read from an existing source.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public interface OverviewIterator {
    /**
     * Returns the next overview level, from finest resolution to coarsest resolution.
     *
     * @param  previous  the image at finer resolution which can be used as a base for the next overview.
     * @return the next overview level, or {@code null} if the iteration is finished.
     * @throws DataStoreException if the source of overviews cannot be read.
     */
    RenderedImage nextOverview(RenderedImage previous) throws DataStoreException;
}
