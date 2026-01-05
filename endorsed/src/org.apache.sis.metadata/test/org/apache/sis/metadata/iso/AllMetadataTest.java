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
package org.apache.sis.metadata.iso;

import java.lang.reflect.Modifier;
import org.opengis.annotation.UML;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.PropertyConsistencyCheck;

// Test dependencies
import org.junit.jupiter.api.Test;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.annotation.Stereotype;
import org.opengis.util.ControlledVocabulary;


/**
 * Tests all known {@link ISOMetadata} subclasses for JAXB annotations and getter/setter methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class AllMetadataTest extends PropertyConsistencyCheck {
    /**
     * Creates a new test case with all GeoAPI interfaces and code lists to test.
     */
    @SuppressWarnings("deprecation")
    public AllMetadataTest() {
        super(MetadataStandard.ISO_19115,
            org.opengis.annotation.Obligation.class,
            org.opengis.metadata.ApplicationSchemaInformation.class,
            org.opengis.metadata.Datatype.class,
            org.opengis.metadata.ExtendedElementInformation.class,
            org.opengis.metadata.Identifier.class,
            org.opengis.metadata.Metadata.class,
            org.opengis.metadata.MetadataExtensionInformation.class,
            org.opengis.metadata.MetadataScope.class,
            org.opengis.metadata.PortrayalCatalogueReference.class,
            org.opengis.metadata.acquisition.AcquisitionInformation.class,
            org.opengis.metadata.acquisition.Context.class,
            org.opengis.metadata.acquisition.EnvironmentalRecord.class,
            org.opengis.metadata.acquisition.Event.class,
            org.opengis.metadata.acquisition.GeometryType.class,
            org.opengis.metadata.acquisition.Instrument.class,
            org.opengis.metadata.acquisition.Objective.class,
            org.opengis.metadata.acquisition.ObjectiveType.class,
            org.opengis.metadata.acquisition.Operation.class,
            org.opengis.metadata.acquisition.OperationType.class,
            org.opengis.metadata.acquisition.Plan.class,
            org.opengis.metadata.acquisition.Platform.class,
            org.opengis.metadata.acquisition.PlatformPass.class,
            org.opengis.metadata.acquisition.Priority.class,
            org.opengis.metadata.acquisition.RequestedDate.class,
            org.opengis.metadata.acquisition.Requirement.class,
            org.opengis.metadata.acquisition.Sequence.class,
            org.opengis.metadata.acquisition.Trigger.class,
            org.opengis.metadata.citation.Address.class,
            org.opengis.metadata.citation.Citation.class,
            org.opengis.metadata.citation.CitationDate.class,
            org.opengis.metadata.citation.Contact.class,
            org.opengis.metadata.citation.DateType.class,
            org.opengis.metadata.citation.OnLineFunction.class,
            org.opengis.metadata.citation.OnlineResource.class,
            org.opengis.metadata.citation.Party.class,
            org.opengis.metadata.citation.PresentationForm.class,
            org.opengis.metadata.citation.Responsibility.class,
            org.opengis.metadata.citation.ResponsibleParty.class,
            org.opengis.metadata.citation.Role.class,
            org.opengis.metadata.citation.Series.class,
            org.opengis.metadata.citation.Telephone.class,
            org.opengis.metadata.constraint.Classification.class,
            org.opengis.metadata.constraint.Constraints.class,
            org.opengis.metadata.constraint.LegalConstraints.class,
            org.opengis.metadata.constraint.Restriction.class,
            org.opengis.metadata.constraint.SecurityConstraints.class,
            org.opengis.metadata.content.AttributeGroup.class,
            org.opengis.metadata.content.Band.class,
            org.opengis.metadata.content.BandDefinition.class,
            org.opengis.metadata.content.ContentInformation.class,
            org.opengis.metadata.content.CoverageContentType.class,
            org.opengis.metadata.content.CoverageDescription.class,
            org.opengis.metadata.content.FeatureCatalogueDescription.class,
            org.opengis.metadata.content.ImageDescription.class,
            org.opengis.metadata.content.ImagingCondition.class,
            org.opengis.metadata.content.PolarisationOrientation.class,
            org.opengis.metadata.content.RangeDimension.class,
            org.opengis.metadata.content.RangeElementDescription.class,
            org.opengis.metadata.content.SampleDimension.class,
            org.opengis.metadata.content.TransferFunctionType.class,
            org.opengis.metadata.distribution.DataFile.class,
            org.opengis.metadata.distribution.DigitalTransferOptions.class,
            org.opengis.metadata.distribution.Distribution.class,
            org.opengis.metadata.distribution.Distributor.class,
            org.opengis.metadata.distribution.Format.class,
            org.opengis.metadata.distribution.Medium.class,
            org.opengis.metadata.distribution.MediumFormat.class,
            org.apache.sis.metadata.iso.legacy.MediumName.class,
            org.opengis.metadata.distribution.StandardOrderProcess.class,
            org.opengis.metadata.extent.BoundingPolygon.class,
            org.opengis.metadata.extent.Extent.class,
            org.opengis.metadata.extent.GeographicBoundingBox.class,
            org.opengis.metadata.extent.GeographicDescription.class,
            org.opengis.metadata.extent.GeographicExtent.class,
            org.opengis.metadata.extent.SpatialTemporalExtent.class,
            org.opengis.metadata.extent.TemporalExtent.class,
            org.opengis.metadata.extent.VerticalExtent.class,
            org.opengis.metadata.identification.AggregateInformation.class,
            org.opengis.metadata.identification.AssociatedResource.class,
            org.opengis.metadata.identification.AssociationType.class,
            org.opengis.metadata.identification.BrowseGraphic.class,
            org.opengis.metadata.identification.CharacterSet.class,
            org.opengis.metadata.identification.CoupledResource.class,
            org.opengis.metadata.identification.DataIdentification.class,
            org.opengis.metadata.identification.Identification.class,
            org.opengis.metadata.identification.InitiativeType.class,
            org.opengis.metadata.identification.Keywords.class,
            org.opengis.metadata.identification.KeywordClass.class,
            org.opengis.metadata.identification.KeywordType.class,
            org.opengis.metadata.identification.Progress.class,
            org.opengis.metadata.identification.OperationChainMetadata.class,
            org.opengis.metadata.identification.OperationMetadata.class,
            org.opengis.metadata.identification.RepresentativeFraction.class,
            org.opengis.metadata.identification.Resolution.class,
            org.opengis.metadata.identification.ServiceIdentification.class,
            org.opengis.metadata.identification.TopicCategory.class,
            org.opengis.metadata.identification.Usage.class,
            org.opengis.metadata.lineage.Algorithm.class,
            org.opengis.metadata.lineage.Lineage.class,
            org.opengis.metadata.lineage.NominalResolution.class,
            org.opengis.metadata.lineage.Processing.class,
            org.opengis.metadata.lineage.ProcessStep.class,
            org.opengis.metadata.lineage.ProcessStepReport.class,
            org.opengis.metadata.lineage.Source.class,
            org.opengis.metadata.maintenance.MaintenanceFrequency.class,
            org.opengis.metadata.maintenance.MaintenanceInformation.class,
            org.opengis.metadata.maintenance.Scope.class,
            org.opengis.metadata.maintenance.ScopeCode.class,
            org.opengis.metadata.maintenance.ScopeDescription.class,
            org.opengis.metadata.quality.AbsoluteExternalPositionalAccuracy.class,
            org.opengis.metadata.quality.AccuracyOfATimeMeasurement.class,
            org.opengis.metadata.quality.AggregationDerivation.class,
            org.opengis.metadata.quality.BasicMeasure.class,
            org.opengis.metadata.quality.Completeness.class,
            org.opengis.metadata.quality.CompletenessCommission.class,
            org.opengis.metadata.quality.CompletenessOmission.class,
            org.opengis.metadata.quality.ConceptualConsistency.class,
            org.opengis.metadata.quality.Confidence.class,
            org.opengis.metadata.quality.ConformanceResult.class,
            org.opengis.metadata.quality.CoverageResult.class,
            org.opengis.metadata.quality.DataEvaluation.class,
            org.opengis.metadata.quality.DataQuality.class,
//          org.opengis.metadata.quality.Description.class,                 // Pending ISO 19157:2022 renaming.
            org.opengis.metadata.quality.DescriptiveResult.class,
            org.opengis.metadata.quality.DomainConsistency.class,
            org.opengis.metadata.quality.Element.class,
            org.opengis.metadata.quality.EvaluationMethod.class,
            org.opengis.metadata.quality.EvaluationMethodType.class,
            org.opengis.metadata.quality.FormatConsistency.class,
            org.opengis.metadata.quality.FullInspection.class,
            org.opengis.metadata.quality.GriddedDataPositionalAccuracy.class,
            org.opengis.metadata.quality.Homogeneity.class,
            org.opengis.metadata.quality.IndirectEvaluation.class,
            org.opengis.metadata.quality.LogicalConsistency.class,
//          org.opengis.metadata.quality.Measure.class,                     // Pending ISO 19157:2022 renaming.
            org.opengis.metadata.quality.MeasureReference.class,
            org.opengis.metadata.quality.Metaquality.class,
            org.opengis.metadata.quality.NonQuantitativeAttributeAccuracy.class,
            org.opengis.metadata.quality.NonQuantitativeAttributeCorrectness.class,
            org.opengis.metadata.quality.PositionalAccuracy.class,
            org.opengis.metadata.quality.QuantitativeAttributeAccuracy.class,
            org.opengis.metadata.quality.QuantitativeResult.class,
            org.opengis.metadata.quality.RelativeInternalPositionalAccuracy.class,
            org.opengis.metadata.quality.Representativity.class,
            org.opengis.metadata.quality.Result.class,
            org.opengis.metadata.quality.SampleBasedInspection.class,
            org.opengis.metadata.quality.SourceReference.class,
//          org.opengis.metadata.quality.StandaloneQualityReportInformation.class,      // Pending ISO 19157:2022 renaming.
            org.opengis.metadata.quality.TemporalAccuracy.class,
            org.opengis.metadata.quality.TemporalConsistency.class,
            org.opengis.metadata.quality.TemporalQuality.class,
            org.opengis.metadata.quality.TemporalValidity.class,
            org.opengis.metadata.quality.ThematicAccuracy.class,
            org.opengis.metadata.quality.ThematicClassificationCorrectness.class,
            org.opengis.metadata.quality.TopologicalConsistency.class,
            org.opengis.metadata.quality.Usability.class,
            org.opengis.metadata.quality.ValueStructure.class,
            org.opengis.metadata.spatial.CellGeometry.class,
            org.opengis.metadata.spatial.Dimension.class,
            org.opengis.metadata.spatial.DimensionNameType.class,
            org.opengis.metadata.spatial.GCP.class,
            org.opengis.metadata.spatial.GCPCollection.class,
            org.opengis.metadata.spatial.GeolocationInformation.class,
            org.opengis.metadata.spatial.GeometricObjects.class,
            org.opengis.metadata.spatial.GeometricObjectType.class,
            org.opengis.metadata.spatial.Georectified.class,
            org.opengis.metadata.spatial.Georeferenceable.class,
            org.opengis.metadata.spatial.GridSpatialRepresentation.class,
            org.opengis.metadata.spatial.PixelOrientation.class,
            org.opengis.metadata.spatial.SpatialRepresentation.class,
            org.opengis.metadata.spatial.SpatialRepresentationType.class,
            org.opengis.metadata.spatial.TopologyLevel.class,
            org.opengis.metadata.spatial.VectorSpatialRepresentation.class);
    }

    /**
     * For every properties in every non-{@code Codelist} types listed in the constructor,
     * tests the property values. This method verifies initial conditions, then sets random values.
     * Once the test is completed, this method verifies that the expected warnings have been logged,
     * and no unexpected logging occurred.
     */
    @Test
    @Override
    public void testPropertyValues() {
        super.testPropertyValues();
        loggings.assertNextLogContains("angularDistance", "distance");
        loggings.assertNextLogContains("distance", "equivalentScale");
        loggings.assertNextLogContains("equivalentScale", "levelOfDetail");
        loggings.assertNextLogContains("levelOfDetail", "vertical");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Returns the name of the XML root element for an interface described by the given UML.
     * This method does the generic work described in super-class, then applies special rules
     * specific to the metadata package.
     */
    @Override
    protected String getExpectedXmlRootElementName(final Stereotype stereotype, final UML uml) {
        String name = super.getExpectedXmlRootElementName(stereotype, uml);
        if (name.equals("DQ_CoverageResult")) name = "QE_CoverageResult";
        return name;
    }

    /**
     * Returns the name of the XML type for an interface described by the given UML.
     * This method does the generic work described in super-class, then applies special rules
     * specific to the metadata package.
     */
    @Override
    protected String getExpectedXmlTypeName(final Stereotype stereotype, final UML uml) {
        String name = super.getExpectedXmlTypeName(stereotype, uml);
        if (name.equals("DQ_CoverageResult_Type")) name = "QE_CoverageResult_Type";
        return name;
    }

    /**
     * Returns the ISO 19115-3 wrapper for the given GeoAPI type,
     * or {@code null} if no adapter is expected for the given type.
     *
     * @return {@inheritDoc}
     * @throws ClassNotFoundException {@inheritDoc}
     */
    @Override
    protected Class<?> getWrapperFor(final Class<?> type) throws ClassNotFoundException {
        if (type.equals(org.opengis.metadata.Metadata.class)) {
            /*
             * We don't have adapter for Metadata, since it is the root element.
             * We explicitly exclude it for avoiding confusion with PropertyType,
             * which is the base class of all other adapters.
             */
            return null;
        }
        final String classname = "org.apache.sis.xml.bind.metadata." +
                (ControlledVocabulary.class.isAssignableFrom(type) ? "code." : "") +
                type.getAnnotation(UML.class).identifier();
        final Class<?> wrapper = Class.forName(classname);
        Class<?>[] expectedFinalClasses = wrapper.getClasses();   // "Since2014" internal class.
        if (expectedFinalClasses.length == 0) {
            expectedFinalClasses = new Class<?>[] {wrapper};      // If no "Since2014", then wrapper itself should be final.
        }
        for (final Class<?> c : expectedFinalClasses) {
            if (!Modifier.isFinal(c.getModifiers())) {
                fail("Expected a final class for " + c.getName());
            }
        }
        return wrapper;
    }

    /**
     * Return {@code false} for the Apache SIS properties which are known to have no setter methods.
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean isWritable(final Class<?> impl, final String property) {
        if (org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction.class.isAssignableFrom(impl)) {
            if (property.equals("doubleValue")) {
                return false;
            }
        }
        return super.isWritable(impl, property);
    }
}
