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
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;


/**
 * From ISO/IEC 23001-17:2024 amendment 1
 * TODO : find box structure, it seems to have a variable size
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TAIClockInfo extends FullBox {

    public static final String FCC = "taic";

    public int timeUncertainty;
    public int clockResolution;
    public int clockDriftRate;
    public int unknown;
    public int clockType;

    @Override
    protected void readProperties(Reader reader) throws IOException {
        timeUncertainty = reader.channel.readInt();
        clockResolution = reader.channel.readInt();
        clockDriftRate = reader.channel.readInt();
        unknown = reader.channel.readInt();
        clockType = (int) reader.channel.readBits(2);
        reader.channel.skipRemainingBits();
    }

}
