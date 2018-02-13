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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.CompositeAgentLoader;
import net.xqhs.flash.core.deployment.DeploymentConfiguration.CategoryName;
import net.xqhs.flash.core.support.Support;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.logging.Unit;

/**
 * The Boot singleton class manages the startup of the multi-agent system. It manages settings, it loads the scenario,
 * loads the agent definitions (agents are actually created later).
 * <p>
 * After performing all initializations, it creates a {@link Node} instance that manages the actual simulation.
 * 
 * @author Andrei Olaru
 */
public class NodeLoader extends Unit implements Loader<Node>
{
	{
		setUnitName("boot").setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * @param configuration
	 *            - program arguments, passed as a {@link TreeParameterSet} containing one simple key (the name does not
	 *            matter) that is associated with all the arguments.
	 */
	@Override
	public Node load(TreeParameterSet configuration)
	{
		return load(configuration.getValues(configuration.getSimpleKeys().get(0)));
	}
	
	static String capitalize(String s)
	{
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	protected static String autoFind(ClassFactory factory, List<String> packages, String given_cp, String root_package,
			String upper_name, String lower_name, String thing, List<String> checkedPaths)
	{
		String D = ".";
		checkedPaths.clear();
		if(factory.canLoadClass(given_cp))
			return given_cp;
		for(String p : packages)
			if(factory.canLoadClass(p + D + given_cp))
				return p + D + given_cp;
		if(given_cp != null)
		{
			checkedPaths.add(root_package + D + given_cp);
			checkedPaths.add(root_package + D + upper_name + D + lower_name + D + given_cp);
		}
		else
		{
			List<String> clsNames = new LinkedList<>();
			clsNames.add(capitalize(upper_name) + capitalize(thing));
			if(lower_name != null)
			{
				clsNames.add(capitalize(lower_name) + capitalize(thing));
				clsNames.add(capitalize(lower_name) + capitalize(upper_name) + capitalize(thing));
			}
			String[] roots = new String[] { root_package, root_package + D + DeploymentConfiguration.CORE_PACKAGE };
			for(String cls : clsNames)
				for(String r : roots)
				{
					checkedPaths.add(r + D + upper_name + D + cls);
					if(lower_name != null)
					{
						checkedPaths.add(r + D + upper_name + D + lower_name + D + cls);
						checkedPaths.add(r + D + lower_name + D + upper_name + D + cls);
						checkedPaths.add(r + D + lower_name + D + cls);
					}
				}
		}
		for(String p : checkedPaths)
			if(factory.canLoadClass(p))
				return p;
		return null;
	}
	
	/**
	 * The method handling main functionality of {@link NodeLoader}.
	 * <p>
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 * @return the {@link Node} to manage to deployed system.
	 */
	protected Node load(List<String> args)
	{
		lf("Booting Flash-MAS.");
		
		// load settings & scenario
		DeploymentConfiguration settings = new DeploymentConfiguration();
		TreeParameterSet deployment;
		try
		{
			deployment = settings.loadConfiguration(args, true, null);
		} catch(ConfigLockedException e)
		{
			le("settings were locked (shouldn't ever happen): " + PlatformUtils.printException(e));
			return null;
		}
		//
		// // create window layout
		// WindowLayout.staticLayout = new GridWindowLayout(settings.getLayout());
		//
		// get general configuration info
		TreeParameterSet config = deployment.getTree(CategoryName.CONFIG.getName());
		if(config == null)
			config = new TreeParameterSet();
		// get packages
		List<String> checkedPaths = new LinkedList<>(); // used to monitor class paths checked by autoFind().
		List<String> packages = deployment.getValues(CategoryName.PACKAGE.getName());
		
		// get instance factory.
		ClassFactory classFactory = PlatformUtils.getClassFactory();
		
		// get loaders
		String NAMESEP = DeploymentConfiguration.NAME_SEPARATOR;
		String ROOT_PACKAGE = DeploymentConfiguration.ROOT_PACKAGE;
		
		// thing -> kind -> loaders
		Map<String, Map<String, List<Loader<?>>>> loaders = new HashMap<>();
		TreeParameterSet loader_configs = deployment.getTree(CategoryName.LOADER.getName());
		if(!loader_configs.getSimpleKeys().isEmpty())
			lw("Simple keys from loader tree ignored: ", loader_configs.getSimpleKeys());
		for(String name : loader_configs.getHierarchicalKeys())
		{
			String thing = null, kind = null;
			if(name.contains(NAMESEP))
			{
				thing = name.split(NAMESEP)[0];
				kind = name.split(NAMESEP, 2)[1];
			}
			else
				thing = name;
			if(thing == null || thing.length() == 0)
				le("Loader name parsing failed for []", name);
			
			String cp = loader_configs.getDeepValue(name, "classpath");
			cp = autoFind(classFactory, packages, cp, ROOT_PACKAGE, thing, kind, CategoryName.LOADER.getName(),
					checkedPaths);
			if(cp == null)
				le("Class [] for loader [] can not be loaded; tried packages and paths ",
						loader_configs.getDeepValue(name, "classpath"), name, checkedPaths);
			else
			{
				if(!loaders.containsKey(thing))
					loaders.put(thing, new HashMap<String, List<Loader<?>>>());
				if(!loaders.get(thing).containsKey(kind))
					loaders.get(thing).put(kind, new LinkedList<Loader<?>>());
				try
				{
					loaders.get(thing).get(kind)
							.add((Loader<?>) classFactory.loadClassInstance(cp, loader_configs.getTree(name), false));
					li("Loader for [] of kind [] successfully loaded from [].", thing, kind, cp);
				} catch(Exception e)
				{
					le("Loader loading failed for []: ", name, PlatformUtils.printException(e));
				}
			}
		}
		// default loaders
		
		// default loader for agents
		String AGENT = CategoryName.AGENT.getName();
		String DEFAULT_AGENT_LOADER = CompositeAgentLoader.class.getName();
		
		if(!loaders.containsKey(AGENT))
			loaders.put(AGENT, new HashMap<String, List<Loader<?>>>());
		if(!loaders.get(AGENT).containsKey(null))
		{
			loaders.get(AGENT).put(null, new LinkedList<Loader<?>>());
			try
			{
				loaders.get(AGENT).get(null)
						.add((Loader<?>) classFactory.loadClassInstance(DEFAULT_AGENT_LOADER, null, false));
			} catch(Exception e)
			{
				le("Default [] loader loading failed: ", AGENT, PlatformUtils.printException(e));
			}
		}
		
		// get support infrastructures
		// kind -> id -> instance
		Map<String, Map<String, Support>> support = new HashMap<>();
		String SUPPORT = CategoryName.SUPPORT.getName();
		TreeParameterSet support_configs = deployment.getTree(SUPPORT);
		if(!support_configs.getSimpleKeys().isEmpty())
			lw("Simple keys from support tree ignored: ", support_configs.getSimpleKeys());
		for(String name : support_configs.getHierarchicalKeys())
		{
			String kind = null, id = null, cp = support_configs.getDeepValue(name, "classpath");
			if(name.contains(NAMESEP))
			{
				kind = name.split(NAMESEP)[0];
				id = name.split(NAMESEP, 2)[1];
			}
			else
				kind = name;
			if(kind == null || kind.length() == 0)
				le("Loader name parsing failed for []", name);
			cp = autoFind(classFactory, packages, cp, ROOT_PACKAGE, kind, null, SUPPORT, checkedPaths);
			if(cp == null)
				le("Class [] for support [] can not be loaded; tried packages and paths ",
						loader_configs.getDeepValue(name, "classpath"), name, checkedPaths);
			else
			{
				if(!support.containsKey(kind))
					support.put(kind, new HashMap<String, Support>());
				// TODO create id
				try
				{
					support.get(kind).put(id, (Support) classFactory.loadClassInstance(cp, support_configs.getTree(name), false));
					li("Support for [] with id [] successfully loaded from [].", kind, id, cp);
				} catch(Exception e)
				{
					le("Support loading failed for []: ", name, PlatformUtils.printException(e));
				}
			}
		}
		
		// get agents
		
		// // the name of the default platform
		// String defaultPlatform = Support.DEFAULT_PLATFORM.toString();
		// // the name of the default agent loader
		// String defaultAgentLoader = AgentLoader.DEFAULT_LOADER.toString();
		// // platform name -> platform loader
		// Map<String, Support> platforms = new HashMap<>();
		// // agent loader name -> agent loader
		// Map<String, AgentLoader> agentLoaders = new HashMap<>();
		// // package names where agent code (adf, java & co) may be located
		// List<String> agentPackages = new ArrayList<>();
		// // platform name -> agent name -> agent manager
		// // for the agent to be started in the container, on the platform
		// Set<AgentCreationData> allAgents = new HashSet<>();
		
		// // add agent packages specified in the scenario
		// Iterator<XMLNode> packagePathsIt = scenarioTree.getRoot().getNodeIterator(
		// AgentParameterName.AGENT_PACKAGE.toString());
		// while(packagePathsIt.hasNext())
		// agentPackages.add((String) packagePathsIt.next().getValue());
		//
		// // iterate over platform entries in the scenario
		// defaultPlatform = loadPlatforms(
		// scenarioTree.getRoot().getNodeIterator(AgentParameterName.AGENT_PLATFORM.toString()), settings,
		// platforms, defaultPlatform);
		//
		// // iterate over agent loader entries in the scenario
		// defaultAgentLoader = loadAgentLoaders(
		// scenarioTree.getRoot().getNodeIterator(AgentParameterName.AGENT_LOADER.toString()), agentLoaders,
		// defaultAgentLoader);
		//
		// if(scenarioTree.getRoot().getNodeIterator("initial").hasNext())
		// // iterate containers and find agents
		// loadContainerAgents(scenarioTree.getRoot().getNodeIterator("initial").next().getNodeIterator("container"),
		// defaultPlatform, platforms, defaultAgentLoader, agentLoaders, agentPackages, allContainers,
		// platformContainers, allAgents);
		//
		// // agents prepared, time to start platforms and the containers.
		// if(startPlatforms(platforms, platformContainers) > 0)
		// {
		// // load timeline (if any)
		// XMLNode timeline = null;
		// if(scenarioTree.getRoot().getNodeIterator(SimulationManager.TIMELINE_NODE.toString()).hasNext())
		// timeline = scenarioTree.getRoot().getNodeIterator(SimulationManager.TIMELINE_NODE.toString()).next();
		//
		// // start simulation
		// if(!new SimulationManager(platforms, allContainers, allAgents, timeline).start())
		// {
		// le("Simulation start failed.");
		// for(PlatformLoader platform : platforms.values())
		// if(!platform.stop())
		// le("Stopping platform [" + platform.getName() + "] failed");
		// if(WindowLayout.staticLayout != null)
		// WindowLayout.staticLayout.doexit();
		// }
		// }
		// else
		// le("No agent platforms loaded. Simulation will not start.");
		doExit();
		
		return null;
	}
	
	// /**
	// * Loads the available platform loaders and fills in the {@link Map} of platforms, also returning the default
	// * platform (decided according to the information in the scenario file).
	// * <p>
	// * The available platform loaders will be the ones mentioned in the scenario file. If the name of the platform is
	// * the name of a standard platform (see {@link StandardPlatformType}), the predefined class path will be used;
	// * otherwise, the class path must be present in the scenario.
	// * <p>
	// * If no platforms are specified in the scenario, the default platform {@link StandardPlatformType#DEFAULT} will
	// be
	// * used, and this will be the default platform of the scenario.
	// * <p>
	// * If only one platform is specified in the scenario, this will be the default platform of the scenario.
	// * <p>
	// * If multiple platforms are specified, there will be no default platform (all agents have to specify their
	// * platform).
	// *
	// * TODO: indicate default platform among multiple platforms; have a default per-container platform.
	// *
	// * @param platformNodes
	// * - {@link Iterator} over the nodes in the scenario file describing platforms.
	// * @param settings
	// * - the {@link BootSettingsManager} containing settings set through application arguments or the
	// * <code>config</code> node in the scenario file.
	// * @param platforms
	// * - map in which to fill in the names of the platforms and the respective {@link PlatformLoader}
	// * instances.
	// * @param defaultPlatformSuggested
	// * - default platform as suggested by Boot.
	// * @return the name of the default platform loader (which will be present in parameter <code>platforms</code>).
	// */
	// protected String loadPlatforms(Iterator<XMLNode> platformNodes, BootSettingsManager settings,
	// Map<String, PlatformLoader> platforms, String defaultPlatformSuggested)
	// {
	// while(platformNodes.hasNext())
	// {
	// XMLNode platformNode = platformNodes.next();
	// String platformName = PlatformUtils.getParameterValue(platformNode, PlatformLoader.NAME_ATTRIBUTE);
	// if(platformName == null)
	// le("Platform name is null.");
	// else if(platforms.containsKey(platformName))
	// le("Platform [" + platformName + "] already defined.");
	// else
	// {
	// String platformClassPath = null;
	// try
	// {
	// platformClassPath = StandardPlatformType.valueOf(platformName.toUpperCase()).getClassName();
	// } catch(IllegalArgumentException e)
	// { // platform is not standard
	// platformClassPath = PlatformUtils.getParameterValue(platformNode,
	// PlatformLoader.CLASSPATH_ATTRIBUTE);
	// if(platformClassPath == null)
	// le("Class path for platform [" + platformName + "] is not known.");
	// }
	// if(platformClassPath != null)
	// try
	// {
	// platforms.put(platformName, ((PlatformLoader) PlatformUtils.loadClassInstance(this,
	// platformClassPath, new Object[0])).setConfig(platformNode, settings));
	// log.info("Platform [" + platformName + "] prepared.");
	// } catch(Exception e)
	// {
	// le("Loading platform [" + platformName + "] failed; platform will not be available:"
	// + PlatformUtils.printException(e));
	// }
	// }
	// }
	// // default platform
	// if(platforms.isEmpty())
	// {
	// // load default platform
	// //StandardPlatformType platform = StandardPlatformType.DEFAULT;
	// StandardPlatformType platform = StandardPlatformType.DEFAULT;
	// try
	// {
	// platforms
	// .put(platform.toString(), ((PlatformLoader) PlatformUtils.loadClassInstance(this,
	// platform.getClassName(), new Object[0])));
	// log.info("Default platform [" + platform.toString() + "] prepared.");
	// } catch(Exception e)
	// {
	// le("Loading platform [" + platform.toString() + "] failed; platform will not be available:"
	// + PlatformUtils.printException(e));
	// }
	// }
	//
	// String defaultPlatform = null;
	// if(platforms.size() == 1)
	// defaultPlatform = platforms.values().iterator().next().getName();
	// lf("Default platform is [" + defaultPlatform + "].");
	// return (defaultPlatform != null) ? defaultPlatform : defaultPlatformSuggested;
	// }
	//
	// /**
	// * Loads the available agent loaders and fills in the {@link Map} of agent loaders. Event if not defined
	// explicitly
	// * in the scenario file (which is possible), all loaders in {@link StandardAgentLoaderType} are also loaded.
	// *
	// * @param loaderNodes
	// * - {@link Iterator} over the nodes in the scenario file describing agent loaders.
	// * @param agentLoaders
	// * - map in which to fill in the names of the agent loaders and the respective {@link AgentLoader}
	// * instances.
	// * @param defaultLoaderSuggested
	// * - default agent loader as suggested by Boot.
	// * @return the name of the default agent loader (which will be present in parameter <code>agentLoaders</code>).
	// */
	// protected String loadAgentLoaders(Iterator<XMLNode> loaderNodes, Map<String, AgentLoader> agentLoaders,
	// String defaultLoaderSuggested)
	// {
	// while(loaderNodes.hasNext())
	// {
	// XMLNode loaderNode = loaderNodes.next();
	// String loaderName = PlatformUtils.getParameterValue(loaderNode, AgentLoader.NAME_ATTRIBUTE);
	// if(loaderName == null)
	// le("Agent loader name is null.");
	// else if(agentLoaders.containsKey(loaderName))
	// le("Agent loader [" + loaderName + "] already defined.");
	// else
	// {
	// String loaderClassPath = null;
	// try
	// {
	// loaderClassPath = StandardAgentLoaderType.valueOf(loaderName.toUpperCase()).getClassName();
	// } catch(IllegalArgumentException e)
	// { // agent loader is not standard
	// loaderClassPath = PlatformUtils.getParameterValue(loaderNode, AgentLoader.CLASSPATH_ATTRIBUTE);
	// if(loaderClassPath == null)
	// le("Class path for agent loader [" + loaderName + "] is not known.");
	// }
	// if(loaderClassPath != null)
	// try
	// {
	// agentLoaders.put(loaderName, ((AgentLoader) PlatformUtils.loadClassInstance(this,
	// loaderClassPath, new Object[0])).setConfig(loaderNode));
	// log.info("Agent loader [" + loaderName + "] prepared.");
	// } catch(Exception e)
	// {
	// le("Loading agent loader [" + loaderName + "] failed; loader will not be available: "
	// + PlatformUtils.printException(e));
	// }
	// }
	// }
	//
	// // add standard agent loaders (except if they have already been specified and configured explicitly.
	//
	// for(StandardAgentLoaderType loader : StandardAgentLoaderType.values())
	// if(!agentLoaders.containsKey(loader.toString()) && (loader.getClassName() != null))
	// try
	// {
	// agentLoaders.put(loader.toString(),
	// (AgentLoader) PlatformUtils.loadClassInstance(this, loader.getClassName()));
	// log.info("Agent loader [" + loader.toString() + "] prepared.");
	// } catch(Exception e)
	// {
	// le("Loading agent loader [" + loader.toString() + "] failed; loader will not be available: "
	// + PlatformUtils.printException(e));
	// }
	// String defaultLoader = null;
	// if(agentLoaders.size() == 1)
	// defaultLoader = agentLoaders.values().iterator().next().getName();
	// lf("Default agent loader is [" + defaultLoader + "].");
	// return (defaultLoader != null) ? defaultLoader : defaultLoaderSuggested;
	// }
	//
	// /**
	// * Loads container and agent information from the scenario file. Based on the first 5 arguments, the method will
	// * fill in the information in the last 3 arguments.
	// *
	// * @param containerNodes
	// * - {@link Iterator} over the nodes in the scenario file describing containers (and, inside, agents).
	// * @param defaultPlatform
	// * - the name of the default platform.
	// * @param platforms
	// * - the {@link Map} of platform names and respective {@link PlatformLoader} instances.
	// * @param defaultAgentLoader
	// * - the name of the default agent loader.
	// * @param agentLoaders
	// * - the {@link Map} of platform names and respective {@link AgentLoader} instances.
	// * @param agentPackages
	// * - the {@link Set} of package names where agent code may be located.
	// * @param allContainers
	// * - the {@link Map} in which the method will fill in all containers, specifying the name and whether the
	// * container should be created.
	// * @param platformContainers
	// * - the {@link Map} in which the method will fill in the containers to load on the local machine, for
	// * each platform (the map contains: platform name &rarr; set of containers to load).
	// * @param allAgents
	// * - the {@link Set} in which the method will fill in the {@link AgentCreationData} instances for all
	// * agents.
	// */
	// protected void loadContainerAgents(Iterator<XMLNode> containerNodes, String defaultPlatform,
	// Map<String, PlatformLoader> platforms, String defaultAgentLoader, Map<String, AgentLoader> agentLoaders,
	// List<String> agentPackages, Map<String, Boolean> allContainers, Map<String, Set<String>> platformContainers,
	// Set<AgentCreationData> allAgents)
	// {
	// while(containerNodes.hasNext())
	// {
	// XMLNode containerConfig = containerNodes.next();
	//
	// // container information
	// String containerName = containerConfig.getAttributeValue("name");
	// boolean doCreateContainer = (containerConfig.getAttributeValue("create") == null)
	// || containerConfig.getAttributeValue("create").equals(new Boolean(true));
	// allContainers.put(containerName, new Boolean(doCreateContainer));
	//
	// // container has no agents, but should be created in said platform
	// if(doCreateContainer && !containerConfig.getNodeIterator("agent").hasNext())
	// {
	// String platformName = containerConfig.getAttributeValue("platform");
	// if((platformName != null) && platforms.containsKey(platformName))
	// {
	// if(!platformContainers.containsKey(platformName))
	// platformContainers.put(platformName, new HashSet<String>());
	// platformContainers.get(platformName).add(containerName);
	// }
	// }
	//
	// // set up creation for all agents in the container
	// for(Iterator<XMLNode> agentNodes = containerConfig.getNodeIterator("agent"); agentNodes.hasNext();)
	// {
	// XMLNode agentNode = agentNodes.next();
	// // agent name
	// String agentName = PlatformUtils.getParameterValue(agentNode, AgentParameterName.AGENT_NAME.toString());
	// if(agentName == null)
	// {
	// le("agent has no name; will not be created.");
	// continue;
	// }
	// // platform
	// String platformName = PlatformUtils.getParameterValue(agentNode,
	// AgentParameterName.AGENT_PLATFORM.toString());
	// if(platformName == null)
	// platformName = defaultPlatform; // no platform specified: go to default
	// if(!platforms.containsKey(platformName))
	// {
	// le("unknown platform [" + platformName + "]; agent [" + agentName + "] will not be created.");
	// continue;
	// }
	//
	// // load agent
	// AgentCreationData agentCreationData = preloadAgent(agentNode, agentName, containerName,
	// doCreateContainer, platforms.get(platformName), defaultAgentLoader, agentLoaders, agentPackages);
	// if(agentCreationData == null)
	// continue;
	// allAgents.add(agentCreationData);
	//
	// // associate container with the platform
	// if(doCreateContainer)
	// {
	// if(!platformContainers.containsKey(platformName))
	// platformContainers.put(platformName, new HashSet<String>());
	// platformContainers.get(platformName).add(containerName);
	// lf("Agent [" + agentName + "] will be run on platform [" + platformName
	// + "], in local container [" + containerName + "]");
	// }
	// else
	// lf("Agent [" + agentName + "] will be run on platform [" + platformName
	// + "], in remote container [" + containerName + "]");
	// }
	// }
	// }
	//
	// /**
	// * Loads agent information from the scenario file and pre-loads the agent using the appropriate {@link
	// AgentLoader}.
	// * <p>
	// * If successful, the method returns an {@link AgentCreationData} instance that can be subsequently be used in a
	// * call to {@link AgentLoader#load(AgentCreationData)} to obtain an {@link AgentManager} instance.
	// *
	// * @param agentNode
	// * - the {@link XMLNode} containing the information about the agent.
	// * @param agentName
	// * - the name of the agent, already determined by the caller.
	// * @param containerName
	// * - the name of the container the agent will reside in.
	// * @param doCreateContainer
	// * - <code>true</code> if the container is local, <code>false</code> if remote.
	// * @param platform
	// * - the platform loader for the platform the agent will execute on.
	// * @param defaultAgentLoader
	// * - the name of the default agent loader.
	// * @param agentLoaders
	// * - the {@link Map} of agent loader names and respective {@link AgentLoader} instances.
	// * @param agentPackages
	// * - the {@link Set} of packages containing agent code.
	// * @return an {@link AgentManager} instance that can be used to control the lifecycle of the just loaded agent, if
	// * the loading was successful; <code>null</code> otherwise.
	// */
	// protected AgentCreationData preloadAgent(XMLNode agentNode, String agentName, String containerName,
	// boolean doCreateContainer, PlatformLoader platform, String defaultAgentLoader,
	// Map<String, AgentLoader> agentLoaders, List<String> agentPackages)
	// {
	// // loader
	// String agentLoaderName = PlatformUtils.getParameterValue(agentNode, AgentParameterName.AGENT_LOADER.toString());
	// if(agentLoaderName == null)
	// agentLoaderName = defaultAgentLoader;
	// if(!agentLoaders.containsKey(agentLoaderName))
	// return (AgentCreationData) log.lr(null, "agent loader [" + agentLoaderName + "] is unknown. agent ["
	// + agentName + "] will not be created.");
	// AgentLoader loader = agentLoaders.get(agentLoaderName);
	//
	// // get all parameters and put them into an AgentParameters instance.
	// AgentParameters parameters = new AgentParameters();
	// for(Iterator<XMLNode> paramIt = agentNode.getNodeIterator("parameter"); paramIt.hasNext();)
	// {
	// XMLNode param = paramIt.next();
	// AgentParameterName parName = AgentParameterName.getName(param.getAttributeValue("name"));
	// if(parName != null)
	// parameters.add(parName, param.getAttributeValue("value"));
	// else
	// {
	// lf("adding unregistered parameter [" + param.getAttributeValue("name") + "].");
	// parameters.add(param.getAttributeValue("name"), param.getAttributeValue("value"));
	// }
	// }
	// for(String pack : agentPackages)
	// parameters.add(AgentParameterName.AGENT_PACKAGE, pack);
	//
	// AgentCreationData agentCreationData = new AgentCreationData(agentName, parameters, agentPackages,
	// containerName, !doCreateContainer, platform.getName(), loader, agentNode);
	// if(!loader.preload(agentCreationData, platform, log))
	// {
	// le("Agent [" + agentName + "] cannot be loaded.");
	// return null;
	// }
	// return agentCreationData;
	// }
	//
	// /**
	// * The method starts the platforms specified in the first parameter and adds to each platform the containers
	// * corresponding to it, as indicated by the second parameter.
	// *
	// * @param platforms
	// * - the {@link Map} of platform names and respective {@link PlatformLoader} instances.
	// * @param platformContainers
	// * - the {@link Map} containing platform name &rarr; {@link Set} of the names of the containers to add to
	// * the platform.
	// * @return the number of platforms successfully started.
	// */
	// protected int startPlatforms(Map<String, PlatformLoader> platforms, Map<String, Set<String>> platformContainers)
	// {
	// int platformsOK = 0;
	// for(Iterator<PlatformLoader> itP = platforms.values().iterator(); itP.hasNext();)
	// {
	// PlatformLoader platform = itP.next();
	// String platformName = platform.getName();
	// if(!platform.start())
	// {
	// le("Platform [" + platformName + "] failed to start.");
	// itP.remove();
	// continue;
	// }
	// log.info("Platform [" + platformName + "] started.");
	// platformsOK++;
	// if(platformContainers.containsKey(platformName))
	// for(Iterator<String> itC = platformContainers.get(platformName).iterator(); itC.hasNext();)
	// {
	// String containerName = itC.next();
	// if(!platform.addContainer(containerName))
	// {
	// le("Adding container [" + containerName + "] to [" + platformName + "] has failed.");
	// itC.remove();
	// }
	// else
	// log.info("Container [" + containerName + "] added to [" + platformName + "].");
	// }
	// }
	// return platformsOK;
	// }
	
}
