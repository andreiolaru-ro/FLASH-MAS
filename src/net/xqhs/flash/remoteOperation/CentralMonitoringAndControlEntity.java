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
package net.xqhs.flash.remoteOperation;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.Operation;
import net.xqhs.flash.core.util.Operation.BaseOperation;
import net.xqhs.flash.core.util.Operation.Field;
import net.xqhs.flash.core.util.Operation.OperationName;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.gui.GUILoad;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.web.WebEntity;

/**
 * This class is used to monitor and control the MAS.
 */
public class CentralMonitoringAndControlEntity extends EntityCore<Pylon> {
	
	/**
	 * Operations supported by the {@link CentralMonitoringAndControlEntity}.
	 */
	@SuppressWarnings("hiding") // this is necessary so that the same name is used for Operation instances and
								// Operations instances.
	public enum Operations implements OperationName {
		UPDATE_ENTITY_STATUS,
		
		REGISTER_ENTITIES,
		
		UPDATE_ENTITY_GUI,
		
		ENTITY_GUI_OUTPUT,
		
		GUI_INPUT_TO_ENTITY,
		
		;
		
		/**
		 * If the first destination element is one of the supported operations, return it. Otherwise, return null.
		 * 
		 * @param wave
		 * @return the operation, or null if the first destination element is not a supported operation.
		 */
		public static Operations getRoute(AgentWave wave) {
			try {
				return valueOf(wave.getFirstDestinationElement().toUpperCase());
			} catch(Exception e) {
				return null;
			}
		}
	}
	
	public enum Fields implements Field {
		SPECIFICATION, RUNNING_STATUS, APPLICATION_STATUS, RUNNING_STATUS_RUNNING, RUNNING_STATUS_STOPPED, APPLICATION_STATUS_RUNNING, APPLICATION_STATUS_STOPPED, STATUS_UNKNOWN
	}
	
	public static final Operation	UPDATE_ENTITY_STATUS	= new BaseOperation(Operations.UPDATE_ENTITY_STATUS,
			Fields.RUNNING_STATUS, Fields.APPLICATION_STATUS);
	public static final Operation	UPDATE_ENTITY_GUI		= new BaseOperation(Operations.UPDATE_ENTITY_GUI,
			Fields.SPECIFICATION);
	public static final Operation	REGISTER_ENTITIES		= new BaseOperation(Operations.REGISTER_ENTITIES,
			(String[]) null);
	public static final Operation	ENTITY_GUI_OUTPUT		= new BaseOperation(Operations.ENTITY_GUI_OUTPUT,
			(String[]) null);
	public static final Operation	GUI_INPUT_TO_ENTITY		= new BaseOperation(Operations.GUI_INPUT_TO_ENTITY,
			(String[]) null);
	
	protected class EntityData {
		String	entityName;
		String	status;
		boolean	registered	= false;
		Element	guiSpecification;
		
		public String getName() {
			return entityName;
		}
		
		public String getStatus() {
			return status;
		}
		
		public Element getGuiSpecification() {
			return guiSpecification;
		}
		
		public EntityData setName(String name) {
			this.entityName = name;
			return this;
		}
		
		public EntityData setStatus(String status) {
			this.status = status;
			return this;
		}
		
		public EntityData setGuiSpecification(Element guiSpecification) {
			this.guiSpecification = guiSpecification;
			return this;
		}
		
		public EntityData insertNewGuiElements(List<Element> elements) {
			int i = 0;
			List<Element> interfaceElements = guiSpecification.getChildren();
			for(Element e : elements) {
				boolean found = false;
				for(Element ie : interfaceElements)
					if(e.getPort().equals(ie.getPort()))
						found = true;
				if(!found)
					interfaceElements.add(i++, e);
			}
			return this;
		}
	}
	
	/**
	 * The central entity is a singleton.
	 */
	public class CentralEntityProxy implements ShardContainer {
		@Override
		public AgentShard getAgentShard(AgentShardDesignation designation) {
			return null;
		}
		
		@Override
		public String getEntityName() {
			return getName();
		}
		
		/**
		 * This is expected to be called by the messaging shard.
		 */
		@Override
		public boolean postAgentEvent(AgentEvent event) {
			switch(event.getType()) {
			case AGENT_WAVE:
				return processWave((AgentWave) event);
			default:
				centralMessagingShard.signalAgentEvent(event);
				return false;
			}
		}
	}
	
	{
		setUnitName("M&C");
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * Use this in conjunction with {@link DeploymentConfiguration#CENTRAL_NODE_KEY} to switch on the web interface.
	 */
	public static final String		WEB_INTERFACE_SWITCH	= "web";
	/**
	 * Use this in conjunction with {@link DeploymentConfiguration#CENTRAL_NODE_KEY} to switch on the swing interface.
	 */
	public static final String		SWING_INTERFACE_SWITCH	= "swing";
	/**
	 * The default port for the web interface.
	 */
	public static final int			WEB_INTERFACE_PORT		= 8080;
	/**
	 * Endpoint element for this shard.
	 */
	protected static final String	ENTITY_STATUS_ELEMENT	= "standard-status";
	protected static final String	ENTITY_LABEL_ELEMENT	= "standard-name";
	/**
	 * File for configuring the default controls for entities.
	 */
	protected static final String	DEFAULT_CONTROLS		= "controls.yml";
	
	/**
	 * Standard controls for all entities.
	 */
	protected Element										standardCtrls;
	/**
	 * The proxy to this entity.
	 */
	public ShardContainer									proxy;
	/**
	 * Messaging shard for this entity.
	 */
	private MessagingShard									centralMessagingShard;
	/**
	 * The GUI for controlling the deployment.
	 */
	private CentralGUI										gui;
	/**
	 * Data for entities.
	 */
	protected Map<String, EntityData>						entitiesData	= new HashMap<>();
	/**
	 * Keeps track of all nodes deployed in the system, along with their {@link List} of entities, indexed by their
	 * categories and names.
	 */
	private HashMap<String, HashMap<String, List<String>>>	allNodeEntities	= new LinkedHashMap<>();
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		this.setUnitName("M&C");
		// this.setHighlighted();
		proxy = new CentralEntityProxy();
		standardCtrls = GUILoad.load(new MultiTreeMap().addOneValue(GUILoad.FILE_SOURCE_PARAMETER, DEFAULT_CONTROLS)
				.addOneValue(CategoryName.PACKAGE.s(), this.getClass().getPackage().getName()), getLogger());
		
		for(String iface : configuration.getValues(DeploymentConfiguration.CENTRAL_NODE_KEY))
			switch(iface) {
			case WEB_INTERFACE_SWITCH: {
				// TODO mock config -- to be added in deployment configuration?
				gui = new WebEntity(WEB_INTERFACE_PORT); // maybe TODO: move this port to a configuration file
				gui.addContext(proxy);
				if(gui.start()) // starts now in order to be available before starting entities
					li("web gui started");
				break;
			}
			case SWING_INTERFACE_SWITCH: {
				// TODO Swing GUI
				// gui = new GUIBoard(new CentralEntityProxy());
				// SwingUtilities.invokeLater(() -> {
				// try {
				// gui.setVisible(true);
				// } catch (RuntimeException e) {
				// e.printStackTrace();
				// }
				// });
				break;
			}
			default:
				return ler(false, "unknown central GUI type");
			}
		return true;
	}
	
	/**
	 * Parses the received wave and calls the appropriate method.
	 *
	 * @param wave
	 *            - the {@link AgentWave} to be parsed
	 * 			
	 * @return - an indication of success
	 */
	public boolean processWave(AgentWave wave) {
		lf("Routing wave", wave);
		String sourceEntity = wave.getFirstSource();
		Operations op = Operations.getRoute(wave);
		if(op == null)
			return ler(false, "Unknown operation [] from [].", wave.getFirstDestinationElement(),
					wave.getCompleteSource());
		switch(op) {
		case REGISTER_ENTITIES:
			String node = sourceEntity;
			if(!allNodeEntities.containsKey(node))
				allNodeEntities.put(node, new LinkedHashMap<>());
			for(String entityName : wave.getContentElements()) {
				String category = wave.get(entityName);
				if(!allNodeEntities.get(node).containsKey(category))
					allNodeEntities.get(node).put(category, new LinkedList<>());
				allNodeEntities.get(node).get(category).add(entityName);
				
				if(!entitiesData.containsKey(entityName))
					entitiesData.put(entityName, new EntityData().setName(entityName));
				EntityData ed = entitiesData.get(entityName);
				ed.registered = true;
				if(ed.getStatus() == null)
					ed.setStatus(Fields.STATUS_UNKNOWN.name());
				if(ed.getGuiSpecification() == null)
					ed.setGuiSpecification(setupStandardControls(ed.getStatus(), entityName));
				li("Registered entity []/[] in []", category, entityName, node);
				gui.updateGui(entityName, entitiesData.get(entityName).getGuiSpecification());
			}
			return true;
		case UPDATE_ENTITY_STATUS:
			String output = wave.getContentElements().stream()
					.map(key -> key + ": " + wave.getObject(key, "null").toString()).collect(Collectors.joining("|"));
			if(!entitiesData.containsKey(sourceEntity) || !entitiesData.get(sourceEntity).registered)
				lw("Entity [] not yet registered when [].", sourceEntity, op);
			entitiesData.computeIfAbsent(sourceEntity, (k) -> new EntityData().setName(sourceEntity)).setStatus(output);
			li("Status update for []: []", sourceEntity, output);
			return gui.sendOutput(new AgentWave(output, sourceEntity, ENTITY_STATUS_ELEMENT));
		case UPDATE_ENTITY_GUI:
			Element interfaceStructure = (Element) wave.getObject(Fields.SPECIFICATION.name());
			Element interfaceContainer = new Element();
			if(interfaceStructure != null)
				// must avoid adding it twice
				for(Element child : interfaceStructure.getChildren())
					if(!interfaceContainer.getChildren().contains(child))
						interfaceContainer.addChild(child);
			if(!entitiesData.containsKey(sourceEntity) || !entitiesData.get(sourceEntity).registered)
				lw("Entity [] not yet registered when [].", sourceEntity, op);
			else
				interfaceContainer.addAllChildren(setupStandardControls(entitiesData.get(sourceEntity).getStatus(),
						entitiesData.get(sourceEntity).getName()).getChildren());
			entitiesData.computeIfAbsent(sourceEntity, (k) -> new EntityData().setName(sourceEntity))
					.setGuiSpecification(interfaceContainer);
			lf("Interface of [] reset to:", sourceEntity, interfaceContainer);
			return gui.updateGui(sourceEntity, interfaceContainer);
		case ENTITY_GUI_OUTPUT:
			// remove the name of Central; add the entity sending the output
			// TODO remove this once status update works
			// EntityData ed = entitiesData.get(sourceEntity);
			// if(ed.getStatus() != null)
			// gui.sendOutput(new AgentWave(ed.getStatus(), ed.getName(), ENTITY_STATUS_ELEMENT));
			return gui.sendOutput(wave.removeFirstDestinationElement().prependDestination(sourceEntity)
					.recomputeCompleteDestination());
		case GUI_INPUT_TO_ENTITY:
			li("GUI input to entity []: []", sourceEntity, wave.toString());
			return centralMessagingShard.sendMessage(wave.removeFirstDestinationElement().recomputeCompleteDestination()
					.addSourceElementFirst(getName()));
		default:
			lw("Unhandled operation [] from [].", wave.getFirstDestinationElement(), wave.getCompleteSource());
			return false;
		}
	}
	
	protected Element setupStandardControls(String status, String entityName) {
		Element element = (Element) standardCtrls.clone();
		element.getChildren(ENTITY_LABEL_ELEMENT).get(0).setValue(entityName);
		element.getChildren(ENTITY_STATUS_ELEMENT).get(0).setValue(status);
		return element;
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		if(centralMessagingShard == null) {
			le("[] unable to start. No messaging shard found.", getName());
			return false;
		}
		centralMessagingShard.register(name);
		li("[] started successfully.", getName());
		return true;
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		PylonProxy pylonProxy = (PylonProxy) context;
		String recommendedShard = pylonProxy.getRecommendedShardImplementation(
				AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING));
		try {
			centralMessagingShard = (MessagingShard) PlatformUtils.getClassFactory().loadClassInstance(recommendedShard,
					null, true);
		} catch(ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
		centralMessagingShard.addContext(proxy);
		return centralMessagingShard.addGeneralContext(context);
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return addContext((MessagingPylonProxy) context);
	}
	
}
