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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.Interpolation;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic;
import org.apache.sis.referencing.operation.transform.AbstractMathTransform2D;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.metadata.citation.Organisation;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DiscreteGlobalGridReferenceSystems {

    /**
     * Store and keep DGGRS list in a consistant order.
     */
    private static final Map<String, DiscreteGlobalGridReferenceSystemFactory> DGGRS = new TreeMap<>();

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
     * @param code
     * @return
     * @throws org.opengis.util.FactoryException
     * @throws org.opengis.util.NoSuchIdentifierException if not found
     */
    public static DiscreteGlobalGridReferenceSystem forCode(String code) throws FactoryException, NoSuchIdentifierException{
        if (code.startsWith("http")) {
            code = code.substring(code.lastIndexOf('/'));
        }
        if (code.startsWith("[ogc-dggrs:") && code.endsWith("]")) {
            code = code.substring(11, code.length() - 1);
        }

        DiscreteGlobalGridReferenceSystemFactory cdt = DGGRS.get(code);
        if (cdt == null) {
            code = code.toUpperCase();
            cdt = DGGRS.get(code);
        }
        if (cdt == null) {
            code = code.toLowerCase();
            cdt = DGGRS.get(code);
        }
        if (cdt != null) {
            return cdt.createDggrs(code, null, null);
        } else {
            throw new NoSuchIdentifierException("Unknown identifier",code);
        }

    }

    public static Organisation createParty(String name, DiscreteGlobalGridSystem dggs) throws TransformException, IOException, URISyntaxException {
        if (dggs == null) return null;
        return new DefaultOrganisation(name, new DefaultBrowseGraphic(toBase64(createLogo(dggs))), null, null);
    }

    private static RenderedImage createLogo(DiscreteGlobalGridSystem dggs) throws TransformException {

        final double scale = 2.0;
        final BufferedImage image = new BufferedImage((int)(360*scale), (int)(180*scale), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setPaint(new LinearGradientPaint(0, 0, image.getWidth(), 0, new float[]{0,1}, new Color[]{Color.BLACK, Color.WHITE}));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try (Stream<Zone> zones = dggs.getHierarchy().getGrids().get(1).getZones()) {
            final Iterator<Zone> iterator = zones.iterator();
            while (iterator.hasNext()) {
                Zone zone = iterator.next();
                org.locationtech.jts.geom.Polygon polygon = DiscreteGlobalGridSystems.toJTSPolygon(zone.getGeographicExtent());
                Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
                for (int i = 0; i < coordinates.length-1; i++) {
                    double x0 = coordinates[i].x;
                    double y0 = coordinates[i].y;
                    double x1 = coordinates[i+1].x;
                    double y1 = coordinates[i+1].y;
                    g.drawLine((int)((x0 + 180)*scale),
                               (int)((y0 + 90)*scale),
                               (int)((x1 + 180)*scale),
                               (int)((y1 + 90)*scale));

                }
            }
        }
        g.dispose();

        final ImageProcessor p = new ImageProcessor();
        p.setInterpolation(Interpolation.LANCZOS);

        RenderedImage imageSphere = p.resample(image, new Rectangle(0, 0, 256, 256), new AbstractMathTransform2D() {
            @Override
            public Matrix transform(double[] source, int inIdx, double[] target, int outIdx, boolean bln) throws TransformException {


                double x = source[inIdx];
                double y = source[inIdx+1];
                double sx = (x - 128.0) / 128.0;
                double sy = (y - 128.0) / 128.0;
                if (Math.hypot(sx, sy) > 1) {
                    target[outIdx] = -1000;
                    target[outIdx+1] = -1000;
                    return null;
                } //out of sphere

                //            z = Math.sin(latr);
                // Math.asin(z) = latr
                double latr = Math.asin(sy);

                //                             y = Math.sin(lonr) * Math.cos(latr)
                //            y / Math.cos(latr) = Math.sin(lonr)
                // Math.asin(y / Math.cos(latr)) = lonr
                double lonr = Math.asin(sx / Math.cos(latr));

                double lon = Math.toDegrees(lonr);
                double lat = Math.toDegrees(latr);
                double lonx = (lon + 180)*scale;
                double laty = (lat + 90)*scale;

                target[outIdx] = lonx;
                target[outIdx+1] = laty;

                return null;
            }
        });

        return imageSphere;
    }

    private static URI toBase64(RenderedImage image) throws IOException, URISyntaxException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean res = ImageIO.write(image, "png", out);
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
