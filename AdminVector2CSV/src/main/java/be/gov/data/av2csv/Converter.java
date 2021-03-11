/*
 * Copyright (c) 2020, FPS BOSA DG DT
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.gov.data.av2csv;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Converter interface
 * 
 * @author Bart Hanssens
 */
public interface Converter {
	// subdirectory in zip file containing WGS84
	public static final String SUBDIR_WGS = "/adminvectorwgs";
	public static final String SUBDIR_L08 = "/adminvector08";

	// shape files
	public static final String AD_1 = "AD_1_MunicipalSection";
	public static final String AD_2 = "AD_2_Municipality";
	public static final String AD_2_CENTER = "AD_2_MunicipalityCenter";
	
	
	// properties inside the shape files
	public static final String NL = "NameDut";
	public static final String FR = "NameFre";
	public static final String DE = "NameGer";
	public static final String PSEUDO = "PseudoNIS";
	public static final String NIS = "NISCode";
	public static final String ZIP = "ZipCode";

	
	/**
	 * Convert from directory with shapefiles to another file format
	 * 
	 * @param infile shapefile input directory
	 * @param outfile output directory
	 * @throws IOException
	 */
	public void convert(Path infile, Path outfile) throws IOException;
}
