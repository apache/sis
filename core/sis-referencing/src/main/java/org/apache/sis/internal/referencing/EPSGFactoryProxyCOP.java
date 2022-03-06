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
package org.apache.sis.internal.referencing;

import java.util.Set;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.opengis.referencing.operation.OperationMethod;


/**
 * Same as {@link EPSGFactoryProxyCRS} but for coordinate operations.
 *
 * <p>This class will be modified on the JDK9 branch in order to replace the proxy by a static {@code provider()}
 * method. This will allow us to remove all the indirection level currently found in this class.</p>
 */
public final class EPSGFactoryProxyCOP extends EPSGFactoryProxy implements CoordinateOperationAuthorityFactory {
    private volatile CoordinateOperationAuthorityFactory factory;

    @Override
    CoordinateOperationAuthorityFactory factory() throws FactoryException {
        CoordinateOperationAuthorityFactory f = factory;
        if (f == null) {
            factory = f = (CoordinateOperationAuthorityFactory) CRS.getAuthorityFactory("EPSG");
        }
        return f;
    }

    @Override
    public OperationMethod createOperationMethod(String code) throws FactoryException {
        return factory().createOperationMethod(code);
    }

    @Override
    public CoordinateOperation createCoordinateOperation(String code) throws FactoryException {
        return factory().createCoordinateOperation(code);
    }

    @Override
    public Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(String sourceCRS, String targetCRS) throws FactoryException {
        return factory().createFromCoordinateReferenceSystemCodes(sourceCRS, targetCRS);
    }
}
