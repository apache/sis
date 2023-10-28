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
package org.apache.sis.storage.coveragejson.binding;

import java.lang.reflect.Type;
import java.util.Map;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.apache.sis.storage.coveragejson.binding.CategoryEncoding.Deserializer;
import org.apache.sis.storage.coveragejson.binding.CategoryEncoding.Serializer;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * CategoryEncoding is an object where each key is equal to an "id" value of
 * the "categories" array within the "observedProperty" member of the
 * parameter object. There MUST be no duplicate keys. The value is either
 * an integer or an array of integers where each integer MUST be unique
 * within the object.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbTypeDeserializer(Deserializer.class)
@JsonbTypeSerializer(Serializer.class)
public final class CategoryEncoding extends Dictionary<Object> {
    public CategoryEncoding() {
    }

    public static class Deserializer implements JsonbDeserializer<CategoryEncoding> {
        public Deserializer() {
        }

        @Override
        public CategoryEncoding deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            final CategoryEncoding candidate = new CategoryEncoding();
            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    // Deserialize inner object
                    final String name = parser.getString();
                    String value = ctx.deserialize(String.class, parser);
                    candidate.setAnyProperty(name, value);
                }
            }
            return candidate;
        }
    }

    public static class Serializer implements JsonbSerializer<CategoryEncoding> {
        public Serializer() {
        }

        @Override
        public void serialize(CategoryEncoding ranges, JsonGenerator jg, SerializationContext sc) {
            jg.writeStartObject();
            for (Map.Entry<String,Object> entry : ranges.any.entrySet()) {
                sc.serialize(entry.getKey(), entry.getValue(), jg);
            }
            jg.writeEnd();
        }
    }
}
