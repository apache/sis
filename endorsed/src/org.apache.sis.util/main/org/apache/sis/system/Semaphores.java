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
package org.apache.sis.system;

import java.util.EnumSet;
import java.util.logging.Level;
import org.apache.sis.util.Workaround;
import org.apache.sis.pending.jdk.ScopedValue;


/**
 * Thread-local flags that need to be shared across different packages. Each thread has its own set of flags.
 * The {@link #clear()} method must be invoked after {@link #set()} in a {@code try} … {@code finally} block.
 * The {@link #execute(Supplier)} method can also be invoked instead.
 *
 * <p>This class duplicates a little bit the service provided by {@link ScopedValue}.
 * We may delete or refactor this class when we will be allowed to target Java 25.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum Semaphores {
    /**
     * A flag to indicate that empty collections should be returned as {@code null}. Returning null
     * collections is not a recommended practice, but is useful in some situations like marshalling
     * a XML document with JAXB, when we want to omit empty XML blocks.
     */
    NULL_FOR_EMPTY_COLLECTION,

    /**
     * A flag to indicate that only metadata are desired and that there is no need to create costly objects.
     * This flag is used during iteration over many coordinate operations before to select a single one by
     * inspecting only their metadata.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-327">SIS-327</a>
     */
    METADATA_ONLY,

    /**
     * A lock for avoiding never-ending recursion in the {@code equals} method of {@code AbstractDerivedCRS}
     * and {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}.
     * It is set to {@code true} when a comparison is in progress. This lock is necessary because
     * {@code AbstractDerivedCRS} objects contain a {@code conversionFromBase} field, which contains a
     * {@code DefaultConversion.targetCRS} field referencing back the {@code AbstractDerivedCRS} object.
     */
    COMPARING_CONVERSION_OR_DERIVED_CRS,

    /**
     * A flag to indicate that {@link org.apache.sis.referencing.operation.AbstractCoordinateOperation}
     * is querying parameters of a {@code MathTransform} enclosed in the operation. This is often at the
     * time of formatting the <abbr>WKT</abbr> of a {@code "ProjectedCRS"} element.
     */
    TRANSFORM_ENCLOSED_IN_OPERATION,

    /**
     * A flag to indicate that a parameter value outside its domain of validity should not cause an exception
     * to be thrown. This flag is set only when creating a deprecated operation from the EPSG database.
     * Typically the operation is deprecated precisely because it used invalid parameter values,
     * but SIS should still be able to create those deprecated objects if a user request them.
     *
     * <p><b>Example:</b> EPSG:3752 was a Mercator (variant A) projection but set the latitude of origin to 41°S.</p>
     */
    @Workaround(library = "EPSG:3752", version = "8.9")        // Deprecated in 2007 but still present in 2016.
    SUSPEND_PARAMETER_CHECK,

    /**
     * A flag to indicate that a finer logging level should be used for reporting geodetic object creations.
     * This flag is used during operations that potentially create a large number of CRSs, for example when
     * trying many CRS candidates in search for a CRS compliant with some criteria.
     */
    FINER_LOG_LEVEL_FOR_OBJECTS_CREATION,

    /**
     * A flag to indicate that a finer logging level should be used for reporting the use of deprecated codes.
     * This flag is used during operations creating an object which is known to be deprecated.
     */
    FINER_LOG_LEVEL_FOR_DEPRECATION;

    /**
     * The flags per running thread.
     */
    private static final ThreadLocal<EnumSet<Semaphores>> FLAGS = new ThreadLocal<>();

    /**
     * Returns the log level to use during the creation of an object.
     * This is used with {@link #FINER_LOG_LEVEL_FOR_OBJECTS_CREATION}
     * and {@link #FINER_LOG_LEVEL_FOR_DEPRECATION}.
     *
     * @param  usual  the log level that would normally be used.
     * @return the log level to use.
     */
    public final Level getLogLevel(final Level usual) {
        return get() ? Level.FINER : usual;
    }

    /**
     * Returns {@code true} if this flag is set.
     *
     * @return {@code true} if this flag is set.
     */
    public final boolean get() {
        final EnumSet<Semaphores> s = FLAGS.get();
        return (s != null) && s.contains(this);
    }

    /**
     * Sets this flag.
     *
     * @return whether the status of this flag changed as a result of this method call.
     */
    public final boolean set() {
        final EnumSet<Semaphores> s = FLAGS.get();
        if (s != null) {
            return s.add(this);
        }
        FLAGS.set(EnumSet.of(this));
        return true;
    }

    /**
     * Clears this flag.
     */
    public final void clear() {
        final EnumSet<Semaphores> s = FLAGS.get();
        if (s != null && s.remove(this) && s.isEmpty()) {
            FLAGS.remove();
        }
    }

    /**
     * Clears this flag if the given value is {@code true}.
     * This is a convenience method for a common pattern with {@code try} … {@code finally} blocks.
     *
     * @param  reset  value returned by {@link #set()}.
     */
    public final void clearIfTrue(final boolean reset) {
        if (reset) clear();
    }

    /**
     * Executes the given method in a block with this flag set.
     * This is an alternative to the use of {@link #set()} and {@link #clearIfTrue(boolean)}.
     * In Apache <abbr>SIS</abbr> code, we use this method only for small blocks of code and
     * the above alternatives for larger blocks.
     *
     * @param  <R>     type of value returned by the supplier.
     * @param  <X>     type of exception thrown by the supplier.
     * @param  method  the method to invoke for getting the value.
     * @return value computed by the given method.
     * @throws X exception thrown by the supplier.
     */
    public final <R, X extends Throwable> R execute(final ScopedValue.CallableOp<R, X> method) throws X {
        EnumSet<Semaphores> s = FLAGS.get();
        if (s == null) {
            s = EnumSet.of(this);
            FLAGS.set(s);
        } else if (!s.add(this)) {
            return method.call();
        }
        try {
            return method.call();
        } finally {
            s.remove(this);
            if (s.isEmpty()) {
                FLAGS.remove();
            }
        }
    }
}
