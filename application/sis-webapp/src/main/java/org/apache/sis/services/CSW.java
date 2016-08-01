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
package org.apache.sis.services;

import org.apache.sis.services.csw.ConfigurationReader;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.sis.services.csw.request.AnyText;
import org.apache.sis.services.csw.request.DescribeRecordRequest;
import org.apache.sis.services.csw.request.GetCapabilitieRequest;
import org.apache.sis.services.csw.Record;
import org.apache.sis.services.csw.reponse.GetRecordByIdReponse;
import org.apache.sis.services.csw.reponse.GetRecordsReponse;
import org.apache.sis.services.csw.request.Capabilities;
import org.apache.sis.services.csw.request.SummaryRecord;

/**
 *
 * @author haonguyen
 */
@Path("/csw/2.0.2")
public class CSW {

    ConfigurationReader path = new ConfigurationReader();

    @GET
    @Path("/getcapabilities")
    @Produces(MediaType.APPLICATION_XML)
    public GetCapabilitieRequest getCapabilities(@QueryParam("service") String service, @QueryParam("version") String Version, @QueryParam("request") String request) {
     if(request.equals("GetCapabilities")){
            Capabilities version = new Capabilities();
            version.setVersion(new String[]{Version});
            Capabilities outputformat = new Capabilities();
            outputformat.setOutputFormat(new String[]{path.getValue("outputFormat")});
            GetCapabilitieRequest capabilite = new GetCapabilitieRequest(version, outputformat);
            return capabilite;
     }
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("/describerecord")
    public DescribeRecordRequest DescribeRecord(@QueryParam("service") String service, @QueryParam("version") String Version, @QueryParam("request") String request) throws ParseException, Exception {
        if (request.equals("DescribeRecord")) {
            DescribeRecordRequest a = new DescribeRecordRequest();
            a.setService(service);
            a.setVersion(Version);
            a.setOutputFormat(path.getValue("outputFormat"));
            a.setSchemaLanguage(path.getValue("schemaLanguage"));
            a.setTypename("Record");

            return a;
        }
        return null;
    }

    @GET
    @Path("/getrecords")
    @Produces(MediaType.APPLICATION_XML)
    public GetRecordsReponse GetRecords(
            @QueryParam("service") String service,
            @QueryParam("version") String Version,
            @QueryParam("request") String request,
            @QueryParam("startPosition") int start,
            @QueryParam("maxRecords") int size) throws ParseException, Exception {
         if (request.equals("GetRecords") && start >= 0 && size > 0){
        Record record = new Record(path.getValue("Path"),Version,service);
        return record.getAllRecordPaginated(start, size);
         }
         return null;
    }
    
    public GetRecordsReponse GetRecordsCQL(
            @QueryParam("service") String service,
            @QueryParam("version") String Version,
            @QueryParam("request") String request,
            @QueryParam("constraintLanguage") String constraintLanguage,
            @QueryParam("constraint") String constraint,
            @QueryParam("startPosition") int start,
            @QueryParam("maxRecords") int size) throws ParseException, Exception {
         if (request.equals("GetRecords") && constraintLanguage.toUpperCase().equals("CQL_TEXT")) {
            
            AnyText record = new AnyText(path.getValue("Path"), Version, service, constraintLanguage,constraint);
            record.filter();
            GetRecordsReponse a = new GetRecordsReponse();
            a.setRecord(record.getData());
            return a;
        }
        return null;
    }
    
    public GetRecordsReponse getRecordAllField(
            @QueryParam("service") String service,
            @QueryParam("version") String Version,
            @QueryParam("request") String request,
            @QueryParam("constraintLanguage") String constraintLanguage,
            @QueryParam("format") String format,
            @QueryParam("identifier") String identifier,
            @QueryParam("west") double west,
            @QueryParam("east") double east,
            @QueryParam("south") double south,
            @QueryParam("north") double north,
            @QueryParam("startDate") String date1,
            @QueryParam("rangeDate") String date2) throws Exception {
        
        if (request.equals("GetRecords") && constraintLanguage.toLowerCase().equals("filter")) {
            
            AnyText record = new AnyText(path.getValue("Path"), Version, service, constraintLanguage, format, identifier, date1, date2);
            record.setBbox(west, east, south, north);
            record.filter();
             GetRecordsReponse a = new GetRecordsReponse();
             a.setRecord(record.getData());
            return a;
        }
        return null;
    }

    @GET
    @Path("/getrecordbyid")
    @Produces(MediaType.APPLICATION_XML)
    public GetRecordByIdReponse getRecordById(
            @QueryParam("service") String service,
            @QueryParam("version") String Version,
            @QueryParam("request") String request,
            @QueryParam("Id") String id) throws ParseException, Exception {
        if (request.equals("GetRecordById")) {
            Record record = new Record(path.getValue("Path"),Version,service);
            GetRecordByIdReponse a = record.getRecordById(id);
            return a;
        }
        return null;
    }

    @GET
    @Path("/download/{name}")
    @Produces("text/plain")
    public Response getFileGeotiff(@PathParam("name") String name) throws IOException {
        File file = new File(path.getValue("Path") + "/" + name);
        ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition", "attachment; filename=" + name);
        return response.build();
    }
}
