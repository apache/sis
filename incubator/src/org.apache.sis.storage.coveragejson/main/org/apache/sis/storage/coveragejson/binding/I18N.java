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
import java.util.Locale;
import java.util.Map;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.opengis.util.InternationalString;
import org.apache.sis.storage.coveragejson.binding.I18N.Serializer;


/**
 * COPIED FROM OGC SPECIFICATION (TODO: ADAPT):
 * The special language tag "und" can be used to identify a value whose language
 * is unknown or undetermined.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbTypeDeserializer(I18N.Deserializer.class)
@JsonbTypeSerializer(Serializer.class)
public final class I18N extends Dictionary<String> implements InternationalString {

    public static final String UNDETERMINED = "und";

    public I18N() {
    }

    public I18N(String lang, String text) {
        setAnyProperty(lang, text);
    }

    private String getDefault() {
        String str = any.get(UNDETERMINED);
        if (str == null && !any.isEmpty()) str = any.get(any.keySet().iterator().next());
        if (str == null) str = "";
        return str;
    }

    public String toString() {
        return getDefault();
    }

    @Override
    public String toString(Locale locale) {
        String str = any.get(locale.getLanguage());
        if (str == null) str = any.get(locale.getISO3Language());
        return getDefault();
    }

    @Override
    public int length() {
        return getDefault().length();
    }

    @Override
    public char charAt(int index) {
        return getDefault().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return getDefault().subSequence(start, end);
    }

    @Override
    public int compareTo(InternationalString o) {
        return getDefault().compareTo(o.toString());
    }

    public static class Deserializer implements JsonbDeserializer<I18N> {
        public Deserializer() {
        }

        @Override
        public I18N deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            final I18N candidate = new I18N();
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

    public static class Serializer implements JsonbSerializer<I18N> {
        public Serializer() {
        }

        @Override
        public void serialize(I18N ranges, JsonGenerator jg, SerializationContext sc) {
            jg.writeStartObject();
            for (Map.Entry<String,String> entry : ranges.any.entrySet()) {
                jg.write(entry.getKey(), entry.getValue());
            }
            jg.writeEnd();
        }
    }
}
