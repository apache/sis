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
package org.apache.sis.storage.ubjson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.*;

/**
 * Jackson mapper implementation for UBJSON.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class UBJsonMapper extends ObjectMapper {

    public static class Builder extends MapperBuilder<UBJsonMapper, Builder> {

        Builder(UBJsonMapper mapper) {
            super(mapper);
        }
    }

    public UBJsonMapper() {
        this(new UBJsonFactory());
    }

    public UBJsonMapper(UBJsonFactory f) {
        super(f);
    }

    public static Builder builder() {
        return new Builder(new UBJsonMapper());
    }

    public static Builder builder(UBJsonFactory streamFactory) {
        return new Builder(new UBJsonMapper(streamFactory));
    }

    @Override
    public UBJsonMapper copy() {
        _checkInvalidCopy(UBJsonMapper.class);
        return new UBJsonMapper(tokenStreamFactory().copy());
    }

    // *********************************************************************
    // Basic accessor overrides
    // *********************************************************************

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public UBJsonFactory tokenStreamFactory() {
        return (UBJsonFactory) _jsonFactory;
    }
}
