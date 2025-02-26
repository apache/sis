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
package org.apache.sis.storage.isobmff.base;

import java.io.IOException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class GeneralType extends Box {
    /**
     * Brand identifier
     */
    public String majorBrand;
    /**
     * Minor version of the major brand.
     */
    public int minorVersion;
    /**
     * List of compatible brands.
     */
    public String[] compatibleBrands;

    @Override
    public void readProperties(Reader reader) throws IOException {
        majorBrand = intToFourCC(reader.channel.readInt());
        minorVersion = reader.channel.readInt();
        int nbCmp = Math.toIntExact(((boxOffset + size) - reader.channel.getStreamPosition()) / 4);
        compatibleBrands = new String[nbCmp];
        for (int i = 0; i < nbCmp; i++) {
            compatibleBrands[i] = intToFourCC(reader.channel.readInt());
        }
    }

}
