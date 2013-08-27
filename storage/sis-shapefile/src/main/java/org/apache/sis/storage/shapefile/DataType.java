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

/**
 * Provides a simple DataType class
 *
 * 
 * 
 * @see <a href="http://www.clicketyclick.dk/databases/xbase/format/data_types.html">Xbase Data Types</a>
 */



public enum DataType {
	
	Character('C'), // < 254 characters 
	Number('N'), // < 18 characters, can include sign and decimal  
	Logical('L'), // 3 way, ? Y,y,T,t  N,n,F,f
	Date('D'), // YYYYMMDD
	Memo('M'), // Pointer to ASCII text field
	FloatingPoint('F'), // 20 digits
	// CharacterNameVariable("?"),  //1-254 Characters
    Picture('P'), // Memo
    Currency('Y'), // Foxpro
    DateTime('T'), // 32 bit little-endian Julian date, 32 byte little endian milliseconds since midnight
    Integer('I'), // 4 byte little endian
    VariField('V'), // ???
    Variant('X'), // ???
    TimeStamp('@'), // see url
    Double('O'), //
    AutoIncrement('+'); // ???
    
	public final char datatype;
	
		
		
	DataType(char datatype) {
		this.datatype = datatype;
	}
	
	public static DataType valueOfDataType(char datatype) {
		for (DataType v : values()) {
			if (v.datatype == datatype) {
				return v;
			}
		}
        throw new IllegalArgumentException(
                "Enum datatype is incorrect");
	}

	
    
    
}
