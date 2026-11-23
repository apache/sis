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
package org.apache.sis.referencing.dggs;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.opengis.metadata.citation.Organisation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic;
import org.apache.sis.referencing.dggs.internal.shared.GlobeLogo;
import org.apache.sis.util.logging.Logging;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DiscreteGlobalGridReferenceSystems {

    /**
     * Logger for the problems which do not prevent the use of the systems.
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.referencing.dggs");

    /**
     * Store and keep DGGRS list in a consistant order.
     * Keys are compared ignoring case.
     */
    private static final NavigableMap<String, DiscreteGlobalGridReferenceSystemFactory> DGGRS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        final Iterator<DiscreteGlobalGridReferenceSystemFactory> ite = ServiceLoader.load(DiscreteGlobalGridReferenceSystemFactory.class).iterator();
        while (ite.hasNext()) {
            final DiscreteGlobalGridReferenceSystemFactory f = ite.next();
            for (String dggh : f.listDggh()) {
                DGGRS.put(dggh, f);
            }
        }
    }

    private DiscreteGlobalGridReferenceSystems(){}

    public static List<DiscreteGlobalGridReferenceSystemFactory> getFactories() {
        return new ArrayList<>(DGGRS.values());
    }

    public static Set<String> listDggrs() {
        return Collections.unmodifiableSet(DGGRS.keySet());
    }

    /**
     * Resolve a DGGRS from it's code.
     * Supported forms are :
     * - IVEA4R
     * - [ogc-dggrs:IVEA4R]
     * - https://www.opengis.net/def/dggrs/IVEA4R
     *
     * <ul>
     *   <li>The identifier is case-insensitive.</li>
     *   <li>Trailing slashes of URLs are ignored.</li>
     * </ul>
     *
     * @param code
     * @return
     * @throws org.opengis.util.FactoryException
     * @throws org.opengis.util.NoSuchIdentifierException if not found
     */
    public static DiscreteGlobalGridReferenceSystem forCode(String code) throws FactoryException, NoSuchIdentifierException{
        if (code.startsWith("http")) {
            int end = code.length();
            while (end > 0 && code.charAt(end - 1) == '/') end--;
            code = code.substring(code.lastIndexOf('/', end - 1) + 1, end);
        }
        if (code.startsWith("[ogc-dggrs:") && code.endsWith("]")) {
            code = code.substring(11, code.length() - 1);
        }
        final DiscreteGlobalGridReferenceSystemFactory factory = DGGRS.get(code);
        if (factory == null) {
            throw new NoSuchIdentifierException("Unknown identifier", code);
        }
        // Give the identifier as declared by the factory.
        return factory.createDggrs(DGGRS.floorKey(code), null, null);
    }

    /**
     * Creates the organisation owning a discrete global grid system.
     * Its logo shows a grid of the system on a globe.
     * If the logo cannot be created, it is omitted and the reason is logged.
     *
     * @param  name  name of the organisation.
     * @param  dggs  the discrete global grid system, or {@code null}.
     * @return the organisation, or {@code null} if the system is null.
     */
    public static Organisation createParty(final String name, final DiscreteGlobalGridSystem dggs) {
        if (dggs == null) return null;
        DefaultBrowseGraphic logo = null;
        try {
            logo = new DefaultBrowseGraphic(toBase64(GlobeLogo.create(dggs)));
        } catch (TransformException | IOException | URISyntaxException | RuntimeException ex) {
            // The logo is decorative, its failure shall not prevent the creation of the organisation.
            Logging.unexpectedException(LOGGER, DiscreteGlobalGridReferenceSystems.class, "createParty", ex);
        }
        return new DefaultOrganisation(name, logo, null, null);
    }

    private static URI toBase64(RenderedImage image) throws IOException, URISyntaxException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("No PNG image writer available.");
        }
        out.flush();
        byte[] bytes = out.toByteArray();
        StringBuilder sb = new StringBuilder();
        sb.append("data:image/png;base64,");
        sb.append(new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8));
        return new URI(sb.toString());
    }

    /**
     * Convert a zone surface in m² to an accuracy in meters.
     * @return accuracy in meters
     */
    public static double surfaceToAccuracy(double surface) {
        return Math.sqrt(surface);
    }
}
