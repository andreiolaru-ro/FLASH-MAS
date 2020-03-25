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
 * <li>There may be multiple destinations for a wave.
 * </ul>
 * <li>path elements of a destination - values of the property {@link #DESTINATION_ELEMENT}.
 * <ul>
 * <li>It may be useful that routers of the wave change this property in order to remove the elements that have already
 * been considered in the routing, leaving the next element to be processed by the next router.
 * <li>It is also up to routers to manage the elements in the case of multiple destinations, as the
 * {@link #DESTINATION_ELEMENT} name can only be associated with the elements of a single "complete" destination.
 * </ul>
 * </ul>
 * 
 * TODO: add functionality to support multiple destinations. TODO: add functionality to support multiple content
 * elements?
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
	
	/**
	 * The name associated with the content.
	 */
	public final String			CONTENT					= "content";
	/**
	 * The name associated with the elements of the source endpoint.
	 */
	public final String			SOURCE_ELEMENT			= "source-element";
	/**
	 * The name associated with the complete destination(s), in its(their) original form.
	 */
	public final String			COMPLETE_DESTINATION	= "destination-complete";
	/**
	 * The name associated with the elements of one of the destinations.
	 */
	public final String			DESTINATION_ELEMENT		= "destination-element";
	
	/**
	 * Creates an agent wave with a <b>single</b> destination.
	 * <p>
	 * A <i>complete</i> destination will be added by assembling the elements of the destination..
	 * 
	 * @param content
	 *                                - the content of the wave.
	 * @param destinationRoot
	 *                                - the first element of the destination endpoint.
	 * @param destinationElements
	 *                                - other elements of the destination endpoint.
	 */
	public AgentWave(String content, String destinationRoot, String... destinationElements)
	{
		super(AgentEventType.AGENT_WAVE);
		add(CONTENT, content);
		resetDestination(destinationRoot, destinationElements);
	}
	
	/**
	 * Appends elements to the list of source endpoint elements.
	 * 
	 * @param sourceElements
	 *                           - endpoint elements to add.
	 * @return the wave itself.
	 */
	public AgentWave setSourceElements(String... sourceElements)
	{
		for(String elem : sourceElements)
			if(elem.length() > 0)
				add(SOURCE_ELEMENT, elem);
		return this;
	}
	
	/**
	 * /** Insert an element at the beginning of the list of source endpoint elements.
	 * 
	 * @param sourceElement
	 *                          - the element that will be the first source endpoint element.
	 * @return the wave itself.
	 */
	public AgentWave addSourceElementFirst(String sourceElement)
	{
		addFirst(SOURCE_ELEMENT, sourceElement);
		return this;
	}
	
	/**
	 * @return the first element of the source endpoint.
	 */
	public String getFirstSource()
	{
		return getValue(SOURCE_ELEMENT);
	}
	
	/**
	 * @return the complete source endpoint, obtained by joining the individual elements with the
	 *         {@link #ADDRESS_SEPARATOR}.
	 */
	public String getCompleteSource()
	{
		return String.join(ADDRESS_SEPARATOR, getValues(SOURCE_ELEMENT));
	}
	
	/**
	 * @return the first (complete) destination endpoint.
	 */
	public String getCompleteDestination()
	{
		return getValue(COMPLETE_DESTINATION);
	}
	
	/**
	 * @return the next element in the list of (remaining) destination endpoint elements.
	 */
	public String getFirstDestinationElement()
	{
		return getValue(DESTINATION_ELEMENT);
	}
	
	/**
	 * Clears all destinations and destinations elements, and sets a new destination, both in <i>complete</i> form and
	 * as the list of elements.
	 * 
	 * @param destinationRoot
	 *                                - the first element of the destination endpoint.
	 * @param destinationElements
	 *                                - other elements of the destination endpoint.
	 * @return the wave itself.
	 */
	public AgentWave resetDestination(String destinationRoot, String... destinationElements)
	{
		if(isSet(COMPLETE_DESTINATION))
			removeKey(COMPLETE_DESTINATION);
		String join = String.join(ADDRESS_SEPARATOR, destinationElements);
		add(COMPLETE_DESTINATION, destinationRoot + (join.length() > 0 ? ADDRESS_SEPARATOR + join : ""));
		if(isSet(DESTINATION_ELEMENT))
			removeKey(DESTINATION_ELEMENT);
		add(DESTINATION_ELEMENT, destinationRoot);
		addAll(DESTINATION_ELEMENT, Arrays.asList(destinationElements));
		return this;
	}
	
	/**
	 * Removes the first element in the list of destination endpoint elements.
	 */
	public void removeFirstDestinationElement()
	{
		removeFirst(DESTINATION_ELEMENT);
	}
	
	/**
	 * @return the content of the wave.
	 */
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
	
	/**
	 * Splits a complete endpoint (a path) into its elements. This version also removes the specified prefix from the
	 * path, so it will not be included in the returned elements.
	 * <p>
	 * The path is split by {@link #ADDRESS_SEPARATOR}.
	 * <p>
	 * If the path does not begin with the given prefix, the prefix will be ignored.
	 * 
	 * @param path
	 *                           - the endpoint description to split.
	 * @param prefixToRemove
	 *                           - the prefix to remove from the endpoint.
	 * @return the elements of the path, excluding the prefix.
	 */
	public static String[] pathToElements(String path, String prefixToRemove)
	{
		String barePath = path.startsWith(prefixToRemove) ? path.substring(prefixToRemove.length()) : path;
		return (barePath.startsWith(ADDRESS_SEPARATOR) ? barePath.substring(1) : barePath).split(ADDRESS_SEPARATOR);
	}
	
	/**
	 * Splits a complete endpoint (a path) into its elements. This version allows the caller to specify a prefix, which
	 * will be returned as the first element of the path.
	 * <p>
	 * The path is split by {@link #ADDRESS_SEPARATOR}.
	 * <p>
	 * If the path does not begin with the given prefix, the prefix will be ignored.
	 * 
	 * @param path
	 *                   - the endpoint description to split.
	 * @param prefix
	 *                   - the prefix of the endpoint to consider as the first element.
	 * @return the elements of the path, including the prefix.
	 */
	public static String[] pathToElementsWith(String path, String prefix)
	{
		String[] elements = pathToElements(path, prefix);
		if(!path.startsWith(prefix))
			return elements;
		String[] ret = new String[elements.length + 1];
		ret[0] = prefix;
		for(int i = 0; i < elements.length; i++)
			ret[i + 1] = elements[i];
		return ret;
	}
}
