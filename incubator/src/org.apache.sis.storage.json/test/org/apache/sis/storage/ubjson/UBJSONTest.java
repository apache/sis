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
package org.apache.sis.storage.ubjson;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class UBJSONTest {

    @Test
    public void testWriteRead() throws JsonProcessingException, IOException {

        final Map<String,Object> submap = new LinkedHashMap<>();
        submap.put("attN", 789);

        final Map<String,Object> map = new LinkedHashMap<>();
        map.put("att0", null);
        map.put("att1", true);
        map.put("att2", 123456);
        map.put("att3", "some test");
        map.put("att4", List.of(1,2,3));
        map.put("att5", submap);

        byte[] bytes = new UBJsonMapper().writeValueAsBytes(map);
        assertArrayEquals(new byte[]{
            UBJson.OBJECT_START,
                UBJson.INT8, 4, 97, 116, 116, 48,   //att0
                UBJson.NULL,                        //null
                UBJson.INT8, 4, 97, 116, 116, 49,   //att1
                UBJson.TRUE,                        //true
                UBJson.INT8, 4, 97, 116, 116, 50,   //att2
                UBJson.INT32, 0, 1, -30, 64,        //123456
                UBJson.INT8, 4, 97, 116, 116, 51,   //att3
                UBJson.STRING, UBJson.INT8, 9, 115, 111, 109, 101, 32, 116, 101, 115, 116, //some text
                UBJson.INT8, 4, 97, 116, 116, 52,   //att4
                UBJson.ARRAY_START,
                    UBJson.INT32, 0, 0, 0, 1,       //1
                    UBJson.INT32, 0, 0, 0, 2,       //2
                    UBJson.INT32, 0, 0, 0, 3,       //3
                UBJson.ARRAY_END,
                UBJson.INT8, 4, 97, 116, 116, 53,   //att5
                UBJson.OBJECT_START,
                    UBJson.INT8, 4, 97, 116, 116, 78,  //attN
                    UBJson.INT32, 0, 0, 3, 21, //789
                UBJson.OBJECT_END,
            UBJson.OBJECT_END}, bytes);

        Map result = new UBJsonMapper().readValue(bytes, Map.class);
        assertEquals(map, result);

    }

}
