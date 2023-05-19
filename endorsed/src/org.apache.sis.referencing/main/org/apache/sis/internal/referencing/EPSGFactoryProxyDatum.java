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

import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.datum.ImageDatum;
import org.opengis.referencing.datum.ParametricDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;


/**
 * Same as {@link EPSGFactoryProxyCRS} but for datum.
 *
 * <p>This class will be modified on the JDK9 branch in order to replace the proxy by a static {@code provider()}
 * method. This will allow us to remove all the indirection level currently found in this class.</p>
 */
public final class EPSGFactoryProxyDatum extends EPSGFactoryProxy implements DatumAuthorityFactory {
    private volatile DatumAuthorityFactory factory;

    @Override
    DatumAuthorityFactory factory() throws FactoryException {
        DatumAuthorityFactory f = factory;
        if (f == null) {
            factory = f = (DatumAuthorityFactory) CRS.getAuthorityFactory("EPSG");
        }
        return f;
    }

    @Override
    public Datum createDatum(String code) throws FactoryException {
        return factory().createDatum(code);
    }

    @Override
    public GeodeticDatum createGeodeticDatum(String code) throws FactoryException {
        return factory().createGeodeticDatum(code);
    }

    @Override
    public Ellipsoid createEllipsoid(String code) throws FactoryException {
        return factory().createEllipsoid(code);
    }

    @Override
    public PrimeMeridian createPrimeMeridian(String code) throws FactoryException {
        return factory().createPrimeMeridian(code);
    }

    @Override
    public EngineeringDatum createEngineeringDatum(String code) throws FactoryException {
        return factory().createEngineeringDatum(code);
    }

    @Override
    public ImageDatum createImageDatum(String code) throws FactoryException {
        return factory().createImageDatum(code);
    }

    @Override
    public TemporalDatum createTemporalDatum(String code) throws FactoryException {
        return factory().createTemporalDatum(code);
    }

    @Override
    public VerticalDatum createVerticalDatum(String code) throws FactoryException {
        return factory().createVerticalDatum(code);
    }

    @Override
    public ParametricDatum createParametricDatum(String code) throws FactoryException {
        return factory().createParametricDatum(code);
    }
}
