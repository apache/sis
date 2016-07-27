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
import org.apache.sis.services.csw.AnyText;
import org.apache.sis.services.csw.CapabilitiesRequest;
import org.apache.sis.services.csw.DescribeRecord;
import org.apache.sis.services.csw.GetCapabilitie;
import org.apache.sis.services.csw.Record;
import org.apache.sis.services.csw.GetRecord;

/**
 *
 * @author haonguyen
 */
@Path("/csw/2.0.2")
public class CSW {

    CapabilitiesRequest d = new CapabilitiesRequest();
    ConfigurationReader path = new ConfigurationReader();

    @GET
    @Path("/GetCapabilities")
    @Produces(MediaType.APPLICATION_XML)
    public List<GetCapabilitie> getCapabilities(@QueryParam("REQUEST") String request, @QueryParam("AcceptVersion") String Version, @QueryParam("AcceptFormat") String format) {
        if (request == "GetCapabilities" && Version == "2.0.2,2.0.0,1.0.7" && format == "application/xml") {
            return d.GetCapabilitiesRequest();
        }
        return d.GetCapabilitiesRequest();
    }

    @GET
    @Path("/DescribeRecord")
    @Produces(MediaType.APPLICATION_XML)
    public DescribeRecord DescribeRecord()  throws ParseException, Exception {
        DescribeRecord a= new DescribeRecord();
        a.setService(path.getValue("service"));
        a.setVersion(path.getValue("version"));
        a.setOutputFormat(path.getValue("outputFormat"));
        a.setSchemaLanguage(path.getValue("schemaLanguage"));
        a.setTypename("csw:Record");
        
        return a;
    }
    @GET
    @Path("/GetRecords")
    @Produces(MediaType.APPLICATION_XML)
    public List<GetRecord> GetRecords()  throws ParseException, Exception {
        Record record = new Record(path.getValue("Path"));
        return record.getAllRecord();
    }

    @GET
    @Path("/GetRecordById")
    @Produces(MediaType.APPLICATION_XML)
    public GetRecord getRecordById(@QueryParam("Id") String id) throws ParseException, Exception {
        Record record = new Record(path.getValue("Path"));
        GetRecord a = record.getRecordById(id);
        return a;
    }

    @GET
    @Path("/GetRecord")
    @Produces(MediaType.APPLICATION_XML)
    public List<GetRecord> getRecordAllField(
            @QueryParam("format") String format,
            @QueryParam("identifier") String identifier,
            @QueryParam("west") double west,
            @QueryParam("east") double east,
            @QueryParam("south") double south,
            @QueryParam("north") double north,
            @QueryParam("startDate") String date1,
            @QueryParam("rangeDate") String date2) throws Exception {

        AnyText record = new AnyText(path.getValue("Path"), format, identifier, date1, date2);
        record.setBbox(west, east, south, north);
        record.setBbox(5, 130, 5, 130);
        record.filter();

        return record.getData();
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
