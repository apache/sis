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

import org.apache.sis.storage.json.DataTransferObject;


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
public final class CategoryEncoding extends DataTransferObject {

    public CategoryEncoding() {
    }

}
