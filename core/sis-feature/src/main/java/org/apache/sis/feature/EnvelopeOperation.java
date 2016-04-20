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

import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.jdk7.Objects;


/**
 * An operation computing the envelope that encompass all geometries found in a list of attributes.
 * Geometries can be in different coordinate reference systems; they will be transformed to the first
 * non-null CRS in the following choices:
 *
 * <ol>
 *   <li>the CRS specified at construction time,</li>
 *   <li>the CRS of the default geometry, or</li>
 *   <li>the CRS of the first non-empty geometry.</li>
 * </ol>
 *
 * <div class="section">Limitations</div>
 * If a geometry contains other geometries, this operation queries only the envelope of the root geometry.
 * It is the root geometry responsibility to take in account the envelope of all its children.
 *
 * <p>This operation is read-only. Calls to {@code Attribute.setValue(Envelope)} will result in an
 * {@link IllegalStateException} to be thrown.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class EnvelopeOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6250548001562807671L;

    /**
     * The parameter descriptor for the "Envelope" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = LinkOperation.parameters("Envelope", 1);

    /**
     * The names of all properties containing a geometry object.
     */
    private final String[] attributeNames;

    /**
     * The coordinate reference system of the envelope to compute, or {@code null}
     * for using the CRS of the default geometry or the first non-empty geometry.
     */
    final CoordinateReferenceSystem crs;

    /**
     * The coordinate conversions or transformations from the CRS used by the geometries to the CRS requested
     * by the user, or {@code null} if there is no operation to apply.  If non-null, the length of this array
     * shall be equal to the length of the {@link #attributeNames} array and element at index <var>i</var> is
     * the operation from the {@code attributeNames[i]} geometry CRS to the {@link #crs}.
     *
     * <p>This array contains null element when the {@code MathTransform} associated to the coordinate operation
     * is the identity transform.</p>
     */
    private final CoordinateOperation[] attributeToCRS;

    /**
     * The property names as an unmodifiable set, created when first needed.
     */
    private transient Set<String> dependencies;

    /**
     * The type of the result returned by the envelope operation.
     */
    private final DefaultAttributeType<Envelope> resultType;

    /**
     * Creates a new operation computing the envelope of features of the given type.
     *
     * @param identification     the name and other information to be given to this operation.
     * @param crs                the coordinate reference system of envelopes to computes, or {@code null}.
     * @param geometryAttributes the operation or attribute type from which to get geometry values.
     */
    EnvelopeOperation(final Map<String,?> identification, CoordinateReferenceSystem crs,
            final AbstractIdentifiedType[] geometryAttributes) throws FactoryException
    {
        super(identification);
        String defaultGeometry = null;
        final String characteristicName = AttributeConvention.CRS_CHARACTERISTIC.toString();
        /*
         * Get all property names without duplicated values. If a property is a link to an attribute,
         * then the key will be the name of the referenced attribute instead than the operation name.
         * The intend is to avoid querying the same geometry twice if the attribute is also specified
         * explicitely in the array of properties.
         *
         * The map values will be the default Coordinate Reference System, or null if none.
         */
        boolean characterizedByCRS = false;
        final Map<String,CoordinateReferenceSystem> names = new LinkedHashMap<String,CoordinateReferenceSystem>(4);
        for (AbstractIdentifiedType property : geometryAttributes) {
            if (AttributeConvention.isGeometryAttribute(property)) {
                final GenericName name = property.getName();
                final String attributeName = (property instanceof LinkOperation)
                                             ? ((LinkOperation) property).referentName : name.toString();
                final boolean isDefault = AttributeConvention.DEFAULT_GEOMETRY_PROPERTY.equals(name.tip());
                if (isDefault) {
                    defaultGeometry = attributeName;
                }
                CoordinateReferenceSystem attributeCRS = null;
                while (property instanceof AbstractOperation) {
                    property = ((AbstractOperation) property).getResult();
                }
                /*
                 * At this point 'property' is an attribute, otherwise isGeometryAttribute(property) would have
                 * returned false. Set 'characterizedByCRS' to true if we find at least one attribute which may
                 * have the "CRS" characteristic. Note that we can not rely on 'attributeCRS' being non-null
                 * because an attribute may be characterized by a CRS without providing default CRS.
                 */
                final DefaultAttributeType<?> at = ((DefaultAttributeType<?>) property).characteristics().get(characteristicName);
                if (at != null && CoordinateReferenceSystem.class.isAssignableFrom(at.getValueClass())) {
                    attributeCRS = (CoordinateReferenceSystem) at.getDefaultValue();              // May still null.
                    if (crs == null && isDefault) {
                        crs = attributeCRS;
                    }
                    characterizedByCRS = true;
                }
                JDK8.putIfAbsent(names, attributeName, attributeCRS);
            }
        }
        /*
         * Copy the names in an array with the default geometry first. If possible, find the coordinate operations
         * now in order to avoid the potentially costly call to CRS.findOperation(…) for each feature on which this
         * EnvelopeOperation will be applied.
         */
        names.remove(null);                                                                     // Paranoiac safety.
        attributeNames = new String[names.size()];
        attributeToCRS = characterizedByCRS ? new CoordinateOperation[attributeNames.length] : null;
        int n = (defaultGeometry == null) ? 0 : 1;
        for (final Map.Entry<String,CoordinateReferenceSystem> entry : names.entrySet()) {
            final int i;
            final String name = entry.getKey();
            if (name.equals(defaultGeometry)) {
                defaultGeometry = null;
                i = 0;
            } else {
                i = n++;
            }
            attributeNames[i] = name;
            if (characterizedByCRS) {
                final CoordinateReferenceSystem value = entry.getValue();
                if (value != null) {
                    if (crs == null) {
                        crs = value;                                    // Fallback if default geometry has no CRS.
                    }
                    final CoordinateOperation op = CRS.findOperation(value, crs, null);
                    if (!op.getMathTransform().isIdentity()) {
                        attributeToCRS[i] = op;
                    }
                }
            }
        }
        resultType = FeatureOperations.POOL.unique(new DefaultAttributeType<Envelope>(
                resultIdentification(identification), Envelope.class, 1, 1, null));
        this.crs = crs;
    }

    /**
     * Returns an empty group of parameters since this operation does not require any parameter.
     *
     * @return empty parameter group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * Returns the type of results computed by this operation, which is {@code AttributeType<Envelope>}.
     * The attribute type name depends on the value of {@code "result.*"} properties (if any)
     * given at construction time.
     *
     * @return an {@code AttributeType<Envelope>}.
     */
    @Override
    public AbstractIdentifiedType getResult() {
        return resultType;
    }

    /**
     * Returns the names of feature properties that this operation needs for performing its task.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized Set<String> getDependencies() {
        if (dependencies == null) {
            dependencies = CollectionsExt.immutableSet(true, attributeNames);
        }
        return dependencies;
    }

    /**
     * Returns an attribute whose value is the union of the envelopes of all geometries in the given feature
     * found in properties specified at construction time.
     *
     * @param  feature     the feature on which to execute the operation.
     * @param  parameters  ignored (can be {@code null}).
     * @return the envelope of geometries in feature property values.
     */
    @Override
    public Property apply(AbstractFeature feature, ParameterValueGroup parameters) {
        return new Result(feature);
    }




    /**
     * The attributes that contains the result of union of all envelope extracted from other attributes.
     * Value is calculated each time it is accessed.
     */
    private final class Result extends AbstractAttribute<Envelope> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 926172863066901618L;

        /**
         * The feature specified to the {@link StringJoinOperation#apply(Feature, ParameterValueGroup)} method.
         */
        private final AbstractFeature feature;

        /**
         * Creates a new attribute for the given feature.
         */
        Result(final AbstractFeature feature) {
            super(resultType);
            this.feature = feature;
        }

        /**
         * Computes an envelope which is the union of envelope of geometry values of all properties
         * specified to the {@link EnvelopeOperation} constructor.
         *
         * @return the union of envelopes of all geometries in the attribute specified to the constructor,
         *         or {@code null} if none.
         */
        @Override
        public Envelope getValue() throws IllegalStateException {
            final String[] attributeNames = EnvelopeOperation.this.attributeNames;
            GeneralEnvelope envelope = null;                                        // Union of all envelopes.
            for (int i=0; i<attributeNames.length; i++) {
                Envelope genv;                                                      // Envelope of a single geometry.
                final String name = attributeNames[i];
                if (attributeToCRS == null) {
                    /*
                     * If there is no CRS characteristic on any of the properties to query, then invoke the
                     * Feature.getPropertyValue(String) method instead than Feature.getProperty(String) in
                     * order to avoid forcing DenseFeature and SparseFeature implementations to wrap the
                     * property values into real property instances. This is an optimization for reducing
                     * the amount of objects to create.
                     */
                    genv = Geometries.getEnvelope(feature.getPropertyValue(name));
                    if (genv == null) continue;
                } else {
                    /*
                     * If there is at least one CRS characteristic to query, then we need the full Property instance.
                     * We do not distinguish which particular property may have a CRS characteristic because SIS 0.7
                     * implementations of DenseFeature and SparseFeature have a "all of nothing" behavior anyway.
                     */
                    final Property property = (Property) feature.getProperty(name);
                    genv = Geometries.getEnvelope(property.getValue());
                    if (genv == null) continue;
                    /*
                     * Get the CRS characteristic if present. Most of the time, 'at' will be null and we will
                     * fallback on the 'attributeToCRS' operations computed at construction time. In the rare
                     * cases where a CRS characteristic is associated to a particular feature, we will let
                     * Envelopes.transform(…) searches a coordinate operation.
                     */
                    final AbstractAttribute<?> at = ((AbstractAttribute<?>) property).characteristics()
                                    .get(AttributeConvention.CRS_CHARACTERISTIC.toString());
                    try {
                        if (at == null) {
                            final CoordinateOperation op = attributeToCRS[i];
                            if (op != null) {                           // Null operation means identity transform.
                                genv = Envelopes.transform(op, genv);
                            }
                        } else {                                                        // Should be a rare case.
                            final Object geomCRS = at.getValue();
                            if (!(geomCRS instanceof CoordinateReferenceSystem)) {
                                throw new IllegalStateException(Errors.format(Errors.Keys.UnspecifiedCRS));
                            }
                            ((GeneralEnvelope) genv).setCoordinateReferenceSystem((CoordinateReferenceSystem) geomCRS);
                            genv = Envelopes.transform(genv, crs);
                        }
                    } catch (TransformException e) {
                        throw new IllegalStateException(Errors.format(Errors.Keys.CanNotTransformEnvelope), e);
                    }
                }
                if (envelope == null) {
                    envelope = GeneralEnvelope.castOrCopy(genv);        // Should always be a cast without copy.
                } else {
                    envelope.add(genv);
                }
            }
            return envelope;
        }

        /**
         * Unconditionally throws an {@link UnsupportedOperationException}.
         */
        @Override
        public void setValue(Envelope value) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, AbstractAttribute.class));
        }
    }

    /**
     * Computes a hash-code value for this operation.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(attributeNames) + Arrays.hashCode(attributeToCRS);
    }

    /**
     * Compares this operation with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (super.equals(obj)) {
            // 'this.result' is compared (indirectly) by the super class.
            final EnvelopeOperation that = (EnvelopeOperation) obj;
            return Arrays.equals(attributeNames, that.attributeNames) &&
                   Arrays.equals(attributeToCRS, that.attributeToCRS) &&
                   Objects.equals(crs, that.crs);
        }
        return false;
    }
}
