package org.apache.sis.storage.shapefile;



/**
 * Provides a CodePage class
 *
 * when dealing with codepages, should first check if there is a .cpg file 
 * 
 * @see <a href="http://www.clicketyclick.dk/databases/xbase/format/dbf.html#DBF_NOTE_5_TARGET">Xbase Data file</a>
 */



public class CodePage {

	private String codepage;
	
	
	
	CodePage(byte value) {

	    switch (value) { 
	    	case 0x01: codepage = "Cp437"; break;
	        case 0x02: codepage = "Cp850"; break;
	        case 0x03: codepage = "Cp1252"; break;
	        case 0x04: codepage = "unsupported"; break;
	        case 0x64: codepage = "Cp852"; break;
	        case 0x65: codepage = "Cp865"; break;
	        case 0x66: codepage = "Cp866"; break;
	        case 0x67: codepage = "Cp861"; break;  // iceland
	        case 0x68: codepage = "unsupported"; break; // Kamenicky (Czech) MS-DOS
	        case 0x69: codepage = "unsupported"; break; // Mazovia (Polish) MS-DOS
	        case 0x6A: codepage = "unsupported"; break; // Greek MS-DOS (437G)"
	        case 0x6B: codepage = "Cp857"; break; // turkish ms-dos
	        case (byte) 0x96: codepage = "unsupported"; break; // russian mac 
	        case (byte) 0x97: codepage = "unsupported"; break; // eastern european macintosh
	        case (byte) 0x98: codepage = "unsupported"; break; // greek macintosh
	        case (byte) 0xC8: codepage = "unsupported"; break; // windows ee
	        case (byte) 0xCA: codepage = "unsupported"; break; // Turkish windows
	        case (byte) 0xCB: codepage = "unsupported"; break; // greek windows
	        default: codepage = "invalid"; break;
	    }
		
		
		
		
	}
	
	
	
}
