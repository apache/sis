/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sis.services;


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
import org.apache.sis.services.csw.ConfigurationReader;
import org.apache.sis.services.csw.GetCapabilitie;
import org.apache.sis.services.csw.Record;
import org.apache.sis.services.csw.SummaryRecord;

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
    public List<SummaryRecord> DescribeRecord() throws ParseException, Exception {
        Record record = new Record(path.getPropValues());
        return record.getAllRecord();
    }
    @GET
    @Path("/GetRecordById")
    @Produces(MediaType.APPLICATION_XML)
    public SummaryRecord getRecordById(@QueryParam("Id") String id) throws ParseException, Exception {
        Record record = new Record(path.getPropValues());
        SummaryRecord a = record.getRecordById(id);
        return a;
    }
    @GET
    @Path("/GetRecord")
    @Produces(MediaType.APPLICATION_XML)
    public List<SummaryRecord> getRecordAllField(
            @QueryParam("format") String format,
            @QueryParam("identifier") String identifier,
            @QueryParam("west") double west,
            @QueryParam("east") double east,
            @QueryParam("south") double south,
            @QueryParam("north") double north,
            @QueryParam("startDate") String date1,
            @QueryParam("rangeDate") String date2) throws Exception {

        AnyText record = new AnyText(path.getPropValues(),format, identifier, date1, date2);
        record.setBbox(west, east, south, north);
        record.setBbox(5, 130, 5, 130);
        record.filter();

        return record.getData();
    }

    @GET
    @Path("/download/{name}")
    @Produces("text/plain")
    public Response getFileGeotiff(@PathParam("name") String name) throws IOException {
        File file = new File(path.getPropValues() + "/" + name);
        ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition", "attachment; filename=" + name);
        return response.build();
    }
}
