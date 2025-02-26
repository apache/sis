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
package org.apache.sis.storage.isobmff;

import java.util.Set;
import org.apache.sis.storage.IllegalNameException;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface BoxRegistry {

    /**
     * Get registry name.
     * This should be a specification identification.
     *
     * @return registry name
     */
    String getName();

    /**
     * @return Set of known boxes identifiers
     */
    Set<String> getBoxesFourCC();

    /**
     * @return Set of known Extension boxes UUID.
     */
    Set<String> getExtensionUUIDs();

    /**
     * Create a new box for given code.
     *
     * @param fourCC box identifier
     * @return empty box
     * @throws org.apache.sis.storage.IllegalNameException if box code is unknown
     */
    Box create(String fourCC) throws IllegalNameException;

    /**
     * Create a new extension box for given code.
     *
     * @param fourCC box identifier
     * @return empty box
     * @throws org.apache.sis.storage.IllegalNameException if box uuid is unknown
     */
    Box createExtension(String uuid) throws IllegalNameException;

}
