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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;

/**
 * UBJSON parser.
 *
 * @author Johann Sorel (Geomatys)
 */
final class UBJsonParser {

    private static final JsonNodeFactory NF = JsonNodeFactory.instance;

    public JsonNode parse(InputStream stream) throws IllegalArgumentException, DataStoreException, IOException {
        final ChannelDataInput di = new StorageConnector(stream).getStorageAs(ChannelDataInput.class);
        final byte marker = di.readByte();
        return switch (marker) {
            case UBJson.OBJECT_START -> parseObject(di);
            case UBJson.ARRAY_START -> parseArray(di);
            default -> parseValue(marker, di);
        };
    }

    private static ObjectNode parseObject(ChannelDataInput in) throws IOException {
        final ObjectNode node = NF.objectNode();
        byte marker;
        while ( (marker = in.readByte()) != UBJson.OBJECT_END) {
            final String name = readStringNoMarker(marker, in);
            final JsonNode value = parseValue(in.readByte(), in);
            node.set(name, value);
        }
        return node;
    }

    private static JsonNode parseArray(ChannelDataInput in) throws IOException {
        byte marker = in.readByte();
        // check for optimized cases
        // https://ubjson.org/type-reference/container-types/#optimized-format
        if (marker == UBJson.OPTIMIZED_TYPE) {
            marker = in.readByte();
            final byte type = marker;
            marker = in.readByte();
            if (marker != UBJson.OPTIMIZED_SIZE) throw new IOException("Optimized size marker not found");
            marker = in.readByte();
            final int size = parseValue(marker, in).asInt();
            if (type == UBJson.UINT8 || type == UBJson.INT8) {
                //use a compact form
                return NF.binaryNode(in.readBytes(size));
            } else {
                final ArrayNode node = NF.arrayNode(size);
                for (int i = 0; i < size; i++) {
                    node.add(parseValue(type, in));
                }
                return node;
            }

        } else if (marker == UBJson.OPTIMIZED_SIZE) {
            //array of fixed size but undefined type
            marker = in.readByte();
            final int size = parseValue(marker, in).asInt();
            final ArrayNode node = NF.arrayNode(size);
            for (int i = 0; i < size; i++) {
                marker = in.readByte();
                node.add(parseValue(marker, in));
            }
            return node;
        } else {
            //normal array or undefined size and type
            final ArrayNode node = NF.arrayNode();
            do {
                node.add(parseValue(marker, in));
                marker = in.readByte();
            } while (marker != UBJson.ARRAY_END);
            return node;
        }
    }

    private static JsonNode parseValue(byte marker, ChannelDataInput in) throws IOException {
        switch (marker) {
            case UBJson.NO_OP : //should not happen
            case UBJson.NULL : return NF.nullNode();
            case UBJson.TRUE : return NF.booleanNode(true);
            case UBJson.FALSE : return NF.booleanNode(false);
            case UBJson.UINT8 : return NF.numberNode(in.readUnsignedByte());
            case UBJson.INT8 : return NF.numberNode(in.readByte());
            case UBJson.INT16 : return NF.numberNode(in.readShort());
            case UBJson.INT32 : return NF.numberNode(in.readInt());
            case UBJson.INT64 : return NF.numberNode(in.readLong());
            case UBJson.FLOAT32 : return NF.numberNode(in.readFloat());
            case UBJson.FLOAT64 : return NF.numberNode(in.readDouble());
            case UBJson.CHAR : return NF.textNode(""+((char)in.readUnsignedByte()));
            case UBJson.HIGH_PRECISION_NUMBER : return NF.numberNode(new BigDecimal(readStringNoMarker(in.readByte(), in)));
            case UBJson.ARRAY_START : return parseArray(in);
            case UBJson.OBJECT_START : return parseObject(in);
            case UBJson.STRING : return NF.textNode(readStringNoMarker(in.readByte(), in));
            default: throw new IOException("Unexpected marker " + marker);
        }
    }

    private static String readStringNoMarker(int marker, ChannelDataInput in) throws IOException {
        final long size;
        switch (marker) {
            case UBJson.INT8 : size = in.readByte(); break;
            case UBJson.INT16 : size = in.readShort(); break;
            case UBJson.INT32 : size = in.readInt(); break;
            case UBJson.INT64 : size = in.readLong(); break;
            default: throw new IOException("Unexpected size marker");
        }
        final byte[] datas = in.readBytes(Math.toIntExact(size));
        return new String(datas, StandardCharsets.UTF_8);
    }

}
