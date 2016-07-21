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
        ImageInputStream input = ImageIO.createImageInputStream(new File(path.getValue("Path") + "/image/" + name));
        ImageReader reader = ImageIO.getImageReadersByFormatName("jpg").next();
        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(5, 5, 0, 0);
        reader.setInput(input);
        BufferedImage image = reader.read(0, param);

        return Response.ok(image).build();
    }
}
