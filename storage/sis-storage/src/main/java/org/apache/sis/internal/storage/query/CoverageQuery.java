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
package org.apache.sis.internal.storage.query;

import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.Query;
import org.apache.sis.util.ArgumentChecks;

/**
 * Define a simple query configuration for coverage resources.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class CoverageQuery extends Query {

    private final GridGeometry domain;
    private final int[] range;
    private final int margin;

    /**
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     */
    public CoverageQuery(GridGeometry domain, int... range) {
        this(domain, 0, range);
    }

    /**
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  margin  reading margin
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     */
    public CoverageQuery(GridGeometry domain, int margin, int... range) {
        ArgumentChecks.ensureNonNull("margin", margin);
        this.domain = domain;
        this.margin = margin;
        this.range = range;
    }

    /**
     * Returns desired grid extent and resolution.
     *
     * @return desired grid extent and resolution, or {@code null} for reading the whole domain.
     */
    public GridGeometry getDomain() {
        return domain;
    }

    /**
     * Returns indices of samples dimensions to read.
     *
     * @return 0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     */
    public int[] getRange() {
        return range;
    }

    /**
     * At reading time it may be necessary to add a margin to the read extent.
     *
     * <p>This margin is used knowing later image processing operations will use a window to
     * iterator over samples. As example Bilinear interpolation uses a 1 pixel
     * window.</p>
     *
     * @return read margin, zero or positive
     */
    public int getMargin() {
        return margin;
    }

}
