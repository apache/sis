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
package org.apache.sis.cql;

import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opengis.filter.SortOrder;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final strictfp class QueryWritingTest extends CQLTestCase {

    @Test
    public void testWrite() throws CQLException {
        final Query query = new Query(
                Arrays.asList(new Query.Projection(FF.property("name"), null), new Query.Projection(FF.literal(4),"col1")),
                FF.equal(FF.property("id"), FF.literal("a")),
                Arrays.asList(
                    FF.sort(FF.property("name"), SortOrder.ASCENDING),
                    FF.sort(FF.property("age"), SortOrder.DESCENDING)),
                5,
                10);

        String cql = CQL.write(query);
        assertEquals("SELECT name, 4 AS 'col1' WHERE id = 'a' ORDER BY name ASC, age DESC OFFSET 5 LIMIT 10", cql);
    }
}
