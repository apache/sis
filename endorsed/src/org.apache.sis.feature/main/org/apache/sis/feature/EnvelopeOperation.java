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
import java.util.Objects;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.CRS;
import org.apache.sis.pending.jdk.JDK21;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureInstantiationException;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;


/**
 * An operation computing the envelope that encompass all geometries found in a list of attributes.
 * Geometries can be in different coordinate reference systems. They will be transformed to the first
 * non-null <abbr>CRS</abbr> in the following choices:
 *
 * <ol>
 *   <li>the CRS specified at construction time,</li>
 *   <li>the CRS of the default geometry, or</li>
 *   <li>the CRS of the first non-empty geometry.</li>
 * </ol>
 *
 * <h2>Limitations</h2>
 * If a geometry contains other geometries, this operation queries only the envelope of the root geometry.
 * It is the root geometry responsibility to take in account the envelope of all its children.
 *
 * <p>This operation is read-only. Calls to {@code Attribute.setValue(Envelope)} will result in an
 * {@link IllegalStateException} to be thrown.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
final class EnvelopeOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2435142477482749321L;

    /**
     * The parameter descriptor for the "Envelope" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = parameters("Envelope");

    /**
     * The names of all properties containing a geometry object. The attributes are the in order specified by the user,
     * except the default geometry (if any) which is always first. Note that the name of the default geometry in this
     * array is usually <em>not</em> {@value AttributeConvention#GEOMETRY}, because this class replaces links by their
     * targets for avoiding to process the same geometries twice.
     */
    private final String[] attributeNames;

    /**
     * The coordinate reference system of the envelope to compute, or {@code null}
     * for using the CRS of the default geometry or the first non-empty geometry.
     * Note that this is the CRS desired by user of this {@link EnvelopeOperation};
     * it may be unrelated to the CRS of stored geometries.
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    final CoordinateReferenceSystem targetCRS;

    /**
     * Whether {@link #targetCRS} has been explicitly specified by the user.
     * If {@code false}, then the <abbr>CRS</abbr> has been inherited from the geometries.
     */
    private final boolean explicitCRS;

    /**
     * The coordinate conversions or transformations from the CRS used by the geometries to the CRS requested
     * by the user, or {@code null} if there is no operation to apply.  If non-null, the length of this array
     * shall be equal to the length of the {@link #attributeNames} array and element at index <var>i</var> is
     * the operation from the {@code attributeNames[i]} geometry CRS to the {@link #targetCRS}. It may be the
     * identity operation, and may also be {@code null} if the property at index <var>i</var> does not declare
     * a default CRS.
     *
     * <h4>Performance note</h4>
     * If this array is {@code null}, then {@link Feature#getProperty(String)} does not need to be invoked at all.
     * A null array is a signal that invoking only the cheaper {@link Feature#getPropertyValue(String)} method is
     * sufficient. However, this array become non-null as soon as there is at least one CRS characteristic to check.
     * We do not distinguish which particular property may have a CRS characteristic because as of Apache SIS 1.0,
     * implementations of {@link DenseFeature} and {@link SparseFeature} have a "all of nothing" behavior anyway.
     * So there is no performance gain to expect from a fine-grained knowledge of which properties declare a CRS.
     */
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
    private final CoordinateOperation[] attributeToCRS;

    /**
     * The property names as an unmodifiable set, created when first needed.
     * This is simply {@link #attributeNames} copied in a unmodifiable set.
     *
     * @see #getDependencies()
     */
    private transient volatile Set<String> dependencies;

    /**
     * The type of the result returned by the envelope operation.
     */
    @SuppressWarnings("serial")                         // Most SIS implementations are serializable.
    private final AttributeType<Envelope> resultType;

    /**
     * Creates a new operation computing the envelope of features of the given type.
     * The {@link #targetCRS} is set to the first non-null <abbr>CRS</abbr> in the following choices:
     *
     * <ol>
     *   <li>the <abbr>CRS</abbr> specified to this constructor,</li>
     *   <li>the <abbr>CRS</abbr> of the default geometry, or</li>
     *   <li>the <abbr>CRS</abbr> of the first non-empty geometry.</li>
     * </ol>
     *
     * <h4>Inheritance</h4>
     * If {@code inheritFrom} is non-null, then the {@code geometryAttributes} array must have the same length
     * as {@code inheritFrom.attributeNames} with elements in the same order. Any null element in the given
     * array will be replaced by the corresponding value of {@code inheritFrom}.
     *
     * @param identification      the name and other information to be given to this operation.
     * @param targetCRS           the coordinate reference system of envelopes to computes, or {@code null}.
     * @param geometryAttributes  the operation or attribute type from which to get geometry values.
     * @param inheritFrom         the existing operation from which to inherit attributes, or {@code null}.
     */
    EnvelopeOperation(final Map<String,?> identification,
                      CoordinateReferenceSystem targetCRS,
                      final PropertyType[] geometryAttributes,
                      final EnvelopeOperation inheritFrom)
            throws FactoryException
    {
        super(identification);
        explicitCRS = (targetCRS != null);      // Whether the CRS was specified by the user or inferred automatically.
        boolean characterizedByCRS = false;     // Whether "sis:crs" characteristics exist, possibly with null values.
        String defaultGeometry = null;          // Attribute name of the target of the "sis:geometry" property.
        boolean defaultIsFirst = true;          // Whether the default geometry is the first entry in the `names` map.
        /*
         * Get all property names without duplicated values, including the targets of links.
         * The map values will be the default Coordinate Reference Systems, or null if none.
         */
        final var names = new LinkedHashMap<String, CoordinateReferenceSystem>(4);
        for (int i=0; i < geometryAttributes.length; i++) {
            final String propertyName;          // Name of `geometryAttributes[i]`, possibly inherited.
            final String attributeName;         // Name of the property after following the link.
            CoordinateReferenceSystem attributeCRS = null;
            final PropertyType property = geometryAttributes[i];
            if (property == null && inheritFrom != null) {
                /*
                 * When this constructor is invoked by `updateDependencies(Map)`, a null property means to inherit
                 * the property at the same index from the previous operation. The caller is responsible to ensure
                 * that the indexes match.
                 */
                propertyName = attributeName = inheritFrom.attributeNames[i];
                if (inheritFrom.attributeToCRS != null) {
                    final CoordinateOperation op = inheritFrom.attributeToCRS[i];
                    if (op != null) {
                        attributeCRS = op.getSourceCRS();
                        characterizedByCRS = true;
                    }
                }
            } else {
                final AttributeType<?> at = Features.toAttribute(property).orElse(null);
                if (at == null || !Geometries.isKnownType(at.getValueClass())) {
                    continue;   // Not a geometry property. Ignore as per method contract.
                }
                /*
                 * If a property is a link to an attribute, then the key will be the name of the referenced
                 * attribute instead of the operation name. This is for avoiding to query the same geometry
                 * twice when the attribute is also specified explicitly in the array of properties.
                 */
                propertyName  = property.getName().toString();
                attributeName = Features.getLinkTarget(property).orElse(propertyName);
                /*
                 * Set `characterizedByCRS` to `true` if we find at least one attribute which have the
                 * "sis:crs" characteristic. Note that we cannot rely on `attributeCRS` being non-null
                 * because an attribute may be characterized by a CRS without providing default CRS.
                 */
                final AttributeType<?> ct = at.characteristics().get(AttributeConvention.CRS);
                if (ct != null && CoordinateReferenceSystem.class.isAssignableFrom(ct.getValueClass())) {
                    attributeCRS = (CoordinateReferenceSystem) ct.getDefaultValue();    // May still be null.
                    characterizedByCRS = true;
                }
            }
            /*
             * If the user did not specified a CRS explicitly, take the CRS of the default geometry.
             * If there is no default geometry, the CRS of the first geometry will be taken in next loop.
             */
            if (AttributeConvention.GEOMETRY.equals(propertyName)) {
                defaultGeometry = attributeName;
                defaultIsFirst = names.isEmpty();
                if (targetCRS == null) {
                    targetCRS = attributeCRS;
                }
            }
            names.putIfAbsent(attributeName, attributeCRS);
        }
        /*
         * Copy the names in an array with the default geometry first. If possible, find the coordinate operations
         * now in order to avoid the potentially costly calls to `CRS.findOperation(…)` for each feature on which
         * this `EnvelopeOperation` will be applied.
         */
        if (!defaultIsFirst) {
            JDK21.putFirst(names, defaultGeometry, names.remove(defaultGeometry));
        }
        names.remove(null);    // Paranoiac safety.
        attributeNames = new String[names.size()];
        attributeToCRS = characterizedByCRS ? new CoordinateOperation[attributeNames.length] : null;
        int i = 0;
        for (final Map.Entry<String, CoordinateReferenceSystem> entry : names.entrySet()) {
            attributeNames[i] = entry.getKey();
            if (characterizedByCRS) {
                final CoordinateReferenceSystem value = entry.getValue();
                if (value != null) {
                    if (targetCRS == null) {
                        targetCRS = value;      // Fallback if the default geometry has no CRS.
                    }
                    /*
                     * The following operation is often identity. We do not filter identity operations
                     * because their source CRS is still a useful information (it is the CRS instance
                     * found in the attribute characteristic, not necessarily identical to `targetCRS`)
                     * and because we keep the null value for meaning that attribute CRS is unspecified.
                     */
                    attributeToCRS[i] = CRS.findOperation(value, targetCRS, null);
                }
            }
            i++;
        }
        resultType = FeatureOperations.POOL.unique(new DefaultAttributeType<>(
                resultIdentification(identification), Envelope.class, 1, 1, null));
        this.targetCRS = targetCRS;
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
    public IdentifiedType getResult() {
        return resultType;
    }

    /**
     * Returns the names of feature properties that this operation needs for performing its task.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<String> getDependencies() {
        Set<String> cached = dependencies;
        if (cached == null) {
            // Not really a problem if computed twice concurrently.
            dependencies = cached = CollectionsExt.immutableSet(true, attributeNames);
        }
        return cached;
    }

    /**
     * Returns the same operation but using different properties as inputs.
     *
     * @param  dependencies  the new properties to use as operation inputs.
     * @return the new operation, or {@code this} if unchanged.
     */
    @Override
    public Operation updateDependencies(final Map<String, PropertyType> dependencies) {
        boolean foundAny = false;
        final var geometryAttributes = new PropertyType[attributeNames.length];
        for (int i=0; i < geometryAttributes.length; i++) {
            foundAny |= (geometryAttributes[i] = dependencies.get(attributeNames[i])) != null;
        }
        if (foundAny) try {
            var op = new EnvelopeOperation(inherit(), explicitCRS ? targetCRS : null, geometryAttributes, this);
            if (!equals(op)) {
                return FeatureOperations.POOL.unique(op);
            }
        } catch (FactoryException e) {
            throw new FeatureInstantiationException(e.getMessage(), e);
        }
        return this;
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
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        return new Result(feature);
    }




    /**
     * The attributes that contains the result of union of all envelope extracted from other attributes.
     * Value is calculated each time it is accessed.
     */
    private final class Result extends OperationResult<Envelope> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 4900962888075807964L;

        /**
         * Creates a new attribute for the given feature.
         */
        Result(final Feature feature) {
            super(resultType, feature);
        }

        /**
         * Computes an envelope which is the union of envelope of geometry values of all properties
         * specified to the {@link EnvelopeOperation} constructor.
         *
         * @return the union of envelopes of all geometries in the attribute specified to the constructor,
         *         or {@code null} if none.
         * @throws FeatureOperationException if the envelope cannot be computed.
         */
        @Override
        public Envelope getValue() throws FeatureOperationException {
            final String[] attributeNames = EnvelopeOperation.this.attributeNames;
            GeneralEnvelope   envelope = null;                  // Union of all envelopes.
            GeneralEnvelope[] deferred = null;                  // Envelopes not yet included in union envelope.
            boolean hasUnknownCRS = false;                      // Whether at least one geometry has no known CRS.
            for (int i = 0; i < attributeNames.length; i++) {
                /*
                 * Call `Feature.getPropertyValue(…)` instead of `Feature.getProperty(…).getValue()`
                 * in order to avoid forcing DenseFeature and SparseFeature implementations to wrap
                 * the property values into new `Property` objects.  The potentially costly call to
                 * `Feature.getProperty(…)` can be avoided in two scenarios:
                 *
                 *   - The constructor determined that no attribute should have CRS characteristics.
                 *     This scenario is identified by (attributeToCRS == null).
                 *
                 *   - The geometry already declares its CRS, in which case that CRS has precedence
                 *     over attribute characteristics, so we don't need to fetch them.
                 *
                 * Inconvenient is that in a third scenario (CRS is defined by attribute characteristics),
                 * we will do two calls to some `Feature.getProperty…` method, which results in two lookups
                 * in hash table. We presume that the gain from the optimistic assumption is worth the cost.
                 */
                GeneralEnvelope genv = Geometries.wrap(feature.getPropertyValue(attributeNames[i]))
                                                  .map(GeometryWrapper::getEnvelope).orElse(null);
                if (genv == null) {
                    continue;
                }
                /*
                 * Get the CRS either directly from the geometry or indirectly from property characteristic.
                 * The CRS associated with the geometry will be kept consistent with `sourceCRS` and will be
                 * null only if no CRS has been found anywhere.  Note that `sourceCRS` may be different than
                 * `op.getSourceCRS()`. This difference will be handled by `Envelopes.transform(…)` later.
                 */
                CoordinateReferenceSystem sourceCRS = genv.getCoordinateReferenceSystem();
                CoordinateOperation op = null;
                if (attributeToCRS != null) {
                    op = attributeToCRS[i];
                    if (sourceCRS == null) {
                        /*
                         * Try to get CRS from property characteristic. Usually `at` is null and we fallback
                         * on the coordinate operation computed at construction time. In the rare case where
                         * a CRS characteristic is associated to a particular feature, setting `op` to null
                         * will cause a new coordinate operation to be searched.
                         */
                        final var at = ((Attribute<?>) feature.getProperty(attributeNames[i]))
                                .characteristics().get(AttributeConvention.CRS);
                        final Object geomCRS;
                        if (at != null && (geomCRS = at.getValue()) != null) {
                            if (!(geomCRS instanceof CoordinateReferenceSystem)) {
                                throw new FeatureOperationException(Resources.formatInternational(
                                        Resources.Keys.IllegalCharacteristicsType_3,
                                        AttributeConvention.CRS_CHARACTERISTIC,
                                        CoordinateReferenceSystem.class,
                                        geomCRS.getClass()));
                            }
                            sourceCRS = (CoordinateReferenceSystem) geomCRS;
                        } else if (op != null) {
                            sourceCRS = op.getSourceCRS();
                        }
                        genv.setCoordinateReferenceSystem(sourceCRS);
                    }
                }
                /*
                 * If the geometry CRS is unknown (sourceCRS == null), leave the geometry CRS to null.
                 * Do not set it to `targetCRS` because that value is the desired CRS, not necessarily
                 * the actual CRS.
                 */
                if (sourceCRS != null && targetCRS != null) try {
                    if (op == null) {
                        op = CRS.findOperation(sourceCRS, targetCRS, null);
                    }
                    if (!op.getMathTransform().isIdentity()) {
                        genv = Envelopes.transform(op, genv);
                    }
                } catch (FactoryException | TransformException e) {
                    throw new FeatureOperationException(Errors.formatInternational(Errors.Keys.CanNotTransformEnvelope), e);
                }
                /*
                 * If there is only one geometry, we will return that geometry as-is even if its CRS is unknown.
                 * It will be up to the user to decide what to do with that. Otherwise (two or more geometries)
                 * we throw an exception if a CRS is unknown, because we don't know how to combine them.
                 */
                hasUnknownCRS |= (sourceCRS == null);
                if (envelope == null) {
                    envelope = genv;
                } else if (hasUnknownCRS) {
                    throw new FeatureOperationException(Errors.formatInternational(Errors.Keys.UnspecifiedCRS));
                } else if (targetCRS != null) {
                    envelope.add(genv);
                } else {
                    if (deferred == null) {
                        deferred = new GeneralEnvelope[attributeNames.length];
                        deferred[0] = envelope;
                    }
                    deferred[i] = genv;
                }
            }
            if (deferred == null) {
                return envelope;
            } else try {
                return Envelopes.union(deferred);
            } catch (TransformException e) {
                throw new FeatureOperationException(Errors.formatInternational(Errors.Keys.CanNotTransformEnvelope), e);
            }
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
            // `this.result` is compared (indirectly) by the super class.
            final var that = (EnvelopeOperation) obj;
            return Arrays.equals(attributeNames, that.attributeNames) &&
                   Arrays.equals(attributeToCRS, that.attributeToCRS) &&
                   Objects.equals(targetCRS,     that.targetCRS) &&
                   explicitCRS == that.explicitCRS;
        }
        return false;
    }
}
