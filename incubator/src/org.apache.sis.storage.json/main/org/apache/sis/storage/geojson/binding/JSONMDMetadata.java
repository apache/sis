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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.MetadataScope;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.referencing.ReferenceSystem;

/**
 * Binding of the {@code MD_Metadata} JSON type, the {@code properties} of a metadata record.
 *
 * <p>The {@code referenceSystemInfo} property is an extension to the ISO 19115-4 Core profile,
 * used by the ISO 19115-4 reference example.
 *
 * <p>Constraints:
 * <ul>
 *   <li>{@code identificationInfo} has a single value, other SIS identifications are ignored with a warning.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 f)"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Metadata</a>
 */
@JsonPropertyOrder({
    JSONMDMetadata.PROPERTY_METADATA_IDENTIFIER,
    JSONMDMetadata.PROPERTY_DEFAULT_LOCALE,
    JSONMDMetadata.PROPERTY_OTHER_LOCALE,
    JSONMDMetadata.PROPERTY_PARENT_METADATA,
    JSONMDMetadata.PROPERTY_METADATA_SCOPE,
    JSONMDMetadata.PROPERTY_CONTACT,
    JSONMDMetadata.PROPERTY_DATE_INFO,
    JSONMDMetadata.PROPERTY_METADATA_STANDARD,
    JSONMDMetadata.PROPERTY_METADATA_PROFILE,
    JSONMDMetadata.PROPERTY_ALTERNATIVE_METADATA_REFERENCE,
    JSONMDMetadata.PROPERTY_METADATA_LINKAGE,
    JSONMDMetadata.PROPERTY_REFERENCE_SYSTEM_INFO,
    JSONMDMetadata.PROPERTY_IDENTIFICATION_INFO,
    JSONMDMetadata.PROPERTY_DISTRIBUTION_INFO,
    JSONMDMetadata.PROPERTY_DATA_QUALITY_INFO,
    JSONMDMetadata.PROPERTY_RESOURCE_LINEAGE,
    JSONMDMetadata.PROPERTY_METADATA_CONSTRAINTS,
    JSONMDMetadata.PROPERTY_METADATA_MAINTENANCE
})
public final class JSONMDMetadata extends DataTransferObject {

    public static final String PROPERTY_METADATA_IDENTIFIER = "metadataIdentifier";
    public static final String PROPERTY_DEFAULT_LOCALE = "defaultLocale";
    public static final String PROPERTY_OTHER_LOCALE = "otherLocale";
    public static final String PROPERTY_PARENT_METADATA = "parentMetadata";
    public static final String PROPERTY_METADATA_SCOPE = "metadataScope";
    public static final String PROPERTY_CONTACT = "contact";
    public static final String PROPERTY_DATE_INFO = "dateInfo";
    public static final String PROPERTY_METADATA_STANDARD = "metadataStandard";
    public static final String PROPERTY_METADATA_PROFILE = "metadataProfile";
    public static final String PROPERTY_ALTERNATIVE_METADATA_REFERENCE = "alternativeMetadataReference";
    public static final String PROPERTY_METADATA_LINKAGE = "metadataLinkage";
    public static final String PROPERTY_REFERENCE_SYSTEM_INFO = "referenceSystemInfo";
    public static final String PROPERTY_IDENTIFICATION_INFO = "identificationInfo";
    public static final String PROPERTY_DISTRIBUTION_INFO = "distributionInfo";
    public static final String PROPERTY_DATA_QUALITY_INFO = "dataQualityInfo";
    public static final String PROPERTY_RESOURCE_LINEAGE = "resourceLineage";
    public static final String PROPERTY_METADATA_CONSTRAINTS = "metadataConstraints";
    public static final String PROPERTY_METADATA_MAINTENANCE = "metadataMaintenance";

    @JsonProperty(PROPERTY_METADATA_IDENTIFIER)
    private JSONMDIdentifier metadataIdentifier;
    @JsonProperty(PROPERTY_DEFAULT_LOCALE)
    private JSONMDLocale defaultLocale;
    @JsonProperty(PROPERTY_OTHER_LOCALE)
    private List<JSONMDLocale> otherLocale;
    @JsonProperty(PROPERTY_PARENT_METADATA)
    private JSONMDCitation parentMetadata;
    @JsonProperty(PROPERTY_METADATA_SCOPE)
    private List<JSONMDMetadataScope> metadataScope;
    @JsonProperty(PROPERTY_CONTACT)
    private List<JSONMDResponsibility> contact;
    @JsonProperty(PROPERTY_DATE_INFO)
    private JSONMDDates dateInfo;
    @JsonProperty(PROPERTY_METADATA_STANDARD)
    private List<JSONMDCitation> metadataStandard;
    @JsonProperty(PROPERTY_METADATA_PROFILE)
    private List<JSONMDCitation> metadataProfile;
    @JsonProperty(PROPERTY_ALTERNATIVE_METADATA_REFERENCE)
    private List<JSONMDCitation> alternativeMetadataReference;
    @JsonProperty(PROPERTY_METADATA_LINKAGE)
    private List<JSONMDOnlineResource> metadataLinkage;
    @JsonProperty(PROPERTY_REFERENCE_SYSTEM_INFO)
    private List<JSONMDReferenceSystem> referenceSystemInfo;
    @JsonProperty(PROPERTY_IDENTIFICATION_INFO)
    private JSONMDDataIdentification identificationInfo;
    @JsonProperty(PROPERTY_DISTRIBUTION_INFO)
    private List<JSONMDDistribution> distributionInfo;
    @JsonProperty(PROPERTY_DATA_QUALITY_INFO)
    private List<JSONMDDataQuality> dataQualityInfo;
    @JsonProperty(PROPERTY_RESOURCE_LINEAGE)
    private List<JSONMDLineage> resourceLineage;
    @JsonProperty(PROPERTY_METADATA_CONSTRAINTS)
    private List<JSONMDConstraints> metadataConstraints;
    @JsonProperty(PROPERTY_METADATA_MAINTENANCE)
    private JSONMDMaintenanceInformation metadataMaintenance;

    public JSONMDMetadata() {
    }

    public JSONMDMetadata(final Metadata source, final JSONMDEncoder encoder) {
        metadataIdentifier           = (source.getMetadataIdentifier() == null) ? null
                                     : new JSONMDIdentifier(source.getMetadataIdentifier(), encoder);
        final List<JSONMDLocale> locales = JSONMDLocale.create(source.getLocalesAndCharsets());
        if (locales != null) {
            defaultLocale = locales.get(0);
            otherLocale   = (locales.size() > 1) ? locales.subList(1, locales.size()) : null;
        }
        parentMetadata               = (source.getParentMetadata() == null) ? null
                                     : new JSONMDCitation(source.getParentMetadata(), encoder);
        metadataScope                = encoder.list(source.getMetadataScopes(), (MetadataScope e) -> new JSONMDMetadataScope(e, encoder));
        contact                      = encoder.list(source.getContacts(), (Responsibility e) -> new JSONMDResponsibility(e, encoder));
        dateInfo                     = encoder.dates(source.getDateInfo());
        metadataStandard             = encoder.list(source.getMetadataStandards(), (Citation e) -> new JSONMDCitation(e, encoder));
        metadataProfile              = encoder.list(source.getMetadataProfiles(), (Citation e) -> new JSONMDCitation(e, encoder));
        alternativeMetadataReference = encoder.list(source.getAlternativeMetadataReferences(), (Citation e) -> new JSONMDCitation(e, encoder));
        metadataLinkage              = encoder.list(source.getMetadataLinkages(), (OnlineResource e) -> new JSONMDOnlineResource(e, encoder));
        referenceSystemInfo          = encoder.list(source.getReferenceSystemInfo(), (ReferenceSystem e) -> new JSONMDReferenceSystem(e, encoder));
        identificationInfo           = encoder.first(source.getIdentificationInfo(),
                                           (Identification e) -> new JSONMDDataIdentification(e, encoder), PROPERTY_IDENTIFICATION_INFO);
        distributionInfo             = encoder.list(source.getDistributionInfo(), (Distribution e) -> new JSONMDDistribution(e, encoder));
        dataQualityInfo              = encoder.list(source.getDataQualityInfo(), (DataQuality e) -> new JSONMDDataQuality(e, encoder));
        resourceLineage              = encoder.list(source.getResourceLineages(), (Lineage e) -> new JSONMDLineage(e, encoder));
        metadataConstraints          = encoder.list(source.getMetadataConstraints(), (Constraints e) -> JSONMDConstraints.create(e, encoder));
        metadataMaintenance          = (source.getMetadataMaintenance() == null) ? null
                                     : new JSONMDMaintenanceInformation(source.getMetadataMaintenance(), encoder);
    }

    public JSONMDIdentifier getMetadataIdentifier() {
        return metadataIdentifier;
    }

    public void setMetadataIdentifier(final JSONMDIdentifier metadataIdentifier) {
        this.metadataIdentifier = metadataIdentifier;
    }

    public JSONMDLocale getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(final JSONMDLocale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public List<JSONMDLocale> getOtherLocale() {
        return otherLocale;
    }

    public void setOtherLocale(final List<JSONMDLocale> otherLocale) {
        this.otherLocale = otherLocale;
    }

    public JSONMDCitation getParentMetadata() {
        return parentMetadata;
    }

    public void setParentMetadata(final JSONMDCitation parentMetadata) {
        this.parentMetadata = parentMetadata;
    }

    public List<JSONMDMetadataScope> getMetadataScope() {
        return metadataScope;
    }

    public void setMetadataScope(final List<JSONMDMetadataScope> metadataScope) {
        this.metadataScope = metadataScope;
    }

    public List<JSONMDResponsibility> getContact() {
        return contact;
    }

    public void setContact(final List<JSONMDResponsibility> contact) {
        this.contact = contact;
    }

    public JSONMDDates getDateInfo() {
        return dateInfo;
    }

    public void setDateInfo(final JSONMDDates dateInfo) {
        this.dateInfo = dateInfo;
    }

    public List<JSONMDCitation> getMetadataStandard() {
        return metadataStandard;
    }

    public void setMetadataStandard(final List<JSONMDCitation> metadataStandard) {
        this.metadataStandard = metadataStandard;
    }

    public List<JSONMDCitation> getMetadataProfile() {
        return metadataProfile;
    }

    public void setMetadataProfile(final List<JSONMDCitation> metadataProfile) {
        this.metadataProfile = metadataProfile;
    }

    public List<JSONMDCitation> getAlternativeMetadataReference() {
        return alternativeMetadataReference;
    }

    public void setAlternativeMetadataReference(final List<JSONMDCitation> alternativeMetadataReference) {
        this.alternativeMetadataReference = alternativeMetadataReference;
    }

    public List<JSONMDOnlineResource> getMetadataLinkage() {
        return metadataLinkage;
    }

    public void setMetadataLinkage(final List<JSONMDOnlineResource> metadataLinkage) {
        this.metadataLinkage = metadataLinkage;
    }

    public List<JSONMDReferenceSystem> getReferenceSystemInfo() {
        return referenceSystemInfo;
    }

    public void setReferenceSystemInfo(final List<JSONMDReferenceSystem> referenceSystemInfo) {
        this.referenceSystemInfo = referenceSystemInfo;
    }

    public JSONMDDataIdentification getIdentificationInfo() {
        return identificationInfo;
    }

    public void setIdentificationInfo(final JSONMDDataIdentification identificationInfo) {
        this.identificationInfo = identificationInfo;
    }

    public List<JSONMDDistribution> getDistributionInfo() {
        return distributionInfo;
    }

    public void setDistributionInfo(final List<JSONMDDistribution> distributionInfo) {
        this.distributionInfo = distributionInfo;
    }

    public List<JSONMDDataQuality> getDataQualityInfo() {
        return dataQualityInfo;
    }

    public void setDataQualityInfo(final List<JSONMDDataQuality> dataQualityInfo) {
        this.dataQualityInfo = dataQualityInfo;
    }

    public List<JSONMDLineage> getResourceLineage() {
        return resourceLineage;
    }

    public void setResourceLineage(final List<JSONMDLineage> resourceLineage) {
        this.resourceLineage = resourceLineage;
    }

    public List<JSONMDConstraints> getMetadataConstraints() {
        return metadataConstraints;
    }

    public void setMetadataConstraints(final List<JSONMDConstraints> metadataConstraints) {
        this.metadataConstraints = metadataConstraints;
    }

    public JSONMDMaintenanceInformation getMetadataMaintenance() {
        return metadataMaintenance;
    }

    public void setMetadataMaintenance(final JSONMDMaintenanceInformation metadataMaintenance) {
        this.metadataMaintenance = metadataMaintenance;
    }

    public DefaultMetadata toMetadata(final JSONMDDecoder decoder) {
        final DefaultMetadata target = new DefaultMetadata();
        if (metadataIdentifier != null) {
            target.setMetadataIdentifier(metadataIdentifier.toMetadata(decoder));
        }
        final Map<Locale, Charset> locales = new LinkedHashMap<>();
        if (defaultLocale != null) {
            defaultLocale.addTo(locales, decoder);
        }
        if (otherLocale != null) {
            for (final JSONMDLocale locale : otherLocale) {
                if (locale != null) {
                    locale.addTo(locales, decoder);
                }
            }
        }
        target.setLocalesAndCharsets(locales);
        if (parentMetadata != null) {
            target.setParentMetadata(parentMetadata.toMetadata(decoder));
        }
        target.setMetadataScopes(decoder.list(metadataScope, (JSONMDMetadataScope e) -> e.toMetadata(decoder)));
        target.setContacts(decoder.list(contact, (JSONMDResponsibility e) -> e.toMetadata(decoder)));
        if (dateInfo != null) {
            target.setDateInfo(dateInfo.toMetadata(decoder));
        }
        target.setMetadataStandards(decoder.list(metadataStandard, (JSONMDCitation e) -> e.toMetadata(decoder)));
        target.setMetadataProfiles(decoder.list(metadataProfile, (JSONMDCitation e) -> e.toMetadata(decoder)));
        target.setAlternativeMetadataReferences(decoder.list(alternativeMetadataReference, (JSONMDCitation e) -> e.toMetadata(decoder)));
        target.setMetadataLinkages(decoder.list(metadataLinkage, (JSONMDOnlineResource e) -> e.toMetadata(decoder)));
        target.setReferenceSystemInfo(decoder.list(referenceSystemInfo, (JSONMDReferenceSystem e) -> e.toMetadata(decoder)));
        if (identificationInfo != null) {
            target.setIdentificationInfo(List.of(identificationInfo.toMetadata(decoder)));
        }
        target.setDistributionInfo(decoder.list(distributionInfo, (JSONMDDistribution e) -> e.toMetadata(decoder)));
        target.setDataQualityInfo(decoder.list(dataQualityInfo, (JSONMDDataQuality e) -> e.toMetadata(decoder)));
        target.setResourceLineages(decoder.list(resourceLineage, (JSONMDLineage e) -> e.toMetadata(decoder)));
        target.setMetadataConstraints(decoder.list(metadataConstraints, (JSONMDConstraints e) -> e.toMetadata(decoder)));
        if (metadataMaintenance != null) {
            target.setMetadataMaintenance(metadataMaintenance.toMetadata(decoder));
        }
        return target;
    }
}
