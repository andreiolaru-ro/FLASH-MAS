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
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.XML.XMLParser;
import net.xqhs.util.XML.XMLTree;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.XML.XMLTree.XMLNode.XMLAttribute;
import net.xqhs.util.logging.DumbLogger;
import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.Logger.Level;
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
 * The configuration is a {@link MultiTreeMap} containing a tree describing the entire deployment and a tree containing
 * an index of all the entities in the deployment, indexed by a locally-generated id.
 * 
 * @author Andrei Olaru
 */
public class DeploymentConfiguration extends MultiTreeMap {
	/**
	 * The class UID.
	 */
	private static final long serialVersionUID = 5157567185843194635L;
	
	/**
	 * Prefix of category names used in CLI.
	 */
	public static final String	CLI_CATEGORY_PREFIX				= "-";
	/**
	 * Separator of parts of a name and of parameter and value.
	 */
	public static final String	NAME_SEPARATOR					= ":";
	/**
	 * Separator for multiple values of the same parameter.
	 */
	public static final String	VALUE_SEPARATOR		= ";";
	// cannot use : because many values are URLs and contain ':'
	/**
	 * Separator for elements in the load order setting.
	 */
	public static final String	LOAD_ORDER_SEPARATOR			= ";";
	/**
	 * The name of nodes containing parameters.
	 */
	public static final String	PARAMETER_ELEMENT_NAME			= "parameter";
	/**
	 * The name of the attribute which contains the kind.
	 */
	public static final String	KIND_ATTRIBUTE_NAME				= "kind";
	/**
	 * The name of the attribute of a parameter node holding the name of the parameter.
	 */
	public static final String	PARAMETER_NAME					= "name";
	/**
	 * The name of the attribute of a parameter node holding the value of the parameter.
	 */
	public static final String	PARAMETER_VALUE					= "value";
	/**
	 * The name of the attribute which contains the name.
	 */
	public static final String	NAME_ATTRIBUTE_NAME				= "name";
	/**
	 * The name of the element(s) which contain entity context.
	 */
	public static final String	CONTEXT_ELEMENT_NAME			= "in-context-of";
	/**
	 * Name of XML nodes for entities other than those in {@link CategoryName}.
	 */
	public static final String	GENERAL_ENTITY_NAME				= "entity";
	/**
	 * The name of the XML attribute specifying the type of the entity.
	 */
	public static final String	GENERAL_ENTITY_TYPE_ATTRIBUTE	= "type";
	
	/**
	 * The name of the (singleton) entry in the configuration tree, under which all entities are listed by their name or
	 * generated identifier.
	 * <p>
	 * This constant is also used as a key in entity nodes for their id (if they don't have an identifiable name.
	 */
	public static final String	LOCAL_ID_ATTRIBUTE		= "#local-id";
	/**
	 * The name under which the category of an element is entered in the element.
	 */
	public static final String	CATEGORY_ATTRIBUTE_NAME	= "#category-type";
	/**
	 * The name of a parameter in the configuration {@link MultiTreeMap} of an entity indicating that the entity has
	 * already been loaded.
	 */
	public static final String	LOADED_ATTRIBUTE_NAME	= "#loaded";
	
	/**
	 * Root package for FLASH classes.
	 */
	public static final String		ROOT_PACKAGE				= "net.xqhs.flash";
	/**
	 * Package for core FLASH functionality
	 */
	public static final String		CORE_PACKAGE				= ROOT_PACKAGE + ".core";
	/**
	 * The default directory for deployment files.
	 */
	public static final String		DEPLOYMENT_FILE_DIRECTORY	= "src-deployment/";
	/**
	 * Directories containing source files (especially for looking up various files).
	 */
	public static final String[]	SOURCE_FILE_DIRECTORIES		= { "src", "src-testing", "src-tests", "src-examples" };
	
	/**
	 * Local IDs of default created entities.
	 */
	protected List<String>			autoCreated	= new LinkedList<>();
	/**
	 * The correspondence between names and local IDs, used to assign contexts by names.
	 */
	protected Map<String, String>	name_ids	= new HashMap<>();
	
	/**
	 * Flag to determine the central node. This will be assigned a CentralMonitoringAndControlEntity.
	 */
	public static final String CENTRAL_NODE_KEY = "central";
	
	/**
	 * Type for net.xqhs.flash.core.monitoring and control entity.
	 */
	public static final String MONITORING_TYPE = "net/xqhs/flash/core/monitoring";
	
	/**
	 * The name of the Central M&C entity.
	 */
	public static final String CENTRAL_MONITORING_ENTITY_NAME = "Monitoring&Control_Entity";
	
	/**
	 * A node in the context stack. The context stack is used in order to keep track of location in the configuration
	 * tree while parsing CLI arguments.
	 */
	static class CtxtTriple {
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
		 *            - category name.
		 * @param categoryTree
		 *            - category tree (may be <code>null</code>).
		 * @param elTree
		 *            - current element tree (may be <code>null</code>).
		 */
		public CtxtTriple(String cat, MultiTreeMap categoryTree, MultiTreeMap elTree) {
			category = cat;
			catTree = categoryTree;
			elemTree = elTree;
		}
		
		@Override
		public String toString() {
			return "{" + category + "/" + (catTree != null ? catTree.toString(1, true) : "-") + "/"
					+ (elemTree != null ? elemTree.toString(1, true) : "-") + "}";
		}
	}
	
	/**
	 * The default configuration. Only single values can be added here at this time.
	 */
	public DeploymentConfiguration() {
		// deployment node
		MultiTreeMap deploymentCat = this.addSingleTreeGet(CategoryName.DEPLOYMENT.s(), new MultiTreeMap());
		MultiTreeMap deployment = new MultiTreeMap();
		integrateName(deployment, CategoryName.DEPLOYMENT.s(), deploymentCat, this, null, name_ids, new DumbLogger());
		
		// default settings
		
		// default schema
		deployment.addSingleValue(CategoryName.SCHEMA.s(), "src-schema/deployment-schema.xsd");
		// default load order
		deployment.addSingleValue(CategoryName.LOAD_ORDER.s(),
				CategoryName.PYLON.s() + LOAD_ORDER_SEPARATOR + CategoryName.AGENT.s());
		// automatically integrate the composite agent loader
		// MultiTreeMap compositeLoader = new MultiTreeMap();
		// compositeLoader.addOneValue(CategoryName.LOADER.nameParts()[0], CategoryName.AGENT.s());
		// compositeLoader.addOneValue(CategoryName.LOADER.nameParts()[1], "Composite"); // FIXME: string
		// compositeLoader.addOneValue(SimpleLoader.CLASSPATH_KEY, CompositeAgentLoader.class.getName());
		// integrateName(compositeLoader, CategoryName.LOADER.s(),
		// deployment.addSingleTreeGet(CategoryName.LOADER.s(), new MultiTreeMap()), this, new DumbLogger());
		// default node
		integrateName(new MultiTreeMap(), CategoryName.NODE.s(),
				deployment.addSingleTreeGet(CategoryName.NODE.s(), new MultiTreeMap()), this, null, name_ids,
				new DumbLogger());
		// default pylon (local support)
		integrateName(new MultiTreeMap().addOneValue(NAME_ATTRIBUTE_NAME, "local:default"), CategoryName.PYLON.s(),
				deployment.addSingleTreeGet(CategoryName.PYLON.s(), new MultiTreeMap()), this, null, name_ids,
				new DumbLogger());
		autoCreated.addAll(this.getSingleTree(LOCAL_ID_ATTRIBUTE).getKeys());
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
	 *            - if the deployment file is parsed and this argument is not <code>null</code>, the resulting
	 *            {@link XMLTree} instance will be stored in this ContentHolder instance.
	 * @return the instance itself, which is also the {@link MultiTreeMap} that contains all settings.
	 * 			
	 * @throws ConfigLockedException
	 *             - if load() is called more than once.
	 */
	public DeploymentConfiguration loadConfiguration(List<String> programArguments, boolean parseDeploymentFile,
			ContentHolder<XMLTree> loadedXML) throws ConfigLockedException {
		locked();
		UnitComponentExt log = (UnitComponentExt) new UnitComponentExt("settings load")
				.setLoggerType(PlatformUtils.platformLogType()).setLogLevel(Level.INFO);
		MultiTreeMap deploymentCat = this.getSingleTree(CategoryName.DEPLOYMENT.s());
		MultiTreeMap deployment = deploymentCat.getSingleTree(null);
		
		log.lf("initial tree:", this);
		
		log.lf("program arguments:", programArguments);
		
		// ====================================== get deployment file and schema
		boolean deploymentArgPresent = false;
		if(programArguments.size() > 0 && programArguments.get(0).length() > 0
				&& !programArguments.get(0).startsWith(CLI_CATEGORY_PREFIX) && !programArguments.get(0).contains(":")) {
			deployment.setValue(CategoryName.DEPLOYMENT_FILE.s(), programArguments.get(0));
			deploymentArgPresent = true;
		}
		else
			for(Iterator<String> it = programArguments.iterator(); it.hasNext();) {
				String arg = it.next();
				if(isCategoryDefinition(arg) && (getCategoryName(arg).equals(CategoryName.DEPLOYMENT_FILE.s())
						|| getCategoryName(arg).equals(CategoryName.SCHEMA.s()))) {
					String val = null;
					if(it.hasNext() || isCategoryDefinition(val = it.next()))
						throw new IllegalArgumentException(
								"Program argument after " + arg + " should be a correct value.");
					deployment.setValue(getCategoryName(arg), val);
				}
			}
		
		// ====================================== parse deployment file
		if(deployment.isSet(CategoryName.DEPLOYMENT_FILE.s())) {
			log.lf("loading deployment file [] with schema [].",
					deployment.getSingleValue(CategoryName.DEPLOYMENT_FILE.s()),
					deployment.getSingleValue(CategoryName.SCHEMA.s()));
			
			// ====================================== context management
			Deque<CtxtTriple> context = null; // categories & elements context
			// do not create a base context here, the deployment will be generated only in XMLtree
			
			// ====================================== load deployment file
			XMLTree XMLtree = XMLParser.validateParse(deployment.getSingleValue(CategoryName.SCHEMA.s()),
					deployment.getSingleValue(CategoryName.DEPLOYMENT_FILE.s()));
			if(loadedXML != null)
				loadedXML.set(XMLtree);
			if(XMLtree != null) {
				context = new LinkedList<>();
				readXML(XMLtree.getRoot(), deploymentCat, context, this, autoCreated, name_ids, log);
				log.lf("after XML tree parse:", this);
				log.lf(">>>>>>>>");
			}
			else
				log.le("Deployment file load failed.");
		}
		else
			log.lf("No deployment file.");
		
		// ====================================== parse CLI args
		Iterator<String> it = programArguments.iterator();
		if(deploymentArgPresent) // already processed
			it.next();
		readCLIArgs(it, new CtxtTriple(CategoryName.DEPLOYMENT.s(), deploymentCat, deployment), this, autoCreated,
				name_ids, log);
		log.lf("after CLI tree parse:", this);
		
		// ====================================== port portables, move elements to correct parents
		
		List<String> categoryContext = new LinkedList<>();
		categoryContext.add(CategoryName.DEPLOYMENT.s());
		postProcess(deployment, CategoryName.DEPLOYMENT.s(), new MultiTreeMap(), new MultiTreeMap(),
				new LinkedList<>(), this, autoCreated, name_ids, log);
		
		addContext(deployment, new LinkedList<>(), name_ids);
		
		// ====================================== remove default created entities
		log.lf("default created entities: []", autoCreated);
		// create a reverse index from each id to the ids in its context
		// iterate in reverse order
		// remove ids (also from their direct contexts -- use the index) which only have default ids depending on them
		// use List.containsAll
		
		for(String id : this.getSingleTree(LOCAL_ID_ATTRIBUTE).getKeys())
			if(autoCreated.contains(id)) {
				// TODO
			}
		
		log.lf("==============================================================");
		log.lf("==============================================================");
		log.li("final config:", new MultiTreeMap().addOneTree(CategoryName.DEPLOYMENT.s(), deployment));
		log.lf("local IDs:", new MultiTreeMap().addSingleTree(LOCAL_ID_ATTRIBUTE, this.getATree(LOCAL_ID_ATTRIBUTE)));
		
		log.doExit();
		lock();
		return this;
	}
	
	/**
	 * Manages ported categories (see {@link CategoryName}).
	 * <p>
	 * For the moment, only categories declared in {@link CategoryName} can be ported.
	 * <p>
	 * The method follows the following steps:
	 * <ul>
	 * <li>from the current (<i>tree node</i>, not <i>node entity</i> in the deployment) node, pick any entities that
	 * need to be ported from the node or automatically moved from the node to a deeper node (the latter are called
	 * 'lifted'); ported entities are only copied (in reference), lifted entities are actually moved.
	 * <li>any previously ported/lifted entities are placed in the current node, if the current node is a correct
	 * parent.
	 * <li>the method is called recursively for the child elements, passing the current list of ported / lifted
	 * entities.
	 * </ul>
	 * 
	 * @param elemTree
	 *            - the node of the current entity.
	 * @param category
	 *            - the category of the current entity.
	 * @param portableEntities
	 *            - configurations for entities that should be ported to their correct parents. The list contains
	 *            entries which are <i>category names</i>, each category names containing trees with the configuration
	 *            of various entities.
	 * @param liftedEntities
	 *            - configurations for entities that should be auto-moved to their correct parents. The list contains
	 *            entries which are <i>category names</i>, each category names containing trees with the configuration
	 *            of various entities.
	 * @param context
	 *            - the list of ancestor categories, above the current entity.
	 * @param rootTree
	 *            - the root tree (containing the entity list).
	 * @param autoCreated
	 *            - the list of entities that have been created automatically (that were not given in the deployment
	 *            scenario).
	 * @param name_ids
	 *            - correspondence between names and local IDs.
	 * @param log
	 *            - the {@link Logger} to use.
	 */
	protected static void postProcess(MultiTreeMap elemTree, String category, MultiTreeMap portableEntities,
			MultiTreeMap liftedEntities, List<String> context, MultiTreeMap rootTree, List<String> autoCreated,
			Map<String, String> name_ids, Logger log) {
		List<String> toRemove = new LinkedList<>();
		List<String> updatedContext = new LinkedList<>(context);
		updatedContext.add(category); // the current context, including this category
		
		// Need to separate the previous and the current to avoid porting entities and then deploying them again as a
		// copy.
		// Very inefficient implementation.
		MultiTreeMap newPortables = portableEntities.copyDeep();
		
		// pick entities to port from this category
		// pick entities to lift from here
		toRemove.clear();
		for(String childCatName : elemTree.getKeys())
			if(CategoryName.byName(childCatName) != null) {
				CategoryName childCat = CategoryName.byName(childCatName);
				if(childCat != null && childCat.portableFrom() != null
						&& updatedContext.contains(childCat.portableFrom().s())
						&& (!elemTree.isHierarchical(childCatName)
								|| allPortable(elemTree.getSingleTree(childCatName), childCatName, log))) {
					// port this from here; this will not be instanced here
					log.lf("While in category [], picking portable [] (shallow copy).", category, childCatName);
					newPortables.copyNameFrom(elemTree, childCatName);
					// toRemove.add(childCatName); // keep here too
				}
				else if(childCat != null && childCat.getParent() != null
						&& !updatedContext.contains(childCat.getParent())) {
					// lift this from here to find the correct parent deeper into the hierarchy e.g. agents declared in
					// deployment that actually need to get under support
					if(CategoryName.byName(category) != null) {
						liftedEntities.copyNameFrom(elemTree, childCatName);
						log.lf("While in category [], lifting [] (shallow copy).", category, childCatName);
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
			if(!portedCatName.equals(category)) // protect against forever adding portable categories to themselves
				if(CategoryName.byName(portedCatName).visibleOnPath()
						|| category.equals(CategoryName.byName(portedCatName).getParent())) {
					// port the ported entities to this element
					if(portableEntities.isHierarchical(portedCatName)) {
						MultiTreeMap portedCatTree = portableEntities.getSingleTree(portedCatName);
						MultiTreeMap targetCatTree = elemTree.getSingleTree(portedCatName, true);
						log.lf("While in category [], dropping portable [] (deep copy, with name integration).",
								category, portedCatName);
						for(String name : portedCatTree.getKeys())
							if(portedCatTree.isSingleton(name))
								log.le("Cannot overwrite existing singleton element [] of category [] with a ported instance.",
										name, portedCatName);
							else
								for(MultiTreeMap elem : portedCatTree.getTrees(name)) {
									MultiTreeMap clone = elem.copyDeep();
									integrateName(clone, portedCatName, targetCatTree, rootTree, autoCreated, name_ids,
											log);
									if(autoCreated.contains(elem.getSingleValue(LOCAL_ID_ATTRIBUTE)))
										// clones of default created entities are also default created
										autoCreated.add(clone.getSingleValue(LOCAL_ID_ATTRIBUTE));
								}
					}
					else {
						log.lf("While in category [], dropping portable [] (deep copy, without name integration).",
								category, portedCatName);
						elemTree.copyNameFromDeep(portableEntities, portedCatName);
					}
					if(!(CategoryName.byName(portedCatName).visibleOnPath()
							&& !category.equals(CategoryName.byName(portedCatName).getParent())))
						toRemove.add(portedCatName);
				}
		for(String rem : toRemove)
			portableEntities.removeKey(rem);
		
		// port appropriate lifted entities to this element
		toRemove.clear();
		for(String liftedCatName : liftedEntities.getKeys())
			if(category.equals(CategoryName.byName(liftedCatName).getParent())) {
				// port the ported entities to this element
				log.lf("While in category [], dropping lifted [] (shallow copy).", category, liftedCatName);
				elemTree.copyNameFrom(liftedEntities, liftedCatName);
				toRemove.add(liftedCatName);
			}
		for(String rem : toRemove)
			liftedEntities.removeKey(rem);
			
		// // auto-generate entities
		// for(String liftedCatName : liftedEntities.getKeys())
		// if(CategoryName.byName(liftedCatName) != null)
		// {
		// CategoryName liftedCat = CategoryName.byName(liftedCatName);
		// for(String pName : liftedCat.getAncestors())
		// {
		// CategoryName p = CategoryName.byName(pName);
		// // check also if not already generated
		// if(p.canBeAutoGenerated() && p.getParent().equals(category) && !elemTree.isHierarchical(pName))
		// {
		// log.li("Autogenerating ancestor [] for [].", pName, liftedCatName);
		// integrateName(new MultiTreeMap(), pName, elemTree.addSingleTreeGet(pName, new MultiTreeMap()),
		// rootTree, log);
		//
		// }
		// }
		// }
		
		// go deeper into child elements
		for(String childCatName : new LinkedList<>(elemTree.getKeys())) { // go deeper
			if(elemTree.isHierarchical(childCatName))
				for(String subElemName : elemTree.getSingleTree(childCatName).getHierarchicalNames()) {
					// for each element in the child category
					List<MultiTreeMap> childElemTrees = new LinkedList<>();
					if(elemTree.getSingleTree(childCatName).isSingleton(subElemName))
						childElemTrees.add(elemTree.getSingleTree(childCatName).getSingleTree(subElemName));
					else
						childElemTrees.addAll(elemTree.getSingleTree(childCatName).getTrees(subElemName));
					for(MultiTreeMap childElemTree : childElemTrees)
						// portables need not necessarily be consumed; liftables do.
						// this must be copy shallow, because the lifted entities need to remain the same entities
						// throughout the deployment tree
						postProcess(childElemTree, childCatName, newPortables.copyShallow(), liftedEntities,
								updatedContext, rootTree, autoCreated, name_ids, log);
				}
		}
		
		toRemove.clear();
		for(String liftedCatName : liftedEntities.getKeys())
			if(CategoryName.byName(liftedCatName).getAncestorsList().contains(category)) {
				// with nowhere to drop this, drop it here
				log.li("Dropping [] entities in [].", liftedCatName, category);
				elemTree.copyNameFrom(liftedEntities, liftedCatName);
				toRemove.add(liftedCatName);
			}
		for(String rem : toRemove)
			liftedEntities.removeKey(rem);
		
		if(!liftedEntities.getKeys().isEmpty())
			log.lw("In category [] lifted categories remained with no placement: [].", category,
					liftedEntities.getKeys());
	}
	
	/**
	 * Checks that all categories inside a category are either not declared, or are portable. This helps to know whether
	 * a particular category should be lifted instead of ported (if this method returns <code>false</code>).
	 * <p>
	 * WARNING: it is assumed that all category keys are singletons.
	 * 
	 * @param tree
	 *            - the tree to check.
	 * @param caller
	 *            - the name of the original category to be ported. Is only used for logging.
	 * @param log
	 *            - the log to use.
	 * @return <code>true</code> if everything inside the category is portable.
	 */
	protected static boolean allPortable(MultiTreeMap tree, String caller, Logger log) {
		// for(MultiTreeMap tree : trees)
		for(String child : tree.getKeys()) {
			if(CategoryName.byName(child) != null && CategoryName.byName(child).portableFrom() == null) {
				// child is a declared category and is not portable -> return false
				log.lf("Found non-portable category [] inside caller category; caller category [] will be lifted instead of ported. ",
						child, caller);
				return false;
			}
			if(tree.isHierarchical(child))
				if(tree.isSingleton(child)) {
					if(!allPortable(tree.getATree(child), caller, log))
						return false;
				}
				else
					for(MultiTreeMap subTree : tree.getTrees(child))
						if(!allPortable(subTree, caller, log))
							return false;
		}
		return true;
	}
	
	/**
	 * Goes through the deployment configuration and adds to each entities the local id of all the entities which for
	 * the context of this entities.
	 * <p>
	 * More specifically, the {@value #LOCAL_ID_ATTRIBUTE} of each of the entities containing this entity is added to
	 * the {@value #CONTEXT_ELEMENT_NAME} attribute of this node. The first entry is the closest, and the last is the
	 * deployment itself.
	 * 
	 * @param node
	 *            - the node to configure.
	 * @param contextAbove
	 *            - the local identifiers of all the entities containing this entity.
	 * @param name_ids
	 *            - correspondence between names and local IDs.
	 */
	protected static void addContext(MultiTreeMap node, List<String> contextAbove, Map<String, String> name_ids) {
		LinkedList<String> currentContext = new LinkedList<>(contextAbove);
		for(String context : node.getValues(CONTEXT_ELEMENT_NAME))
			if(!context.startsWith(LOCAL_ID_ATTRIBUTE.substring(0, 1)) && name_ids.containsKey(context)) {
				// must convert name to local ID
				node.addOneValue(CONTEXT_ELEMENT_NAME, name_ids.get(context));
				currentContext.push(name_ids.get(context));
			}
		for(String context : contextAbove)
			node.addOneValue(CONTEXT_ELEMENT_NAME, context);
		currentContext.push(node.getSingleValue(LOCAL_ID_ATTRIBUTE));
		
		for(String childCatName : node.getHierarchicalNames())
			for(String subElemName : node.getSingleTree(childCatName).getHierarchicalNames()) {
				// for each element in the child category
				List<MultiTreeMap> childElemTrees = new LinkedList<>();
				if(node.getSingleTree(childCatName).isSingleton(subElemName))
					childElemTrees.add(node.getSingleTree(childCatName).getSingleTree(subElemName));
				else
					childElemTrees.addAll(node.getSingleTree(childCatName).getTrees(subElemName));
				for(MultiTreeMap childElemTree : childElemTrees)
					addContext(childElemTree, currentContext, name_ids);
			}
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
	 *            - the XML node to read.
	 * @param catTree
	 *            - the configuration tree corresponding to category containing this node.
	 * @param context
	 *            - the context of the current node, down to the parent entity of this node.
	 * @param rootTree
	 *            - the root deployment tree, where identifiable entities should be added.
	 * @param autoCreated
	 *            - the list of entity IDs that have been created automatically (that were not given in the deployment
	 *            scenario).
	 * @param name_ids
	 *            - correspondence between names and local IDs.
	 * @param log
	 *            - the {@link Logger} to use.
	 */
	protected static void readXML(XMLNode XMLnode, MultiTreeMap catTree, Deque<CtxtTriple> context,
			MultiTreeMap rootTree, List<String> autoCreated, Map<String, String> name_ids, Logger log) {
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
		for(XMLNode child : XMLnode.getNodes()) {
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
		integrateName(nodeTree, catName, catTree, rootTree, autoCreated, name_ids, log);
		
		for(XMLNode child : childEntities) {
			// node must be integrated as a different entity
			String childCatName = getXMLNodeCategory(child);
			MultiTreeMap childCatTree = integrateChildCat(context.getFirst().elemTree, childCatName, log);
			if(childCatTree == null)
				continue;
			// process child
			readXML(child, childCatTree, new LinkedList<>(context), rootTree, autoCreated, name_ids, log);
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
	 *            - an {@link Iterator} through the arguments.
	 * @param baseContext
	 *            - the {@link CategoryName#DEPLOYMENT} context entry.
	 * @param rootTree
	 *            - the configuration tree. The given tree is expected to already contain the data from the XML
	 *            deployment file.
	 * @param autoCreated
	 *            - the list of entity IDs that have been created automatically (that were not given in the deployment
	 *            scenario).
	 * @param name_ids
	 *            - correspondence between names and local IDs.
	 * @param log
	 *            - the {@link Logger} to use.
	 */
	protected static void readCLIArgs(Iterator<String> args, CtxtTriple baseContext, MultiTreeMap rootTree,
			List<String> autoCreated, Map<String, String> name_ids, UnitComponentExt log) {
		Deque<CtxtTriple> context = new LinkedList<>();
		context.push(baseContext);
		while(args.hasNext()) {
			// log.lf(context.toString());
			String a = args.next();
			if(a.trim().length() == 0)
				continue;
			if(isCategoryDefinition(a)) {
				// get category
				String catName = getCategoryName(a);
				CategoryName category = CategoryName.byName(getCategoryName(a));
				if(!args.hasNext()) { // must check this before creating any trees
					log.lw("Empty category [] in CLI arguments.", catName);
					return;
				}
				
				// create / find the context
				// search upwards in the current context for a parent or at least an ancestor
				// save the current context, in case no appropriate context found upwards
				Deque<CtxtTriple> savedContext = new LinkedList<>(context);
				while(!context.isEmpty()) {
					CategoryName cCat = CategoryName.byName(context.peek().category);
					if(cCat != null && cCat.isNotEntity()) {
						// cannot add inside a value; pop and this is the new saved context
						context.pop();
						savedContext = new LinkedList<>(context);
						continue;
					}
					if(context.peek().elemTree != null && context.peek().elemTree.isHierarchical(catName)) {
						// found a level that contains the same category;
						// will insert new element in this context
						// childCatTree = context.peek().elemTree.getSingleTree(catName);
						break;
					}
					if(category != null && category.getParent() != null
							&& category.getParent().equals(context.peek().category)) {
						// found the parent in the current context;
						// can leave the element in the initial context.
						// re-added this because pylons declared inside nodes go back to the entry for the default
						// pylon, which is in the deployment.
						context = new LinkedList<>(savedContext);
						break;
					}
					// if(category != null && category.getAncestorsList().contains(context.peek().category)) {
					// // found the parent in the current context;
					// // can leave the element in the initial context.
					// // this is true for any declared category - the context contains deployment, which is in the
					// // ancestor list.
					// context = new LinkedList<>(savedContext);
					// break;
					// }
					// TODO: and current context is not a registered category
					// no match yet
					context.pop();
				}
				if(context.isEmpty()) {
					String msg = "Category [] not found elsewhere in the hierarchy;";
					// String msg = "Category [] has parent []" + (category != null && category.getParent() == null ? ""
					// : " and no instance of parent could be found") + ";";
					// if(category == null || category.getParent() == null)
					// { // category not known a-priori or category has no parent
					log.lw(msg + " adding in current context.", catName);
					// ,category == null ? "unknown" : category.getParent());
					context = new LinkedList<>(savedContext);
					// }
					// else
					// {
					// log.lw(msg + " adding to top level.", catName, category.getParent());
					// context = new LinkedList<>();
					// context.add(baseContext);
					// }
				}
				
				// integrate in current context.
				CtxtTriple cCtxt = context.peek();
				if(cCtxt.elemTree == null)
					log.le("Unable to integrate category [] in current context [] which does not support subordinate categories.",
							catName, cCtxt.category);
				else if(category != null && category.isValue()) { // it is a simple value
					context.push(new CtxtTriple(catName, null, null));
				}
				else { // it is an entity; get entity name
					String name = args.next();
					MultiTreeMap subCatTree = integrateChildCat(cCtxt.elemTree, catName, log);
					MultiTreeMap node;
					if(subCatTree.isHierarchical(name))
						node = subCatTree.isSingleton(name) ? subCatTree.getSingleTree(name)
								: subCatTree.getFirstTree(name);
					else {
						node = new MultiTreeMap();
						node.addOneValue(NAME_ATTRIBUTE_NAME, name);
						integrateName(node, catName, subCatTree, rootTree, autoCreated, name_ids, log);
					}
					context.push(new CtxtTriple(catName, subCatTree, node));
				}
			}
			else {
				if(context.size() <= 1) {
					log.le("cannot add parameters to the root context (parameter was [])", a);
					continue;
				}
				if(context.peek().elemTree == null) {
					// could be a simple value category
					CategoryName paramCateg = CategoryName.byName(context.peek().category);
					if(context.size() > 1 && paramCateg != null && paramCateg.isValue()) {
						// place as value in the higher context
						CtxtTriple placeholder = context.pop();
						addParameter(context.peek().elemTree, paramCateg.s(), a, paramCateg.isUnique(), log);
						context.push(placeholder);
					}
					else {
						log.le("incorrect context for parameter []", a);
						continue;
					}
				}
				else {
					String parameter, value = null;
					if(a.contains(NAME_SEPARATOR)) { // parameter name & value
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
	}
	
	/**
	 * Checks if the given command line argument designates a category (begins with {@value #CLI_CATEGORY_PREFIX}).
	 * 
	 * @param arg
	 *            - the argument.
	 * @return <code>true</code> if it designates a category.
	 */
	protected static boolean isCategoryDefinition(String arg) {
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
	protected static String getCategoryName(String arg) {
		return isCategoryDefinition(arg) ? arg.substring(1) : null;
	}
	
	/**
	 * Common XML/CLI functionality: add a parameter and its value to a tree; if already added as a single, overwrite.
	 * It is added as a multiple name by default.
	 * 
	 * @param node
	 *            - the node in which to place the parameter.
	 * @param par
	 *            - the name of the parameter.
	 * @param val
	 *            - the value of the parameter
	 * @param asSingleton
	 *            - if the parameter is a singleton value.
	 * @param log
	 *            - the log to use.
	 */
	protected static void addParameter(MultiTreeMap node, String par, String val, boolean asSingleton, Logger log) {
		if(node.containsHierarchicalName(par))
			log.le("Name [] already present as hierarchical name.", par);
		else if(asSingleton)
			node.setValue(par, val);
		else if(val != null && val.contains(VALUE_SEPARATOR))
			for(String oneval : val.split(VALUE_SEPARATOR))
				node.addOneValue(par, oneval);
		else
			node.addOneValue(par, val);
	}
	
	/**
	 * Common XML/CLI functionality: retrieve or create a category node for a child entity inside the node of a parent
	 * entity.
	 * <p>
	 * If the child category is unique, checks if there is an existing entity in that category.
	 * 
	 * @param parentNodeTree
	 *            - the node in which to integrate / from which to retrieve the category; the node should represent an
	 *            entity.
	 * @param subCatName
	 *            - the category to integrate / retrieve.
	 * @param log
	 *            - the log to use.
	 * @return the node associated with the category; is a new node if the category was not pre-existing.
	 */
	protected static MultiTreeMap integrateChildCat(MultiTreeMap parentNodeTree, String subCatName, Logger log) {
		CategoryName subCat = CategoryName.byName(subCatName);
		if(parentNodeTree.containsKey(subCatName)) {
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
	 * entity (see name parts in {@link CategoryName}) and integrates the generated name in the entity's tree.
	 * <li>integrates the tree describing the entity into the tree of its category, if a category tree is given, under
	 * the given or generated name (or under the <code>null</code> name, if no name could be created. It is added as a
	 * singleton value or not depending on the value returned by {@link CategoryName#isUnique()}; the default (e.g. if
	 * no category data is found) is as a non-singleton value.
	 * <li>the entity gets a unique generated id and is added to the local <i>id list</i>.
	 * </ul>
	 * 
	 * @param node
	 *            - the tree describing the entity.
	 * @param categoryName
	 *            - the name of the category of the entity.
	 * @param catTree
	 *            - the tree describing the category of the entity.
	 * @param rootTree
	 *            - the tree describing the entire deployment.
	 * @param autoCreated
	 *            - the list of entity IDs that have been created automatically (that were not given in the deployment
	 *            scenario).
	 * @param name_ids
	 *            - correspondence between names and local IDs.
	 * @param log
	 *            - the {@link Logger} to use.
	 * 			
	 * @return the name of the entity that was added.
	 */
	protected static String integrateName(MultiTreeMap node, String categoryName, MultiTreeMap catTree,
			MultiTreeMap rootTree, List<String> autoCreated, Map<String, String> name_ids, Logger log) {
		String name = node.getFirstValue(NAME_ATTRIBUTE_NAME);
		boolean nameGenerated = name == null;
		CategoryName category = CategoryName.byName(categoryName);
		if(name == null && category != null && category.hasNameWithParts()) {
			// node has a registered category and its elements have two-parts names
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
		if(catTree != null) {
			if(category != null && category.isUnique())
				catTree.addSingleTree(name, node);
			else {
				if(autoCreated != null && catTree.getHierarchicalNames().size() == 1) {
					String firstName = catTree.getHierarchicalNames().get(0);
					if(catTree.getTrees(firstName).size() == 1
							&& autoCreated.contains(catTree.getATree(firstName).getSingleValue(LOCAL_ID_ATTRIBUTE))) {
						// the only tree is an auto-created category and it will be removed.
						String id = catTree.getATree(firstName).getSingleValue(LOCAL_ID_ATTRIBUTE);
						catTree.removeKey(firstName);
						rootTree.getSingleTree(LOCAL_ID_ATTRIBUTE).removeKey(id);
						log.li("removed auto-created [] entity [] to replace with entity [].", category, firstName,
								name);
					}
				}
				catTree.addOneTree(name, node);
			}
		}
		
		// create a local id and add it to the list
		String id = "#" + node.hashCode();
		rootTree.getSingleTree(LOCAL_ID_ATTRIBUTE, true).addSingleTree(id, node);
		node.addSingleValue(LOCAL_ID_ATTRIBUTE, id);
		node.addSingleValue(CATEGORY_ATTRIBUTE_NAME, categoryName);
		name_ids.put(name, id);
		
		return name;
	}
	
	/**
	 * For a node in the deployment XML, retrieve the name of the category of the entity that the node describes. Can be
	 * either the actual tag, or the value of the {@value #GENERAL_ENTITY_TYPE_ATTRIBUTE} attribute.
	 * 
	 * @param XMLnode
	 *            - the node to process.
	 * @return the name of the category of the node.
	 */
	protected static String getXMLNodeCategory(XMLNode XMLnode) {
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
	 *            - the node containing the configuration information for the agent.
	 * @param searchName
	 *            - the name of the searched attribute / parameter / node.
	 * @return the value associated with the searched name, or <code>null</code> if nothing found.
	 */
	public static String getXMLValue(XMLNode node, String searchName) {
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
	
	/**
	 * Creates an entity list, as a list of {@link MultiTreeMap} instances, each instance being the configuration of an
	 * entity.
	 * <p>
	 * Each instance should contain:
	 * <ul>
	 * <li>a {@link #LOCAL_ID_ATTRIBUTE} singleton parameter, having a unique local id as value.
	 * <li>a {@link #CATEGORY_ATTRIBUTE_NAME} singleton parameter, containing the category (the entity type, for
	 * entities).
	 * <li>a {@link #CONTEXT_ELEMENT_NAME} attribute, containing as values the local IDs of entities that the entity is
	 * in the context of, with the first being the 'closest' entity.
	 * <li>optionally, a {@link #NAME_ATTRIBUTE_NAME} attribute, containing the name of the entity, potentially inferred
	 * from other attributes.
	 * </ul>
	 * 
	 * @return the entity list.
	 */
	public List<MultiTreeMap> getEntityList() {
		LinkedList<MultiTreeMap> ret = new LinkedList<>();
		for(String name : getSingleTree(LOCAL_ID_ATTRIBUTE).getHierarchicalNames())
			ret.add(getSingleTree(LOCAL_ID_ATTRIBUTE).getSingleTree(name));
		return ret;
	}
	
	/**
	 * Filters, from an entity list (originally generated by {@link #getEntityList()}), those entities which are in the
	 * context of the entity with the given ID.
	 * 
	 * @param entities
	 *            - a list of entities.
	 * @param contextLocalID
	 *            - the context to filter for.
	 * @return the list of entities which are in the given context.
	 */
	public static List<MultiTreeMap> filterContext(List<MultiTreeMap> entities, String contextLocalID) {
		return filterCategoryInContext(entities, null, contextLocalID);
	}
	
	/**
	 * Filters, from an entity list (originally generated by {@link #getEntityList()}), those entities which are in the
	 * context of the entity with the given ID and which are of a specific category.
	 * 
	 * @param entities
	 *            - a list of entities.
	 * @param categoryName
	 *            - the category of the filtered entities.
	 * @param contextLocalID
	 *            - the context to filter for.
	 * @return the list of entities which are in the given context and of the specified category.
	 */
	public static List<MultiTreeMap> filterCategoryInContext(List<MultiTreeMap> entities, String categoryName,
			String contextLocalID) {
		LinkedList<MultiTreeMap> ret = new LinkedList<>();
		for(MultiTreeMap element : entities)
			if(categoryName == null || element.getSingleValue(CATEGORY_ATTRIBUTE_NAME).equals(categoryName))
				if(contextLocalID == null || element.getValues(CONTEXT_ELEMENT_NAME).contains(contextLocalID))
					ret.add(element);
		return ret;
	}
}
