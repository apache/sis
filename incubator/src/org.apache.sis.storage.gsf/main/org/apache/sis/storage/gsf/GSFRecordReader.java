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
package org.apache.sis.storage.gsf;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.privy.AbstractIterator;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class GSFRecordReader {

    private final Path file;

    public GSFRecordReader(Path file) {
        this.file = file;
    }

    public Stream<Records> records() throws DataStoreException{

        final Arena arena = Arena.ofShared();
        final MemorySegment address = arena.allocateFrom(file.toFile().toString(), StandardCharsets.UTF_8);
        final MemorySegment handle = arena.allocate(4);
        final int error = GSF.gsfOpen(address, GSF.GSF_READONLY, handle);
        if (error != 0) {
            arena.close();
            throw new DataStoreException("GSF open error " + GSF.gsfIntError());
        }
        final int handleId = handle.getAtIndex(ValueLayout.JAVA_INT, 0);

        final Iterator ite = new AbstractIterator() {
            @Override
            public boolean hasNext() {
                if (next != null) return true;
                final DataID dataId = new DataID(arena);
                final Records records = new Records(arena);
                records.setDataId(dataId);
                final int error = GSF.gsfRead(handleId, GSF.GSF_NEXT_RECORD, dataId.getMemorySegment(), records.getMemorySegment(), MemorySegment.NULL, 0);
                if (error < 0) {
                    int errorCode = GSF.gsfIntError();
                    if (errorCode == GSF.GSF_READ_TO_END_OF_FILE) {
                        return false;
                    } else {
                        throw new RuntimeException(error + " GSF read error " + errorCode);
                    }
                }
                next = records;
                return true;
            }
        };

        final Spliterator<Records> spliterator = Spliterators.spliteratorUnknownSize(ite, Spliterator.ORDERED);
        final Stream<Records> stream = StreamSupport.stream(spliterator, false);
        return stream.onClose(new Runnable() {
            @Override
            public void run() {
                GSF.gsfClose(handleId);
                arena.close();
            }
        });
    }

}
