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
package org.apache.sis.util.internal.shared;


/**
 * Workaround for the absence of public {@code clone()} method in the standard interface.
 * The purpose of this interface is to avoid the following exception when {@link Cloner}
 * tries to clone a class defined in an internal package of another module:
 *
 * <blockquote>{@link IllegalAccessException}: class {@link Cloner}
 * (in module {@code org.apache.sis.util}) cannot access class <var>Foo</var>
 * (in module <var>bar</var>) because module <var>bar</var> does not export
 * <var>foo</var> to module {@code org.apache.sis.util}</blockquote>
 *
 * This workaround is needed only for Apache SIS internal classes, because {@link Cloner}
 * usage of reflection should work for exported packages.
 *
 * <p>This interface may be removed in any future Apache SIS version if we find a better
 * way to workaround the lack of public {@code clone()} method in {@link Cloneable}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public interface CloneAccess extends java.lang.Cloneable {
    /**
     * Returns a clone of this object.
     *
     * @return a clone of this object.
     * @throws CloneNotSupportedException if clones are not supported.
     */
    Object clone() throws CloneNotSupportedException;
}
