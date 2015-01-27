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
package org.apache.sis.internal.jaxb.geometry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.Workaround;


/**
 * A minimalist XML object factory for getting JAXB to work without throwing exceptions when
 * there is no GML module in the classpath. This factory is extended with more complete methods
 * in the GML module.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@XmlRegistry
public class ObjectFactory {
    /**
     * The qualified name of {@code <AbstractGeometry>}.
     */
    protected static final QName AbstractGeometry_QNAME   = new QName(Namespaces.GML, "AbstractGeometry");

    /**
     * The qualified name of {@code <AbstractGML>}.
     */
    protected static final QName AbstractGML_QNAME   = new QName(Namespaces.GML, "AbstractGML");

    /**
     * The qualified name of {@code <AbstractObject>}.
     */
    protected static final QName AbstractObject_QNAME   = new QName(Namespaces.GML, "AbstractObject");

    /**
     * Creates an instance of {@code JAXBElement<Object>}}.
     *
     * @param  value The {@code Object} value to wrap.
     * @return The wrapped value.
     */
    @XmlElementDecl(name = "AbstractObject", namespace = Namespaces.GML)
    public JAXBElement<Object> createObject(final Object value) {
        return new JAXBElement<Object>(AbstractObject_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@code JAXBElement<AbstractGMLType>}}.
     * The type declared in the method signature should be {@code AbstractGMLType}.
     * However it is declared here as {@code Object} in order to avoid a dependency
     * toward the GML module.
     *
     * @param  value The GML {@code AbstractGMLType} value to wrap.
     * @return The wrapped value.
     */
    @Workaround(library = "JAXB", version = "2.1")
    @XmlElementDecl(name = "AbstractGML",
            namespace = Namespaces.GML,
            substitutionHeadName = "AbstractObject",
            substitutionHeadNamespace = Namespaces.GML) // Not necessary according javadoc, but appears to be in practice (JAXB 2.1 bug?)
    public JAXBElement<Object> createAbstractGML(final Object value) {
        return new JAXBElement<Object>(AbstractGML_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@code JAXBElement<AbstractGeometryType>}}.
     * The type declared in the method signature should be {@code AbstractGeometryType}.
     * However it is declared here as {@code Object} in order to avoid a dependency
     * toward the GML module.
     *
     * @param  value The {@code AbstractGeometryType} value to wrap.
     * @return The wrapped value.
     */
    @Workaround(library = "JAXB", version = "2.1")
    @XmlElementDecl(name = "AbstractGeometry",
            namespace = Namespaces.GML,
            substitutionHeadName = "AbstractGML",
            substitutionHeadNamespace = Namespaces.GML) // Not necessary according javadoc, but appears to be in practice (JAXB 2.1 bug?)
    public JAXBElement<Object> createAbstractGeometry(final Object value) {
        return new JAXBElement<Object>(AbstractGeometry_QNAME, Object.class, null, value);
    }
}
