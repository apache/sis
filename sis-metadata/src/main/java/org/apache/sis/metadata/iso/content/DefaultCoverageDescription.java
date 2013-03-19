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
package org.apache.sis.metadata.iso.content;

import java.util.Collection;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.RangeDimension;
import org.opengis.metadata.content.RangeElementDescription;
import org.opengis.util.RecordType;
import org.apache.sis.metadata.iso.ISOMetadata;


public class DefaultCoverageDescription extends ISOMetadata implements CoverageDescription {

    private RecordType attributeDescription;

    private CoverageContentType contentType;

    private Collection<RangeDimension> dimensions;

    private Collection<RangeElementDescription> rangeElementDescriptions;

    @Override
    public synchronized RecordType getAttributeDescription() {
        return attributeDescription;
    }

    @Override
    public synchronized CoverageContentType getContentType() {
        return contentType;
    }

    @Override
    public synchronized Collection<RangeDimension> getDimensions() {
        return dimensions;
    }

    @Override
    public synchronized Collection<RangeElementDescription> getRangeElementDescriptions() {
        return rangeElementDescriptions;
    }
}
