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
import jakarta.json.stream.JsonParser.Event;
import org.apache.sis.storage.coveragejson.binding.Parameters.Deserializer;
import org.apache.sis.storage.coveragejson.binding.Parameters.Serializer;


/**
 * Contains a map of parameter objects.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbTypeDeserializer(Deserializer.class)
@JsonbTypeSerializer(Serializer.class)
public final class Parameters extends Dictionary<Parameter> {
    public Parameters() {
    }

    public static class Deserializer implements JsonbDeserializer<Parameters> {
        public Deserializer() {
        }

        @Override
        public Parameters deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            final Parameters parameters = new Parameters();
            while (parser.hasNext()) {
                final Event event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    // Deserialize inner object
                    final String name = parser.getString();
                    final Parameter value = ctx.deserialize(Parameter.class, parser);
                    parameters.setAnyProperty(name, value);
                }
            }
            return parameters;
        }
    }

    public static class Serializer implements JsonbSerializer<Parameters> {
        public Serializer() {
        }

        @Override
        public void serialize(Parameters parameters, JsonGenerator jg, SerializationContext sc) {
            jg.writeStartObject();
            for (Map.Entry<String,Parameter> entry : parameters.any.entrySet()) {
                sc.serialize(entry.getKey(), entry.getValue(), jg);
            }
            jg.writeEnd();
        }
    }
}
