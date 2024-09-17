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
import org.apache.sis.storage.DataStoreException;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GSFRecordReader implements AutoCloseable {

    private final GSFStore store;
    private final Path file;
    private final GSF gsf;
    private final Arena arena;

    private final MemorySegment handle;
    private final int handleId;

    //singleton instance for iteration
    private DataID dataId;
    private Records records;
    private boolean needsFree = false;

    public GSFRecordReader(GSFStore store) throws DataStoreException {
        this.store = store;
        this.file = store.getComponentFiles()[0];
        this.gsf = store.getProvider().GSF();
        this.arena = Arena.ofShared();


        final MemorySegment address = arena.allocateFrom(file.toFile().toString(), StandardCharsets.UTF_8);
        handle = arena.allocate(4);
        try {
            final int error = gsf.open(address, GSF.GSF_READONLY, handle);
            if (error != 0) {
                arena.close();
                gsf.catchError(error);
            }
        } catch (Throwable ex) {
            throw new DataStoreException("Error while executing a GSFLib call.", ex);
        }
        handleId = handle.getAtIndex(ValueLayout.JAVA_INT, 0);
    }

    public Records next() throws DataStoreException {
        try {
            if (dataId == null) {
                dataId = new DataID(arena);
                records = new Records(arena);
                records.setDataId(dataId);
            } else {
                //clean record
                //TODO : we should do this, but GSFLib has pointer issues causing corrupted memory errors if we activate it
                //DEBUG : this error happens because GSFLib keeps an internal Records in a table for each opened file
                // and should copy the pointer in the return Records.
                // When we free the records, it frees the pointed tables but the pointers are not reset in the internal Records.
                //gsf.free(records.getMemorySegment());
            }
            needsFree = false;

            final int result = gsf.read(handleId, GSF.GSF_NEXT_RECORD, dataId.getMemorySegment(), records.getMemorySegment(), MemorySegment.NULL, 0);
            if (result == -1) {
                //error
                final int errorCode = gsf.intError();
                if (errorCode == -23 /*GSF_READ_TO_END_OF_FILE*/) {
                    return null;
                }
                gsf.catchError(errorCode);
            }
            needsFree = true;
            return records;
        } catch (Throwable ex) {
            throw new DataStoreException("Error while executing a GSFLib call.", ex);
        }
    }

    @Override
    public void close() throws Exception {
        final GSFException exception = new GSFException(0, "Failed to release all GSFLib resources.");

        //TODO See above comment line 75
//        if (needsFree) {
//            try {
//                gsf.free(records.getMemorySegment());
//            } catch (Throwable ex) {
//                exception.addSuppressed(ex);
//            }
//        }

        try {
            gsf.close(handleId);
        } catch (Throwable ex) {
            exception.addSuppressed(ex);
        }
        this.arena.close();

        if (exception.getSuppressed().length > 0) {
            throw exception;
        }
    }
}
