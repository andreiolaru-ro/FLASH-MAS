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
package net.xqhs.flash.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.util.ContentHolder;
import net.xqhs.flash.core.util.MultiTreeMap;
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
 * <p>
 * The configuration is created as an "entity list" which is a tree (this class itself extends {@link MultiTreeMap}), in
 * which the first level are entity types, and other each type entities are listed by name (if any).
 * 
 * @author Andrei Olaru
 */
public class DeploymentConfiguration extends MultiTreeMap
{
	/**
	 * The class UID.
	 */
	private static final long				serialVersionUID				= 5157567185843194635L;
	
	/**
	 * Prefix of category names used in CLI.
	 */
	public static final String				CLI_CATEGORY_PREFIX				= "-";
	/**
	 * Separator of parts of a name and of parameter and value.
	 */
	public static final String				NAME_SEPARATOR					= ":";
	/**
	 * Separator for elements in the load order setting.
	 */
	public static final String				LOAD_ORDER_SEPARATOR			= " ";
	/**
	 * The name of nodes containing parameters.
	 */
	public static final String				PARAMETER_ELEMENT_NAME			= "parameter";
	/**
	 * The name of the attribute which contains the kind.
	 */
	public static final String				KIND_ATTRIBUTE_NAME				= "kind";
	/**
	 * The name of the attribute of a parameter node holding the name of the parameter.
	 */
	public static final String				PARAMETER_NAME					= "name";
	/**
	 * The name of the attribute of a parameter node holding the value of the parameter.
	 */
	public static final String				PARAMETER_VALUE					= "value";
	/**
	 * The name of the attribute which contains the name.
	 */
	public static final String				NAME_ATTRIBUTE_NAME				= "name";
	/**
	 * The name of the element(s) which contain entity context.
	 */
	public static final String				CONTEXT_ELEMENT_NAME			= "in-context-of";
	/**
	 * Name of XML nodes for entities other than those in {@link CategoryName}.
	 */
	public static final String				GENERAL_ENTITY_NAME				= "entity";
	/**
	 * The name of the XML attribute specifying the type of the entity.
	 */
	public static final String				GENERAL_ENTITY_TYPE_ATTRIBUTE	= "type";
	
	/**
	 * The name of the (singleton) entry in the configuration tree, under which all entities are listed by their name or
	 * generated identifier.
	 * <p>
	 * This constant is also used as a key in entity nodes for their id (if they don't have an identifiable name.
	 */
	public static final String				NAME_LIST_ENTRY					= "#by-name";
	
	/**
	 * Root package for FLASH classes.
	 */
	public static final String				ROOT_PACKAGE					= "net.xqhs.flash";
	/**
	 * Package for core FLASH functionality
	 */
	public static final String				CORE_PACKAGE					= "core";
	/**
	 * The default directory for deployment files.
	 */
	public static final String				DEPLOYMENT_FILE_DIRECTORY		= "src-deployment/";
	
	/**
	 * Default values.
	 */
	public static final Map<String, String>	DEFAULTS						= new HashMap<>();
	
	/**
	 * A node in the context stack. The context stack is used in order to keep track of location in the configuration
	 * tree while parsing CLI arguments.
	 */
	static class CtxtTriple
	{
		/**
		 * The name of the current category.
		 */
		String			category;
		/**
		 * The subtree of the current category, will contain elements in this category.
		 */
		MultiTreeMap	catTree;
		/**
		 * The subtree of the current element, will contain parameters or subordinate categories.
		 */
		MultiTreeMap	elemTree;
		
		/**
		 * Constructor.
		 * 
		 * @param cat
		 *                         - category name.
		 * @param categoryTree
		 *                         - category tree (may be <code>null</code>).
		 * @param elTree
		 *                         - current element tree (may be <code>null</code>).
		 */
		public CtxtTriple(String cat, MultiTreeMap categoryTree, MultiTreeMap elTree)
		{
			category = cat;
			catTree = categoryTree;
			elemTree = elTree;
		}
		
		@Override
		public String toString()
		{
			return "{" + category + "/" + (catTree != null ? catTree.toString(1, true) : "-") + "/"
					+ (elemTree != null ? elemTree.toString(1, true) : "-") + "}";
		}
	}
	
	/**
	 * The default configuration. Only single values can be added here at this time.
	 */
	static
	{
		DEFAULTS.put(CategoryName.SCHEMA.s(), "src-schema/deployment-schema.xsd");
		DEFAULTS.put(CategoryName.DEPLOYMENT_FILE.s(), DEPLOYMENT_FILE_DIRECTORY +
		
		// "ChatAgents/deployment-chatAgents.xml"
				"ComplexDeployment/deployment-complexDeployment.xml"
		// "scenario/examples/sclaim_tatami2/simpleScenarioE/scenarioE-tATAmI2-plus.xml"
		
		);
		DEFAULTS.put(CategoryName.LOAD_ORDER.s(), "support agent");
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
	 *                                - the arguments passed to the application, exactly as they were passed.
	 * @param parseDeploymentFile
	 *                                - if <code>true</code>, the deployment file will be parsed to obtain the setting
	 *                                values placed in the deployment; also, the {@link XMLTree} instance resulting from
	 *                                the parsing will be placed as content in the last parameter.
	 * @param loadedXML
	 *                                - if the deployment file is parsed and this argument is not <code>null</code>, the
	 *                                resulting {@link XMLTree} instance will be stored in this ContentHolder instance.
	 * @return the instance itself, which is also the {@link MultiTreeMap} that contains all settings.
	 * 
	 * @throws ConfigLockedException
	 *                                   - if load() is called more than once.
	 */
	public MultiTreeMap loadConfiguration(List<String> programArguments, boolean parseDeploymentFile,
			ContentHolder<XMLTree> loadedXML) throws ConfigLockedException
	{
		locked();
		UnitComponentExt log = (UnitComponentExt) new UnitComponentExt().setUnitName("settings load");
		
		MultiTreeMap deploymentCat = this.addSingleTreeGet(CategoryName.DEPLOYMENT.s(), new MultiTreeMap());
		MultiTreeMap deployment = deploymentCat.addSingleTreeGet(null, new MultiTreeMap());
		
		// ====================================== get default settings
		for(String setting : DEFAULTS.keySet())
			deployment.addSingleValue(setting, DEFAULTS.get(setting));
		log.lf("initial tree:", this);
		
		log.lf("program arguments:", programArguments);
		
		// ====================================== get deployment file and schema
		boolean scenarioFirst = false;
		if(programArguments.size() > 0 && programArguments.get(0).length() > 0
				&& !programArguments.get(0).startsWith(CLI_CATEGORY_PREFIX) && !programArguments.get(0).contains(":"))
		{
			deployment.setValue(CategoryName.DEPLOYMENT_FILE.s(), programArguments.get(0));
			scenarioFirst = true;
		}
		else
			for(Iterator<String> it = programArguments.iterator(); it.hasNext();)
			{
				String arg = it.next();
				if(isCategory(arg) && (getCategory(arg).equals(CategoryName.DEPLOYMENT_FILE.s())
						|| getCategory(arg).equals(CategoryName.SCHEMA.s())))
				{
					String val = null;
					if(it.hasNext() || isCategory(val = it.next()))
						throw new IllegalArgumentException(
								"Program argument after " + arg + " should be a correct value.");
					deployment.setValue(getCategory(arg), val);
				}
			}
		log.lf("loading scenario [] with schema [].", deployment.getSingleValue(CategoryName.DEPLOYMENT_FILE.s()),
				deployment.getSingleValue(CategoryName.SCHEMA.s()));
		
		// ====================================== context management
		Deque<CtxtTriple> context = null; // categories & elements context
		// do not create a base context here, the deployment will be generated only in XMLtree
		
		// ====================================== load deployment file
		XMLTree XMLtree = XMLParser.validateParse(deployment.getSingleValue(CategoryName.SCHEMA.s()),
				deployment.getSingleValue(CategoryName.DEPLOYMENT_FILE.s()));
		if(loadedXML != null)
			loadedXML.set(XMLtree);
		if(XMLtree != null)
		{
			context = new LinkedList<>();
			readXML(XMLtree.getRoot(), deploymentCat, context, this, log);
			log.lf("after XML tree parse:", this);
			log.lf(">>>>>>>>");
		}
		else
			log.le("Deployment file load failed.");
		
		// ====================================== parse CLI args
		Iterator<String> it = programArguments.iterator();
		if(scenarioFirst) // already processed
			it.next();
		readCLIArgs(it, new CtxtTriple(CategoryName.DEPLOYMENT.s(), deploymentCat, deployment), this, log);
		log.lf("after CLI tree parse:", this);
		
		// ====================================== port portables
		
		List<String> categoryContext = new LinkedList<>();
		categoryContext.add(CategoryName.DEPLOYMENT.s());
		postProcess(deployment, CategoryName.DEPLOYMENT.s(), new MultiTreeMap(), new MultiTreeMap(),
				new LinkedList<String>(), this, log);
		
		log.lf("final config:", this);
		
		log.doExit();
		lock();
		return this;
	}
	
	/**
	 * Manages ported categories, auto-added entities and auto-generated parents (see {@link CategoryName}).
	 * <p>
	 * For the moment, only categories declared in {@link CategoryName} can be ported, auto-added, or their parents
	 * auto-generated.
	 * <p>
	 * The method follows the following steps:
	 * <ul>
	 * <li>from the current node, pick any entities that need to be ported from the node or automatically moved from the
	 * node to a deeper node (the latter are called 'lifted'); ported entities are only copied (in reference), lifted
	 * entities are actually moved.
	 * <li>any previously ported/lifted entities are placed in the current node, it the current node is a correct
	 * parent.
	 * <li>if any of the lifted entities has an ancestor that can be auto-generated as a child of this node, it is done
	 * so.
	 * <li>the method is called recursively for the child elements, passing the current list of ported / lifted
	 * entities.
	 * </ul>
	 * 
	 * @param elemTree
	 *                             - the node of the current entity.
	 * @param category
	 *                             - the category of the current entity.
	 * @param portableEntities
	 *                             - configurations for entities that should be ported to their correct parents. The
	 *                             list contains entries which are <i>category names</i>, each category names containing
	 *                             trees with the configuration of various entities.
	 * @param liftedEntities
	 *                             - configurations for entities that should be auto-moved to their correct parents. The
	 *                             list contains entries which are <i>category names</i>, each category names containing
	 *                             trees with the configuration of various entities.
	 * @param context
	 *                             - the list of ancestor categories, above the current entity.
	 * @param rootTree
	 *                             - the root tree (containing the entity list).
	 * @param log
	 *                             - the {@link Logger} to use.
	 */
	protected static void postProcess(MultiTreeMap elemTree, String category, MultiTreeMap portableEntities,
			MultiTreeMap liftedEntities, LinkedList<String> context, MultiTreeMap rootTree, Logger log)
	{
		List<String> toRemove = new LinkedList<>();
		LinkedList<String> updatedContext = new LinkedList<>(context);
		updatedContext.add(category);
		
		// pick entities to port from this category
		// pick entities to lift from here
		toRemove.clear();
		for(String childCatName : elemTree.getKeys())
			if(CategoryName.byName(childCatName) != null)
			{
				CategoryName childCat = CategoryName.byName(childCatName);
				if(childCat != null && childCat.portableFrom() != null && category.equals(childCat.portableFrom().s()))
				{ // port this from here; this will not be instanced here
					portableEntities.copyNameFrom(elemTree, childCatName);
					// toRemove.add(childCatName); // keep here too
				}
				else if(childCat != null && childCat.getParent() != null
						&& !updatedContext.contains(childCat.getParent()))
				{ // lift this from here to find the correct parent deeper into the hierarchy
					if(CategoryName.byName(category) != null)
					{
						liftedEntities.copyNameFrom(elemTree, childCatName);
						toRemove.add(childCatName);
					}
					else
						log.lw("Misplaced category [] found inside non-pre-defined category [], will leave here.",
								childCat, category);
				}
			}
		for(String rem : toRemove)
			elemTree.removeKey(rem);
		
		// port appropriate portables to this element
		toRemove.clear();
		for(String portedCatName : portableEntities.getKeys())
			if(category.equals(CategoryName.byName(portedCatName).getParent()))
			{ // port the ported entities to this element
				elemTree.copyNameFrom(portableEntities, portedCatName);
				toRemove.add(portedCatName);
			}
		for(String rem : toRemove)
			portableEntities.removeKey(rem);
		
		// port appropriate lifted entities to this element
		toRemove.clear();
		for(String liftedCatName : liftedEntities.getKeys())
			if(category.equals(CategoryName.byName(liftedCatName).getParent()))
			{ // port the ported entities to this element
				elemTree.copyNameFrom(liftedEntities, liftedCatName);
				toRemove.add(liftedCatName);
			}
		for(String rem : toRemove)
			liftedEntities.removeKey(rem);
			
		// auto-generate entities
		for(String liftedCatName : liftedEntities.getKeys())
			if(CategoryName.byName(liftedCatName) != null)
			{
				CategoryName liftedCat = CategoryName.byName(liftedCatName);
				for(String pName : liftedCat.getAncestors())
				{
					CategoryName p = CategoryName.byName(pName);
					// check also if not already generated
					if(p.canBeAutoGenerated() && p.getParent().equals(category) && !elemTree.isHierarchical(pName))
					{
						log.li("Autogenerating ancestor [] for [].", pName, liftedCatName);
						integrateName(new MultiTreeMap(), p, elemTree.addSingleTreeGet(pName, new MultiTreeMap()),
								rootTree, log);
						
					}
				}
			}
		
		// go deeper into child elements
		for(String childCatName : new LinkedList<>(elemTree.getKeys()))
		{ // go deeper
			if(elemTree.isHierarchical(childCatName))
				for(String subElemName : elemTree.getSingleTree(childCatName).getHierarchicalNames())
				{// for each element in the child category
					List<MultiTreeMap> childElemTrees = new LinkedList<>();
					if(elemTree.getSingleTree(childCatName).isSingleton(subElemName))
						childElemTrees.add(elemTree.getSingleTree(childCatName).getSingleTree(subElemName));
					else
						childElemTrees.addAll(elemTree.getSingleTree(childCatName).getTrees(subElemName));
					for(MultiTreeMap childElemTree : childElemTrees)
						// portables need not necessarily be consumed; liftables do.
						postProcess(childElemTree, childCatName, portableEntities.copyShallow(), liftedEntities,
								updatedContext, rootTree, log);
				}
		}
		if(!liftedEntities.getKeys().isEmpty())
			log.lw("In category [] lifted categories remained with no placement: [].", category,
					liftedEntities.getKeys());
	}
	
	/**
	 * Recursive method (recursing on XML nodes) which reads data from an XML (sub-)tree from the deployment file into
	 * the given configuration tree.
	 * <p>
	 * While processing the current XML node the corresponding entity node is created. The category node must be created
	 * while processing the parent, so as to add multiple entities to the same category. The node will add itself to its
	 * context category.
	 * 
	 * It also:
	 * <ul>
	 * <li>assigns names to entities, potentially auto-generated, based on the rules in {@link CategoryName};
	 * <li>assigns contexts in the tree structure based on the value of the {@value #CONTEXT_ELEMENT_NAME} attributes;
	 * <li>creates {@value #CONTEXT_ELEMENT_NAME} parameters based on tree structure;
	 * <li>adds entities to the entity list, using the actual subtrees in the configuration tree;
	 * </ul>
	 * 
	 * @param XMLnode
	 *                     - the XML node to read.
	 * @param catTree
	 *                     - the configuration tree corresponding to category containing this node.
	 * @param context
	 *                     - the context of the current node, down to the parent entity of this node.
	 * @param rootTree
	 *                     - the root deployment tree, where identifiable entities should be added.
	 * @param log
	 *                     - the {@link Logger} to use.
	 */
	protected static void readXML(XMLNode XMLnode, MultiTreeMap catTree, Deque<CtxtTriple> context,
			MultiTreeMap rootTree, Logger log)
	{
		// String l = "Node " + node.getName() + " with attributes ";
		// for(XMLAttribute a : node.getAttributes())
		// l += a.getName() + ",";
		// l += " and children ";
		// for(XMLNode n : node.getNodes())
		// l += n.getName() + ",";
		// log.lf(l);
		
		// get information on the node's category
		String catName = getXMLNodeCategory(XMLnode);
		CategoryName category = CategoryName.byName(catName);
		
		// create node
		// special case: the deployment node already exists
		MultiTreeMap nodeTree = (category != null && category.equals(CategoryName.DEPLOYMENT))
				? catTree.getSingleTree(null)
				: new MultiTreeMap();
		
		if(!context.isEmpty()) // not at root
			// read attributes, transform them to parameters
			for(XMLAttribute a : XMLnode.getAttributes())
				addParameter(nodeTree, a.getName(), a.getValue(), false, log);
			
		// add self to context
		context.push(new CtxtTriple(catName, catTree, nodeTree));
		
		// check subordinate XML nodes and integrate them.
		// two passes, because a name must be generated (for adding context to subordinate nodes) before checking
		// subordinate nodes.
		ArrayList<XMLNode> childEntities = new ArrayList<>();
		for(XMLNode child : XMLnode.getNodes())
		{
			if(child.getName().equals(PARAMETER_ELEMENT_NAME))
				// parameter nodes, add their values to the current tree
				addParameter(nodeTree, child.getAttributeValue(PARAMETER_NAME),
						child.getAttributeValue(PARAMETER_VALUE), false, log);
			else if(child.getNodes().isEmpty() && child.getAttributes().isEmpty()
					&& (CategoryName.byName(getXMLNodeCategory(child)) == null
							|| CategoryName.byName(getXMLNodeCategory(child)).isValue()))
				// text node, that will also be treated as parameter - value
				addParameter(nodeTree, child.getName(), (String) child.getValue(),
						(CategoryName.byName(getXMLNodeCategory(child)) != null
								&& CategoryName.byName(getXMLNodeCategory(child)).isUnique()),
						log);
			else
				childEntities.add(child);
		}
		
		// get the node's name or create it according to the child's category / entity; add to entity list
		// is here in order to be after checking subordinate parameter nodes (for name or name parts)
		integrateName(nodeTree, category, catTree, rootTree, log);
		
		for(XMLNode child : childEntities)
		{
			// node must be integrated as a different entity
			String childCatName = getXMLNodeCategory(child);
			// create implicit root entity, if necessary
			// manageImplicitRoot(CategoryName.byName(childCatName), rootTree, context);
			MultiTreeMap childCatTree = integrateChildCat(context.getFirst().elemTree, context.getFirst().category,
					childCatName, log);
			if(childCatTree == null)
				continue;
			// process child
			readXML(child, childCatTree, new LinkedList<>(context), rootTree, log);
		}
	}
	
	/**
	 * Reads data from program arguments into the given configuration tree. The parser attempts to place the parameters
	 * in the correct categories / elements in the already existing tree (read from the XML) or introduce new elements
	 * at the correct places. The CLI arguments are parsed in order as follows:
	 * <ul>
	 * <li>if the argument is a category / entity ("-category"), its correct place in the tree is found, either by
	 * advancing in the tree or going upwards in the tree until in the correct context.
	 * <li>the category name must be immediately followed by the element name (may be new or existing).
	 * <li>what follows until the next category name are arguments of the form "parameter:value" or just "parameter".
	 * </ul>
	 * <p>
	 * For integrating entities that are not already in the XML, a stack is used to keep track of the current position
	 * in the tree. The "current position" in the tree is decided by the existing tree (for existing elements and
	 * entities), by the hierarchy described in {@link CategoryName}, for known entities, and otherwise each new entity
	 * is considered as subordinate to the previous entity and for known entities the stack is popped until getting to
	 * the level where the entity appeared previously.
	 * 
	 * @param args
	 *                        - an {@link Iterator} through the arguments.
	 * @param baseContext
	 *                        - the {@link CategoryName#DEPLOYMENT} context entry.
	 * @param rootTree
	 *                        - the configuration tree. The given tree is expected to already contain the data from the
	 *                        XML deployment file.
	 * @param log
	 *                        - the {@link Logger} to use.
	 */
	protected static void readCLIArgs(Iterator<String> args, CtxtTriple baseContext, MultiTreeMap rootTree,
			UnitComponentExt log)
	{
		Deque<CtxtTriple> context = new LinkedList<>();
		context.push(baseContext);
		while(args.hasNext())
		{
			// log.lf(context.toString());
			String a = args.next();
			if(a.trim().length() == 0)
				continue;
			if(isCategory(a))
			{
				// get category
				String catName = getCategory(a);
				CategoryName category = CategoryName.byName(getCategory(a));
				if(!args.hasNext())
				{ // must check this before creating any trees
					log.lw("Empty unknown category [] in CLI arguments.", catName);
					return;
				}
				// get name
				String name = args.next();
				
				// create / find the context
				// search upwards in the current context
				// save the current context, in case no appropriate context found upwards
				Deque<CtxtTriple> savedContext = new LinkedList<>(context);
				while(!context.isEmpty())
				{
					if(context.peek().elemTree.isHierarchical(catName)
							|| (category != null && context.peek().category.equals(category.getParent()))) // TODO use
																											// ancestor
																											// list
					{ // found a level that contains the same category;
						// will insert new element in this context
						// childCatTree = context.peek().elemTree.getSingleTree(catName);
						break;
					}
					// no match yet
					context.pop();
				}
				if(context.isEmpty())
				{
					String msg = "Category [] has parent [] and no instance of parent could be found;";
					if(category == null)
					{ // category not known a-priori
						log.lw(msg + " adding in current context.", catName, "unknown");
						context = new LinkedList<>(savedContext);
					}
					else
					{
						log.lw(msg + " adding to top level.", catName, category.getParent());
						context = new LinkedList<>();
						context.add(baseContext);
					}
				}
				
				// integrate in current context.
				CtxtTriple cCtxt = context.peek();
				
				MultiTreeMap subCatTree = integrateChildCat(cCtxt.elemTree, cCtxt.category, catName, log);
				MultiTreeMap node;
				if(subCatTree.isHierarchical(name))
					node = subCatTree.isSingleton(name) ? subCatTree.getSingleTree(name)
							: subCatTree.getFirstTree(name);
				else
				{
					node = new MultiTreeMap();
					node.addOneValue(NAME_ATTRIBUTE_NAME, name);
					integrateName(node, category, subCatTree, rootTree, log);
				}
				context.push(new CtxtTriple(catName, subCatTree, node));
			}
			else
			{
				if(context.size() <= 1)
				{
					log.le("cannot add parameters to the root context (parameter was [])", a);
					continue;
				}
				if(context.peek().elemTree == null)
				{
					log.le("incorrect context for parameter []", a);
					continue;
				}
				String parameter, value = null;
				if(a.contains(NAME_SEPARATOR))
				{ // parameter name & value
					String[] es = a.split(NAME_SEPARATOR, 2);
					parameter = es[0];
					value = es[1];
				}
				else
					parameter = a;
				addParameter(context.peek().elemTree, parameter, value,
						(CategoryName.byName(parameter) != null && CategoryName.byName(parameter).isUnique()), log);
			}
		}
	}
	
	/**
	 * Checks if the given command line argument designates a category (begins with {@value #CLI_CATEGORY_PREFIX}).
	 * 
	 * @param arg
	 *                - the argument.
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
	 *                - the argument.
	 * @return the category name.
	 */
	protected static String getCategory(String arg)
	{
		return isCategory(arg) ? arg.substring(1) : null;
	}
	
	/**
	 * Common XML/CLI functionality: add a parameter and its value to a tree; if already added as a single, overwrite.
	 * It is added as a multiple name by default. it..
	 * 
	 * @param asSingleton
	 *                        - if the parameter is a singleton value.
	 */
	@SuppressWarnings("javadoc")
	protected static void addParameter(MultiTreeMap node, String par, String val, boolean asSingleton, Logger log)
	{
		if(node.containsHierarchicalName(par))
			log.le("Name [] already present as hierarchical name.", par);
		else if(asSingleton)
			node.setValue(par, val);
		else
			node.addOneValue(par, val);
	}
	
	/**
	 * Common XML/CLI functionality: retrieve or create a category node for a child entity inside the node of a parent
	 * entity.
	 * <p>
	 * Checks if it is correct to nest the child category inside the parent category.
	 * <p>
	 * If the child category is unique, checks if there is an existing entity in that category.
	 */
	@SuppressWarnings("javadoc")
	protected static MultiTreeMap integrateChildCat(MultiTreeMap parentNodeTree, String parentCat, String subCatName,
			Logger log)
	{
		CategoryName subCat = CategoryName.byName(subCatName);
		if(parentNodeTree.containsKey(subCatName))
		{
			if(parentNodeTree.isHierarchical(subCatName))
				return parentNodeTree.getSingleTree(subCatName);
			log.le(null, "Child node category [] is already present as a simple key. Will not process this node.",
					subCat);
			return null;
		}
		// category not already present
		return parentNodeTree.addSingleTreeGet(subCatName, new MultiTreeMap());
	}
	
	/**
	 * Common XML/CLI functionality: this method does the following:
	 * <ul>
	 * <li>if there is no existing name of the entity, attempts to generate a name based on other attributes of the
	 * entity (see name parts in CategoryName) and integrates the generated name in the entity's tree.
	 * <li>integrates the tree describing the entity into the tree of its category, under the given or generated name
	 * (or under the <code>null</code> name, if no name could be created. It is added as a singleton value or not
	 * depending on the value returned by {@link CategoryName#isUnique()}.
	 * <li>if the category is identifiable, adds the entity to the global <i>entity list</i>; if the entity has no name,
	 * a generated id is used.
	 * <li>the entity is added to the global <i>name list</i>; if the entity has no name, the generated id is used.\
	 * </ul>
	 * 
	 * @param node
	 *                     - the tree describing the entity.
	 * @param category
	 *                     - the category of the entity.
	 * @param catTree
	 *                     - the tree describing the category of the entity.
	 * @param rootTree
	 *                     - the tree describing the entire deployment.
	 * @param log
	 *                     - the {@link Logger} to use.
	 * 
	 * @return the name of the entity that
	 */
	protected static String integrateName(MultiTreeMap node, CategoryName category, MultiTreeMap catTree,
			MultiTreeMap rootTree, Logger log)
	{
		String name = node.getFirstValue(NAME_ATTRIBUTE_NAME);
		boolean nameGenerated = name == null;
		if(name == null && category != null && category.hasNameWithParts())
		{ // node has a registered category and its elements have two-parts names
			String[] partNames = category.nameParts();
			String part1 = node.getFirstValue(partNames[0]);
			String part2 = node.getFirstValue(partNames[1]);
			if(part2 == null && !category.isNameSecondPartOptional())
				return (String) log.lr(null, "Node of [] entity does not contain necessary name part attribute [].",
						category.s(), partNames[0]);
			name = (part1 != null ? part1 : "") + (part2 != null ? NAME_SEPARATOR + part2 : "");
		}
		if(name != null && name.trim().length() == 0)
			name = null; // no 0-length names
		if(nameGenerated && name != null && !node.containsKey(NAME_ATTRIBUTE_NAME))
			// add name parameter containing the generated name
			node.addOneValue(NAME_ATTRIBUTE_NAME, name);
		
		// add to category containing this entity
		if(catTree != null)
		{
			if(category != null && category.isUnique())
				catTree.addSingleTree(name, node);
			else
				catTree.addOneTree(name, node);
		}
		
		// add to entity list if the entity is identifiable (even if no name)
		String id = "#" + node.hashCode();
		if(category != null && category.isIdentifiable())
		{
			if(category.isUnique())
				rootTree.getSingleTree(category.s(), true).addSingleTree(name, node);
			else
				rootTree.getSingleTree(category.s(), true).addOneTree(name, node);
		}
		
		// add to name list, with id as fallback for name
		if(category != null && name != null && category.isIdentifiable())
			rootTree.getSingleTree(NAME_LIST_ENTRY, true).addSingleTree(name, node);
		else if(category != CategoryName.DEPLOYMENT)
		{
			rootTree.getSingleTree(NAME_LIST_ENTRY, true).addSingleTree(id, node);
			node.addSingleValue(NAME_LIST_ENTRY, id);
		}
		
		return name;
	}
	
	@SuppressWarnings("javadoc")
	protected static String getXMLNodeCategory(XMLNode XMLnode)
	{
		String catName = XMLnode.getName();
		if(catName.equals(GENERAL_ENTITY_NAME))
			catName = XMLnode.getAttributeValue(GENERAL_ENTITY_TYPE_ATTRIBUTE);
		return catName;
	}
	
	/**
	 * Method to simplify the access to a parameter/attribute of an parametric XML node.
	 * <p>
	 * Having the {@link XMLNode} instance associated with the parametric node, the method retrieves the first value
	 * found among the following:
	 * <ul>
	 * <li>the value of the attribute with the searched name, if any, or otherwise
	 * <li>the value associated with the first occurrence of the desired parameter name, if any, or otherwise
	 * <li>the value in the first node with the searched name, if any.
	 * </ul>
	 * 
	 * @param node
	 *                       - the node containing the configuration information for the agent.
	 * @param searchName
	 *                       - the name of the searched attribute / parameter / node.
	 * @return the value associated with the searched name, or <code>null</code> if nothing found.
	 */
	public static String getXMLValue(XMLNode node, String searchName)
	{
		if(node.getAttributeValue(searchName) != null)
			// from an attribute
			return node.getAttributeValue(searchName);
		if(node.getAttributeOfFirstNodeWithValue(PARAMETER_ELEMENT_NAME, PARAMETER_NAME, searchName,
				PARAMETER_VALUE) != null)
			// from a parameter (e.g. <parameter name="search" value="the name">)
			return node.getAttributeOfFirstNodeWithValue(PARAMETER_ELEMENT_NAME, PARAMETER_NAME, searchName,
					PARAMETER_VALUE);
		if(node.getNode(searchName, 0) != null)
			return node.getNode(searchName, 0).getValue().toString();
		return null;
	}
	
}
