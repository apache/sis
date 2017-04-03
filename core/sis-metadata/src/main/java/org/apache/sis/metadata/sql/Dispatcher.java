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
package org.apache.sis.metadata.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * The handler for metadata proxy that implement (indirectly) metadata interfaces like
 * {@link org.opengis.metadata.Metadata}, {@link org.opengis.metadata.citation.Citation},
 * <i>etc</i>.
 *
 * Any call to a method in a metadata interface is redirected toward the {@link #invoke} method.
 * This method uses reflection in order to find the caller's method and class name. The class
 * name is translated into a table name, and the method name is translated into a column name.
 * Then the information is fetched in the underlying metadata database.
 *
 * @author  Touraïvane (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class Dispatcher implements InvocationHandler {
    /**
     * The identifier used in order to locate the record for this metadata entity in the database.
     * This is usually the primary key in the table which contains this entity.
     */
    final String identifier;

    /**
     * The connection to the database. All metadata handlers created from a single database
     * should share the same source.
     */
    private final MetadataSource source;

    /**
     * Index in the {@code CachedStatement} cache array where to search first. This is only a hint for increasing
     * the chances to find quickly a {@code CachedStatement} instance for the right type and identifier.
     */
    int preferredIndex;

    /**
     * Creates a new metadata handler.
     *
     * @param identifier  the identifier used in order to locate the record for this metadata entity in the database.
     *                    This is usually the primary key in the table which contains this entity.
     * @param source      the connection to the table which contains this entity.
     */
    public Dispatcher(final String identifier, final MetadataSource source) {
        this.identifier = identifier;
        this.source     = source;
        preferredIndex  = -1;
    }

    /**
     * Invoked when any method from a metadata interface is invoked.
     *
     * @param  proxy   the object on which the method is invoked.
     * @param  method  the method invoked.
     * @param  args    the argument given to the method.
     * @return the value to be returned from the public method invoked by the method.
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) {
        final Class<?> type = method.getDeclaringClass();
        final String   name = method.getName();
        final int      n    = (args != null) ? args.length : 0;
        switch (name) {
            case "toString": {
                if (n != 0) break;
                return toString(type);
            }
            case "hashCode": {
                if (n != 0) break;
                return System.identityHashCode(proxy);
            }
            case "equals": {
                if (n != 1) break;
                return proxy == args[0];
            }
            case "identifier": {
                if (n != 1) break;
                return (args[0] == source) ? identifier : null;
            }
            default: {
                if (n != 0) break;
                if (!source.standard.isMetadata(type)) break;
                /*
                 * The invoked method is a method from the metadata interface.
                 * Consequently, the information should exist in the database.
                 */
                try {
                    return source.getValue(type, method, this);
                } catch (SQLException | MetadataStoreException e) {
                    Class<?> returnType = method.getReturnType();
                    if (Collection.class.isAssignableFrom(returnType)) {
                        final Class<?> elementType = Classes.boundOfParameterizedProperty(method);
                        if (elementType != null) {
                            returnType = elementType;
                        }
                    }
                    throw new BackingStoreException(Errors.format(Errors.Keys.DatabaseError_2, returnType, identifier), e);
                }
            }
        }
        throw new BackingStoreException(Errors.format(Errors.Keys.UnsupportedOperation_1, type + "." + name));
    }

    /**
     * Returns a string representation of a metadata of the given type.
     */
    private String toString(final Class<?> type) {
        return Classes.getShortName(type) + "[id=“" + identifier + "”]";
    }

    /**
     * Returns a string representation of this handler.
     * This is mostly for debugging purpose.
     */
    @Override
    public String toString() {
        return toString(getClass());
    }
}
