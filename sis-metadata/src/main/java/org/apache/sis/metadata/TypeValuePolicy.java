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
package org.apache.sis.metadata;


/**
 * The kind of values in the {@link MetadataStandard#asTypeMap MetadataStandard.asTypeMap(…)}.
 * This enumeration specifies whether the values shall be property types, element types (same
 * as property types except for collections) or the declaring classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 *
 * @see MetadataStandard#asTypeMap(Class, KeyNamePolicy, TypeValuePolicy)
 */
public enum TypeValuePolicy {
    /**
     * The type of a property, as inferred from the
     * {@linkplain java.lang.reflect.Method#getReturnType() return type} of the property method.
     * Collections are not handled in any special way; if the return type is a collection, then
     * the value is {@code Collection.class} (or a subclass).
     */
    PROPERTY_TYPE,

    /**
     * The type of a property, or type of elements if the property is a collection. This is the
     * same than {@link #PROPERTY_TYPE} except that collections are handled in a special way:
     * if the property is a collection, then the value is the type of <em>elements</em> in that
     * collection.
     *
     * {@note Current implementation has an additional slight difference: if the getter method
     *        in the implementation class declares a more specific return value than the getter
     *        method in the interface, and if the setter method (if any) expects the same specialized
     *        type, then <code>ELEMENT_TYPE</code> will use that specialized type. This is different
     *        than <code>PROPERTY_TYPE</code> which always use the type declared in the interface.}
     */
    ELEMENT_TYPE,

    /**
     * The type of the interface that declares the method. For any metadata object, different
     * properties may have different declaring interfaces if some properties were inherited
     * from parent interfaces.
     */
    DECLARING_INTERFACE,

    /**
     * The type of the class that declares the method. This is similar to
     * {@link #DECLARING_INTERFACE}, except that the implementation class
     * from the metadata standard is returned instead than the interface.
     */
    DECLARING_CLASS
}
