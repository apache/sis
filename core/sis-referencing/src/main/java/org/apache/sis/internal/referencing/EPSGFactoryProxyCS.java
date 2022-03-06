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

import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CylindricalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.ParametricCS;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.cs.VerticalCS;
import org.apache.sis.referencing.CRS;


/**
 * Same as {@link EPSGFactoryProxyCRS} but for coordinate systems.
 *
 * <p>This class will be modified on the JDK9 branch in order to replace the proxy by a static {@code provider()}
 * method. This will allow us to remove all the indirection level currently found in this class.</p>
 */
public final class EPSGFactoryProxyCS extends EPSGFactoryProxy implements CSAuthorityFactory {
    private volatile CSAuthorityFactory factory;

    @Override
    CSAuthorityFactory factory() throws FactoryException {
        CSAuthorityFactory f = factory;
        if (f == null) {
            factory = f = (CSAuthorityFactory) CRS.getAuthorityFactory("EPSG");
        }
        return f;
    }

    @Override
    public CoordinateSystem createCoordinateSystem(String code) throws FactoryException {
        return factory().createCoordinateSystem(code);
    }

    @Override
    public CartesianCS createCartesianCS(String code) throws FactoryException {
        return factory().createCartesianCS(code);
    }

    @Override
    public PolarCS createPolarCS(String code) throws FactoryException {
        return factory().createPolarCS(code);
    }

    @Override
    public CylindricalCS createCylindricalCS(String code) throws FactoryException {
        return factory().createCylindricalCS(code);
    }

    @Override
    public SphericalCS createSphericalCS(String code) throws FactoryException {
        return factory().createSphericalCS(code);
    }

    @Override
    public EllipsoidalCS createEllipsoidalCS(String code) throws FactoryException {
        return factory().createEllipsoidalCS(code);
    }

    @Override
    public VerticalCS createVerticalCS(String code) throws FactoryException {
        return factory().createVerticalCS(code);
    }

    @Override
    public TimeCS createTimeCS(String code) throws FactoryException {
        return factory().createTimeCS(code);
    }

    @Override
    public ParametricCS createParametricCS(String code) throws FactoryException {
        return factory().createParametricCS(code);
    }

    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(String code) throws FactoryException {
        return factory().createCoordinateSystemAxis(code);
    }

    @Override
    public Unit<?> createUnit(String code) throws FactoryException {
        return factory().createUnit(code);
    }
}
