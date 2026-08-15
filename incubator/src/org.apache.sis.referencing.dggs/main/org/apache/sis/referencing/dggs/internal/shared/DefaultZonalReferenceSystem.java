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
package org.apache.sis.referencing.dggs.internal.shared;

import org.apache.sis.referencing.dggs.ZonalReferenceSystem;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultZonalReferenceSystem implements ZonalReferenceSystem {

    private final String identifier;
    private final String description;
    private final boolean supportUInt64;

    public DefaultZonalReferenceSystem(String identifier, String description, boolean supportUInt64) {
        this.identifier = identifier;
        this.description = description;
        this.supportUInt64 = supportUInt64;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean supportUInt64Form() {
        return supportUInt64;
    }

}
