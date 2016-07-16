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
 * @author Thi Phuong Hao NGUYEN
 * @author Minh Chinh VU
 */
public class AnyText {
String format;
    String identifier;
    BoundingBox bbox = new BoundingBox();
    String startDate;
    String rangeDate;
    List<SummaryRecord> data = new ArrayList<SummaryRecord>();

    public AnyText() throws Exception {
        XMLReader a = new XMLReader();
        data.addAll(a.Metadata());
    }
    
    public List<SummaryRecord> getData(){
        return data;
    }

    public void setBbox(double west , double east, double south, double north) {
        bbox.setLowerCorner(west + " " + south);
        bbox.setUpperCorner(east + " " + north);
    }
    
    public AnyText(String format, String identifier, String startDate, String rangeDate) throws Exception {
        XMLReader a = new XMLReader();
        data.addAll(a.Metadata());
        this.format = format;
        this.identifier = identifier;
        this.startDate = startDate;
        this.rangeDate = rangeDate;
    }
    
    /**
    * CheckBBOX
    * True: input in Element Bound
    */
    
    public boolean checkBBOX(double east, double west, double south, double north, BoundingBox bound){
        String lower[] = bound.getLowerCorner().split(" ");
        String upper[] = bound.getUpperCorner().split(" ");
              
        double itWest = Double.parseDouble(lower[0]);
        double itNorth = Double.parseDouble(upper[1 ]);
        double itSouth = Double.parseDouble(lower[1]);
        double itEast = Double.parseDouble(upper[0]);
        
        if(east < itWest) return false;
        if(west > itEast)return false;
        if(north < itSouth) return false;
        if(south > itNorth) return false;
        
        return true;
    }
    
    //true:
    public boolean checkDate(String date1, String date2, SummaryRecord record) throws Exception {             
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date da1 = df.parse(date1);
        Date da2 = df.parse(date2);

        long day = (da2.getTime() - da1.getTime()) / (24 * 60 * 60 * 1000);

        Date da3 = record.getModified();
        long day1 = (da3.getTime() - da1.getTime()) / (24 * 60 * 60 * 1000);
        if (day1 >= 0 && day1 <= day) return true;
        return false;
    }
     
    public void filter() throws Exception{
        String lower[] = bbox.getLowerCorner().split(" ");
        String upper[] = bbox.getUpperCorner().split(" ");

        double west = Double.parseDouble(lower[0]);
        double north = Double.parseDouble(upper[1]);
        double south = Double.parseDouble(lower[1]);
        double east = Double.parseDouble(upper[0]);
        
        for (Iterator<SummaryRecord> it = data.iterator(); it.hasNext();) {
            SummaryRecord itSum = it.next();
            
            //Remove Out of range Date
            if(!this.checkDate(startDate,rangeDate,itSum)){
                it.remove();
                continue;
            }
            
            //Check by identifier
            if (!itSum.getIdentifier().contains(identifier)){
                it.remove();
                continue;
            }
            
            //Check by format type
            if (!itSum.getFormat().contains(format)){
                it.remove();
                continue; // NOTE: Iterator's remove method, not ArrayList's, is used.
            }
             
            //Remove picture out of BBOX range
            if(!checkBBOX(east, west, south, north, itSum.getBoundingBox())){
                it.remove();
                continue;
            }
            
        }
    }
    
//    public static void main(String[] args) throws Exception {
////        AnyText a= new AnyText("","","2009-04-04","2015-05-05");
////        AnyText b= new AnyText();
////        a.setBbox(5,130 , 5, 130);
////        a.filter();
////        System.out.println(a.getData().size());
////        String test[];
////        test = a.getData().get(0).getBoundingBox().getUpperCorner().split(" ");
////        System.out.println(test[1]+"  " + test[0]);
////        System.out.println(a.getData().get(0).getBoundingBox().getWestBoundLongitude());
////        System.out.println(a.getData().get(0).getBoundingBox().getEastBoundLongitude());
////        System.out.println(a.getData().get(0).getBoundingBox().getSouthBoundLongitude());
////        System.out.println(a.getData().get(0).getBoundingBox().getNorthBoundLongitude());
//    }
}
