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
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.referencing.GeodeticException;


/**
 * A factory that redirect all method to another factory. This factory is normally useless and not used by Apache SIS.
 * The sole purpose of this factory is to give an access to the EPSG factory through {@link java.util.ServiceLoader}.
 * We have to use this indirection level because the EPSG factory is managed in a special way by Apache SIS.
 *
 * <p>This class will be modified on the JDK9 branch in order to replace the proxy by a static {@code provider()}
 * method. This will allow us to remove all the indirection level currently found in this class.</p>
 */
public abstract class EPSGFactoryProxy implements AuthorityFactory {
    EPSGFactoryProxy() {
    }

    abstract AuthorityFactory factory() throws FactoryException;

    @Override
    public final Citation getAuthority() {
        try {
            return factory().getAuthority();
        } catch (FactoryException e) {
            throw new GeodeticException(e);
        }
    }

    @Override
    public final Citation getVendor() {
        try {
            return factory().getVendor();
        } catch (FactoryException e) {
            throw new GeodeticException(e);
        }
    }

    @Override
    public final InternationalString getDescriptionText(String code) throws FactoryException {
        return factory().getDescriptionText(code);
    }

    @Override
    public final IdentifiedObject createObject(String code) throws FactoryException {
        return factory().createObject(code);
    }

    @Override
    public final Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) throws FactoryException {
        return factory().getAuthorityCodes(type);
    }
}
