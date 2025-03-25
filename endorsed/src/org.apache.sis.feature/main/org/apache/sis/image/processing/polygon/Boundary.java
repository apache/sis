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

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.sis.measure.NumberRange;

/**
 * Define an "in construction" geometries.
 * This type of objects contain a list of floeatings coordinates sequences
 * that are progressivly merged together to obtain a polygon which might contain holes.
 *
 * @author Johann Sorel (Geomatys)
 */
final class Boundary {

    //finished geometries
    private final List<GeneralPath> holes = new ArrayList<>();

    //in construction geometries
    private final LinkedList<LinkedList<Point2D.Double>> floatings = new LinkedList<LinkedList<Point2D.Double>>();
    final int classe;

    public Boundary(final int classe){
        this.classe = classe;
    }

    public void start(final int firstX, final int secondX, final int y){
        if(firstX == secondX) throw new IllegalArgumentException("bugging algorithm");
        final LinkedList<Point2D.Double> exterior = new LinkedList<Point2D.Double>();
        exterior.addFirst(new Point2D.Double(firstX, y));
        exterior.addFirst(new Point2D.Double(firstX, y+1));
        exterior.addLast(new Point2D.Double(secondX, y));
        exterior.addLast(new Point2D.Double(secondX, y+1));
        floatings.add(exterior);

        //checkValidity();
    }

    public void addFloating(final Point2D.Double from, final Point2D.Double to){
        if (from.equals(to)) {
            throw new IllegalArgumentException("bugging algorithm");
        }
        //System.err.println("Add Floating From: " + from + " New : " + to );

        final LinkedList<Point2D.Double> ll = new LinkedList<Point2D.Double>();
        ll.addFirst(to);
        ll.addLast(from);
        floatings.add(ll);

        //checkValidity();
    }

    public void add(final Point2D.Double from, final Point2D.Double to){
        if(from.equals(to)){
            throw new IllegalArgumentException("bugging algorithm");
        }

        //System.err.println("Add From: " + from + " New : " + to );


        for(final LinkedList<Point2D.Double> ll : floatings){
            final Point2D.Double first = ll.getFirst();
            if(first.equals(from)){

                if(from.x == to.x && ll.size() > 1){
                    final Point2D.Double second = ll.get(1);
                    if(second.x == from.x){
                        //points are aligned, just move the first point
                        first.y = to.y;
                    }else{
                        //points are not aligned, must create a new point
                        ll.addFirst(to);
                    }
                }else if(from.y == to.y && ll.size() > 1){
                    final Point2D.Double second = ll.get(1);
                    if(second.y == from.y){
                        //points are aligned, just move the first point
                        first.x = to.x;
                    }else{
                        //points are not aligned, must create a new point
                        ll.addFirst(to);
                    }
                }else{
                    ll.addFirst(to);
                }

                //checkValidity();
                return;
            }

            final Point2D.Double last = ll.getLast();
            if(last.equals(from)){
                if(from.x == to.x && ll.size() > 1){
                    final Point2D.Double second = ll.get(ll.size()-2);
                    if(second.x == from.x){
                        //points are aligned, just move the first point
                        last.y = to.y;
                    }else{
                        //points are not aligned, must create a new point
                        ll.addLast(to);
                    }
                }else if(from.y == to.y && ll.size() > 1){
                    final Point2D.Double second = ll.get(ll.size()-2);
                    if(second.y == from.y){
                        //points are aligned, just move the first point
                        last.x = to.x;
                    }else{
                        //points are not aligned, must create a new point
                        ll.addLast(to);
                    }
                }else{
                    ll.addLast(to);
                }

                //checkValidity();
                return;
            }
        }

        //checkValidity();
        throw new IllegalArgumentException("bugging algorithm");
    }

    public Shape link(final Point2D.Double from, final Point2D.Double to){
        if(from.equals(to)){
            throw new IllegalArgumentException("bugging algorithm : " + to);
        }

        //System.err.println("Link From: " + from + " to : " + to );


        LinkedList<Point2D.Double> fromList = null;
        boolean fromStart = false;
        LinkedList<Point2D.Double> toList = null;
        boolean toStart = false;

        for(final LinkedList<Point2D.Double> ll : floatings){

            if(fromList == null){
                final Point2D.Double first = ll.getFirst();
                final Point2D.Double last = ll.getLast();
                if(first.equals(from)){
                    fromStart = true;
                    fromList = ll;
                }else if(last.equals(from)){
                    fromStart = false;
                    fromList = ll;
                }
            }

            if(toList == null){
                final Point2D.Double first = ll.getFirst();
                final Point2D.Double last = ll.getLast();
                if(first.equals(to)){
                    toStart = true;
                    toList = ll;
                }else if(last.equals(to)){
                    toStart = false;
                    toList = ll;
                }
            }

            if(fromList != null && toList != null) break;
        }


        if(fromList != null && toList != null){
            if(fromList == toList){
                //same list finish it
                //checkValidity();
                return finish(fromList);
            }else{
                combine(fromList, fromStart, toList, toStart);
                //checkValidity();
                return null;
            }

        }else if(fromList != null ){
            add(from, to);
            //checkValidity();
            return null;
        }else if(toList != null){
            add(to, from);
            //checkValidity();
            return null;
        }

        //checkValidity();
        throw new IllegalArgumentException("bugging algorithm");
    }

    private void combine(final LinkedList<Point2D.Double> fromList, final boolean fromStart,
                         final LinkedList<Point2D.Double> toList, final boolean toStart){

        if(fromStart){
            if(toStart){
                while(!toList.isEmpty()){
                    fromList.addFirst(toList.pollFirst());
                }
            }else{
                while(!toList.isEmpty()){
                    fromList.addFirst(toList.pollLast());
                }
            }

        }else{
            if(toStart){
                while(!toList.isEmpty()){
                    fromList.addLast(toList.pollFirst());
                }
            }else{
                while(!toList.isEmpty()){
                    fromList.addLast(toList.pollLast());
                }
            }
        }

        floatings.remove(toList);
        //checkValidity();
    }


    public void checkValidity(){

        //check for list with less than 2 elements
        for(LinkedList<Point2D.Double> ll : floatings){
            if(ll.size() < 2){
                //System.err.println(">>>> ERROR : " + this.toString());
                throw new IllegalArgumentException("What ? a list with less than 2 elements, not valid !");
            }
        }

        //check for diagonal cases
        for(LinkedList<Point2D.Double> ll : floatings){
            Point2D.Double last = ll.get(0);
            for(int i=1;i<ll.size();i++){
                Point2D.Double current = ll.get(i);
                if(last.x != current.x && last.y != current.y){
                    //System.err.println(">>>> ERROR : " + this.toString());
                    throw new IllegalArgumentException("What ? a diagonal, not valid !");
                }
                last = current;
            }
        }

    }



    private Shape finish(final LinkedList<Point2D.Double> coords){

        if(floatings.size() == 1){
            //closing the polygon enveloppe
            Shape geom = toClosedShape(coords);
            //ring.setUserData(range);
            floatings.remove(coords);
            
            if (!holes.isEmpty()) {
                Area area = new Area(geom);
                for (Shape hole : holes) {
                    area.subtract(new Area(hole));
                }
                geom = area;
            }
            
            return geom;
        }else{
            //closing a hole in the geometry
            GeneralPath ring = toClosedShape(coords);
            holes.add(ring);
            floatings.remove(coords);
            return null;
        }

    }

    private static void reverse(final Point2D.Double[] array){
        for(int l=0, r=array.length-1; l<r; l++, r--) {
            Point2D.Double temp = array[l];
            array[l] = array[r];
            array[r] = temp;
        }
    }
    
    private GeneralPath toClosedShape(List<Point2D.Double> points) {
        final GeneralPath path = new GeneralPath();
        Point2D.Double start = points.get(0);
        path.moveTo(start.x, start.y);
        for (int i = 1, n = points.size(); i < n; i++) {
            Point2D.Double pt = points.get(i);
            path.lineTo(pt.x, pt.y);
        }
        path.closePath();
        
        return path;
    }

    public void merge(final Boundary candidate){
        //merge the floating sequences
        this.floatings.addAll(candidate.floatings);
        //merge the holes
        this.holes.addAll(candidate.holes);
        candidate.floatings.clear();
        candidate.holes.clear();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Boundary : ");
        sb.append(classe);

        for(LinkedList<Point2D.Double> coords : floatings){
            sb.append("  \t{");
            for(Point2D.Double c : coords){
                sb.append('[').append((int)c.x).append(';').append((int)c.y).append(']');
            }
            sb.append('}');
        }

        return sb.toString();
    }

    public boolean isEmpty(){
        return floatings.isEmpty();
    }

}
