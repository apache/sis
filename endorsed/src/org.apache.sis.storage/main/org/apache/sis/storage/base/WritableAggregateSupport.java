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
package org.apache.sis.storage.base;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.lang.reflect.Modifier;
import org.opengis.util.GenericName;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IncompatibleResourceException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Localized;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Helper classes for the management of {@link WritableAggregate}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WritableAggregateSupport implements Localized {
    /**
     * The resource where to write.
     */
    private final WritableAggregate target;

    /**
     * Creates a new helper class.
     *
     * @param  target  the resource where to write.
     */
    public WritableAggregateSupport(final WritableAggregate target) {
        this.target = target;
    }

    /**
     * {@return the locale used by the targetresource for error messages, or {@code null} if unknown}.
     */
    @Override
    public final Locale getLocale() {
        return (target instanceof Localized) ? ((Localized) target).getLocale() : null;
    }

    /**
     * Writes the components of the given aggregate.
     *
     * @param  resource  the aggregate to write.
     * @return the effectively added aggregate.
     * @throws DataStoreException if an error occurred while writing a component.
     */
    public Resource writeComponents(final Aggregate resource) throws DataStoreException {
        try {
            final Collection<? extends Resource> components = resource.components();
            final var effectives = new ArrayList<Resource>(components.size());
            for (final Resource component : components) {
                effectives.add(target.add(component));
            }
            return new SimpleAggregate(target, effectives);
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
    }

    /**
     * Returns the given resource as a grid coverage, or throws an exception if it cannot be cast.
     *
     * @param  resource  the resource which is required to be a grid coverage resource.
     * @return the given resource after cast.
     * @throws IncompatibleResourceException if the given resource is not for a grid coverage.
     */
    public GridCoverageResource asGridCoverage(final Resource resource) throws DataStoreException {
        if (Objects.requireNonNull(resource) instanceof GridCoverageResource) {
            return (GridCoverageResource) resource;
        }
        throw new IncompatibleResourceException(message(GridCoverageResource.class, resource)).addAspect("class");
    }

    /**
     * Returns the error message for a resource that cannot be added to an aggregate.
     *
     * @param  expected  the expected type of resource.
     * @param  actual    the actual resource.
     * @return the error message to give to the exception to be thrown.
     */
    private String message(final Class<? extends Resource> expected, final Resource actual) throws DataStoreException {
        Class<? extends Resource> type = actual.getClass();
        for (Class<? extends Resource> t : Classes.getLeafInterfaces(type, Resource.class)) {
            if (Modifier.isPublic(t.getModifiers())) {
                type = t;
                break;
            }
        }
        return Resources.forLocale(getLocale()).getString(Resources.Keys.IllegalResourceTypeForAggregate_3,
                target.getIdentifier().map(GenericName::toString).orElse(Classes.getShortName(actual.getClass())), expected, type);
    }
}
