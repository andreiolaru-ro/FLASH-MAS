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
package net.xqhs.flash.sclaim.constructs;

import java.util.StringTokenizer;
import java.util.Vector;

import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.UnitComponentExt;

/**
 * Structure returned by the parser for structs
 * 
 * @author tudor
 * 
 */
public class ClaimStructure extends ClaimConstruct
{
	private static final long	serialVersionUID	= 8509247150199469666L;
	/*/**
	 * The behavior to which this construct belongs.
	 */
	/*private ClaimBehaviorDefinition	myBehavior;*/
	
	/**
	 * <em>fields</em> will contain a vector of ClaimConstruct objects, with the following possible types: 1. VALUE 2. VARIABLE 3. STRUCTURE
	 * 
	 */
	private Vector<ClaimConstruct>	fields;
	
	@Override
	public String toString()
	{
		if(fields == null)
			return new String("( struct )");
		String structure = new String("( struct ");
		for(ClaimConstruct currentConstruct : fields)
		{
			switch(currentConstruct.getType())
			{
			case VALUE:
				structure += ((ClaimValue)currentConstruct).toString() + " ";
				break;
			case VARIABLE:
				structure += ((ClaimVariable)currentConstruct).toString() + " ";
				break;
			case STRUCTURE:
				structure += ((ClaimStructure)currentConstruct).toString() + " ";
				break;
			default:
				break;
			}
		}
		
		structure += ")";
		
		return structure;
	}
	
	/**
	 * static method that parses a string and returns a ClaimStructure with the value specified by the string.
	 * 
	 * @param str
	 *            - string to be parsed
	 */
	public static ClaimStructure parseString(String str)
	{
		String unitName = "structureParser";

		// TODO
		//java.util.logging.Logger log = java.util.logging.Logger.getLogger(unitName);
		
		Logger log = (UnitComponentExt) new UnitComponentExt().setUnitName(unitName);
		ClaimStructure structure = parseString(str, log);
		
		//Log.exitLogger(unitName);
		//Logging.exitLogger(unitName);
		return structure;
	}
	
	/**
	 * static method that parses a string and returns a ClaimStructure with the value specified by the string. It also uses a log it receives as argument
	 * 
	 * @param str
	 *            - string to be parsed
	 * @param log
	 *            - log to be used by the method, in order to show messages
	 */
	public static ClaimStructure parseString(String str, Logger log)
	{
		Vector<ClaimConstruct> fields = new Vector<ClaimConstruct>();
		
		StringTokenizer tokenizer = new StringTokenizer(str);
		
		// read the first '('
		if(!tokenizer.nextToken().equals("("))
		{
			log.error("Error while parsing a string to a structure.");
			return null;
		}
		// read the struct keyword
		if(!tokenizer.nextToken().equals("struct"))
		{
			log.error("Error while parsing a string to a structure.");
			return null;
		}
		
		while(tokenizer.hasMoreTokens())
		{
			String token = tokenizer.nextToken();
			
			if(token.startsWith("?"))
				fields.add(ClaimVariable.parseString(token));
			else if(token.equals("("))
			{
				if(tokenizer.nextToken().equals("struct"))
				{
					String innerStructure = new String("( struct ");
					do
					{
						token = tokenizer.nextToken();
						
						innerStructure += token + " ";
					} while(!token.equals(")"));
					
					fields.add(ClaimStructure.parseString(innerStructure, log));
				}
				else
				{
					log.error("Error while parsing a string to a structure.");
					return null;
				}
			}
			else if(token.equals(")"))
			{
				return new ClaimStructure(fields);
			}
			else
				fields.add(ClaimValue.parseString(token));
		}
		
		log.error("Error while parsing a string to a structure.");
		return null;
	}
	
	public ClaimStructure()
	{
		super(ClaimConstructType.STRUCTURE);
		setFields(new Vector<ClaimConstruct>());
	}
	
	public ClaimStructure(Vector<ClaimConstruct> _fields)
	{
		super(ClaimConstructType.STRUCTURE);
		setFields(_fields);
	}
	
	/*public void setMyBehavior(ClaimBehaviorDefinition _myBehavior)
	{
		this.myBehavior = _myBehavior;
	}
	
	public ClaimBehaviorDefinition getMyBehavior()
	{
		return myBehavior;
	}*/
	
	public void setFields(Vector<ClaimConstruct> _fields)
	{
		this.fields = _fields;
	}
	
	public Vector<ClaimConstruct> getFields()
	{
		return fields;
	}
}
