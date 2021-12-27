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
package org.apache.sis.internal.sql.feature;

import java.sql.ResultSet;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;


/**
 * An implementation of {@link ResultSet} where the {@code getBytes(1)} method returns a
 * predefined array of bytes. Because of the large amount of methods in {@code ResultSet},
 * this class implements that interface using reflection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class ResultSetMock implements InvocationHandler {
    /**
     * The bytes to return on invocation on {@link ResultSet#getBytes(int)}.
     */
    private final byte[] wkb;

    /**
     * Creates a new handler for a {@link ResultSet} mock.
     */
    private ResultSetMock(final byte[] wkb) {
        this.wkb = wkb;
    }

    /**
     * Creates a mock where {@link ResultSet#getBytes(int)} returns the given array of bytes.
     */
    static ResultSet create(final byte[] wkb) {
        return (ResultSet) Proxy.newProxyInstance(ResultSetMock.class.getClassLoader(),
                            new Class<?>[] {ResultSet.class}, new ResultSetMock(wkb));
    }

    /**
     * Implementation of all {@link ResultSet} methods. Only {@code ResultSet.getBytes(1)}
     * is currently supported. All other method invocations cause an {@link AssertionError}
     * to be thrown.
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) {
        if (method.getName().equals("getBytes") && args.length == 1 && Integer.valueOf(1).equals(args[0])) {
            return wkb;
        }
        throw new AssertionError(method);
    }
}
