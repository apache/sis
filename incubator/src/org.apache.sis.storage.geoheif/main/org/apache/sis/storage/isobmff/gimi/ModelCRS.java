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
package org.apache.sis.storage.isobmff.gimi;

import java.io.IOException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.base.ItemFullProperty;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ModelCRS extends ItemFullProperty {

    public static final String UUID = "137a1742-75ac-4747-82bc-659576e8675b";

    public String wkt2;

    @Override
    protected void readProperties(Reader reader) throws IOException {
        wkt2 = reader.readUtf8String();
    }

    public CoordinateReferenceSystem toCRS() throws FactoryException {
        //TODO remove this hack when SIS support BASEGEOGCRS
        String wkt = this.wkt2.replace("BASEGEOGCRS", "BASEGEODCRS");
        return CRS.fromWKT(wkt);
    }

}
