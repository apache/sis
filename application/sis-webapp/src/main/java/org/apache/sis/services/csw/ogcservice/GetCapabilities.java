package org.apache.sis.services.csw.ogcservice;

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


import org.apache.sis.services.csw.discovery.AbstractRecord;
import org.apache.sis.services.csw.manager.TransactionResponse;
import org.apache.sis.services.csw.manager.TransactionSummary;
import org.apache.sis.services.csw.discovery.BoundingBox;
import org.apache.sis.services.csw.discovery.BriefRecord;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.acquisition.DefaultOperation;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultAddress;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultContact;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.citation.DefaultTelephone;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.metadata.iso.identification.DefaultOperationMetadata;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.apache.sis.services.csw.common.ElementSetName;
import org.apache.sis.services.csw.common.RequestBase;
import org.apache.sis.services.csw.common.RequiredElementSetNamesType;
import org.apache.sis.services.csw.common.Service;
import org.apache.sis.services.csw.common.Test;
import org.apache.sis.services.csw.manager.InsertResult;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.xml.IdentifierSpace;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.identification.DistributedComputingPlatform;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.identification.OperationMetadata;
import org.opengis.metadata.identification.ServiceIdentification;
import org.opengis.util.InternationalString;
import org.xml.sax.SAXException;

/**
 *
 * @author haonguyen
 */
public class GetCapabilities {

    private final DefaultServiceIdentification serviceProvide;
    private final DefaultServiceIdentification serviceIdentification;
    private final DefaultOperationMetadata operationMetadata;

    /**
     *
     */
    public GetCapabilities() {
        this.serviceProvide = new DefaultServiceIdentification();
        this.serviceIdentification = new DefaultServiceIdentification();
        this.operationMetadata = new DefaultOperationMetadata();
    }

//    public DefaultResponsibleParty res;

    /**
     *
     * @return
     */
    public ServiceIdentification getServiceIdentification() {
        DefaultCitation citation = new DefaultCitation();
        citation.setTitle(new SimpleInternationalString("Catalogue Service for Spatial Information"));
        serviceIdentification.setCitation(citation);

        serviceIdentification.setAbstract(new SimpleInternationalString("terraCatalog 3.2 based OGC CSW 3.0 Catalogue \n"
                + "           Service for OGC core and ISO metadata (describing geospatial \n"
                + "           services, datasets and series)"));
//        Service keyword
        List<InternationalString> key = new ArrayList<>();
        key.add(new SimpleInternationalString("OGC"));
        key.add(new SimpleInternationalString("CSW"));
        key.add(new SimpleInternationalString("Catalog Service"));
        key.add(new SimpleInternationalString("metadata"));
        key.add(new SimpleInternationalString("CSW"));
        DefaultKeywords keyword = new DefaultKeywords();
        keyword.setKeywords(key);
//        keyword type
        keyword.setType(KeywordType.THEME);
        List<DefaultKeywords> keywords = new ArrayList<>();
        keywords.add(keyword);
        serviceIdentification.setDescriptiveKeywords(keywords);
//        Service type version
        List<String> serviceTypeVersion = new ArrayList<String>();
        serviceTypeVersion.add("3.0.0");

        serviceIdentification.setServiceTypeVersions(serviceTypeVersion);
//Service type
//        GenericName generic = new GenericName();
//        generic.toInternationalString();
        serviceIdentification.setServiceType(Names.createLocalName(null, null, "le nom"));
//       Service constrains
        List<DefaultConstraints> constraints = new ArrayList<DefaultConstraints>();
//        constraints.
        serviceIdentification.setResourceConstraints(constraints);
        return serviceIdentification;
    }

    /**
     *
     * @return
     */
    public Metadata myMethod() {
        UUID identifier = UUID.randomUUID();
        DefaultMetadata metadata = new DefaultMetadata();
        metadata.getIdentifierMap().putSpecialized(IdentifierSpace.UUID, identifier);
        System.out.println(metadata.getIdentifierMap().putSpecialized(IdentifierSpace.UUID, identifier));
        return metadata;
    }

    /**
     *
     * @return
     */
    public ServiceIdentification getServiceProvide() {
//        ResponsibleParty
        DefaultResponsibleParty responsibleParty = new DefaultResponsibleParty();
        responsibleParty.setOrganisationName(new SimpleInternationalString("Apache SIS"));
        DefaultContact contactinfo = new DefaultContact();
        DefaultTelephone phone = new DefaultTelephone();
        List<DefaultTelephone> listphone = new ArrayList<>();
        List<String> voice = new ArrayList<>();
        List<String> facsimile = new ArrayList<>();
        voice.add("+49-251-7474-400");
        facsimile.add("+49-251-7474-100");
        phone.setVoices(voice);
        phone.setFacsimiles(facsimile);
        listphone.add(phone);

//        Address
//        List<DefaultAddress> listaddres = new ArrayList<>();
        List<InternationalString> deliverypoint = new ArrayList<>();
        deliverypoint.add(new SimpleInternationalString("Marting-Luther-King-Weg 24"));
        DefaultAddress address = new DefaultAddress();
        address.setDeliveryPoints(deliverypoint);
        address.setCity(new SimpleInternationalString("Muenster"));
        address.setAdministrativeArea(new SimpleInternationalString("NRW"));
        address.setPostalCode("48165");
        address.setCountry(new SimpleInternationalString("Germany"));
        List<String> mail = new ArrayList<>();
        mail.add("conterra@contera.de");
        address.setElectronicMailAddresses(mail);
//        OnlineResource
        DefaultOnlineResource onlineresource = new DefaultOnlineResource();
        onlineresource.setLinkage(URI.create("mailto:conterra@conterra.de"));
        contactinfo.setPhones(listphone);
        contactinfo.setAddress(address);
        contactinfo.setOnlineResource(onlineresource);
        responsibleParty.setContactInfo(contactinfo);
        List<DefaultResponsibleParty> listrespon = new ArrayList<>();
        listrespon.add(responsibleParty);
        serviceProvide.setPointOfContacts(listrespon);
//        serviceProvide.set
        return serviceProvide;
    }

    /**
     *
     * @return
     */
    public OperationMetadata getOperationMetadata() {
        operationMetadata.setOperationName("GetCapabilities");
        List<DistributedComputingPlatform> a = new ArrayList<>();
        a.add(DistributedComputingPlatform.HTTP);
//        a.add(DistributedComputingPlatform.)
//        DistributedComputingPlatform.
        operationMetadata.setDistributedComputingPlatforms(a);
        List<DefaultOnlineResource> listresource = new ArrayList<>();
        DefaultOnlineResource onlineresource = new DefaultOnlineResource();
        onlineresource.setProtocolRequest("GET");
        onlineresource.setLinkage(URI.create("http://www.sdisuite.de/terraCatalog/soapServices/CSWStartup?"));
        listresource.add(onlineresource);
        DefaultOnlineResource onlineresource1 = new DefaultOnlineResource();
        onlineresource1.setProtocolRequest("POST");
        onlineresource1.setLinkage(URI.create("http://www.sdisuite.de/terraCatalog/soapService/services/CSWDiscovery"));
        listresource.add(onlineresource1);
        operationMetadata.setConnectPoints(listresource);
        //       Parameter
//       DefaultParameterDescriptor parameter = new DefaultParameterDescriptor(map)
//       operationMetadata.setParameters(newValues);
        return operationMetadata;
    }
    
    /**
     *
     * @param args
     * @throws JAXBException
     * @throws SAXException
     */
    public static void main(String[] args) throws JAXBException, SAXException {
//        GetCapabilities a = new GetCapabilities();
//        String b = XML.marshal(a.getOperationMetadata());
//        System.out.println(b);

        JAXBContext ctx = JAXBContext.newInstance(ElementSetName.class);
        BriefRecord brief = new BriefRecord();
        TransactionResponse root  =new TransactionResponse();
        InsertResult insert = new InsertResult();
        ElementSetName x = new ElementSetName() ;
//        ElementSetType x = new ElementSetType();
//        x.setValue("dsdsd");
        x.setValue("1234");
//        Date date = new Date();
//        root.setExpires(date);
//        RequestStatus type = new RequestStatus();
//        type.setStatus(RequestStatusType.CLUBS);
//        Date date = new Date();
//        System.out.println(date);
//        RequestStatus requestStatus = new RequestStatus();
//        requestStatus.setTimestamp(date);
//        requestStatus.setValue(Serch.complete);
//        date.
//        TestType a = new TestType();
//        a.setName("dsd");
//        a.setValue("dsds");
//        root.setSearchStatus(requestStatus);
        
        
//        root.setTitle("dsds");
////        root.setContributor("dsdsds");
//        BoundingBox bbox = new BoundingBox();
//        List<Double> upper = new ArrayList<>();
//        upper.add(18.9);
//        upper.add(19.9);
////        double[] u = {18.9 ,20.9};
//        List<Double> lower = new ArrayList<>();
//        lower.add(20.9);
//        lower.add(30.9);
//        bbox.setLowerCorner(lower);
//        bbox.setUpperCorner(upper);
//        
//        root.setCoverage(bbox);
//        root.setCreator("dsds");
//        root.setDescription("sdsd");
//        root.setFormat("sasa");
//        root.setLanguage("dsdsd");
//        root.setRelation("dsds");
//        root.setPublisher("dsds");
//        root.setRights("dsdsd");
//        root.setType("tee");
////        root.setSource("dsds");
        brief.setIdentifier("dwsds");
        List<AbstractRecord> b = new ArrayList<>();
        b.add(brief);
        TransactionSummary summary = new TransactionSummary();
        summary.setTotalDeleted(1);
//        BriefRecord b =new BriefRecord();
//        b.setIdentifier("dsds");
        
        insert.setBriefRecord(brief);
        root.setInsertResult(insert);
        root.setTransactionSummary(summary);
        root.setVersion("3.0.0");
//        root.setSubject("dsds");
//        String schemaLanguage = XMLConstants.W3C_XML_SCHEMA_NS_URI;
//        SchemaFactory sf = SchemaFactory.newInstance(schemaLanguage);
//        File f = new File("build/classes/org/apache/sis/services/catalog/schema/record.xsd");

         // create new schema
//        Schema schema = sf.newSchema(f);
        Marshaller m = ctx.createMarshaller();
//        m.setSchema(schema);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(x, System.out);
    }  
}