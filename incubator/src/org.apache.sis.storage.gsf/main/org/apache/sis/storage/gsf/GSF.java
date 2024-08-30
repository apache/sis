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

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.stream.Collectors;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GSF {

    static final Arena LIBRARY_ARENA = Arena.ofAuto();
    static final boolean TRACE_DOWNCALLS = Boolean.getBoolean("jextract.trace.downcalls");

    static void traceDowncall(String name, Object... args) {
         String traceArgs = Arrays.stream(args)
                       .map(Object::toString)
                       .collect(Collectors.joining(", "));
         System.out.printf("%s(%s)\n", name, traceArgs);
    }

    public static final ValueLayout.OfBoolean C_BOOL = ValueLayout.JAVA_BOOLEAN;
    public static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
    public static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT;
    public static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
    public static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
    public static final ValueLayout.OfFloat C_FLOAT = ValueLayout.JAVA_FLOAT;
    public static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
    public static final AddressLayout C_POINTER = ValueLayout.ADDRESS
            .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, JAVA_BYTE));
    public static final ValueLayout.OfLong C_LONG = ValueLayout.JAVA_LONG;

    static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup()
            .or(Linker.nativeLinker().defaultLookup());

    public static final MemorySegment NULL = MemorySegment.ofAddress(0L);
    public static final int EOF = -1;


    static MemorySegment findOrThrow(String symbol) {
        return SYMBOL_LOOKUP.find(symbol)
            .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + symbol));
    }

    private static class gsfOpen {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_INT,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfOpen");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfOpen(const char *filename, const int mode, int *handle)
     * }
     */
    public static int gsfOpen(MemorySegment filename, int mode, MemorySegment handle) {
        var mh$ = gsfOpen.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfOpen", filename, mode, handle);
            }
            return (int)mh$.invokeExact(filename, mode, handle);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfOpenBuffered {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_INT
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfOpenBuffered");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfOpenBuffered(const char *filename, const int mode, int *handle, int buf_size)
     * }
     */
    public static int gsfOpenBuffered(MemorySegment filename, int mode, MemorySegment handle, int buf_size) {
        var mh$ = gsfOpenBuffered.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfOpenBuffered", filename, mode, handle, buf_size);
            }
            return (int)mh$.invokeExact(filename, mode, handle, buf_size);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfClose {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfClose");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfClose(const int handle)
     * }
     */
    public static int gsfClose(int handle) {
        var mh$ = gsfClose.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfClose", handle);
            }
            return (int)mh$.invokeExact(handle);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfSeek {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_INT
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfSeek");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfSeek(int handle, int option)
     * }
     */
    public static int gsfSeek(int handle, int option) {
        var mh$ = gsfSeek.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfSeek", handle, option);
            }
            return (int)mh$.invokeExact(handle, option);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfRead {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER,
            GSF.C_POINTER,
            GSF.C_INT
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfRead");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfRead(int handle, int desiredRecord, gsfDataID *dataID, gsfRecords *rec, unsigned char *stream, int max_size)
     * }
     */
    public static int gsfRead(int handle, int desiredRecord, MemorySegment dataID, MemorySegment rec, MemorySegment stream, int max_size) {
        var mh$ = gsfRead.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfRead", handle, desiredRecord, dataID, rec, stream, max_size);
            }
            return (int)mh$.invokeExact(handle, desiredRecord, dataID, rec, stream, max_size);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfWrite {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfWrite");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfWrite(int handle, gsfDataID *id, gsfRecords *record)
     * }
     */
    public static int gsfWrite(int handle, MemorySegment id, MemorySegment record_) {
        var mh$ = gsfWrite.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfWrite", handle, id, record_);
            }
            return (int)mh$.invokeExact(handle, id, record_);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfLoadScaleFactor {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_INT,
            GSF.C_CHAR,
            GSF.C_DOUBLE,
            GSF.C_INT
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfLoadScaleFactor");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfLoadScaleFactor(gsfScaleFactors *sf, unsigned int subrecordID, char c_flag, double precision, int offset)
     * }
     */
    public static int gsfLoadScaleFactor(MemorySegment sf, int subrecordID, byte c_flag, double precision, int offset) {
        var mh$ = gsfLoadScaleFactor.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfLoadScaleFactor", sf, subrecordID, c_flag, precision, offset);
            }
            return (int)mh$.invokeExact(sf, subrecordID, c_flag, precision, offset);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfGetScaleFactor {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfGetScaleFactor");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfGetScaleFactor(int handle, unsigned int subrecordID, unsigned char *c_flag, double *multiplier, double *offset)
     * }
     */
    public static int gsfGetScaleFactor(int handle, int subrecordID, MemorySegment c_flag, MemorySegment multiplier, MemorySegment offset) {
        var mh$ = gsfGetScaleFactor.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfGetScaleFactor", handle, subrecordID, c_flag, multiplier, offset);
            }
            return (int)mh$.invokeExact(handle, subrecordID, c_flag, multiplier, offset);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfFree {
        public static final FunctionDescriptor DESC = FunctionDescriptor.ofVoid(GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfFree");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * void gsfFree(gsfRecords *rec)
     * }
     */
    public static void gsfFree(MemorySegment rec) {
        var mh$ = gsfFree.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfFree", rec);
            }
            mh$.invokeExact(rec);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfPrintError {
        public static final FunctionDescriptor DESC = FunctionDescriptor.ofVoid(GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfPrintError");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * void gsfPrintError(FILE *fp)
     * }
     */
    public static void gsfPrintError(MemorySegment fp) {
        var mh$ = gsfPrintError.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfPrintError", fp);
            }
            mh$.invokeExact(fp);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfIntError {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT    );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfIntError");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfIntError()
     * }
     */
    public static int gsfIntError() {
        var mh$ = gsfIntError.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfIntError");
            }
            return (int)mh$.invokeExact();
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfStringError {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_POINTER    );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfStringError");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * const char *gsfStringError()
     * }
     */
    public static MemorySegment gsfStringError() {
        var mh$ = gsfStringError.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfStringError");
            }
            return (MemorySegment)mh$.invokeExact();
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfIndexTime {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfIndexTime");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfIndexTime(int handle, int recordID, int record_number, time_t *sec, long *nsec)
     * }
     */
    public static int gsfIndexTime(int handle, int recordID, int record_number, MemorySegment sec, MemorySegment nsec) {
        var mh$ = gsfIndexTime.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfIndexTime", handle, recordID, record_number, sec, nsec);
            }
            return (int)mh$.invokeExact(handle, recordID, record_number, sec, nsec);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfPercent {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfPercent");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfPercent(int handle)
     * }
     */
    public static int gsfPercent(int handle) {
        var mh$ = gsfPercent.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfPercent", handle);
            }
            return (int)mh$.invokeExact(handle);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfGetNumberRecords {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_INT
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfGetNumberRecords");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfGetNumberRecords(int handle, int desiredRecord)
     * }
     */
    public static int gsfGetNumberRecords(int handle, int desiredRecord) {
        var mh$ = gsfGetNumberRecords.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfGetNumberRecords", handle, desiredRecord);
            }
            return (int)mh$.invokeExact(handle, desiredRecord);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfCopyRecords {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfCopyRecords");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfCopyRecords(gsfRecords *target, const gsfRecords *source)
     * }
     */
    public static int gsfCopyRecords(MemorySegment target, MemorySegment source) {
        var mh$ = gsfCopyRecords.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfCopyRecords", target, source);
            }
            return (int)mh$.invokeExact(target, source);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfPutMBParams {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER,
            GSF.C_INT,
            GSF.C_INT
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfPutMBParams");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfPutMBParams(const gsfMBParams *p, gsfRecords *rec, int handle, int numArrays)
     * }
     */
    public static int gsfPutMBParams(MemorySegment p, MemorySegment rec, int handle, int numArrays) {
        var mh$ = gsfPutMBParams.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfPutMBParams", p, rec, handle, numArrays);
            }
            return (int)mh$.invokeExact(p, rec, handle, numArrays);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfGetMBParams {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfGetMBParams");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfGetMBParams(const gsfRecords *rec, gsfMBParams *p, int *numArrays)
     * }
     */
    public static int gsfGetMBParams(MemorySegment rec, MemorySegment p, MemorySegment numArrays) {
        var mh$ = gsfGetMBParams.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfGetMBParams", rec, p, numArrays);
            }
            return (int)mh$.invokeExact(rec, p, numArrays);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfGetSwathBathyBeamWidths {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfGetSwathBathyBeamWidths");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfGetSwathBathyBeamWidths(const gsfRecords *data, double *fore_aft, double *athwartship)
     * }
     */
    public static int gsfGetSwathBathyBeamWidths(MemorySegment data, MemorySegment fore_aft, MemorySegment athwartship) {
        var mh$ = gsfGetSwathBathyBeamWidths.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfGetSwathBathyBeamWidths", data, fore_aft, athwartship);
            }
            return (int)mh$.invokeExact(data, fore_aft, athwartship);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfIsStarboardPing {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfIsStarboardPing");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfIsStarboardPing(const gsfRecords *data)
     * }
     */
    public static int gsfIsStarboardPing(MemorySegment data) {
        var mh$ = gsfIsStarboardPing.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfIsStarboardPing", data);
            }
            return (int)mh$.invokeExact(data);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfLoadDepthScaleFactorAutoOffset {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_INT,
            GSF.C_INT,
            GSF.C_DOUBLE,
            GSF.C_DOUBLE,
            GSF.C_POINTER,
            GSF.C_CHAR,
            GSF.C_DOUBLE
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfLoadDepthScaleFactorAutoOffset");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfLoadDepthScaleFactorAutoOffset(gsfSwathBathyPing *ping, unsigned int subrecordID, int reset, double min_depth, double max_depth, double *last_corrector, char c_flag, double precision)
     * }
     */
    public static int gsfLoadDepthScaleFactorAutoOffset(MemorySegment ping, int subrecordID, int reset, double min_depth, double max_depth, MemorySegment last_corrector, byte c_flag, double precision) {
        var mh$ = gsfLoadDepthScaleFactorAutoOffset.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfLoadDepthScaleFactorAutoOffset", ping, subrecordID, reset, min_depth, max_depth, last_corrector, c_flag, precision);
            }
            return (int)mh$.invokeExact(ping, subrecordID, reset, min_depth, max_depth, last_corrector, c_flag, precision);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfGetSwathBathyArrayMinMax {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfGetSwathBathyArrayMinMax");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfGetSwathBathyArrayMinMax(const gsfSwathBathyPing *ping, unsigned int subrecordID, double *min_value, double *max_value)
     * }
     */
    public static int gsfGetSwathBathyArrayMinMax(MemorySegment ping, int subrecordID, MemorySegment min_value, MemorySegment max_value) {
        var mh$ = gsfGetSwathBathyArrayMinMax.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfGetSwathBathyArrayMinMax", ping, subrecordID, min_value, max_value);
            }
            return (int)mh$.invokeExact(ping, subrecordID, min_value, max_value);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfGetSonarTextName {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfGetSonarTextName");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * const char *gsfGetSonarTextName(const gsfSwathBathyPing *ping)
     * }
     */
    public static MemorySegment gsfGetSonarTextName(MemorySegment ping) {
        var mh$ = gsfGetSonarTextName.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfGetSonarTextName", ping);
            }
            return (MemorySegment)mh$.invokeExact(ping);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfFileSupportsRecalculateXYZ {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfFileSupportsRecalculateXYZ");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfFileSupportsRecalculateXYZ(int handle, int *status)
     * }
     */
    public static int gsfFileSupportsRecalculateXYZ(int handle, MemorySegment status) {
        var mh$ = gsfFileSupportsRecalculateXYZ.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfFileSupportsRecalculateXYZ", handle, status);
            }
            return (int)mh$.invokeExact(handle, status);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfFileSupportsRecalculateTPU {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfFileSupportsRecalculateTPU");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfFileSupportsRecalculateTPU(int handle, int *status)
     * }
     */
    public static int gsfFileSupportsRecalculateTPU(int handle, MemorySegment status) {
        var mh$ = gsfFileSupportsRecalculateTPU.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfFileSupportsRecalculateTPU", handle, status);
            }
            return (int)mh$.invokeExact(handle, status);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfFileSupportsRecalculateNominalDepth {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfFileSupportsRecalculateNominalDepth");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfFileSupportsRecalculateNominalDepth(int handle, int *status)
     * }
     */
    public static int gsfFileSupportsRecalculateNominalDepth(int handle, MemorySegment status) {
        var mh$ = gsfFileSupportsRecalculateNominalDepth.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfFileSupportsRecalculateNominalDepth", handle, status);
            }
            return (int)mh$.invokeExact(handle, status);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfFileContainsMBAmplitude {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfFileContainsMBAmplitude");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfFileContainsMBAmplitude(int handle, int *status)
     * }
     */
    public static int gsfFileContainsMBAmplitude(int handle, MemorySegment status) {
        var mh$ = gsfFileContainsMBAmplitude.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfFileContainsMBAmplitude", handle, status);
            }
            return (int)mh$.invokeExact(handle, status);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfFileContainsMBImagery {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfFileContainsMBImagery");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfFileContainsMBImagery(int handle, int *status)
     * }
     */
    public static int gsfFileContainsMBImagery(int handle, MemorySegment status) {
        var mh$ = gsfFileContainsMBImagery.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfFileContainsMBImagery", handle, status);
            }
            return (int)mh$.invokeExact(handle, status);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfIsNewSurveyLine {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_DOUBLE,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfIsNewSurveyLine");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfIsNewSurveyLine(int handle, const gsfRecords *rec, double azimuth_change, double *last_heading)
     * }
     */
    public static int gsfIsNewSurveyLine(int handle, MemorySegment rec, double azimuth_change, MemorySegment last_heading) {
        var mh$ = gsfIsNewSurveyLine.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfIsNewSurveyLine", handle, rec, azimuth_change, last_heading);
            }
            return (int)mh$.invokeExact(handle, rec, azimuth_change, last_heading);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfInitializeMBParams {
        public static final FunctionDescriptor DESC = FunctionDescriptor.ofVoid(GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfInitializeMBParams");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * void gsfInitializeMBParams(gsfMBParams *p)
     * }
     */
    public static void gsfInitializeMBParams(MemorySegment p) {
        var mh$ = gsfInitializeMBParams.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfInitializeMBParams", p);
            }
            mh$.invokeExact(p);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfStat {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_INT,
            GSF.C_POINTER,
            GSF.C_POINTER
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfStat");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * int gsfStat(const char *filename, long long *sz)
     * }
     */
    public static int gsfStat(MemorySegment filename, MemorySegment sz) {
        var mh$ = gsfStat.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfStat", filename, sz);
            }
            return (int)mh$.invokeExact(filename, sz);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfGetPositionDestination {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_POINTER,
            Position.LAYOUT,
            PositionOffsets.LAYOUT,
            GSF.C_DOUBLE,
            GSF.C_DOUBLE
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfGetPositionDestination");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * GSF_POSITION *gsfGetPositionDestination(GSF_POSITION gp, GSF_POSITION_OFFSETS offsets, double hdg, double dist_step)
     * }
     */
    public static MemorySegment gsfGetPositionDestination(MemorySegment gp, MemorySegment offsets, double hdg, double dist_step) {
        var mh$ = gsfGetPositionDestination.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfGetPositionDestination", gp, offsets, hdg, dist_step);
            }
            return (MemorySegment)mh$.invokeExact(gp, offsets, hdg, dist_step);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    private static class gsfGetPositionOffsets {
        public static final FunctionDescriptor DESC = FunctionDescriptor.of(GSF.C_POINTER,
            Position.LAYOUT,
            Position.LAYOUT,
            GSF.C_DOUBLE,
            GSF.C_DOUBLE
        );

        public static final MemorySegment ADDR = GSF.findOrThrow("gsfGetPositionOffsets");

        public static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(ADDR, DESC);
    }

    /**
     * {@snippet lang=c :
     * GSF_POSITION_OFFSETS *gsfGetPositionOffsets(GSF_POSITION gp_from, GSF_POSITION gp_to, double hdg, double dist_step)
     * }
     */
    public static MemorySegment gsfGetPositionOffsets(MemorySegment gp_from, MemorySegment gp_to, double hdg, double dist_step) {
        var mh$ = gsfGetPositionOffsets.HANDLE;
        try {
            if (TRACE_DOWNCALLS) {
                traceDowncall("gsfGetPositionOffsets", gp_from, gp_to, hdg, dist_step);
            }
            return (MemorySegment)mh$.invokeExact(gp_from, gp_to, hdg, dist_step);
        } catch (Throwable ex$) {
           throw new AssertionError("should not reach here", ex$);
        }
    }

    public static final int GSF_MAX_RECORD_SIZE = 524288;
    public static final int GSF_MAX_OPEN_FILES = 4;
    public static final int GSF_CREATE = 1;
    public static final int GSF_READONLY = 2;
    public static final int GSF_UPDATE = 3;
    public static final int GSF_READONLY_INDEX = 4;
    public static final int GSF_UPDATE_INDEX = 5;
    public static final int GSF_APPEND = 6;
    public static final int GSF_REWIND = 1;
    public static final int GSF_END_OF_FILE = 2;
    public static final int GSF_PREVIOUS_RECORD = 3;
    public static final int GSF_SHORT_SIZE = 2;
    public static final int GSF_LONG_SIZE = 4;
    public static final int GSF_NEXT_RECORD = 0;
    public static final int NUM_REC_TYPES = 13;
    public static final int GSF_MAX_PING_ARRAY_SUBRECORDS = 30;
    public static final int GSF_VERSION_SIZE = 12;
    public static final int GSF_SEABAT_WIDE_MODE = 1;
    public static final int GSF_SEABAT_9002 = 2;
    public static final int GSF_SEABAT_STBD_HEAD = 4;
    public static final int GSF_SEABAT_9003 = 8;
    public static final int GSF_8101_WIDE_MODE = 1;
    public static final int GSF_8101_TWO_HEADS = 2;
    public static final int GSF_8101_STBD_HEAD = 4;
    public static final int GSF_8101_AMPLITUDE = 8;
    public static final int GSF_7100_PITCH_STAB = 1;
    public static final int GSF_7100_ROLL_STAB = 1;
    public static final int GSF_2112_SVP_CORRECTION = 1;
    public static final int GSF_2112_LOW_FREQUENCY = 2;
    public static final int GSF_2112_AUTO_DEPTH_GATE = 4;
    public static final int GSF_2112_POOR_QUALITY = 1;
    public static final int GSF_2112_DATA_SOURCE_WMT = 16;
    public static final int GSF_MKII_LOW_FREQUENCY = 1;
    public static final int GSF_MKII_SOURCE_MODE = 2;
    public static final int GSF_MKII_SOURCE_POWER = 4;
    public static final int GSF_MKII_STBD_HEAD = 8;
    public static final int GSF_8100_WIDE_MODE = 1;
    public static final int GSF_8100_TWO_HEADS = 2;
    public static final int GSF_8100_STBD_HEAD = 4;
    public static final int GSF_8100_AMPLITUDE = 8;
    public static final int GSF_8100_PITCH_STAB = 16;
    public static final int GSF_8100_ROLL_STAB = 32;
    public static final int GSF_SB_MPP_SOURCE_UNKNOWN = 0;
    public static final int GSF_SB_MPP_SOURCE_GPS_3S = 1;
    public static final int GSF_SB_MPP_SOURCE_GPS_TASMAN = 2;
    public static final int GSF_SB_MPP_SOURCE_DGPS_TRIMBLE = 3;
    public static final int GSF_SB_MPP_SOURCE_DGPS_TASMAN = 4;
    public static final int GSF_SB_MPP_SOURCE_DGPS_MAG = 5;
    public static final int GSF_SB_MPP_SOURCE_RANGE_MFIX = 6;
    public static final int GSF_SB_MPP_SOURCE_RANGE_TRIS = 7;
    public static final int GSF_SB_MPP_SOURCE_RANGE_OTHER = 8;
    public static final int GSF_MAX_EM4_SECTORS = 9;
    public static final int GSF_MAX_EM3_SECTORS = 20;
    public static final int GSF_MAX_KMALL_SECTORS = 9;
    public static final int GSF_EM_WAVEFORM_CW = 0;
    public static final int GSF_EM_WAVEFORM_FM_UP = 1;
    public static final int GSF_EM_WAVEFORM_FM_DOWN = 2;
    public static final int GSF_EM_MODE_VERY_SHALLOW = 0;
    public static final int GSF_EM_MODE_SHALLOW = 1;
    public static final int GSF_EM_MODE_MEDIUM = 2;
    public static final int GSF_EM_MODE_DEEP = 3;
    public static final int GSF_EM_MODE_VERY_DEEP = 4;
    public static final int GSF_EM_MODE_EXTRA_DEEP = 5;
    public static final int GSF_EM_MODE_MASK = 7;
    public static final int GSF_EM_MODE_DS_OFF = 192;
    public static final int GSF_EM_MODE_DS_FIXED = 64;
    public static final int GSF_EM_MODE_DS_DYNAMIC = 128;
    public static final int GSF_EM_VALID_1_PPS = 1;
    public static final int GSF_EM_VALID_POSITION = 2;
    public static final int GSF_EM_VALID_ATTITUDE = 4;
    public static final int GSF_EM_VALID_CLOCK = 8;
    public static final int GSF_EM_VALID_HEADING = 16;
    public static final int GSF_EM_PU_ACTIVE = 32;
    public static final int GSF_MAX_KMALL_EXTRA_CLASSES = 11;
    public static final int GSF_MAX_KMALL_EXTRA_DETECT = 1024;
    public static final int GSF_KMALL_MRZ = 1;
    public static final int PORT_PING = 0;
    public static final int STBD_PING = 1;
    public static final int GSF_FIELD_SIZE_DEFAULT = 0;
    public static final int GSF_FIELD_SIZE_ONE = 16;
    public static final int GSF_FIELD_SIZE_TWO = 32;
    public static final int GSF_FIELD_SIZE_FOUR = 64;
    public static final int GSF_FIELD_SIZE_COUNT = 3;
    public static final int GSF_DISABLE_COMPRESSION = 0;
    public static final int GSF_ENABLE_COMPRESSION = 1;
    public static final int GSF_MAX_PROCESSING_PARAMETERS = 128;
    public static final int GSF_MAX_SENSOR_PARAMETERS = 128;
    public static final int GSF_OPERATOR_LENGTH = 64;
    public static final int GSF_HOST_NAME_LENGTH = 64;
    public static final int GSF_MAX_OFFSETS = 2;
    public static final int GSF_COMPENSATED = 1;
    public static final int GSF_UNCOMPENSATED = 0;
    public static final int GSF_TRUE_DEPTHS = 1;
    public static final int GSF_DEPTHS_RE_1500_MS = 2;
    public static final int GSF_DEPTH_CALC_UNKNOWN = 3;
    public static final int GSF_TRUE = 1;
    public static final int GSF_FALSE = 0;
    public static final int GSF_NUMBER_PROCESSING_PARAMS = 49;
    public static final int GSF_PLATFORM_TYPE_SURFACE_SHIP = 0;
    public static final int GSF_PLATFORM_TYPE_AUV = 1;
    public static final int GSF_PLATFORM_TYPE_ROTV = 2;
    public static final int GSF_HORIZONTAL_PITCH_AXIS = 1;
    public static final int GSF_ROTATED_PITCH_AXIS = 2;
    public static final int GSF_H_DATUM_ADI = 1;
    public static final int GSF_H_DATUM_ARF = 2;
    public static final int GSF_H_DATUM_ARS = 3;
    public static final int GSF_H_DATUM_AUA = 4;
    public static final int GSF_H_DATUM_BAT = 5;
    public static final int GSF_H_DATUM_BID = 6;
    public static final int GSF_H_DATUM_BUR = 7;
    public static final int GSF_H_DATUM_CAI = 8;
    public static final int GSF_H_DATUM_CAM = 9;
    public static final int GSF_H_DATUM_CAP = 10;
    public static final int GSF_H_DATUM_CAA = 11;
    public static final int GSF_H_DATUM_CHO = 12;
    public static final int GSF_H_DATUM_CHU = 13;
    public static final int GSF_H_DATUM_COA = 14;
    public static final int GSF_H_DATUM_ENB = 15;
    public static final int GSF_H_DATUM_EUR = 16;
    public static final int GSF_H_DATUM_GDA = 17;
    public static final int GSF_H_DATUM_GEO = 18;
    public static final int GSF_H_DATUM_GHA = 19;
    public static final int GSF_H_DATUM_GSB = 20;
    public static final int GSF_H_DATUM_GSF = 21;
    public static final int GSF_H_DATUM_GUA = 22;
    public static final int GSF_H_DATUM_HEN = 23;
    public static final int GSF_H_DATUM_HER = 24;
    public static final int GSF_H_DATUM_HJO = 25;
    public static final int GSF_H_DATUM_HTN = 26;
    public static final int GSF_H_DATUM_IDA = 27;
    public static final int GSF_H_DATUM_IND = 28;
    public static final int GSF_H_DATUM_IRE = 29;
    public static final int GSF_H_DATUM_KEA = 30;
    public static final int GSF_H_DATUM_LIB = 31;
    public static final int GSF_H_DATUM_LOC = 32;
    public static final int GSF_H_DATUM_LUZ = 33;
    public static final int GSF_H_DATUM_MER = 34;
    public static final int GSF_H_DATUM_MET = 35;
    public static final int GSF_H_DATUM_MOL = 36;
    public static final int GSF_H_DATUM_NAN = 37;
    public static final int GSF_H_DATUM_NAR = 38;
    public static final int GSF_H_DATUM_NAS = 39;
    public static final int GSF_H_DATUM_NIG = 40;
    public static final int GSF_H_DATUM_OGB = 41;
    public static final int GSF_H_DATUM_OHA = 42;
    public static final int GSF_H_DATUM_OSI = 43;
    public static final int GSF_H_DATUM_PLN = 44;
    public static final int GSF_H_DATUM_PRP = 45;
    public static final int GSF_H_DATUM_QUO = 46;
    public static final int GSF_H_DATUM_SIB = 47;
    public static final int GSF_H_DATUM_TAN = 48;
    public static final int GSF_H_DATUM_TIL = 49;
    public static final int GSF_H_DATUM_TOK = 50;
    public static final int GSF_H_DATUM_UND = 51;
    public static final int GSF_H_DATUM_VOI = 52;
    public static final int GSF_H_DATUM_WGA = 53;
    public static final int GSF_H_DATUM_WGB = 54;
    public static final int GSF_H_DATUM_WGC = 55;
    public static final int GSF_H_DATUM_WGD = 56;
    public static final int GSF_H_DATUM_WGE = 57;
    public static final int GSF_H_DATUM_WGS = 58;
    public static final int GSF_H_DATUM_XXX = 59;
    public static final int GSF_H_DATUM_YAC = 60;
    public static final int GSF_V_DATUM_UNKNOWN = 1;
    public static final int GSF_V_DATUM_MLLW = 2;
    public static final int GSF_V_DATUM_MLW = 3;
    public static final int GSF_V_DATUM_ALAT = 4;
    public static final int GSF_V_DATUM_ESLW = 5;
    public static final int GSF_V_DATUM_ISLW = 6;
    public static final int GSF_V_DATUM_LAT = 7;
    public static final int GSF_V_DATUM_LLW = 8;
    public static final int GSF_V_DATUM_LNLW = 9;
    public static final int GSF_V_DATUM_LWD = 10;
    public static final int GSF_V_DATUM_MLHW = 11;
    public static final int GSF_V_DATUM_MLLWS = 12;
    public static final int GSF_V_DATUM_MLWN = 13;
    public static final int GSF_V_DATUM_MSL = 14;
    public static final int GSF_V_DATUM_ALLW = 15;
    public static final int GSF_V_DATUM_LNT = 16;
    public static final int GSF_V_DATUM_AMLWS = 17;
    public static final int GSF_V_DATUM_AMLLW = 18;
    public static final int GSF_V_DATUM_MLWS = 19;
    public static final int GSF_V_DATUM_AMSL = 20;
    public static final int GSF_V_DATUM_AMLW = 21;
    public static final int GSF_V_DATUM_AISLW = 22;
    public static final int GSF_V_DATUM_ALLWS = 23;
    public static final int GSF_NORMAL = 0;

    public static final int FLT_EVAL_METHOD = 0;
    public static final int FLT_RADIX = 2;
    public static final int FLT_MANT_DIG = 24;
    public static final int DBL_MANT_DIG = 53;
    public static final int LDBL_MANT_DIG = 64;
    public static final int DECIMAL_DIG = 21;
    public static final int FLT_DIG = 6;
    public static final int DBL_DIG = 15;
    public static final int LDBL_DIG = 18;
    public static final int FLT_MIN_EXP = -125;
    public static final int DBL_MIN_EXP = -1021;
    public static final int LDBL_MIN_EXP = -16381;
    public static final int FLT_MIN_10_EXP = -37;
    public static final int DBL_MIN_10_EXP = -307;
    public static final int LDBL_MIN_10_EXP = -4931;
    public static final int FLT_MAX_EXP = 128;
    public static final int DBL_MAX_EXP = 1024;
    public static final int LDBL_MAX_EXP = 16384;
    public static final int FLT_MAX_10_EXP = 38;
    public static final int DBL_MAX_10_EXP = 308;
    public static final int LDBL_MAX_10_EXP = 4932;
    public static final float FLT_MAX = 3.4028234663852886E38f;
    public static final double DBL_MAX = 1.7976931348623157E308d;
    public static final float FLT_EPSILON = 1.1920928955078125E-7f;
    public static final double DBL_EPSILON = 2.220446049250313E-16d;
    public static final float FLT_MIN = 1.1754943508222875E-38f;
    public static final double DBL_MIN = 2.2250738585072014E-308d;
    public static final float FLT_TRUE_MIN = 1.401298464324817E-45f;
    public static final double DBL_TRUE_MIN = 4.9E-324d;
    public static final int FLT_DECIMAL_DIG = 9;
    public static final int DBL_DECIMAL_DIG = 17;
    public static final int LDBL_DECIMAL_DIG = 21;
    public static final int FLT_HAS_SUBNORM = 1;
    public static final int DBL_HAS_SUBNORM = 1;
    public static final int LDBL_HAS_SUBNORM = 1;
    public static final int LITTLE_ENDIAN = 1234;
    public static final int BIG_ENDIAN = 4321;
    public static final int PDP_ENDIAN = 3412;
    public static final int BYTE_ORDER = 1234;
    public static final long _SIGSET_NWORDS = 16;
    public static final int __NFDBITS = 64;
    public static final int FD_SETSIZE = 1024;
    public static final int NFDBITS = 64;
    public static final int __PTHREAD_RWLOCK_ELISION_EXTRA = 0;
    /**
     * {@snippet lang=c :
     * #define GSF_MAJOR_VERSION "03"
     * }
     */
    public static MemorySegment GSF_MAJOR_VERSION() {
        class Holder {
            static final MemorySegment GSF_MAJOR_VERSION
                = GSF.LIBRARY_ARENA.allocateFrom("03");
        }
        return Holder.GSF_MAJOR_VERSION;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_MINOR_VERSION "09"
     * }
     */
    public static MemorySegment GSF_MINOR_VERSION() {
        class Holder {
            static final MemorySegment GSF_MINOR_VERSION
                = GSF.LIBRARY_ARENA.allocateFrom("09");
        }
        return Holder.GSF_MINOR_VERSION;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_VERSION "GSF-v03.09"
     * }
     */
    public static MemorySegment GSF_VERSION() {
        class Holder {
            static final MemorySegment GSF_VERSION
                = GSF.LIBRARY_ARENA.allocateFrom("GSF-v03.09");
        }
        return Holder.GSF_VERSION;
    }
    public static final int GSF_RECORD_HEADER = 1;
    public static final int GSF_RECORD_SWATH_BATHYMETRY_PING = 2;
    public static final int GSF_RECORD_SOUND_VELOCITY_PROFILE = 3;
    public static final int GSF_RECORD_PROCESSING_PARAMETERS = 4;
    public static final int GSF_RECORD_SENSOR_PARAMETERS = 5;
    public static final int GSF_RECORD_COMMENT = 6;
    public static final int GSF_RECORD_HISTORY = 7;
    public static final int GSF_RECORD_NAVIGATION_ERROR = 8;
    public static final int GSF_RECORD_SWATH_BATHY_SUMMARY = 9;
    public static final int GSF_RECORD_SINGLE_BEAM_PING = 10;
    public static final int GSF_RECORD_HV_NAVIGATION_ERROR = 11;
    public static final int GSF_RECORD_ATTITUDE = 12;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DEPTH_ARRAY = 1;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ACROSS_TRACK_ARRAY = 2;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ALONG_TRACK_ARRAY = 3;
    public static final int GSF_SWATH_BATHY_SUBRECORD_TRAVEL_TIME_ARRAY = 4;
    public static final int GSF_SWATH_BATHY_SUBRECORD_BEAM_ANGLE_ARRAY = 5;
    public static final int GSF_SWATH_BATHY_SUBRECORD_MEAN_CAL_AMPLITUDE_ARRAY = 6;
    public static final int GSF_SWATH_BATHY_SUBRECORD_MEAN_REL_AMPLITUDE_ARRAY = 7;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ECHO_WIDTH_ARRAY = 8;
    public static final int GSF_SWATH_BATHY_SUBRECORD_QUALITY_FACTOR_ARRAY = 9;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RECEIVE_HEAVE_ARRAY = 10;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DEPTH_ERROR_ARRAY = 11;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ACROSS_TRACK_ERROR_ARRAY = 12;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ALONG_TRACK_ERROR_ARRAY = 13;
    public static final int GSF_SWATH_BATHY_SUBRECORD_NOMINAL_DEPTH_ARRAY = 14;
    public static final int GSF_SWATH_BATHY_SUBRECORD_QUALITY_FLAGS_ARRAY = 15;
    public static final int GSF_SWATH_BATHY_SUBRECORD_BEAM_FLAGS_ARRAY = 16;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SIGNAL_TO_NOISE_ARRAY = 17;
    public static final int GSF_SWATH_BATHY_SUBRECORD_BEAM_ANGLE_FORWARD_ARRAY = 18;
    public static final int GSF_SWATH_BATHY_SUBRECORD_VERTICAL_ERROR_ARRAY = 19;
    public static final int GSF_SWATH_BATHY_SUBRECORD_HORIZONTAL_ERROR_ARRAY = 20;
    public static final int GSF_SWATH_BATHY_SUBRECORD_INTENSITY_SERIES_ARRAY = 21;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SECTOR_NUMBER_ARRAY = 22;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DETECTION_INFO_ARRAY = 23;
    public static final int GSF_SWATH_BATHY_SUBRECORD_INCIDENT_BEAM_ADJ_ARRAY = 24;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SYSTEM_CLEANING_ARRAY = 25;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DOPPLER_CORRECTION_ARRAY = 26;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SONAR_VERT_UNCERT_ARRAY = 27;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SONAR_HORZ_UNCERT_ARRAY = 28;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DETECTION_WINDOW_ARRAY = 29;
    public static final int GSF_SWATH_BATHY_SUBRECORD_MEAN_ABS_COEF_ARRAY = 30;
    public static final int GSF_SWATH_BATHY_SUBRECORD_UNKNOWN = 0;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SCALE_FACTORS = 100;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABEAM_SPECIFIC = 102;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM12_SPECIFIC = 103;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM100_SPECIFIC = 104;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM950_SPECIFIC = 105;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM121A_SPECIFIC = 106;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM121_SPECIFIC = 107;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SASS_SPECIFIC = 108;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEAMAP_SPECIFIC = 109;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABAT_SPECIFIC = 110;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM1000_SPECIFIC = 111;
    public static final int GSF_SWATH_BATHY_SUBRECORD_TYPEIII_SEABEAM_SPECIFIC = 112;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SB_AMP_SPECIFIC = 113;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABAT_II_SPECIFIC = 114;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABAT_8101_SPECIFIC = 115;
    public static final int GSF_SWATH_BATHY_SUBRECORD_SEABEAM_2112_SPECIFIC = 116;
    public static final int GSF_SWATH_BATHY_SUBRECORD_ELAC_MKII_SPECIFIC = 117;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3000_SPECIFIC = 118;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM1002_SPECIFIC = 119;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM300_SPECIFIC = 120;
    public static final int GSF_SWATH_BATHY_SUBRECORD_CMP_SASS_SPECIFIC = 121;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8101_SPECIFIC = 122;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8111_SPECIFIC = 123;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8124_SPECIFIC = 124;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8125_SPECIFIC = 125;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8150_SPECIFIC = 126;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_8160_SPECIFIC = 127;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM120_SPECIFIC = 128;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3002_SPECIFIC = 129;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3000D_SPECIFIC = 130;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3002D_SPECIFIC = 131;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM121A_SIS_SPECIFIC = 132;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM710_SPECIFIC = 133;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM302_SPECIFIC = 134;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM122_SPECIFIC = 135;
    public static final int GSF_SWATH_BATHY_SUBRECORD_GEOSWATH_PLUS_SPECIFIC = 136;
    public static final int GSF_SWATH_BATHY_SUBRECORD_KLEIN_5410_BSS_SPECIFIC = 137;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_7125_SPECIFIC = 138;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM2000_SPECIFIC = 139;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM300_RAW_SPECIFIC = 140;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM1002_RAW_SPECIFIC = 141;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM2000_RAW_SPECIFIC = 142;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3000_RAW_SPECIFIC = 143;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM120_RAW_SPECIFIC = 144;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3002_RAW_SPECIFIC = 145;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3000D_RAW_SPECIFIC = 146;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM3002D_RAW_SPECIFIC = 147;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM121A_SIS_RAW_SPECIFIC = 148;
    public static final int GSF_SWATH_BATHY_SUBRECORD_EM2040_SPECIFIC = 149;
    public static final int GSF_SWATH_BATHY_SUBRECORD_DELTA_T_SPECIFIC = 150;
    public static final int GSF_SWATH_BATHY_SUBRECORD_R2SONIC_2022_SPECIFIC = 151;
    public static final int GSF_SWATH_BATHY_SUBRECORD_R2SONIC_2024_SPECIFIC = 152;
    public static final int GSF_SWATH_BATHY_SUBRECORD_R2SONIC_2020_SPECIFIC = 153;
    public static final int GSF_SWATH_BATHY_SUBRECORD_RESON_TSERIES_SPECIFIC = 155;
    public static final int GSF_SWATH_BATHY_SUBRECORD_KMALL_SPECIFIC = 156;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_UNKNOWN = 0;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_ECHOTRAC_SPECIFIC = 201;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_BATHY2000_SPECIFIC = 202;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_MGD77_SPECIFIC = 203;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_BDB_SPECIFIC = 204;
    public static final int GSF_SINGLE_BEAM_SUBRECORD_NOSHDB_SPECIFIC = 205;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_ECHOTRAC_SPECIFIC = 206;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_BATHY2000_SPECIFIC = 207;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_MGD77_SPECIFIC = 208;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_BDB_SPECIFIC = 209;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_NOSHDB_SPECIFIC = 210;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_PDD_SPECIFIC = 211;
    public static final int GSF_SWATH_BATHY_SB_SUBRECORD_NAVISOUND_SPECIFIC = 212;
    public static final double GSF_NULL_LATITUDE = 91.0d;
    public static final double GSF_NULL_LONGITUDE = 181.0d;
    public static final double GSF_NULL_HEADING = 361.0d;
    public static final double GSF_NULL_COURSE = 361.0d;
    public static final double GSF_NULL_SPEED = 99.0d;
    public static final double GSF_NULL_PITCH = 99.0d;
    public static final double GSF_NULL_ROLL = 99.0d;
    public static final double GSF_NULL_HEAVE = 99.0d;
    public static final double GSF_NULL_DRAFT = 0.0d;
    public static final double GSF_NULL_DEPTH_CORRECTOR = 99.99d;
    public static final double GSF_NULL_TIDE_CORRECTOR = 99.99d;
    public static final double GSF_NULL_SOUND_SPEED_CORRECTION = 99.99d;
    public static final double GSF_NULL_HORIZONTAL_ERROR = -1.0d;
    public static final double GSF_NULL_VERTICAL_ERROR = -1.0d;
    public static final double GSF_NULL_HEIGHT = 9999.99d;
    public static final double GSF_NULL_SEP = 9999.99d;
    public static final double GSF_NULL_SEP_UNCERTAINTY = 0.0d;
    public static final double GSF_NULL_DEPTH = 0.0d;
    public static final double GSF_NULL_ACROSS_TRACK = 0.0d;
    public static final double GSF_NULL_ALONG_TRACK = 0.0d;
    public static final double GSF_NULL_TRAVEL_TIME = 0.0d;
    public static final double GSF_NULL_BEAM_ANGLE = 0.0d;
    public static final double GSF_NULL_MC_AMPLITUDE = 0.0d;
    public static final double GSF_NULL_MR_AMPLITUDE = 0.0d;
    public static final double GSF_NULL_ECHO_WIDTH = 0.0d;
    public static final double GSF_NULL_QUALITY_FACTOR = 0.0d;
    public static final double GSF_NULL_RECEIVE_HEAVE = 0.0d;
    public static final double GSF_NULL_DEPTH_ERROR = 0.0d;
    public static final double GSF_NULL_ACROSS_TRACK_ERROR = 0.0d;
    public static final double GSF_NULL_ALONG_TRACK_ERROR = 0.0d;
    public static final double GSF_NULL_NAV_POS_ERROR = 0.0d;
    public static final double GSF_BEAM_WIDTH_UNKNOWN = -1.0d;
    public static final int GSF_GEOSWATH_PLUS_PORT_PING = 0;
    public static final int GSF_GEOSWATH_PLUS_STBD_PING = 1;
    public static final int GSF_IGNORE_PING = 1;
    public static final int GSF_PING_USER_FLAG_01 = 2;
    public static final int GSF_PING_USER_FLAG_02 = 4;
    public static final int GSF_PING_USER_FLAG_03 = 8;
    public static final int GSF_PING_USER_FLAG_04 = 16;
    public static final int GSF_PING_USER_FLAG_05 = 32;
    public static final int GSF_PING_USER_FLAG_06 = 64;
    public static final int GSF_PING_USER_FLAG_07 = 128;
    public static final int GSF_PING_USER_FLAG_08 = 256;
    public static final int GSF_PING_USER_FLAG_09 = 512;
    public static final int GSF_PING_USER_FLAG_10 = 1024;
    public static final int GSF_PING_USER_FLAG_11 = 2048;
    public static final int GSF_PING_USER_FLAG_12 = 4096;
    public static final int GSF_PING_USER_FLAG_13 = 8192;
    public static final int GSF_PING_USER_FLAG_14 = 16384;
    public static final int GSF_PING_USER_FLAG_15 = 32768;
    public static final int GSF_IGNORE_BEAM = 1;
    public static final int GSF_BEAM_USER_FLAG_01 = 2;
    public static final int GSF_BEAM_USER_FLAG_02 = 4;
    public static final int GSF_BEAM_USER_FLAG_03 = 8;
    public static final int GSF_BEAM_USER_FLAG_04 = 16;
    public static final int GSF_BEAM_USER_FLAG_05 = 32;
    public static final int GSF_BEAM_USER_FLAG_06 = 64;
    public static final int GSF_BEAM_USER_FLAG_07 = 128;
    public static final int GSF_INTENSITY_LINEAR = 1;
    public static final int GSF_INTENSITY_CALIBRATED = 2;
    public static final int GSF_INTENSITY_POWER = 4;
    public static final int GSF_INTENSITY_GAIN = 8;
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_UNKN "UNKN"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_UNKN() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_UNKN
                = GSF.LIBRARY_ARENA.allocateFrom("UNKN");
        }
        return Holder.GSF_POS_TYPE_UNKN;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_GPSU "GPSU"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_GPSU() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_GPSU
                = GSF.LIBRARY_ARENA.allocateFrom("GPSU");
        }
        return Holder.GSF_POS_TYPE_GPSU;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PPSD "PPSD"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PPSD() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PPSD
                = GSF.LIBRARY_ARENA.allocateFrom("PPSD");
        }
        return Holder.GSF_POS_TYPE_PPSD;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PPSK "PPSK"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PPSK() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PPSK
                = GSF.LIBRARY_ARENA.allocateFrom("PPSK");
        }
        return Holder.GSF_POS_TYPE_PPSK;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PPSS "PPSS"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PPSS() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PPSS
                = GSF.LIBRARY_ARENA.allocateFrom("PPSS");
        }
        return Holder.GSF_POS_TYPE_PPSS;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PPSG "PPSG"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PPSG() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PPSG
                = GSF.LIBRARY_ARENA.allocateFrom("PPSG");
        }
        return Holder.GSF_POS_TYPE_PPSG;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_SPSD "SPSD"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_SPSD() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_SPSD
                = GSF.LIBRARY_ARENA.allocateFrom("SPSD");
        }
        return Holder.GSF_POS_TYPE_SPSD;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_SPSK "SPSK"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_SPSK() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_SPSK
                = GSF.LIBRARY_ARENA.allocateFrom("SPSK");
        }
        return Holder.GSF_POS_TYPE_SPSK;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_SPSS "SPSS"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_SPSS() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_SPSS
                = GSF.LIBRARY_ARENA.allocateFrom("SPSS");
        }
        return Holder.GSF_POS_TYPE_SPSS;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_SPSG "SPSG"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_SPSG() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_SPSG
                = GSF.LIBRARY_ARENA.allocateFrom("SPSG");
        }
        return Holder.GSF_POS_TYPE_SPSG;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_GPPP "GPPP"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_GPPP() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_GPPP
                = GSF.LIBRARY_ARENA.allocateFrom("GPPP");
        }
        return Holder.GSF_POS_TYPE_GPPP;
    }
    /**
     * {@snippet lang=c :
     * #define GPS_POS_TYPE_GPPK "GPPK"
     * }
     */
    public static MemorySegment GPS_POS_TYPE_GPPK() {
        class Holder {
            static final MemorySegment GPS_POS_TYPE_GPPK
                = GSF.LIBRARY_ARENA.allocateFrom("GPPK");
        }
        return Holder.GPS_POS_TYPE_GPPK;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_INUA "INUA"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_INUA() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_INUA
                = GSF.LIBRARY_ARENA.allocateFrom("INUA");
        }
        return Holder.GSF_POS_TYPE_INUA;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_INVA "INVA"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_INVA() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_INVA
                = GSF.LIBRARY_ARENA.allocateFrom("INVA");
        }
        return Holder.GSF_POS_TYPE_INVA;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_INWA "INWA"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_INWA() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_INWA
                = GSF.LIBRARY_ARENA.allocateFrom("INWA");
        }
        return Holder.GSF_POS_TYPE_INWA;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_LBLN "LBLN"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_LBLN() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_LBLN
                = GSF.LIBRARY_ARENA.allocateFrom("LBLN");
        }
        return Holder.GSF_POS_TYPE_LBLN;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_USBL "USBL"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_USBL() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_USBL
                = GSF.LIBRARY_ARENA.allocateFrom("USBL");
        }
        return Holder.GSF_POS_TYPE_USBL;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PIUA "PIUA"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PIUA() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PIUA
                = GSF.LIBRARY_ARENA.allocateFrom("PIUA");
        }
        return Holder.GSF_POS_TYPE_PIUA;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PIVA "PIVA"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PIVA() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PIVA
                = GSF.LIBRARY_ARENA.allocateFrom("PIVA");
        }
        return Holder.GSF_POS_TYPE_PIVA;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PIWA "PIWA"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PIWA() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PIWA
                = GSF.LIBRARY_ARENA.allocateFrom("PIWA");
        }
        return Holder.GSF_POS_TYPE_PIWA;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PLBL "PLBL"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PLBL() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PLBL
                = GSF.LIBRARY_ARENA.allocateFrom("PLBL");
        }
        return Holder.GSF_POS_TYPE_PLBL;
    }
    /**
     * {@snippet lang=c :
     * #define GSF_POS_TYPE_PSBL "PSBL"
     * }
     */
    public static MemorySegment GSF_POS_TYPE_PSBL() {
        class Holder {
            static final MemorySegment GSF_POS_TYPE_PSBL
                = GSF.LIBRARY_ARENA.allocateFrom("PSBL");
        }
        return Holder.GSF_POS_TYPE_PSBL;
    }
    public static final double GSF_UNKNOWN_PARAM_VALUE = 2.2250738585072014E-308d;
    public static final int GSF_UNKNOWN_PARAM_INT = -99;
    public static final int GSF_FOPEN_ERROR = -1;
    public static final int GSF_UNRECOGNIZED_FILE = -2;
    public static final int GSF_BAD_ACCESS_MODE = -3;
    public static final int GSF_READ_ERROR = -4;
    public static final int GSF_WRITE_ERROR = -5;
    public static final int GSF_INSUFFICIENT_SIZE = -6;
    public static final int GSF_RECORD_SIZE_ERROR = -7;
    public static final int GSF_CHECKSUM_FAILURE = -8;
    public static final int GSF_FILE_CLOSE_ERROR = -9;
    public static final int GSF_TOO_MANY_ARRAY_SUBRECORDS = -10;
    public static final int GSF_TOO_MANY_OPEN_FILES = -11;
    public static final int GSF_MEMORY_ALLOCATION_FAILED = -12;
    public static final int GSF_UNRECOGNIZED_RECORD_ID = -13;
    public static final int GSF_STREAM_DECODE_FAILURE = -14;
    public static final int GSF_BAD_SEEK_OPTION = -15;
    public static final int GSF_FILE_SEEK_ERROR = -16;
    public static final int GSF_UNRECOGNIZED_SENSOR_ID = -17;
    public static final int GSF_UNRECOGNIZED_DATA_RECORD = -18;
    public static final int GSF_UNRECOGNIZED_ARRAY_SUBRECORD_ID = -19;
    public static final int GSF_UNRECOGNIZED_SUBRECORD_ID = -20;
    public static final int GSF_ILLEGAL_SCALE_FACTOR_MULTIPLIER = -21;
    public static final int GSF_CANNOT_REPRESENT_PRECISION = -22;
    public static final int GSF_READ_TO_END_OF_FILE = -23;
    public static final int GSF_BAD_FILE_HANDLE = -24;
    public static final int GSF_HEADER_RECORD_DECODE_FAILED = -25;
    public static final int GSF_MB_PING_RECORD_DECODE_FAILED = -26;
    public static final int GSF_SVP_RECORD_DECODE_FAILED = -27;
    public static final int GSF_PROCESS_PARAM_RECORD_DECODE_FAILED = -28;
    public static final int GSF_SENSOR_PARAM_RECORD_DECODE_FAILED = -29;
    public static final int GSF_COMMENT_RECORD_DECODE_FAILED = -30;
    public static final int GSF_HISTORY_RECORD_DECODE_FAILED = -31;
    public static final int GSF_NAV_ERROR_RECORD_DECODE_FAILED = -32;
    public static final int GSF_HEADER_RECORD_ENCODE_FAILED = -25;
    public static final int GSF_MB_PING_RECORD_ENCODE_FAILED = -26;
    public static final int GSF_SVP_RECORD_ENCODE_FAILED = -27;
    public static final int GSF_PROCESS_PARAM_RECORD_ENCODE_FAILED = -28;
    public static final int GSF_SENSOR_PARAM_RECORD_ENCODE_FAILED = -29;
    public static final int GSF_COMMENT_RECORD_ENCODE_FAILED = -30;
    public static final int GSF_HISTORY_RECORD_ENCODE_FAILED = -31;
    public static final int GSF_NAV_ERROR_RECORD_ENCODE_FAILED = -32;
    public static final int GSF_SETVBUF_ERROR = -33;
    public static final int GSF_FLUSH_ERROR = -34;
    public static final int GSF_FILE_TELL_ERROR = -35;
    public static final int GSF_INDEX_FILE_OPEN_ERROR = -36;
    public static final int GSF_CORRUPT_INDEX_FILE_ERROR = -37;
    public static final int GSF_SCALE_INDEX_CALLOC_ERROR = -38;
    public static final int GSF_RECORD_TYPE_NOT_AVAILABLE = -39;
    public static final int GSF_SUMMARY_RECORD_DECODE_FAILED = -40;
    public static final int GSF_SUMMARY_RECORD_ENCODE_FAILED = -41;
    public static final int GSF_INVALID_NUM_BEAMS = -42;
    public static final int GSF_INVALID_RECORD_NUMBER = -43;
    public static final int GSF_INDEX_FILE_READ_ERROR = -44;
    public static final int GSF_PARAM_SIZE_FIXED = -45;
    public static final int GSF_SINGLE_BEAM_ENCODE_FAILED = -46;
    public static final int GSF_HV_NAV_ERROR_RECORD_ENCODE_FAILED = -47;
    public static final int GSF_HV_NAV_ERROR_RECORD_DECODE_FAILED = -48;
    public static final int GSF_ATTITUDE_RECORD_ENCODE_FAILED = -49;
    public static final int GSF_ATTITUDE_RECORD_DECODE_FAILED = -50;
    public static final int GSF_OPEN_TEMP_FILE_FAILED = -51;
    public static final int GSF_PARTIAL_RECORD_AT_END_OF_FILE = -52;
    public static final int GSF_QUALITY_FLAGS_DECODE_ERROR = -53;
    public static final int GSF_COMPRESSION_UNSUPPORTED = -55;
    public static final int GSF_COMPRESSION_FAILED = -56;

}

