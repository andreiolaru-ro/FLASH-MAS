package net.xqhs.flash.core.agent;

import java.util.Arrays;

import net.xqhs.flash.core.util.MultiValueMap;

/**
 * The <i>wave</i> is a type of event that conveys information between two entities, whether these entities are agents,
 * shards inside different agents, or shards inside the same agent.
 * <p>
 * A <i>wave</i> has a source and one or more possible destinations, all of which are <i>endpoints</i>, as well as a
 * content and any number of other properties. Since {@link AgentWave} extends {@link MultiValueMap}, all properties are
 * accessible via their keys.
 * 
 * In order to facilitate routing waves, we define the following elements of a wave (for which there are methods
 * defined):
 * <ul>
 * <li>the content - property {@link #CONTENT}. The main content of the wave.
 * <li>path elements of the source endpoint - values of the property {@link #SOURCE_ELEMENT}.
 * <li>the complete source, representing the complete source endpoint as generated from the path elements.
 * <li>the destinations, as complete endpoints - values of the property {@link #COMPLETE_DESTINATION}.
 * <ul>
 * <li>The intention is that this values should remain unchanged throughout the lifetime of the wave.
 * </ul>
 * <li>path elements of a destination - values of the property {@link #DESTINATION_ELEMENT}.
 * <ul>
 * <li>It may be useful that routers of the wave change this property in order to remove the elements that have already
 * been considered in the routing, leaving the next element to be processed by the next router.
 * </ul>
 * </ul>
 * 
 * 
 * @author Andrei Olaru
 */
public class AgentWave extends AgentEvent
{
	public final String	CONTENT					= "content";
	public final String	SOURCE_ELEMENT			= "source-element";
	public final String	COMPLETE_DESTINATION	= "source-complete";
	public final String	DESTINATION_ELEMENT		= "source-element";
	/**
	 * The string separating elements of an endpoint address.
	 */
	public static final String	ADDRESS_SEPARATOR		= "/";
	
	public AgentWave(String content, String destination, String... destinationElements)
	{
		super(AgentEventType.AGENT_WAVE);
		add(CONTENT, content);
		add(COMPLETE_DESTINATION,
				destination + (destinationElements.length > 0
						? ADDRESS_SEPARATOR + String.join(ADDRESS_SEPARATOR, destinationElements)
						: ""));
	}
	
	public AgentWave setSource(String... sourceElements)
	{
		addAll(SOURCE_ELEMENT, Arrays.asList(sourceElements));
		return this;
	}
	
	public AgentWave addSource(String sourceElement)
	{
		addFirst(SOURCE_ELEMENT, sourceElement);
		return this;
	}
	
	public String getFirstSource()
	{
		return getValue(SOURCE_ELEMENT);
	}
	
	public String getCompleteSource()
	{
		return String.join(ADDRESS_SEPARATOR, getValues(SOURCE_ELEMENT));
	}
	
	public String getCompleteDestination()
	{
		return getValue(COMPLETE_DESTINATION);
	}
	
	public String getNextDestination()
	{
		return getValue(DESTINATION_ELEMENT);
	}
	
	public void removeFirstDestinationElement()
	{
		removeFirst(DESTINATION_ELEMENT);
	}
	
	public String getContent()
	{
		return getValue(CONTENT);
	}
	
	/**
	 * Produces an endpoint description by assembling the start of the address with the rest of the elements. They will
	 * be separated by the address separator specified as constant.
	 * <p>
	 * Elements that are <code>null</code> will not be assembled in the path.
	 * <p>
	 * If the start is <code>null</code> the result will begin with a slash.
	 * 
	 * @param start
	 *                     - start of the address.
	 * @param elements
	 *                     - other elements in the address
	 * @return the resulting address.
	 */
	public static String makePathHelper(String start, String... elements)
	{
		String ret = (start != null) ? start : "";
		for(String elem : elements)
			if(elem != null)
				ret += ADDRESS_SEPARATOR + elem;
		return ret;
	}
}
