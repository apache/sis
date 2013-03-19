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
package org.apache.sis.metadata.iso.quality;

import java.util.Collection;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.opengis.metadata.quality.Scope;
import org.apache.sis.metadata.iso.ISOMetadata;

public class DefaultScope extends ISOMetadata implements Scope {

    private ScopeCode level;

    private Extent extent;

    private Collection<ScopeDescription> levelDescription;

    @Override
    public synchronized ScopeCode getLevel() {
        return level;
    }

    @Override
    public synchronized Collection<ScopeDescription> getLevelDescription() {
        return levelDescription;
    }

    @Override
    public synchronized Extent getExtent() {
        return extent;
    }
}
