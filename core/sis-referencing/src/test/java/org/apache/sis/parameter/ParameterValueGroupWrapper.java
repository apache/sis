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
package org.apache.sis.parameter;

import java.util.List;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * {@link ParameterValueGroup} wrapper for hiding the implementation class.
 * Used when we want to prevent the optimizations detected by checks like
 * {@code if (x instanceof DefaultParameterValueGroup)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@SuppressWarnings("CloneInNonCloneableClass")
final strictfp class ParameterValueGroupWrapper implements ParameterValueGroup {
    /**
     * The implementation to hide.
     */
    private final ParameterValueGroup impl;

    /**
     * Creates a new wrapper for the given implementation.
     */
    ParameterValueGroupWrapper(final ParameterValueGroup impl) {
        this.impl = impl;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override public ParameterValueGroup         clone()                {return impl.clone();}
    @Override public ParameterDescriptorGroup    getDescriptor()        {return impl.getDescriptor();}
    @Override public List<GeneralParameterValue> values()               {return impl.values();}
    @Override public ParameterValue<?>           parameter(String name) {return impl.parameter(name);}
    @Override public List<ParameterValueGroup>   groups(String name)    {return impl.groups(name);}
    @Override public ParameterValueGroup         addGroup(String name)  {return impl.addGroup(name);}
}
