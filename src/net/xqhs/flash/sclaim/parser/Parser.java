/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.sclaim.parser;

import java.io.FileNotFoundException;

import net.xqhs.flash.sclaim.constructs.ClaimAgentDefinition;
import net.xqhs.flash.sclaim.parser.generation.ParserSClaim;

/**
 * The Claim2 parser. It is intended to be an interface between the class
 * ParserClaim2 and the user. 
 * 
 * @author tudor
 *
 */
public class Parser {
	/**
	 * the BYACC/J parser used to analyze the specified <em>*.adf2</em> file
	 */
	ParserSClaim yyparser;
	
	/**
	 * Constructor taking as parameter a String representing the file name and the path to it.
	 * 
	 * @param fileNameAndPath - the path and the file name (including the extension) to the 
	 * <em>*.adf2</em> file to be parsed
	 */
	public Parser(String fileNameAndPath)
	{
		yyparser = new ParserSClaim(fileNameAndPath);
	}
	
	/** a way to use the parser - inside the code, static
	 * @throws FileNotFoundException */
	public static ClaimAgentDefinition parseFile(String filePathAndName) {
	  Parser parser = new Parser(filePathAndName);

	  return parser.parse();
	}

	/** a way to use the parser - main function 
	 * @throws FileNotFoundException */
	public static void main(String args[]) {
		
		args = new String[] { "src-experiments/sclaim/BaseScenario.adf2" };
		
	  Parser parser;
	  System.out.println(args[0]);
	  
	  if(args.length>0)
	  {
		parser = new Parser(args[0]);
	    System.out.println(parser.parse());
	  }
	  else
	  {
		System.out.println("No argument was specified. The file name to be parsed together with its path are needed.");
	  }
	}
	
	/**
	 * Method used to parse the input file (which was specified using the constructor)
	 * 
	 * @return
	 */
	public ClaimAgentDefinition parse()
	{
		return yyparser.parse();
	}
}
