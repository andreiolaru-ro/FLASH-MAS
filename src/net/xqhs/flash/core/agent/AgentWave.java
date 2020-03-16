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
	/**
	 * The serial UID.
	 */
	private static final long	serialVersionUID		= 8405841078556036608L;
	
	/**
	 * The string separating elements of an endpoint address.
	 */
	public static final String	ADDRESS_SEPARATOR		= "/";
	
	public final String			CONTENT					= "content";
	public final String			SOURCE_ELEMENT			= "source-element";
	public final String			COMPLETE_DESTINATION	= "destination-complete";
	public final String			DESTINATION_ELEMENT		= "destination-element";
	
	public AgentWave(String content, String destinationRoot, String... destinationElements)
	{
		super(AgentEventType.AGENT_WAVE);
		add(CONTENT, content);
		resetDestination(destinationRoot, destinationElements);
	}
	
	public AgentWave setSourceElements(String... sourceElements)
	{
		for(String elem : sourceElements)
			if(elem.length() > 0)
				add(SOURCE_ELEMENT, elem);
		return this;
	}
	
	public AgentWave addSourceElementFirst(String sourceElement)
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
	
	public String getFirstDestination()
	{
		return getValue(DESTINATION_ELEMENT);
	}
	
	public AgentWave resetDestination(String destination, String... destinationElements)
	{
		if(isSet(COMPLETE_DESTINATION))
			removeKey(COMPLETE_DESTINATION);
		String join = String.join(ADDRESS_SEPARATOR, destinationElements);
		add(COMPLETE_DESTINATION, destination + (join.length() > 0 ? ADDRESS_SEPARATOR + join : ""));
		if(isSet(DESTINATION_ELEMENT))
			removeKey(DESTINATION_ELEMENT);
		add(DESTINATION_ELEMENT, destination);
		addAll(DESTINATION_ELEMENT, Arrays.asList(destinationElements));
		return this;
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
	 * be separated by what is the value of the {@link #ADDRESS_SEPARATOR}.
	 * <p>
	 * Elements that are <code>null</code> will not be assembled in the path.
	 * <p>
	 * If the start is <code>null</code> the result will begin with a slash.
	 * 
	 * @param start
	 *                     - start of the address. Special cases:
	 *                     <ul>
	 *                     <li><code>null</code> - the path will start with the first element.
	 *                     <li>{@link #ADDRESS_SEPARATOR} - the path will start with {@value #ADDRESS_SEPARATOR},
	 *                     followed by the first element
	 *                     </ul>
	 * @param elements
	 *                     - other elements in the address
	 * @return the resulting address.
	 */
	public static String makePath(String start, String... elements)
	{
		String ret = (start != null) ? start : "";
		for(String elem : elements)
			if(elem != null && elem.length() > 0)
				ret += ADDRESS_SEPARATOR + elem;
		return ret;
	}
	
	public static String[] pathToElements(String path, String prefixToRemove)
	{
		String barePath = path.startsWith(prefixToRemove) ? path.substring(prefixToRemove.length()) : path;
		return (barePath.startsWith(ADDRESS_SEPARATOR) ? barePath.substring(1) : barePath).split(ADDRESS_SEPARATOR);
	}
	
	public static String[] pathToElementsWith(String path, String prefix)
	{
		
		String barePath = path.startsWith(prefix) ? path.substring(prefix.length()) : path;
		String[] elements = (barePath.startsWith(ADDRESS_SEPARATOR) ? barePath.substring(1) : barePath)
				.split(ADDRESS_SEPARATOR);
		String[] ret = new String[elements.length + 1];
		ret[0] = prefix;
		for(int i = 0; i < elements.length; i++)
			ret[i + 1] = elements[i];
		return ret;
	}
}
