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
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.base.ItemFullProperty;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ModelTransformation extends ItemFullProperty {

    public static final String UUID = "763cf838-b630-440b-84f8-be44bf9910af";

    public double[] transform;

    @Override
    protected void readProperties(Reader reader) throws IOException {
        if ((flags & 0x01) == 1) {
            //2D
            transform = reader.channel.readDoubles(6);
        } else {
            //3D
            transform = reader.channel.readDoubles(12);
        }
    }

    public MathTransform toMathTransform() {
        if (transform.length == 6) {
            return new AffineTransform2D(transform[0], transform[3], transform[1], transform[4], transform[2], transform[5]);
        } else {
            throw new UnsupportedOperationException("3D transform not supported yet");
        }
    }

}
