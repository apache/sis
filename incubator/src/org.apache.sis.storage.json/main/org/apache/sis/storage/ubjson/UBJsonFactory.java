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

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import org.apache.sis.storage.DataStoreException;

/**
 * Jackson factory implementation for UBJSON.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class UBJsonFactory extends JsonFactory {

    public final static String FORMAT_NAME_UBJSON = "ubjson";

    public UBJsonFactory() {
        super();
    }

    public UBJsonFactory(ObjectCodec codec) {
        super(codec);
    }

    protected UBJsonFactory(UBJsonFactory src, ObjectCodec oc) {
        super(src, oc);
    }

    protected UBJsonFactory(UBJsonFactoryBuilder b) {
        super(b, false);
    }

    @Override
    public UBJsonFactoryBuilder rebuild() {
        return new UBJsonFactoryBuilder(this);
    }

    /**
     * Main factory method to use for constructing {@link UBJsonFactory} instances with different configuration.
     */
    public static UBJsonFactoryBuilder builder() {
        return new UBJsonFactoryBuilder();
    }

    @Override
    public UBJsonFactory copy() {
        _checkInvalidCopy(UBJsonFactory.class);
        return new UBJsonFactory(this, null);
    }

    // **********************************************************
    // Versioned
    // **********************************************************

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    // *********************************************************
    // Format detection functionality
    // *********************************************************

    @Override
    public String getFormatName() {
        return FORMAT_NAME_UBJSON;
    }

    /**
     * Sub-classes need to override this method
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException {
        return MatchStrength.INCONCLUSIVE;
    }

    // *********************************************************
    // Capability introspection
    // *********************************************************

    @Override
    public boolean requiresPropertyOrdering() {
        return false;
    }

    @Override
    public boolean canHandleBinaryNatively() {
        return true;
    }

    @Override
    public boolean canUseCharArrays() {
        return false;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return false;
    }

    // *********************************************************************
    // Overridden internal factory methods, parser
    // *********************************************************************

    @Override
    public JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        return new TreeTraversingParser(parse(ctxt, in));
    }

    @Override
    public JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        throw new UnsupportedOperationException("Can not create UBJson binary parser from text input");
    }

    @Override
    public JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        return _createParser(new ByteArrayInputStream(data, offset, len), ctxt);
    }

    @Override
    protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) throws IOException {
        throw new UnsupportedOperationException("Can not create UBJson binary parser from text input");
    }

    // *********************************************************************
    //  Overridden internal factory methods, generator
    // *********************************************************************

    @Override
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        throw new UnsupportedOperationException("Can not create UBJson binary generator for text output");
    }

    @Override
    protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        try {
            return new UBJsonGenerator(ctxt, _generatorFeatures, _objectCodec, out);
        } catch (IllegalArgumentException | DataStoreException ex) {
            throw new IOException(ex);
        }
    }

    // *********************************************************************
    // Low-level methods for reading/writing UBJSON
    // *********************************************************************

    private JsonNode parse(IOContext ctxt, InputStream in) throws IOException {
        final UBJsonParser parser = new UBJsonParser();
        try {
            return parser.parse(in);
        } catch (IllegalArgumentException | DataStoreException ex) {
            throw new IOException(ex);
        }
    }

}
