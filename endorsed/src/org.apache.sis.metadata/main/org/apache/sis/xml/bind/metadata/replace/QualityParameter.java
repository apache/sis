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
package org.apache.sis.xml.bind.metadata.replace;

import java.util.Map;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.TypeName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.quality.DefaultMeasureDescription;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.bind.gco.GO_GenericName;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Names;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.ReferenceIdentifier;

// Specific to the main branch:
import org.apache.sis.metadata.internal.shared.ReferencingServices;


/**
 * Parameter information conform to the ISO 19157:2013 specification.
 * GeoAPI tries to provides a single API for the parameter classes defined in various specifications
 * (ISO 19111, ISO 19115, ISO 19157, Web Processing Service).
 * But we still need separated representations at XML (un)marshalling time.
 * This class is for the ISO 19157:2013 case.
 *
 * <p>Note that this implementation is simple and serves no other purpose than being a container for XML
 * parsing and formatting. For real parameter framework, consider using {@link org.apache.sis.parameter}
 * package instead.</p>
 *
 * <h2>Note about raw-type usage</h2>
 * We use raw type (i.e. we implement {@code ParameterDescriptor} instead of {@code ParameterDescriptor<T>})
 * because there is no way we can know {@code <T>} for sure at unmarshalling time. This is not a recommended
 * practice, so <strong>this class shall not be in public API</strong>. However, it should be okay to create
 * {@code QualityParameter} instances in Apache SIS internal code if all methods creating such instances
 * declare {@code ParameterDescriptor<?>} as their return type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("rawtypes")   // For the omission of <T> in Parameter<T> - see javadoc.
@XmlType(name = "DQM_Parameter_Type", namespace = Namespaces.DQM, propOrder = {
    "code",
    "definition",
    "description",
    "valueType"
})
@XmlRootElement(name = "DQM_Parameter", namespace = Namespaces.DQM)
public final class QualityParameter extends Parameter {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4333632866772665659L;

    /**
     * Name of the data quality parameter, to be represented as identifier code.
     *
     * @see #getName()
     */
    @XmlElement(name="name", required=true)
    public String code;

    /**
     * Definition of the data quality parameter.
     * Stored in {@link ReferenceIdentifier#getDescription()}.
     *
     * @see #getName()
     */
    @XmlElement(required = true)
    @SuppressWarnings("serial")                 // Most Apache SIS implementations are serializable.
    public InternationalString definition;

    /**
     * Description of the data quality parameter.
     */
    @XmlElement
    public DefaultMeasureDescription description;

    /**
     * Value type of the data quality parameter (shall be one of the data types defined in ISO/TS 19103:2005).
     *
     * @see #getValueType()
     * @see #getValueClass()
     */
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(GO_GenericName.class)   // Not in package-info because shall not be applied to getLegacyName().
    @SuppressWarnings("serial")                 // Most Apache SIS implementations are serializable.
    public TypeName valueType;

    /**
     * Creates an initially empty parameter.
     * This constructor is needed by JAXB at unmarshalling time.
     */
    public QualityParameter() {
    }

    /**
     * Creates a parameter initialized to the values of the given one.
     * This is used for marshalling an arbitrary parameter as an ISO 19157 parameter.
     */
    @SuppressWarnings("unchecked")
    private QualityParameter(final ParameterDescriptor<?> parameter) {
        super(parameter);
        final Identifier id = parameter.getName();
        if (id != null) {
            code = id.getCode();
        }
        valueType = ReferencingServices.getInstance().getValueType(parameter);
    }

    /**
     * Returns the given parameter as an instance of {@code QualityParameter}.
     *
     * @param  parameter  the parameter (may be {@code null}).
     * @return the service parameter, or {@code null} if the given argument was null.
     */
    public static QualityParameter castOrCopy(final ParameterDescriptor<?> parameter) {
        if (parameter == null || parameter instanceof QualityParameter) {
            return (QualityParameter) parameter;
        }
        return new QualityParameter(parameter);
    }

    @Override public int getMinimumOccurs() {return 0;}
    @Override public int getMaximumOccurs() {return 1;}

    /**
     * Returns the name as an {@code Identifier}, which is the type requested by ISO 19111.
     * Note that this is different than the type requested by ISO 19157, which is {@link String}.
     *
     * @return the parameter name as an identifier (the type specified by ISO 19111).
     */
    @Override
    public synchronized ReferenceIdentifier getName() {
        if (name == null && code != null) {
            final var id = new RS_Identifier(null, code, null);
            id.setDescription(definition);
            id.transitionTo(DefaultIdentifier.State.FINAL);
            name = id;
        }
        return name;
    }

    /**
     * Infers the value class from the type name.
     * This method is the reason why we cannot parameterize this {@code QualityParameter} class
     * (see <cite>Note about raw-type usage</cite> in class javadoc), because there is no way we
     * can ensure that the class inferred from {@link #valueType} is really for type {@code <T>}.
     *
     * @return the value class inferred from the attribute type, or {@code null} if unknown.
     */
    @Override
    public Class<?> getValueClass() {
        Class<?> type = super.getValueClass();
        if (type == null) {
            type = Names.toClass(valueType);
        }
        return type;
    }

    /**
     * Returns the name that describes the type of parameter values.
     *
     * @return the type name of value component(s) in this parameter.
     */
    @Override
    public TypeName getValueType() {
        return valueType;
    }

    /**
     * Suggests a type name for the components of given collection or array class.
     * The component type is fetched on a <em>best effort</em> basis only.
     * This method does the following checks:
     * <ul>
     *   <li>If the given class is a class, then its {@linkplain Class#getComponentType() component type} is used.</li>
     *   <li>Otherwise if the class is an {@link Iterable}, then the upper bound of elements is fetched.</li>
     *   <li>Otherwise if the class is a {@link Map}, then the upper bound of keys is fetched.</li>
     *   <li>Otherwise if the class is a {@link Matrix}, then {@link Double} components is assumed.</li>
     *   <li>Otherwise the given class is used as if it was already a component type (i.e. a singleton item).</li>
     * </ul>
     *
     * @todo {@code Coverage} case needs to be added. It would be handle like {@link Matrix}.
     *
     * @param  valueClass  the type of values for which to infer a {@link TypeName} instance.
     * @return a type name for components of the given type.
     */
    public static TypeName getValueType(Class<?> valueClass) {
        if (valueClass.isArray()) {
            valueClass = valueClass.getComponentType();
        } else if (Iterable.class.isAssignableFrom(valueClass) || Map.class.isAssignableFrom(valueClass)) {
            valueClass = Classes.boundOfParameterizedDeclaration(valueClass);
        } else if (Matrix.class.isAssignableFrom(valueClass)) {
            valueClass = Double.class;
        }
        return Names.createTypeName(valueClass);
    }
}
