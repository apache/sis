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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.PackageVersion;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;

/**
 * Jackson generator implementation for UBJSON.
 *
 * @author Johann Sorel (Geomatys)
 */
final class UBJsonGenerator extends GeneratorBase {

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    /**
     * @since 2.16
     */
    protected final StreamWriteConstraints _streamWriteConstraints;

    /**
     * Underlying {@link Writer} used for output.
     */
    protected final ChannelDataOutput _out;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */
    /**
     * Current context, in form we can use it (GeneratorBase has untyped reference; left as null)
     */
    protected UBJsonWriteContext _streamWriteContext;

    protected final StringBuilder _basePath = new StringBuilder(50);

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    public UBJsonGenerator(IOContext ioCtxt, int stdFeatures, ObjectCodec codec, OutputStream out) throws IllegalArgumentException, DataStoreException {
        super(stdFeatures, codec, ioCtxt);
        _streamWriteConstraints = ioCtxt.streamWriteConstraints();
        _streamWriteContext = UBJsonWriteContext.createRootContext();
        _out = new StorageConnector(out).getStorageAs(ChannelDataOutput.class);
    }

    @Override
    public StreamWriteConstraints streamWriteConstraints() {
        return _streamWriteConstraints;
    }

    /*
    /**********************************************************************
    /* Versioned
    /**********************************************************************
     */
    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************************
    /* Overridden methods: low-level I/O
    /**********************************************************************
     */
    @Override
    public void close() throws IOException {
        if (!isClosed()) {
            if (_out != null) {
                if (_ioContext.isResourceManaged() || isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET)) {
                    _out.flush();
                    _out.channel.close();
                } else if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                    // If we can't close it, we should at least flush
                    _out.flush();
                }
            }
            // Internal buffer(s) generator has can now be released as well
            _releaseBuffers();
            super.close();
        }
    }

    @Override
    public void flush() throws IOException {
        if (_out != null) {
            if (isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)) {
                _out.flush();
            }
        }
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */
    @Override
    protected void _releaseBuffers() {
    }

    /*
    /**********************************************************************
    /* Overridden output state handling methods
    /**********************************************************************
     */
    @Override
    public Object currentValue() {
        return _streamWriteContext.getCurrentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        _streamWriteContext.setCurrentValue(v);
    }

    /*
    /**********************************************************************
    /* Overrides: capability introspection methods
    /**********************************************************************
     */
    @Override
    public boolean canWriteObjectId() {
        return false;
    }

    @Override
    public boolean canWriteTypeId() {
        return false;
    }

    /*
    /**********************************************************************
    /* Overridden methods; writing property names
    /**********************************************************************
     */
    @Override
    public void writeFieldName(String name) throws IOException {
        if (!_streamWriteContext.writeName(name)) {
            _reportError("Cannot write a property name, expecting a value");
        }
        _writeStringNoMarker(name);
    }

    /*
    /**********************************************************************
    /* Public API: structural output
    /**********************************************************************
     */
    @Override
    public void writeStartArray() throws IOException {
        writeStartArray(null);
    }

    @Override
    public void writeStartArray(Object currValue) throws IOException {
        // arrays are always inline, force writing the current key
        // NOTE: if this ever changes, we need to add empty array handling in writeEndArray
        _verifyValueWrite("start an array");
        _streamWriteContext = _streamWriteContext.createChildArrayContext(currValue,
                _basePath.length());
        streamWriteConstraints().validateNestingDepth(_streamWriteContext.getNestingDepth());
        _out.writeByte(UBJson.ARRAY_START);
    }

    @Override
    public void writeEndArray() throws IOException {
        if (!_streamWriteContext.inArray()) {
            _reportError("Current context not an Array but " + _streamWriteContext.typeDesc());
        }
        _out.writeByte(UBJson.ARRAY_END);
        _streamWriteContext = _streamWriteContext.getParent();
    }

    @Override
    public void writeStartObject() throws IOException {
        writeStartObject(null);
    }

    @Override
    public void writeStartObject(Object forValue) throws IOException {
        // objects aren't always materialized right now
        _verifyValueWrite("start an object");
        _streamWriteContext = _streamWriteContext.createChildObjectContext(forValue, _basePath.length());
        streamWriteConstraints().validateNestingDepth(_streamWriteContext.getNestingDepth());
        _out.writeByte(UBJson.OBJECT_START);
    }

    @Override
    public void writeEndObject() throws IOException {
        if (!_streamWriteContext.inObject()) {
            _reportError("Current context not an Object but " + _streamWriteContext.typeDesc());
        }
        _out.writeByte(UBJson.OBJECT_END);
        _streamWriteContext = _streamWriteContext.getParent();
    }

    /*
    /**********************************************************************
    /* Output method implementations, textual
    /**********************************************************************
     */
    @Override
    public void writeString(String text) throws IOException {
        if (text == null) {
            writeNull();
        } else {
            final byte[] utf8Buffer = text.getBytes(StandardCharsets.UTF_8);
            writeUTF8String(utf8Buffer, 0, utf8Buffer.length);
        }
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        writeString(new String(text, offset, len));
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len) throws IOException {
        _verifyValueWrite("write String value");
        _out.writeByte(UBJson.STRING);
        _writeInt(len, false);
        _out.write(text, offset, len);

    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int len) throws IOException {
        writeRawUTF8String(text, offset, len);
    }

    /*
    /**********************************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************************
     */
    @Override
    public void writeRaw(String text) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(char c) throws IOException {
        throw new UnsupportedOperationException();
    }

    /*
    /**********************************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************************
     */
    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
            throws IOException {
        if (data == null) {
            writeNull();
        } else {
            //create a compact ubjson byte array
            _out.writeByte(UBJson.ARRAY_START);
            _out.writeByte(UBJson.OPTIMIZED_TYPE);
            _out.writeByte(UBJson.UINT8);
            _out.writeByte(UBJson.OPTIMIZED_SIZE);
            _writeInt(len, false);
            _out.write(data, offset, len);
        }
    }

    /*
    /**********************************************************************
    /* Output method implementations, scalars
    /**********************************************************************
     */
    @Override
    public void writeBoolean(boolean state) throws IOException {
        _verifyValueWrite("write boolean value");
        _out.writeByte(state ? UBJson.TRUE : UBJson.FALSE);
    }

    @Override
    public void writeNumber(short v) throws IOException {
        writeNumber((int) v);
        _out.writeByte(UBJson.INT16);
        _out.writeShort(v);
    }

    @Override
    public void writeNumber(int i) throws IOException {
        _verifyValueWrite("write number");
        _out.writeByte(UBJson.INT32);
        _out.writeInt(i);
    }

    @Override
    public void writeNumber(long l) throws IOException {
        _verifyValueWrite("write number");
        _out.writeByte(UBJson.INT64);
        _out.writeLong(l);
    }

    @Override
    public void writeNumber(BigInteger dec) throws IOException {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        String str = dec.toString();
        _out.writeByte(UBJson.HIGH_PRECISION_NUMBER);
        _writeStringNoMarker(str);
    }

    @Override
    public void writeNumber(double d) throws IOException {
        _verifyValueWrite("write number");
        _out.writeByte(UBJson.FLOAT64);
        _out.writeDouble(d);
    }

    @Override
    public void writeNumber(float f) throws IOException {
        _verifyValueWrite("write number");
        _out.writeByte(UBJson.FLOAT32);
        _out.writeFloat(f);
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        String str = isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN) ? dec.toPlainString() : dec.toString();
        _out.writeByte(UBJson.HIGH_PRECISION_NUMBER);
        _writeStringNoMarker(str);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeNull() throws IOException {
        _verifyValueWrite("write null value");
        _out.writeByte(UBJson.NULL);
    }

    /*
    /**********************************************************************
    /* Implementations for methods from base class
    /**********************************************************************
     */
    @Override
    protected void _verifyValueWrite(String typeMsg) throws IOException {
        // check that name/value cadence works
        if (!_streamWriteContext.writeValue()) {
            _reportError("Cannot " + typeMsg + ", expecting a property name");
        }
    }

    //internals
    private void _writeStringNoMarker(String txt) throws IOException {
        final byte[] bytes = txt.getBytes(StandardCharsets.UTF_8);
        final int length = bytes.length;
        _writeInt(length, false);
        _out.write(bytes);
    }

    private void _writeInt(long value, boolean allowUnsigned8) throws IOException {
        if (allowUnsigned8 && value >= 0 && value <= 255) {
            _out.writeByte(UBJson.UINT8);
            _out.writeByte((int) value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            _out.writeByte(UBJson.INT8);
            _out.writeByte((int) value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            _out.writeByte(UBJson.INT16);
            _out.writeShort((int) value);
        } else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            _out.writeByte(UBJson.INT32);
            _out.writeInt((int) value);
        } else if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
            _out.writeByte(UBJson.INT64);
            _out.writeLong(value);
        }
    }

}
