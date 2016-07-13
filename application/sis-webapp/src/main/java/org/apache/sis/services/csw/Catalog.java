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
 
import java.util.Map; 
import java.util.LinkedHashMap; 
import java.io.BufferedReader; 
import java.io.IOException; 
import java.nio.file.Path; 
import java.nio.file.Files; 
import java.nio.file.DirectoryStream; 
import javax.xml.bind.JAXBException; 
import org.opengis.metadata.Metadata; 
import org.apache.sis.storage.DataStoreException; 
import org.apache.sis.storage.geotiff.LandsatReader; 
import org.apache.sis.xml.XML; 
 
 
/** 
 * Collection of ISO 19115 metadata. 
 * Current implementation parses the metadata from all supported files found in the given directory. 
 * 
 * @author  Thi Phuong Hao Nguyen (VNSC) 
 * @since   0.8 
 * @version 0.8 
 * @module 
 */ 
public class Catalog { 
    /** 
     * All metadata known to this {@code Catalog} class. 
     * Keys are the metadata identifiers. 
     */ 
    private final Map<Long,Metadata> metadata = new LinkedHashMap<>(); 
 
    /** 
     * Creates a new catalog initialized with the metadata of all files found in the given directory. 
     * The current implementation does not scan the sub-directories. 
     * 
     * @param directory the directory to scan. 
     * @throws DataStoreException if an error occurred while reading a metadata file. 
     */ 
    public Catalog(final Path directory) throws DataStoreException, JAXBException { 
        try (DirectoryStream<Path> content = Files.newDirectoryStream(directory)) { 
            for (final Path file : content) { 
                final String filename = file.getFileName().toString(); 
                final Metadata md; 
                if (filename.endsWith(".txt")) { 
                    try (BufferedReader in = Files.newBufferedReader(file)) { 
                        final LandsatReader reader = new LandsatReader(in); 
                        md = reader.read(); 
                    } 
                } else if (filename.endsWith(".ISO19115")) try { 
                    md = (Metadata) XML.unmarshal(file); 
                } catch (JAXBException e) { 
                    throw new DataStoreException("Can not read " + file, e); 
                } else { 
                    continue;   // Ignore (for now) unrecognized format. 
                } 
                metadata.put(metadata.size() + 1L, md); 
            } 
        } catch (IOException e) { 
            throw new DataStoreException(e); 
        } 
    } 
 
    /** 
     * Returns the metadata for the given identifier. 
     * 
     * @param  id the identifier of the metadata to lookup. 
     * @return the metadata for the given identifier. 
     * @throws IllegalArgumentException if there is no record for the given identifier. 
     */ 
    public Metadata getRecordById(long id) { 
        Metadata message = metadata.get(id); 
        if (message == null) { 
            throw new IllegalArgumentException("Record with id " + id +" not found"); 
        } 
        return message; 
    } 
} 
