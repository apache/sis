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
package org.apache.sis.internal.netcdf;

import java.util.Iterator;
import java.util.Optional;
import org.apache.sis.internal.referencing.LazySet;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import ucar.nc2.constants.CDM;


/**
 * Extends the CF-Conventions with some conventions particular to a data producer.
 * By default, Apache SIS netCDF reader applies the <a href="http://cfconventions.org">CF conventions</a>.
 * But some data producers does not provides all necessary information for allowing Apache SIS to read the
 * netCDF file. Some information may be missing because considered implicit by the data producer.
 * This class provides a mechanism for supplying the implicit values.
 * Conventions can be registered in a file having this exact path:
 *
 * <blockquote><pre>META-INF/services/org.apache.sis.internal.netcdf.Convention</pre></blockquote>
 *
 * <p><b>This is an experimental class for internal usage only (for now).</b>
 * The API of this class is likely to change in any future Apache SIS version.
 * This class may become public (in a modified form) in the future if we gain
 * enough experience about extending netCDF conventions.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-315">SIS-315</a>
 *
 * @since 1.0
 * @module
 */
public class Convention {
    /**
     * All conventions found on the classpath.
     */
    private static final LazySet<Convention> AVAILABLES = new LazySet<>(Convention.class);

    /**
     * The convention to use when no specific conventions were found.
     */
    private static final Convention DEFAULT = new Convention();

    /**
     * For subclass constructors.
     */
    protected Convention() {
    }

    /**
     * Finds the convention to apply to the file opened by the given decoder, or {@code null} if none.
     */
    static synchronized Convention find(final Decoder decoder) {
        final Iterator<Convention> it;
        Convention c;
        synchronized (AVAILABLES) {
            it = AVAILABLES.iterator();
            if (!it.hasNext()) return DEFAULT;
            c = it.next();
        }
        while (!c.isApplicableTo(decoder)) {
            synchronized (AVAILABLES) {
                if (!it.hasNext()) return DEFAULT;
                c = it.next();
            }
        }
        return c;
    }

    /**
     * Detects if this set of conventions applies to the given netCDF file.
     *
     * @param  decoder  the netCDF file to test.
     * @return {@code true} if this set of conventions can apply.
     */
    protected boolean isApplicableTo(final Decoder decoder) {
        return false;
    }

    /**
     * Returns the role of the given variable. In particular, this method shall return
     * {@link VariableRole#AXIS} if the given variable seems to be a coordinate system axis.
     *
     * @param  variable  the variable for which to get the role, or {@code null}.
     * @return role of the given variable, or {@code null} if the given variable was null.
     */
    public VariableRole roleOf(final Variable variable) {
        return (variable != null) ? variable.getRole() : null;
    }

    /**
     * Returns the names of the variables containing data for all dimension of a variable.
     * Each netCDF variable can have an arbitrary number of dimensions identified by their name.
     * The data for a dimension are usually stored in a variable of the same name, but not always.
     * This method gives an opportunity for subclasses to select the axis variables using other criterion.
     * This happen for example if a netCDF file defines two grids for the same dimensions.
     * The order in returned array will be the axis order in the Coordinate Reference System.
     *
     * <p>The default implementation returns {@code null}.</p>
     *
     * @param  variable  the variable for which the list of axis variables are desired, in CRS order.
     * @return names of the variables containing axis values, or {@code null} if this
     *         method performs applies no special convention for the given variable.
     */
    public String[] namesOfAxisVariables(Variable variable) {
        return null;
    }

    /**
     * Build a function aiming to convert values packed in given variable into geophysical measures.
     *
     * @param source The variable specifying the transfer function.
     *
     * @return A transfer function matching given variable. Note that if we cannot find any information in input
     * variable, we return an identity transform. Never null.
     */
    public TransferFunction getTransferFunction(final Variable source) {
        /*
         * If scale_factor and/or add_offset variable attributes are present, then this is
         * a "packed" variable. Otherwise the transfer function is the identity transform.
         */
        final TransferFunction tr = new TransferFunction();
        final double scale = source.getAttributeAsNumber(CDM.SCALE_FACTOR);
        final double offset = source.getAttributeAsNumber(CDM.ADD_OFFSET);
        if (!Double.isNaN(scale)) {
            tr.setScale(scale);
        }
        if (!Double.isNaN(offset)) {
            tr.setOffset(offset);
        }

        return tr;
    }

    /**
     * Search in given variable for information about its range of valid values.
     *
     * @param source The variable to get valid range of values for.
     * @return The range of expected measures, or nothing if we didn't find any related information.
     */
    public Optional<NumberRange<?>> getValidValues(final Variable source) {
        return Optional.ofNullable(source.getValidValues());
    }
}
