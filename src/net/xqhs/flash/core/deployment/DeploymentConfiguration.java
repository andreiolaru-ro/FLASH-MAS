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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.xqhs.flash.core.util.ContentHolder;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.XML.XMLParser;
import net.xqhs.util.XML.XMLTree;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.XML.XMLTree.XMLNode.XMLAttribute;
import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.UnitComponentExt;

/**
 * This class manages deployment configurations. It handles loading the elements of the configuration from various
 * sources -- default values, arguments given to the program, or settings specified in the deployment file.
 * <p>
 * The precedence of values for settings is the following (latter values override former values):
 * <ul>
 * <li>values given in DEFAULTS member;
 * <li>values given in the deployment file.
 * <li>values given as command-line arguments;
 * </ul>
 * 
 * The configuration is created as a tree of categories and elements. All elements belong in a category; all categories
 * belong in elements or at the root level. Elements may contain key-value pairs.
 * 
 * @author Andrei Olaru
 */
public class DeploymentConfiguration extends TreeParameterSet
{
	/**
	 * Types of categories in the configuration. Categories are defined by their name, and my have an optional or
	 * mandatory hierarchy requirement (a parent category).
	 * 
	 * @author andreiolaru
	 */
	public enum CategoryName {
		/**
		 * The XML schema file against which to validate to deployment file. Values beyond the first value are ignored.
		 */
		SCHEMA,
		
		/**
		 * The XML deployment file. Values beyond the first value are ignored.
		 */
		DEPLOYMENT,
		
		/**
		 * Java packages that contain classes needed in the deployment.
		 */
		PACKAGE,
		
		/**
		 * Classes that are able to load various categories of elements in the configuration.
		 */
		LOADER,
		
		/**
		 * Support infrastructures used in the deployment.
		 */
		SUPPORT,
		
		/**
		 * Agents to create in the deployment, potentially inside particular support infrastructures.
		 */
		AGENT(SUPPORT, true),
		
		/**
		 * Features to be deployed in agents.
		 */
		FEATURE(AGENT),
		
		;
		
		/**
		 * The parent of the category.
		 */
		CategoryName	parent				= null;
		/**
		 * <code>false</code> if the category must necessarily appear inside its parent category; <code>true</code> if
		 * the category may also appear at top level.
		 */
		boolean			optional_hierarchy	= false;
		
		/**
		 * Constructor for a top-level category.
		 */
		private CategoryName()
		{
		}
		
		/**
		 * Constructor for a category with a parent (hierarchy is mandatory).
		 * 
		 * @param _parent
		 *            - the parent category.
		 */
		private CategoryName(CategoryName _parent)
		{
			parent = _parent;
		}
		
		/**
		 * Constructor for a category that has a potentially optional parent.
		 * 
		 * @param _parent
		 *            - the parent.
		 * @param parent_optional
		 *            - <code>true</code> if hierarchy is optional.
		 */
		private CategoryName(CategoryName _parent, boolean parent_optional)
		{
			this(_parent);
			optional_hierarchy = parent_optional;
		}
		
		/**
		 * @return the name of the category, in lower case.
		 */
		public String getName()
		{
			return this.name().toLowerCase();
		}
		
		/**
		 * @return the name of the parent category, if any was defined; <code>null</code> otherwise.
		 */
		public String getParent()
		{
			return parent != null ? parent.getName() : null;
		}
		
		/**
		 * @return <code>true</code> if hierarchy is optional.
		 */
		public boolean isParentOptional()
		{
			return optional_hierarchy;
		}
		
		/**
		 * @return the hierarchical path of the category, with ancestors separated by
		 *         {@value DeploymentConfiguration#PATH_SEP}.
		 */
		public String getPath()
		{
			if(parent == null)
				return getName();
			return parent.getPath() + PATH_SEP + getName();
		}
		
		/**
		 * Find the {@link CategoryName} identified by the given name.
		 * 
		 * @param name
		 *            - the name.
		 * @return the category.
		 */
		public static CategoryName byName(String name)
		{
			for(CategoryName s : CategoryName.values())
				if(s.getName().equals(name))
					return s;
			return null;
		}
	}
	
	/**
	 * The class UID.
	 */
	private static final long				serialVersionUID			= 5157567185843194635L;
	
	/**
	 * Separator of category hierarchy path elements.
	 */
	public static final String				PATH_SEP					= "/";
	
	/**
	 * Prefix of category names used in CLI.
	 */
	public static final String				CLI_CATEGORY_PREFIX			= "-";
	
	/**
	 * The name of nodes containing parameters.
	 */
	public static final String				PARAMETER_NODE_NAME			= "parameter";
	/**
	 * The name of the attribute of a parameter node holding the name of the parameter.
	 */
	public static final String				PARAMETER_NAME				= "name";
	/**
	 * The name of the attribute of a parameter node holding the value of the parameter.
	 */
	public static final String				PARAMETER_VALUE				= "value";
	
	/**
	 * The default directory for deployment files.
	 */
	public static final String				DEPLOYMENT_FILE_DIRECTORY	= "src-deployment/";
	
	/**
	 * Default values.
	 */
	public static final Map<String, String>	DEFAULTS					= new HashMap<>();
	
	/**
	 * In XML parsing, name under which to put unnamed entities.
	 */
	public static final String				OTHER_NAME					= "other";
	
	static
	{
		DEFAULTS.put(CategoryName.SCHEMA.getName(), "src-schema/deployment-schema.xsd");
		DEFAULTS.put(CategoryName.DEPLOYMENT.getName(),
				DEPLOYMENT_FILE_DIRECTORY + "ChatAgents/deployment-chatAgents.xml");
		// + "scenario/examples/sclaim_tatami2/simpleScenarioE/scenarioE-tATAmI2-plus.xml";
	}
	
	/**
	 * The method loads all available values from the specified sources.
	 * <p>
	 * The only given source is the arguments the program has received, as the name of the deployment file will be
	 * decided by this method. If it is instructed through the parameter, the deployment file is parsed, producing an
	 * additional source of configuration values.
	 * <p>
	 * The <code>load()</code> method can be called only once. It is why all sources must be given in a single call to
	 * <code>load()</code>.
	 * <p>
	 * Therefore, if it is desired to pick <i>any</i> settings from the deployment file, the <code>boolean</code>
	 * argument should be set to <code>true</code>.
	 * 
	 * @param programArguments
	 *            - the arguments passed to the application, exactly as they were passed.
	 * @param parseDeploymentFile
	 *            - if <code>true</code>, the deployment file will be parsed to obtain the setting values placed in the
	 *            deployment; also, the {@link XMLTree} instance resulting from the parsing will be placed as content in
	 *            the last parameter.
	 * @param loadedXML
	 *            - if the deployment file is parsed, the resulting {@link XMLTree} instance will be stored in this
	 *            ContentHolder instance.
	 * @return the instance itself, which is also the {@link TreeParameterSet} that contains all settings.
	 * 
	 * @throws ConfigLockedException
	 *             - if load() is called more than once.
	 */
	public TreeParameterSet load(String programArguments[], boolean parseDeploymentFile,
			ContentHolder<XMLTree> loadedXML) throws ConfigLockedException
	{
		locked();
		
		UnitComponentExt log = (UnitComponentExt) new UnitComponentExt().setUnitName("settings load");
		
		// 1. get default settings
		for(String setting : DEFAULTS.keySet())
			this.add(setting, DEFAULTS.get(setting));
		log.lf("initial tree:", this);
		
		// 2. parse deployment file
		boolean scenarioFirst = false;
		if(programArguments.length > 0 && !programArguments[0].startsWith(CLI_CATEGORY_PREFIX)
				&& !programArguments[0].contains(":"))
		{
			set(CategoryName.DEPLOYMENT.getName(), programArguments[0]);
			scenarioFirst = true;
		}
		else
			for(int i = 0; i < programArguments.length; i++)
				if(isCategory(programArguments[i])
						&& (getCategory(programArguments[i]).equals(CategoryName.DEPLOYMENT.getName())
								|| getCategory(programArguments[i]).equals(CategoryName.SCHEMA.getName())))
				{
					if(i + 1 >= programArguments.length || isCategory(programArguments[i + 1]))
						throw new IllegalArgumentException(
								"Program argument after " + programArguments[i] + " should be a correct value.");
					set(getCategory(programArguments[i]), programArguments[i + 1]);
				}
			
		XMLTree XMLtree = XMLParser.validateParse(get(CategoryName.SCHEMA.getName()),
				get(CategoryName.DEPLOYMENT.getName()));
		loadedXML.set(XMLtree);
		readXML(XMLtree.getRoot(), this, log);
		log.lf("after XML tree parse:", this);
		log.lf(">>>>>>>>");
		
		// 3. parse CLI args
		List<String> arg_list = new LinkedList<>(Arrays.asList(programArguments));
		if(scenarioFirst)
			arg_list.remove(0);
		readCLIArgs(arg_list.iterator(), this, log);
		log.lf("after CLI tree parse:", this);
		
		// 4. add names and contexts; fuse element with optional hierarchy.
		// TODO
		
		log.doExit();
		lock();
		return this;
	}
	
	/**
	 * Reads data from the XML tree read from the deployment file into the given configuration tree.
	 * 
	 * @param node
	 *            - the XML node to read.
	 * @param tree
	 *            - the configuration tree.
	 * @param log
	 *            - the {@link Logger} to use.
	 */
	protected static void readXML(XMLNode node, TreeParameterSet tree, UnitComponentExt log)
	{
		// String l = "Node " + node.getName() + " with attributes ";
		// for(XMLAttribute a : node.getAttributes())
		// l += a.getName() + ",";
		// l += " and children ";
		// for(XMLNode n : node.getNodes())
		// l += n.getName() + ",";
		// log.lf(l);
		
		for(XMLAttribute a : node.getAttributes())
			tree.add(a.getName(), a.getValue());
		Set<String> named = new LinkedHashSet<>();
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
				if(subTree.getValue(PARAMETER_NAME) != null)
					named.add(n.getName());
				// log.lw("Node [] does not contain a name.", n.getName());
				tree.addTree(n.getName(), subTree);
			}
		}
		for(String name : named)
		{
			List<TreeParameterSet> trees = tree.getTrees(name);
			TreeParameterSet newtree = new TreeParameterSet();
			tree.clear(name);
			tree.addTree(name, newtree);
			for(TreeParameterSet t : trees)
			{
				String elName = t.getValue(PARAMETER_NAME);
				newtree.addTree(elName != null ? elName : OTHER_NAME, t);
			}
		}
	}
	
	/**
	 * Reads data from program arguments into the given configuration tree.
	 * 
	 * @param args
	 *            - an {@link Iterator} through the arguments.
	 * @param tree
	 *            - the configuration tree.
	 * @param log
	 *            - the {@link Logger} to use.
	 */
	protected static void readCLIArgs(Iterator<String> args, TreeParameterSet tree, UnitComponentExt log)
	{
		class CTriple
		{
			String				cat;
			TreeParameterSet	tc;
			TreeParameterSet	te;
			
			public CTriple(String category, TreeParameterSet catTree, TreeParameterSet elTree)
			{
				cat = category;
				tc = catTree;
				te = elTree;
			}
		}
		Stack<CTriple> context = new Stack<>(); // categories & elements context
		CTriple treeRoot = new CTriple(null, tree, null);
		context.push(treeRoot);
		
		while(args.hasNext())
		{
			String a = args.next();
			if(isCategory(a))
			{
				String catName = getCategory(a);
				CategoryName category = CategoryName.byName(getCategory(a));
				if(!args.hasNext())
				{
					log.lw("Empty unknown category [] in CLI arguments.", catName);
					return;
				}
				
				if(category == null || category.getParent() == null)
				{ // category unknown or root category -> go to toplevel
					context.clear(); // reset context
					context.push(treeRoot);
					// put category
					if(tree.isSimple(catName))
					{
						log.le("Name [] should not be used as a category; it is a simple name.", catName);
						continue;
					}
					TreeParameterSet c = tree.getTree(catName);
					if(c == null)
					{ // category does not already exist
						c = new TreeParameterSet();
						tree.addTree(catName, c);
					}
					context.push(new CTriple(catName, c, null));
				}
				else
				{ // subordinate category of some (other) context
					// move up context
					while(!context.isEmpty())
					{
						if(category.getName().equals(context.peek().cat))
							// will insert new element here
							break;
						else if(category.getParent().equals(context.peek().cat))
						{ // found the correct parent for the category
							TreeParameterSet elementTree = context.peek().te;
							assert (elementTree != null);
							if(elementTree.isSimple(catName))
							{
								log.le("Name [] should not be used as a category; it is a simple name.", catName);
								continue;
							}
							TreeParameterSet c = elementTree.getTree(catName);
							if(c == null)
							{ // category does not already exist
								c = new TreeParameterSet();
								elementTree.addTree(catName, c);
							}
							context.push(new CTriple(catName, c, null));
							break;
						}
						// no match yet
						context.pop();
					}
					if(context.isEmpty())
					{
						String msg = "Category [] has parent [] but no instance of parent could be found;";
						if(!category.isParentOptional())
						{
							log.le(msg + " ignoring other arguments beginning with [].", catName, category.getParent(),
									a);
							return;
						}
						log.lw(msg + " adding to top level.", catName, category.getParent());
						context.push(treeRoot);
					}
				}
				// the category has been added, now its time to add the new element
				String name = args.next();
				TreeParameterSet t = new TreeParameterSet();
				context.peek().tc.addTree(name, t);
				context.peek().te = t;
			}
			else
			{
				String parameter, value = null;
				if(a.contains(":"))
				{ // parameter name & value
					String[] es = a.split(":", 2);
					parameter = es[0];
					value = es[1];
				}
				else
					parameter = a;
				context.peek().te.add(parameter, value);
			}
		}
	}
	
	/**
	 * Checks if the given command line argument designates a category (begins with {@value #CLI_CATEGORY_PREFIX}).
	 * 
	 * @param arg
	 *            - the argument.
	 * @return <code>true</code> if it designates a category.
	 */
	protected static boolean isCategory(String arg)
	{
		return arg.startsWith(CLI_CATEGORY_PREFIX);
	}
	
	/**
	 * Returns the actual category name designated by a command line argument (removes preceding
	 * {@value #CLI_CATEGORY_PREFIX})
	 * 
	 * @param arg
	 *            - the argument.
	 * @return the category name.
	 */
	protected static String getCategory(String arg)
	{
		return isCategory(arg) ? arg.substring(1) : null;
	}
	
}
