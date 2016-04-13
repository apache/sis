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
package org.apache.sis.feature;

import java.util.Map;
import java.util.Collections;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.MultiValuedPropertyException;
import org.opengis.feature.Property;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.apache.sis.util.ObjectConverters;


/**
 * An aggregate operation concatenating the values of multiple properties.
 * This operation is mainly used as an {@code id} generator for multiple properties used as primary keys.
 *
 * <p>This operation support both reading and writing. When setting the operation value,
 * the value will be split and forwarded to each attribute element.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class AggregateOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2303047827010821381L;

    /**
     * The type of the result returned by the aggregate operation.
     */
    private static final AttributeType<String> TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(DefaultAttributeType.NAME_KEY, "String"), String.class, 1, 1, null);

    /**
     * The parameter descriptor for the "Aggregate" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = LinkOperation.parameters("Aggregate", 1);

    private final GenericName[] attributeNames;
    private final String prefix;
    private final String suffix;
    private final String separator;

    AggregateOperation(Map<String, ?> identification, String prefix, String suffix, String separator, GenericName ... attributeNames) {
        super(identification);
        this.attributeNames = attributeNames;
        this.prefix = (prefix == null) ? "" : prefix;
        this.suffix = (suffix == null) ? "" : suffix;
        this.separator = separator;
    }

    /**
     * Aggregation operation do not require any parameter.
     *
     * @return empty parameter group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * Aggregation operation generates a String result.
     *
     * @return aggregation string type
     */
    @Override
    public IdentifiedType getResult() {
        return TYPE;
    }

    /**
     * {@inheritDoc }
     *
     * @param  feature    The feature on which to execute the operation.
     *                    Can not be {@code null}.
     * @param  parameters The parameters to use for executing the operation.
     *                    Can be {@code null}.
     * @return The operation result, never null.
     */
    @Override
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        return new AggregateAttribute(feature);
    }

    /**
     * Operation attribute.
     * Value is calculated each time it is accessed.
     */
    private final class AggregateAttribute extends AbstractAttribute<String> {

        private final Feature feature;

        public AggregateAttribute(final Feature feature) {
            super(TYPE);
            this.feature = feature;
        }

        @Override
        public String getValue() throws MultiValuedPropertyException {
            final StringBuilder sb = new StringBuilder();
            sb.append(prefix);

            for (int i=0; i < attributeNames.length; i++) {
                if (i!=0) sb.append(separator);
                sb.append(feature.getPropertyValue(attributeNames[i].toString()));
            }

            sb.append(suffix);
            return sb.toString();
        }

        @Override
        public void setValue(String value) {
            //check prefix
            if (!value.startsWith(prefix)) {
                throw new IllegalArgumentException("Unvalid string, does not start with "+prefix);
            }
            if (!value.endsWith(suffix)) {
                throw new IllegalArgumentException("Unvalid string, does not end with "+suffix);
            }

            //split values, we don't use the regex split to avoid possible reserverd regex characters
            final Object[] values = new Object[attributeNames.length];
            int i = 0;
            int offset = 0;
            //remove prefix and suffix
            value = value.substring(prefix.length(), value.length() - suffix.length());
            while (true) {
                if (i >= values.length) {
                    throw new IllegalArgumentException("Unvalid string, expected "+values.length+" values, but found more");
                }
                final int idx = value.indexOf(separator, offset);
                if (idx == -1) {
                    //last element
                    values[i] = value.substring(offset);
                    i++;
                    break;
                } else {
                    values[i] = value.substring(offset, idx);
                    i++;
                    offset = (idx + separator.length());
                }
            }

            if (i != values.length) {
                throw new IllegalArgumentException("Unvalid string, number of values do not match, found "+(i)+" but expected "+values.length);
            }

            //set values, convert them if necessary
            final FeatureType type = feature.getType();
            for (int k=0; k < values.length; k++) {
                final String propName = attributeNames[k].toString();
                final AttributeType<?> pt = (AttributeType<?>) type.getProperty(propName);
                final Object val = ObjectConverters.convert(values[k], pt.getValueClass());
                feature.setPropertyValue(propName, val);
            }
        }
    }
}
