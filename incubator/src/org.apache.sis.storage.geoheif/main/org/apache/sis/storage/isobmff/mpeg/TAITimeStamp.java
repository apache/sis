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
 * distributed under the License is distributed on an "AS IS" BASIS,z
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.isobmff.mpeg;

import java.io.IOException;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.base.ItemFullProperty;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TAITimeStamp extends ItemFullProperty {

    public static final String FCC = "itai";

    public long TAITimestamp;
    public boolean synchronizationState;
    public boolean timestampGenerationFailure;
    public boolean timestampIsModified;

    @Override
    protected void readProperties(Reader reader) throws IOException {
        TAITimestamp = reader.channel.readLong();
        synchronizationState = reader.channel.readBit() == 1;
        timestampGenerationFailure = reader.channel.readBit() == 1;
        timestampIsModified = reader.channel.readBit() == 1;
        reader.channel.skipRemainingBits();
    }

}
