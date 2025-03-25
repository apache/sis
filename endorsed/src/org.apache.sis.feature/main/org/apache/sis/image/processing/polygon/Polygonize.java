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
package org.apache.sis.image.processing.polygon;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleToIntFunction;
import org.apache.sis.image.PixelIterator;
import org.opengis.coverage.grid.SequenceType;


/**
 * Process to extract Polygon from an image.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Polygonize {

    private static final int LAST_LINE = 0;
    private static final int CURRENT_LINE = 1;

    //last line cache boundary
    private final List<Map<Integer, List<Shape>>> polygons = new ArrayList<>();

    //buffer[band][LAST_LINE] holds last line buffer
    //buffer[band][CURRENT_LINE] holds current line buffer
    private Boundary[][][] buffers;

    //current pixel block
    private Block[] blocks;

    private final RenderedImage image;
    private final DoubleToIntFunction[] classifiers;
    
    /**
     *
     * @param coverage coverage to process
     * @param ranges data value ranges
     * @param band coverage band to process
     */
    public Polygonize(RenderedImage image, DoubleToIntFunction[] classifiers){
        this.image = image;
        this.classifiers = classifiers;
    }
    
    public List<Map<Integer, List<Shape>>> polygones() {

        final PixelIterator iter = new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR).create(image);
        final int nbBand = iter.getNumBands();
        blocks = new Block[nbBand];
        
        final DoubleToIntFunction[] classifiers = new DoubleToIntFunction[nbBand];
        for (int band = 0; band < nbBand; band++) {            
            final Map<Integer, List<Shape>> bandState = new HashMap<>();
            polygons.add(bandState);            
            blocks[band] = new Block();
            classifiers[band] = this.classifiers[Math.max(this.classifiers.length-1, band)];
        }
        
        /*
         This algorithm create polygons which follow the contour of each pixel.
         The 0,0 coordinate will match the pixel corner.
        */
        final Point gridPosition = new Point(0, 0);
        double[] pixel = null;

        final int width = image.getWidth();
        final int height = image.getHeight();
        buffers = new Boundary[nbBand][2][width];
        
        for (int y = 0; y < height; y++) {
            iter.moveTo(0, y);
            gridPosition.y = y;
            for (int x = 0; x < width; x++) {
                gridPosition.x = x;
                pixel = iter.getPixel(pixel);
                for (int band = 0; band < nbBand; band++) {
                    append(classifiers[band], polygons.get(band), buffers[band], blocks[band], gridPosition, pixel[band]);
                }
                iter.next();
            }
                        
            //insert last geometry
            for (int band = 0; band < nbBand; band++) {
                constructBlock(polygons.get(band), blocks[band], buffers[band]);
                
                //flip buffers, reuse old buffer line.
                Boundary[] oldLine = buffers[band][LAST_LINE];
                buffers[band][LAST_LINE] = buffers[band][CURRENT_LINE];
                buffers[band][CURRENT_LINE] = oldLine;

                blocks[band].reset();    
            }
        
        }
        
        //we have finish, close all geometries
        gridPosition.y += 1;
        for (int band = 0; band < nbBand; band++) {
            for(int i = 0; i < buffers[band][LAST_LINE].length;i++) {
                Shape poly = buffers[band][LAST_LINE][i].link(
                        new Point2D.Double(i, gridPosition.y),
                        new Point2D.Double(i+1, gridPosition.y)
                        );
                if (poly != null) {
                    final int classe = buffers[band][LAST_LINE][i].classe;
                    final Map<Integer, List<Shape>> map = polygons.get(band);
                    List<Shape> lst = map.get(classe);
                    if (lst == null) {
                        lst = new ArrayList<>();
                        map.put(classe, lst);
                    }
                    lst.add(poly);
                }
            }
        }
        
        //avoid memory use
        buffers = null;
        final List<Map<Integer, List<Shape>>> copy = new ArrayList<>(polygons);
        polygons.clear();        
        return copy;
    }

    private static void append(DoubleToIntFunction classifier, Map<Integer, List<Shape>> results, final Boundary[][] buffers, final Block block, final Point point, Number value) {

        final int classe = classifier.applyAsInt(value.doubleValue());
        if (classe == block.classe) {
            //last pixel was in the same class
            block.endX = point.x;
            return;
        } else if (block.classe != -1) {
            //last pixel was in a different range, save it's geometry
            constructBlock(results, block, buffers);
        }

        //start a pixel serie
        block.classe = classe;
        block.startX = point.x;
        block.endX = point.x;
        block.y = point.y;
    }

    private static void constructBlock(Map<Integer, List<Shape>> results, final Block block, final Boundary[][] buffers) {

        //System.err.println("BLOCK ["+block.startX+","+block.endX+"]");

        if(block.y == 0) {
            //first line, the buffer is empty, must fill it
            final Boundary boundary = new Boundary(block.classe);
            boundary.start(block.startX, block.endX+1, block.y);

            for(int i=block.startX; i<=block.endX; i++) {
                buffers[CURRENT_LINE][i] = boundary;
            }
        }else{
            Boundary currentBoundary = null;

            //first pass to close unfriendly blocks ----------------------------
            for (int i=block.startX; i<=block.endX;) {
                final Boundary candidate = buffers[LAST_LINE][i];
                final int[] candidateExtent = findExtent(buffers, i);

                //do not treat same blockes here
                if (candidate.classe != block.classe) {
                    //System.err.println("A different block extent : "+ candidateExtent[0] + " " + candidateExtent[1]);
                    //System.err.println("before :" + candidate.toString());

                    if (candidateExtent[0] >= block.startX && candidateExtent[1] <= block.endX) {
                        //block overlaps completly candidate
                        final Shape poly = candidate.link(
                                new Point2D.Double(candidateExtent[0], block.y),
                                new Point2D.Double(candidateExtent[1]+1, block.y)
                                );
                        if (poly != null) {
                            List<Shape> lst = results.get(candidate.classe);
                            if (lst == null) {
                                lst = new ArrayList<>();
                                results.put(candidate.classe, lst);
                            }
                            lst.add(poly);
                        }
                    } else {
                        final Shape poly = candidate.link(
                                new Point2D.Double( (block.startX<candidateExtent[0]) ? candidateExtent[0]: block.startX, block.y),
                                new Point2D.Double( (block.endX>candidateExtent[1]) ? candidateExtent[1]+1: block.endX+1, block.y)
                                );
                        if (poly != null) {
                            List<Shape> lst = results.get(candidate.classe);
                            if (lst == null) {
                                lst = new ArrayList<>();
                                results.put(candidate.classe, lst);
                            }
                            lst.add(poly);
                        }
                    }

                    //System.err.println("after :" + candidate.toString());
                }

                i = candidateExtent[1]+1;
            }

            //second pass to fuse with friendly blocks -------------------------

            //we first merge the last line boundary if needed
            int firstAnchor = Integer.MAX_VALUE;
            int lastAnchor = Integer.MIN_VALUE;

            for (int i = block.startX; i <= block.endX; ) {
                final Boundary candidate = buffers[LAST_LINE][i];
                final int[] candidateExtent = findExtent(buffers, i);

                //do not treat different blocks here
                if (candidate.classe == block.classe) {

                    if (currentBoundary == null) {
                        //set the current boundary, will expend this one
                        currentBoundary = candidate;
                    } else if(currentBoundary != null) {
                        if(currentBoundary != candidate) {
                            //those two blocks doesnt belong to the same boundaries, we must merge them
                            currentBoundary.merge(candidate);
                        }
                        currentBoundary.link(
                            new Point2D.Double(lastAnchor, block.y),
                            new Point2D.Double(candidateExtent[0], block.y)
                            );

                        replaceInLastLigne(buffers, candidate, currentBoundary);
                    }

                    if (candidateExtent[0] < firstAnchor) {
                        firstAnchor = candidateExtent[0];
                    }
                    lastAnchor = candidateExtent[1]+1;
                }

                i = candidateExtent[1]+1;
            }

            if (currentBoundary == null) {
                //no previous friendly boundary to link with
                //make a new one
                currentBoundary = new Boundary(block.classe);
                currentBoundary.start(block.startX, block.endX+1, block.y);
            } else {
                if (firstAnchor < block.startX) {
                    //the previous block has created a floating sequence to this end
                    firstAnchor = block.startX;
                }

                //add the coordinates
                if (firstAnchor == block.startX) {
                    currentBoundary.add(
                        new Point2D.Double(firstAnchor, block.y),
                        new Point2D.Double(block.startX, block.y+1)
                        );
                } else {
                    currentBoundary.add(
                        new Point2D.Double(firstAnchor, block.y),
                        new Point2D.Double(block.startX, block.y)
                        );
                    currentBoundary.add(
                        new Point2D.Double(block.startX, block.y),
                        new Point2D.Double(block.startX, block.y+1)
                        );
                }

                if (block.endX+1 >= lastAnchor) {
                    if (lastAnchor == block.endX+1) {
                        currentBoundary.add(
                            new Point2D.Double(lastAnchor, block.y),
                            new Point2D.Double(block.endX+1, block.y+1)
                            );
                    } else {
                        currentBoundary.add(
                            new Point2D.Double(lastAnchor, block.y),
                            new Point2D.Double(block.endX+1, block.y)
                            );
                        currentBoundary.add(
                            new Point2D.Double(block.endX+1, block.y),
                            new Point2D.Double(block.endX+1, block.y+1)
                            );
                    }
                } else {
                    currentBoundary.addFloating(
                            new Point2D.Double(block.endX+1, block.y),
                            new Point2D.Double(block.endX+1, block.y+1)
                            );
                }
            }

            //fill in the current line -----------------------------------------

            for (int i = block.startX; i <= block.endX; i++) {
                if (currentBoundary.isEmpty()) {
                    throw new IllegalArgumentException("An empty boundary inserted ? not possible.");
                }

                buffers[CURRENT_LINE][i] = currentBoundary;
            }

        }

    }

    private static void replaceInLastLigne(final Boundary[][] buffers, final Boundary old, final Boundary newone) {
        for (int i = 0, n = buffers[LAST_LINE].length; i < n; i++) {
            if (buffers[LAST_LINE][i] == old) {
                buffers[LAST_LINE][i] = newone;
            }

            if (buffers[CURRENT_LINE][i] == old) {
                buffers[CURRENT_LINE][i] = newone;
            }
        }
    }


    private static int[] findExtent(final Boundary[][] buffers, final int index) {
        final int[] extent = new int[]{index,index};
        final Boundary bnd = buffers[LAST_LINE][index];

        while (extent[0] > 0 && buffers[LAST_LINE][ extent[0]-1 ] == bnd) {
            extent[0]--;
        }

        while (extent[1] < buffers[LAST_LINE].length-1 && buffers[LAST_LINE][ extent[1]+1 ] == bnd) {
            extent[1]++;
        }

        return extent;
    }

}
