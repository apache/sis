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

package org.apache.sis.storage.shapefile;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
//import java.nio.channels.ReadableByteChannel;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
//import org.apache.sis.internal.storage.ChannelImageInputStream;


/**
 * Provides a ShapeFile Reader
 *
 * @see <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf">ESRI Shapefile Specification</a>
 * @see <a href="http://ulisse.elettra.trieste.it/services/doc/dbase/DBFstruct.htm">dBASE III File Structure</a>
 */
public class ShapeFile {

	public int FileCode;   // big
	public int FileLength;  // big // The value for file length is the total length of the file in 16-bit words
	public int Version; // little
	public ShapeTypeEnum ShapeType; // little
	public double xmin; // little
	public double ymin; // little 
	public double xmax; // little 
	public double ymax; // little 
	public double zmin; // little 
	public double zmax; // little 
	public double mmin; // little
	public double mmax; // little 
	
	
	// http://ulisse.elettra.trieste.it/services/doc/dbase/DBFstruct.htm
	public byte DbaseVersion;
	public byte[] DbaseLastUpdate = new byte[3];
	public int FeatureCount;
	public short DbaseHeaderBytes;
	public short DbaseRecordBytes;
	// reserve 3 bytes
	public byte[] DbasePlusLanReserved = new byte[13];
	// reserve 4 bytes
	
	public ArrayList<FieldDescriptor> FDArray = new ArrayList<FieldDescriptor>();
	public Map<Integer, Feature> FeatureMap = new HashMap<Integer, Feature>();
	
	
	
	public ShapeFile(String shpfile) throws IOException {
			
		FileInputStream fis = new FileInputStream(shpfile);
		FileChannel fc = fis.getChannel();
		int fsize = (int) fc.size();
		MappedByteBuffer rf = fc.map(FileChannel.MapMode.READ_ONLY, 0, fsize);
				
		this.FileCode = rf.getInt();
		rf.getInt(); rf.getInt(); rf.getInt(); rf.getInt(); rf.getInt();
		this.FileLength = rf.getInt() * 2;
		
		byte [] data = new byte[4];
		
		rf.order(ByteOrder.LITTLE_ENDIAN);
		this.Version = rf.getInt();
		this.ShapeType = ShapeTypeEnum.get(rf.getInt());
		this.xmin = rf.getDouble();
		this.ymin = rf.getDouble();
		this.xmax = rf.getDouble();
		this.ymax = rf.getDouble();
		this.zmin = rf.getDouble();
		this.zmax = rf.getDouble();
		this.mmin = rf.getDouble();
		this.mmax = rf.getDouble();
		rf.order(ByteOrder.BIG_ENDIAN);
		
		StringBuilder b = new StringBuilder(shpfile);
		b.replace(shpfile.length() - 3, shpfile.length() , "dbf");
		String file_base = b.toString();
		
			
		FileInputStream fis2 = new FileInputStream(file_base);
		FileChannel fc2 = fis2.getChannel();
		int fsize2 = (int) fc2.size();
		MappedByteBuffer df = fc2.map(FileChannel.MapMode.READ_ONLY, 0, fsize2);
		
		
		
		this.DbaseVersion = df.get();
		df.get(this.DbaseLastUpdate);
		
		df.order(ByteOrder.LITTLE_ENDIAN);
		this.FeatureCount = df.getInt();
		this.DbaseHeaderBytes = df.getShort();
		this.DbaseRecordBytes = df.getShort();
		df.order(ByteOrder.BIG_ENDIAN);		
		df.getShort(); // reserved 
		df.get(); // reserved
		df.get(DbasePlusLanReserved);
		df.getInt();
			
		while(df.position() <  this.DbaseHeaderBytes - 1) {
			FieldDescriptor fd = new FieldDescriptor();
			df.get(fd.FieldName);
			char dt = (char) df.get();
			fd.FieldType = DataType.valueOfDataType(dt);	
			df.get(fd.FieldAddress);
			fd.FieldLength = df.get();
			fd.FieldDecimalCount = df.get();
			df.getShort(); // reserved
			df.get(fd.DbasePlusLanReserved2);
			fd.WorkAreaID = df.get();
			df.get(fd.DbasePlusLanReserved3);  
			fd.SetFields = df.get();
			data = new byte[6];
			df.get(data); // reserved
			
			this.FDArray.add(fd);
		// loop until you hit the 0Dh field terminator 
		}
		
		
		df.get(); // should be 0d for field terminator 
		
					
		
		for (Integer i = 0; i < this.FeatureCount; i++) {
			// insert points into some type of list
			int RecordNumber = rf.getInt();
			int ContentLength = rf.getInt();
			
			data = new byte[4];	
			rf.order(ByteOrder.LITTLE_ENDIAN);
			int ShapeType = rf.getInt();
			Feature f = new Feature();
			f.setRecord(new HashMap<String, String>());
			
			if (ShapeType == ShapeTypeEnum.Point.getValue()) {
				double x = rf.getDouble();
				double y = rf.getDouble();
				Point pnt = new Point(x,y);
				f.setGeom(pnt);
							
			} else if (ShapeType == ShapeTypeEnum.Polygon.getValue()) {
				double xmin = rf.getDouble();
				double ymin = rf.getDouble();
				double xmax = rf.getDouble();
				double ymax = rf.getDouble();
				int NumParts = rf.getInt();
				int NumPoints = rf.getInt();
				
				if (NumParts > 1) {
					// not implemented yet
					// TODO: throw an exception
					System.out.println("Not implemented yet.");
					System.exit(-1);
				}
				
				// read the one part 
				int Part = rf.getInt();
				Polygon poly = new Polygon();
				
				// create a line from the points
				double xpnt = rf.getDouble();
				double ypnt = rf.getDouble();
				//Point oldpnt = new Point(xpnt, ypnt);
				poly.startPath(xpnt, ypnt);
				for (int j=0; j < NumPoints-1; j++) {
					xpnt = rf.getDouble();
					ypnt = rf.getDouble();
					poly.lineTo(xpnt, ypnt);	
				}
				f.setGeom(poly);
				
			} else if (ShapeType == ShapeTypeEnum.PolyLine.getValue()) {
				double xmin = rf.getDouble();
				double ymin = rf.getDouble();
				double xmax = rf.getDouble();
				double ymax = rf.getDouble();
				
				int NumParts = rf.getInt();
				int NumPoints = rf.getInt();
				
				int [] NumPartArr = new int[NumParts+1];
				
				for (int n=0; n < NumParts; n++) {
					int idx = rf.getInt();
					NumPartArr[n] = idx;
				}
				NumPartArr[NumParts] = NumPoints;
						
				data = new byte[8];
			
				
				double xpnt, ypnt;
				Polyline ply = new Polyline();
				
			
				for (int m=0; m < NumParts; m++) {
					xpnt = rf.getDouble();
					ypnt = rf.getDouble();
					ply.startPath(xpnt, ypnt);
					
					for (int j=NumPartArr[m]; j < NumPartArr[m+1] - 1; j++) {
						xpnt = rf.getDouble();
						ypnt = rf.getDouble();
						ply.lineTo(xpnt, ypnt);
					}
				}
								
				f.setGeom(ply);
				
			} else {
				// TODO: throw an exception
				System.out.println("Unsupported shapefile type");
				System.out.println(this.ShapeType);
				System.exit(-1);
			}
			
			rf.order(ByteOrder.BIG_ENDIAN);
			// read in each Record and Populate the Feature
		
			// TODO: ignore deleted records
			df.get(); // denotes whether deleted or current
			// read first part of record 	

			for (FieldDescriptor fd: this.FDArray) {
				data = new byte[fd.getLength()];
				df.get(data);
				String value = new String(data);
				f.getRecord().put(fd.getName(), value);				
			}
			
			
			this.FeatureMap.put(RecordNumber, f);
			
		}
	
		fc.close();
		fc2.close();
		fis.close();
		fis2.close();
		
	}
	

	public String toString() { 
		
		StringBuilder s = new StringBuilder();
		String lineSeparator = System.getProperty("line.separator", "\n");
		
		s.append("FileCode: ").append(FileCode).append(lineSeparator);
		s.append("FileLength: ").append(FileLength).append(lineSeparator);
		s.append("Version: ").append(Version).append(lineSeparator);
		s.append("ShapeType: ").append(ShapeType).append(lineSeparator);
		s.append("xmin: ").append(xmin).append(lineSeparator);
		s.append("ymin: ").append(ymin).append(lineSeparator);
		s.append("xmax: ").append(xmax).append(lineSeparator);
		s.append("ymax: ").append(ymax).append(lineSeparator);
		s.append("zmin: ").append(zmin).append(lineSeparator);
		s.append("zmax: ").append(zmax).append(lineSeparator);
		s.append("mmin: ").append(mmin).append(lineSeparator);
		s.append("mmax: ").append(mmax).append(lineSeparator);	
		s.append("------------------------").append(lineSeparator);
		s.append("DbaseVersion: ").append(DbaseVersion).append(lineSeparator);
		s.append("DbaseLastUpdate: ").append(new String(DbaseLastUpdate)).append(lineSeparator);
		s.append("FeatureCount: ").append(FeatureCount).append(lineSeparator);
		s.append("DbaseHeaderBytes: ").append(DbaseHeaderBytes).append(lineSeparator);
		s.append("DbaseRecordBytes: ").append(DbaseRecordBytes).append(lineSeparator);
		s.append("DbasePlusLanReserved: ").append(DbasePlusLanReserved).append(lineSeparator);
		
		return s.toString();
	}
	
	
	
	
}
