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
package org.apache.sis.services.csw;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author Thi Phuong Hao Nguyen (VNSC)
 * @author Minh Chinh Vu (VNSC)
 * @since 0.8
 * @version 0.8
 * @module
 */
public class AnyText {

    /**
     * The physical or digital manifestation of the resource use to search.
     */
    String format;
    /**
     * A unique reference to the record within the catalogue use to search.
     */
    String identifier;
    /**
     * A bouding box for identifying a geographic area of interest use to
     * search.
     */
    BoundingBox bbox = new BoundingBox();
    /**
     * The value startDate use to search .
     */
    String startDate;
    /**
     * The value rangeDate use to search .
     */
    String rangeDate;
    List<SummaryRecord> data = new ArrayList<SummaryRecord>();

    /**
     * Constructor for AnyText
     *
     * @throws Exception checked exceptions. Checked exceptions need to be
     * declared in a method or constructor's {@code throws} clause if they can
     * be thrown by the execution of the method or constructor and propagate
     * outside the method or constructor boundary.
     */
    public AnyText() throws Exception {
        Record a = new Record();
        data.addAll(a.getAllRecord());
    }

    /**
     * Return metadata
     *
     * @return data
     */
    public List<SummaryRecord> getData() {
        return data;
    }

    /**
     * Sets a bounding box use to search record.
     *
     * @param west The value is expressed in longitude in decimal degrees
     * (positive east)
     * @param east The value is expressed in longitude in decimal degrees
     * (positive east)
     * @param south The value is expressed in latitude in decimal degrees
     * (positive north).
     * @param north The value is expressed in latitude in decimal degrees
     * (positive north).
     */
    public void setBbox(double west, double east, double south, double north) {
        bbox.setLowerCorner(west + " " + south);
        bbox.setUpperCorner(east + " " + north);
    }
/**
     * AnyText used to search.
     *
     * @param format the physical or digital manifestation of the resource
     * @param identifier a unique reference to the record within the catalogue
     * @param startDate date from
     * @param rangeDate date to
     * @throws Exception Constructs a new exception with the specified detail
     * message.
     */
    public AnyText(String format, String identifier, String startDate, String rangeDate) throws Exception {
        Record a = new Record();
        data.addAll(a.getAllRecord());
        this.format = format;
        this.identifier = identifier;
        this.startDate = startDate;
        this.rangeDate = rangeDate;
    }

    /**
     * Set Bouding Box 
     * @param west The value is expressed in longitude in decimal degrees
     * (positive east)
     * @param east The value is expressed in longitude in decimal degrees
     * (positive east)
     * @param south The value is expressed in latitude in decimal degrees
     * (positive north).
     * @param north The value is expressed in latitude in decimal degrees
     * (positive north).
     * @param bound bounding box
     * @return bounding box define 
     */
    public boolean checkBBOX(double east, double west, double south, double north, BoundingBox bound) {
        String lower[] = bound.getLowerCorner().split(" ");
        String upper[] = bound.getUpperCorner().split(" ");

        double itWest = Double.parseDouble(lower[0]);
        double itNorth = Double.parseDouble(upper[1]);
        double itSouth = Double.parseDouble(lower[1]);
        double itEast = Double.parseDouble(upper[0]);

        if (east < itWest) {
            return false;
        }
        if (west > itEast) {
            return false;
        }
        if (north < itSouth) {
            return false;
        }
        if (south > itNorth) {
            return false;
        }

        return true;
    }

    /**
     * CheckDate
     *
     * @param date1 set the date value start
     * @param date2 set the date value final
     * @param record set the record define
     * @return the record define
     * @throws Exception checked exceptions. Checked exceptions need to be
     * declared in a method or constructor's {@code throws} clause if they can
     * be thrown by the execution of the method or constructor and propagate
     * outside the method or constructor boundary.
     */
    public boolean checkDate(String date1, String date2, SummaryRecord record) throws Exception {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date da1 = df.parse(date1);
        Date da2 = df.parse(date2);

        long day = (da2.getTime() - da1.getTime()) / (24 * 60 * 60 * 1000);

        Date da3 = record.getModified();
        long day1 = (da3.getTime() - da1.getTime()) / (24 * 60 * 60 * 1000);
        if (day1 >= 0 && day1 <= day) {
            return true;
        }
        return false;
    }

    /**
     * Filter a bounding box for identifying a geographic area of interest use to search.
     * @throws Exception  Exception checked exceptions. Checked exceptions need to be
     * declared in a method or constructor's {@code throws} clause if they can
     * be thrown by the execution of the method or constructor and propagate
     * outside the method or constructor boundary.
     */
    public void filter() throws Exception {
        String lower[] = bbox.getLowerCorner().split(" ");
        String upper[] = bbox.getUpperCorner().split(" ");

        double west = Double.parseDouble(lower[0]);
        double north = Double.parseDouble(upper[1]);
        double south = Double.parseDouble(lower[1]);
        double east = Double.parseDouble(upper[0]);

        for (Iterator<SummaryRecord> it = data.iterator(); it.hasNext();) {
            SummaryRecord itSum = it.next();

            /**
             * Remove Out of range Date.
             */
            if (!this.checkDate(startDate, rangeDate, itSum)) {
                it.remove();
                continue;
            }

            /**
             * Check by identifier.
             */
            if (!itSum.getIdentifier().contains(identifier)) {
                it.remove();
                continue;
            }

            /**
             * Check by format type.
             */
            if (!itSum.getFormat().contains(format)) {
                it.remove();
                continue;
                /**
                 * NOTE: Iterator's remove method, not ArrayList's, is used.
                 *
                 */
            }

            /**
             * Remove picture out of BBOX range.
             */
            if (!checkBBOX(east, west, south, north, itSum.getBoundingBox())) {
                it.remove();
                continue;
            }

        }
    }
}
