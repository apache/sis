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
import org.apache.sis.measure.NumberRange;
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
    private final List<Map<NumberRange, List<Shape>>> polygons = new ArrayList<>();

    //buffer[band][LAST_LINE] holds last line buffer
    //buffer[band][CURRENT_LINE] holds current line buffer
    private Boundary[][][] buffers;

    //current pixel block
    private Block[] blocks;

    private final RenderedImage image;
    private final NumberRange[][] ranges;
    
    /**
     *
     * @param coverage coverage to process
     * @param ranges data value ranges
     * @param band coverage band to process
     */
    public Polygonize(RenderedImage image, NumberRange[][] ranges){
        this.image = image;
        this.ranges = ranges;
    }
    
    public List<Map<NumberRange, List<Shape>>> polygones() {

        final PixelIterator iter = new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR).create(image);
        final int nbBand = iter.getNumBands();
        blocks = new Block[nbBand];
        
        final NumberRange NaNRange = new NaNRange();
        for (int band = 0; band < nbBand; band++) {            
            final Map<NumberRange, List<Shape>> bandState = new HashMap<>();            
            //add a range for Nan values.            
            bandState.put(NaNRange, new ArrayList<>());
            polygons.add(bandState);

            for (final NumberRange range : ranges[Math.min(band, ranges.length-1)]) {
                bandState.put(range, new ArrayList<>());
            }
            
            blocks[band] = new Block();
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
                    append(polygons.get(band), buffers[band], blocks[band], gridPosition, pixel[band]);
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
                    polygons.get(band).get(buffers[band][LAST_LINE][i].range).add(poly);
                }
            }
        }
        
        //avoid memory use
        buffers = null;
        final List<Map<NumberRange, List<Shape>>> copy = new ArrayList<>(polygons);
        //remove the NaNRange
        for (Map m : copy) {
            m.remove(NaNRange);
        }
        polygons.clear();        
        return copy;
    }

    private static void append(Map<NumberRange, List<Shape>> results, final Boundary[][] buffers, final Block block, final Point point, Number value) {

        //special case for NaN or null
        final NumberRange valueRange;
        if (value == null || Double.isNaN(value.doubleValue())) {
            valueRange = new NaNRange();
        } else {
            valueRange = results.keySet().stream()
                    .filter(range -> range.containsAny(value))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Value not in any range :" + value));
        }

        if (valueRange.equals(block.range)) {
            //last pixel was in the same range
            block.endX = point.x;
            return;
        } else if (block.range != null) {
            //last pixel was in a different range, save it's geometry
            constructBlock(results, block, buffers);
        }

        //start a pixel serie
        block.range = valueRange;
        block.startX = point.x;
        block.endX = point.x;
        block.y = point.y;
    }

    private static void constructBlock(Map<NumberRange, List<Shape>> results, final Block block, final Boundary[][] buffers) {

        //System.err.println("BLOCK ["+block.startX+","+block.endX+"]");

        if(block.y == 0) {
            //first line, the buffer is empty, must fill it
            final Boundary boundary = new Boundary(block.range);
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
                if (candidate.range != block.range) {
                    //System.err.println("A different block extent : "+ candidateExtent[0] + " " + candidateExtent[1]);
                    //System.err.println("before :" + candidate.toString());

                    if (candidateExtent[0] >= block.startX && candidateExtent[1] <= block.endX) {
                        //block overlaps completly candidate
                        final Shape poly = candidate.link(
                                new Point2D.Double(candidateExtent[0], block.y),
                                new Point2D.Double(candidateExtent[1]+1, block.y)
                                );
                        if(poly != null) results.get(candidate.range).add(poly);
                    } else {
                        final Shape poly = candidate.link(
                                new Point2D.Double( (block.startX<candidateExtent[0]) ? candidateExtent[0]: block.startX, block.y),
                                new Point2D.Double( (block.endX>candidateExtent[1]) ? candidateExtent[1]+1: block.endX+1, block.y)
                                );
                        if (poly != null) results.get(candidate.range).add(poly);
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
                if (candidate.range == block.range) {
                    //System.err.println("A firnet block extent : "+ candidateExtent[0] + " " + candidateExtent[1]);
//                    //System.err.println("before :" + candidate.toString());

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
                        //System.out.println("Merging : " + currentBoundary.toString());
                    }

                    if (candidateExtent[0] < firstAnchor) {
                        firstAnchor = candidateExtent[0];
                    }
                    lastAnchor = candidateExtent[1]+1;
                }

                i = candidateExtent[1]+1;
            }

            if (currentBoundary != null) {
                //System.err.println("before :" + currentBoundary.toString());
            }

            if (currentBoundary == null) {
                //no previous friendly boundary to link with
                //make a new one
                currentBoundary = new Boundary(block.range);
                currentBoundary.start(block.startX, block.endX+1, block.y);
            } else {
                if (firstAnchor < block.startX) {
                    //the previous block has created a floating sequence to this end
                    firstAnchor = block.startX;
                }

                //add the coordinates
                //System.err.println("> first anchor : " +firstAnchor + " lastAnchor : " +lastAnchor);
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
                        //System.err.println("0 add :" + currentBoundary.toString());
                        currentBoundary.add(
                            new Point2D.Double(lastAnchor, block.y),
                            new Point2D.Double(block.endX+1, block.y)
                            );
                        //System.err.println("1 add:" + currentBoundary.toString());
                        currentBoundary.add(
                            new Point2D.Double(block.endX+1, block.y),
                            new Point2D.Double(block.endX+1, block.y+1)
                            );
                        //System.err.println("after add:" + currentBoundary.toString());
                    }
                } else {
                    currentBoundary.addFloating(
                            new Point2D.Double(block.endX+1, block.y),
                            new Point2D.Double(block.endX+1, block.y+1)
                            );
                }

                //System.err.println(currentBoundary.toString());

            }

            //System.err.println("after :" + currentBoundary.toString());

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

    private static class NaNRange extends NumberRange{

        public NaNRange() {
            super(Double.class, 0d, true, 0d, true);
        }

        @Override
        public boolean contains(final Comparable number) throws IllegalArgumentException {
            return Double.isNaN(((Number) number).doubleValue());
        }
    }

}
