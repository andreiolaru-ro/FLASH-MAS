/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.deployment;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.util.ContentHolder;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.XML.XMLParser;
import net.xqhs.util.XML.XMLTree;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.XML.XMLTree.XMLNode.XMLAttribute;
import net.xqhs.util.logging.UnitComponentExt;

/**
 * This class manages settings for simulations. It handles loading these settings from various sources --
 * {@link BootDefaultSettings}, arguments given to the <code>main()</code> method in {@link Boot}, or settings specified
 * in the deployment file.
 * <p>
 * The precedence of values for settings is the following (latter values override former values):
 * <ul>
 * <li>values given in {@link BootDefaultSettings};
 * <li>values given in the deployment file.
 * <li>values given as arguments to method <code>main()</code> in {@link Boot} (command-line arguments);
 * </ul>
 * Each setting may come from values only in some of the above state sources. The specific cases are mentioned in the
 * documentation of each setting.
 * 
 * @author Andrei Olaru
 */
public class BootSettingsManager extends TreeParameterSet
{
	public enum SettingName {
		SCHEMA,
		
		DEPLOYMENT,
		
		PACKAGE,
		
		SUPPORT,
		
		AGENT,
		
		COMPONENT(AGENT),
		
		;
		
		SettingName parent = null;
		
		private SettingName()
		{
		}
		
		private SettingName(SettingName _parent)
		{
			parent = _parent;
		}
		
		public String getName()
		{
			return this.name().toLowerCase();
		}
		
		public String getPath()
		{
			if(parent == null)
				return getName();
			return parent.getPath() + PATH_SEP + getName();
		}
	}
	
	/**
	 * The class UID.
	 */
	private static final long				serialVersionUID	= 5157567185843194635L;
	
	public static final String				PATH_SEP			= "/";
	
	public static final String				CLI_TYPE_PREFIX		= "-";
	
	/**
	 * The name of nodes containing parameters.
	 */
	public static final String				PARAMETER_NODE_NAME	= "parameter";
	/**
	 * The name of the attribute of a parameter node holding the name of the parameter.
	 */
	public static final String				PARAMETER_NAME		= "name";
	/**
	 * The name of the attribute of a parameter node holding the value of the parameter.
	 */
	public static final String				PARAMETER_VALUE		= "value";
	
	/**
	 * The default directory for scenarios.
	 */
	public static final String				SCENARIO_DIRECTORY	= "src-scenario/";
	
	/**
	 * Default values.
	 */
	public static final Map<String, String>	DEFAULTS			= new HashMap<>();

	public static final String OTHER_NAME = "other";
	
	static
	{
		DEFAULTS.put(SettingName.SCHEMA.getName(), "src-schema/deployment-schema.xsd");
		DEFAULTS.put(SettingName.DEPLOYMENT.getName(), SCENARIO_DIRECTORY + "ChatAgents/deployment-chatAgents.xml");
		// + "scenario/examples/sclaim_tatami2/simpleScenarioE/scenarioE-tATAmI2-plus.xml";
	}
	
	/**
	 * The method loads all available values from the specified sources.
	 * <p>
	 * The only given source is the arguments the program has received, as the name of the scneario file will be decided
	 * by this method. If it is instructed through the parameter, the deployment file is parsed, producing an additional
	 * source of setting values.
	 * <p>
	 * The <code>load()</code> method can be called only once. It is why all sources must be given in a single call to
	 * <code>load()</code>.
	 * <p>
	 * Therefore, if it is desired to pick <i>any</i> settings from the deployment file, the <code>boolean</code>
	 * argument should be set to <code>true</code>.
	 * 
	 * @param programArguments
	 *            - the arguments passed to the application, exactly as they were passed.
	 * @param parsedeploymentFile
	 *            - if <code>true</code>, the deployment file will be parsed to obtain the setting values placed in the
	 *            deployment; also, the {@link XMLTree} instance resulting from the parsing will be returned.
	 * @param loadedXML
	 *            - if the deployment file is parsed, the resulting {@link XMLTree} instance will be stored in this
	 *            ContentHolder instance.
	 * @return the instance itself, which is also the {@link TreeParameterSet} that contains all settings.
	 * 
	 * @throws ConfigLockedException
	 *             - if load is called more than once.
	 */
	public TreeParameterSet load(String programArguments[], boolean parsedeploymentFile,
			ContentHolder<XMLTree> loadedXML) throws ConfigLockedException
	{
		locked();
		
		UnitComponentExt log = (UnitComponentExt) new UnitComponentExt().setUnitName("settings load");
		
		// 1. get default settings
		for(String setting : DEFAULTS.keySet())
			this.add(setting, DEFAULTS.get(setting));
		log.trace("initial tree:", this);
		
		// 2. parse deployment file
		if(programArguments.length > 0 && !programArguments[0].startsWith(CLI_TYPE_PREFIX))
			set(SettingName.DEPLOYMENT.getName(), programArguments[0]);
		else
			for(int i = 0; i < programArguments.length; i++)
				if(isCategory(programArguments[i])
						&& (getCategory(programArguments[i]).equals(SettingName.DEPLOYMENT.getName())
								|| getCategory(programArguments[i]).equals(SettingName.SCHEMA.getName())))
				{
					if(i + 1 >= programArguments.length || isCategory(programArguments[i + 1]))
						throw new IllegalArgumentException(
								"Program argument after " + programArguments[i] + " should be a correct value.");
					set(getCategory(programArguments[i]), programArguments[i + 1]);
				}
			
		XMLTree XMLtree = XMLParser.validateParse(get(SettingName.SCHEMA.getName()),
				get(SettingName.DEPLOYMENT.getName()));
		readXML(XMLtree.getRoot(), this, log);
		log.trace("after XML tree parse:", this);
		
		// 3. parse CLI args
		
		log.doExit();
		lock();
		return this;
	}
	
	protected static void readXML(XMLNode node, TreeParameterSet tree, UnitComponentExt log)
	{
		for(XMLAttribute a : node.getAttributes())
			tree.add(a.getName(), a.getValue());
		for(XMLNode n : node.getNodes())
		{
			if(n.getName().equals(PARAMETER_NODE_NAME))
				tree.add(n.getAttributeValue(PARAMETER_NAME), n.getAttributeValue(PARAMETER_VALUE));
			else if(n.getNodes().isEmpty() && n.getAttributes().isEmpty())
				// here missing the case of a node with no children but with attributes
				tree.add(n.getName(), (String) n.getValue());
			else
			{
				TreeParameterSet subTree = new TreeParameterSet();
				readXML(n, subTree, log);
				String name = subTree.getValue(PARAMETER_NAME);
				if(name != null)
					tree.addTree(n.getName(), name, subTree);
				else
				{
					log.lw("Node [] does not contain a name.", n.getName());
					tree.addTree(OTHER_NAME, n.getName(), subTree);
				}
			}
				
		}
	}
	
	protected static boolean isCategory(String arg)
	{
		return arg.startsWith(CLI_TYPE_PREFIX);
	}
	
	protected static String getCategory(String arg)
	{
		return isCategory(arg) ? arg.substring(1) : null;
	}
	
}
