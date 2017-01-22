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
package org.apache.sis.referencing.operation;

import java.util.Collections;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.internal.referencing.provider.Mercator1SP;


/**
 * Collection of defining conversions for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class HardCodedConversions {
    /**
     * A defining conversion for a <cite>Mercator (variant A)</cite> (also known as "1SP") projection
     * with a scale factor of 1.
     */
    public static final DefaultConversion MERCATOR;
    static {
        final OperationMethod method = new Mercator1SP();
        MERCATOR = new DefaultConversion(Collections.singletonMap(OperationMethod.NAME_KEY, "Mercator"),
                method, null, method.getParameters().createValue());
    }

    /**
     * Do not allow instantiation of this class.
     */
    private HardCodedConversions() {
    }
}
