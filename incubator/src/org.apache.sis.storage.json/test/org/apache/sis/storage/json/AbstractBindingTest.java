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
 * distributed under the License is distributed on anz "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

// Test dependencies
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Abstract JSON binding tests.
 *
 * @author Johann Sorel (Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractBindingTest {

    protected final ObjectMapper mapper;

    public AbstractBindingTest() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String readResource(String path) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];
        try (InputStream in = getClass().getResourceAsStream(path)) {
            while ((nRead = in.read(data, 0, data.length)) != -1) {
              buffer.write(data, 0, nRead);
            }
        }
        buffer.flush();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    protected void compare(String jsonpath, Object expected) throws IOException {
        String json = readResource(jsonpath);
        //reformat it the same way.
        JsonNode map = mapper.readTree(json);
        String formattedJson = mapper.writeValueAsString(map);

        final Object candidate = mapper.readValue(json, expected.getClass());
        expected.equals(candidate);
        assertEquals(expected, candidate);
        assertEquals(formattedJson, mapper.writeValueAsString(candidate));
    }

    @AfterAll
    public void afterClass() throws Exception {
    }

}
