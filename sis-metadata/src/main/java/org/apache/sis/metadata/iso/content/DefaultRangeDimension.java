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

import org.opengis.metadata.content.RangeDimension;
import org.opengis.util.InternationalString;
import org.opengis.util.MemberName;
import org.apache.sis.metadata.iso.ISOMetadata;

public class DefaultRangeDimension extends ISOMetadata implements RangeDimension {

    private MemberName sequenceIdentifier;

    private InternationalString descriptor;

    @Override
    public synchronized MemberName getSequenceIdentifier() {
        return sequenceIdentifier;
    }

    @Override
    public synchronized InternationalString getDescriptor() {
        return descriptor;
    }
}
