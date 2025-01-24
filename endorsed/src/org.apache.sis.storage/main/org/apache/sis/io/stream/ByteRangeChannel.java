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
package org.apache.sis.io.stream;

import java.nio.channels.ReadableByteChannel;


/**
 * A byte channel where the range of bytes to read can be specified as a hint.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class ByteRangeChannel implements ReadableByteChannel {
    /**
     * Creates a new channel.
     */
    protected ByteRangeChannel() {
    }

    /**
     * Specifies a range of bytes which is expected to be read.
     * The range of bytes is only a hint and may be ignored, depending on subclasses.
     * Reading more bytes than specified is okay, only potentially less efficient.
     * It is okay for this method to do nothing.
     *
     * @param  lower  position (inclusive) of the first byte to be requested.
     * @param  upper  position (exclusive) of the last byte to be requested.
     */
    public abstract void rangeOfInterest(long lower, long upper);
}
