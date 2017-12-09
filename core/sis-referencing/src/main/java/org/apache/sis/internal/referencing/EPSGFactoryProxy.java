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
import org.apache.sis.referencing.CRS;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ImageCRS;
import org.opengis.referencing.crs.ParametricCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;


/**
 * A factory that redirect all method to another factory. This factory is normally useless and not used by Apache SIS.
 * The sole purpose of this factory is to give an access to the EPSG factory through {@link java.util.ServiceLoader}.
 * We have to use this indirection level because the EPSG factory is managed in a special way by Apache SIS.
 *
 * <p>This class will be modified on the JDK9 branch in order to replace the proxy by a static {@code provider()}
 * method. This will allow us to remove all the indirection level currently found in this class.</p>
 */
public final class EPSGFactoryProxy implements CRSAuthorityFactory {
    private volatile CRSAuthorityFactory factory;

    public EPSGFactoryProxy() {
    }

    private CRSAuthorityFactory factory() throws FactoryException {
        CRSAuthorityFactory f = factory;
        if (f == null) {
            factory = f = CRS.getAuthorityFactory("EPSG");
        }
        return f;
    }

    @Override
    public Citation getAuthority() {
        try {
            return factory().getAuthority();
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Citation getVendor() {
        try {
            return factory().getVendor();
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InternationalString getDescriptionText(String code) throws FactoryException {
        return factory().getDescriptionText(code);
    }

    @Override
    public IdentifiedObject createObject(String code) throws FactoryException {
        return factory().createObject(code);
    }

    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(String code) throws FactoryException {
        return factory().createCoordinateReferenceSystem(code);
    }

    @Override
    public CompoundCRS createCompoundCRS(String code) throws FactoryException {
        return factory().createCompoundCRS(code);
    }

    @Override
    public DerivedCRS createDerivedCRS(String code) throws FactoryException {
        return factory().createDerivedCRS(code);
    }

    @Override
    public EngineeringCRS createEngineeringCRS(String code) throws FactoryException {
        return factory().createEngineeringCRS(code);
    }

    @Override
    public GeographicCRS createGeographicCRS(String code) throws FactoryException {
        return factory().createGeographicCRS(code);
    }

    @Override
    public GeocentricCRS createGeocentricCRS(String code) throws FactoryException {
        return factory().createGeocentricCRS(code);
    }

    @Override
    public ImageCRS createImageCRS(String code) throws FactoryException {
        return factory().createImageCRS(code);
    }

    @Override
    public ProjectedCRS createProjectedCRS(String code) throws FactoryException {
        return factory().createProjectedCRS(code);
    }

    @Override
    public TemporalCRS createTemporalCRS(String code) throws FactoryException {
        return factory().createTemporalCRS(code);
    }

    @Override
    public VerticalCRS createVerticalCRS(String code) throws FactoryException {
        return factory().createVerticalCRS(code);
    }

    @Override
    public ParametricCRS createParametricCRS(String code) throws FactoryException {
        return factory().createParametricCRS(code);
    }

    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) throws FactoryException {
        return factory().getAuthorityCodes(type);
    }
}
