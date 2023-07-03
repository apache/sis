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
package org.apache.sis.internal.system;

import java.util.Set;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.function.Consumer;


/**
 * Contextual information fetching by reflection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.4
 * @since   0.8
 */
public final class Reflect implements Consumer<StackWalker.StackFrame> {
    /**
     * Returns the context class loader, but makes sure that it has Apache SIS on its classpath.
     * First, this method invokes {@link Thread#getContextClassLoader()} for the current thread.
     * Then this method scans over all Apache SIS classes on the stack trace. For each SIS class,
     * its loader is compared to the above-cited context class loader. If the context class loader
     * is equal or is a child of the SIS loader, then it is left unchanged. Otherwise the context
     * class loader is replaced by the SIS one.
     *
     * <p>The intent of this method is to ensure that {@link ServiceLoader#load(Class)} will find the
     * Apache SIS services even in an environment that defined an unsuitable context class loader.
     * Note that the call to {@code ServiceLoader.load(…)} must be done from the caller class.
     * We cannot provide this convenience in this class because of JPMS encapsulation.</p>
     *
     * @return the context class loader if suitable, or another class loader otherwise.
     * @throws SecurityException if this method is not allowed to get the current thread
     *         context class loader or one of its parent.
     */
    public static ClassLoader getContextClassLoader() throws SecurityException {
        final Reflect walker = new Reflect();
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk((stream) -> {
            stream.forEach(walker);
            return walker.loader;
        });
    }

    /**
     * The context class loader to be returned by {@link #getContextClassLoader()}.
     */
    private ClassLoader loader;

    /**
     * All parents of {@link #loader}.
     */
    private final Set<ClassLoader> parents;

    /**
     * Creates a new walker initialized to the context class loader of current thread.
     */
    private Reflect() {
        parents = new HashSet<>();
        setClassLoader(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Set the class loader to the given value, which may be null.
     */
    private void setClassLoader(ClassLoader c) {
        loader = c;
        while (c != null) {
            parents.add(c);
            c = c.getParent();
        }
    }

    /**
     * Action to be executed for each stack frame inspected by {@link #getContextClassLoader()}.
     * The action is initialized to the context class loader of current thread.
     * Then it checks if the class loader should be replaced by another one
     * containing at least the Apache SIS class loader.
     *
     * @param  frame  a stack frame being inspected.
     */
    @Override
    public void accept(final StackWalker.StackFrame frame) {
        if (frame.getClassName().startsWith(Modules.CLASSNAME_PREFIX)) {
            ClassLoader c = frame.getDeclaringClass().getClassLoader();
            if (!parents.contains(c)) {
                parents.clear();
                setClassLoader(c);
            }
        }
    }
}
