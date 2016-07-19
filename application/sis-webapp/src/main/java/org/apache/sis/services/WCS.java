/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sis.services;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.apache.sis.services.csw.ConfigurationReader;

/**
 *
 * @author haonguyen
 */
@Path("/wcs")
public class WCS {
 ConfigurationReader path = new ConfigurationReader();
    @GET
    @Path("/image/{name}")
    @Produces({"image/png", "image/jpg"})
    public Response getFullImage(@PathParam("name") String name) throws IOException {
        ImageInputStream input = ImageIO.createImageInputStream(new File(path.getPropValues()+"/image/"+name));
        ImageReader reader = ImageIO.getImageReadersByFormatName("jpg").next();
        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(5, 5, 0, 0);
        reader.setInput(input);
        BufferedImage image = reader.read(0, param);
		
        return Response.ok(image).build();
    } 
}
