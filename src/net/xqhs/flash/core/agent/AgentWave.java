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
package net.xqhs.flash.core.agent;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
 * <li>The intention is that these values should remain unchanged throughout the lifetime of the wave.
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
 * TODO: add functionality to support multiple destinations.
 * 
 * TODO: add functionality to support multiple content elements?
 * 
 * 
 * @author Andrei Olaru
 */
public class 		AgentWave extends AgentEvent {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 8405841078556036608L;
	
	/**
	 * The string separating elements of an endpoint address.
	 */
	public static final String ADDRESS_SEPARATOR = "/";
	
	/**
	 * The name associated with the content.
	 */
	public static final String	CONTENT					= "content";
	/**
	 * The name associated with the elements of the source endpoint.
	 */
	public static final String	SOURCE_ELEMENT			= "source-element";
	/**
	 * The name associated with the complete destination(s), in its(their) original form.
	 */
	public static final String	COMPLETE_DESTINATION	= "destination-complete";
	/**
	 * The name associated with the elements of one of the destinations.
	 */
	public static final String	DESTINATION_ELEMENT		= "destination-element";
	
	/**
	 * The keys which have special meanings in the wave and are not part of actual content.
	 */
	@SuppressWarnings("hiding")
	protected static final String[] specialKeys = { EVENT_TYPE_PARAMETER_NAME, SOURCE_ELEMENT, COMPLETE_DESTINATION,
			DESTINATION_ELEMENT };
	
	/**
	 * Creates an agent wave with <b>no</b> destination, with <b>no</b> content.
	 */
	public AgentWave() {
		super(AgentEventType.AGENT_WAVE);
	}
	
	/**
	 * Creates an agent wave with <b>no</b> destination.
	 * <p>
	 * A <i>complete</i> destination will be added by assembling the elements of the destination..
	 * <p>
	 * This is used for MPI support.
	 * 
	 * @param content
	 *            - the content of the wave.
	 */
	public AgentWave(String content) {
		super(AgentEventType.AGENT_WAVE);
		try {
			// is this a serialized content?
			MultiValueMap contentMap = MultiValueMap.fromSerializedString(content);
			for(String key : contentMap.getKeys())
				addAll(key, contentMap.getValues(key));
		} catch(Exception e) {
			// not a serialized content
			add(CONTENT, content);
		}
	}
	
	/**
	 * Creates an agent wave with a <b>single</b> destination.
	 * <p>
	 * A <i>complete</i> destination will be added by assembling the elements of the destination.
	 * <p>
	 * The <code>content</code> argument may be a serialized form produced by {@link #getSerializedContent()} and, if
	 * so, the content will be unpacked accordingly.
	 * 
	 * @param content
	 *            - the content of the wave, that can be the result of previous serialization.
	 * @param destinationRoot
	 *            - the first element of the destination endpoint.
	 * @param destinationElements
	 *            - other elements of the destination endpoint.
	 */
	public AgentWave(String content, String destinationRoot, String... destinationElements) {
		super(AgentEventType.AGENT_WAVE);
		if(content != null)
			try {
				// is this a serialized content?
				MultiValueMap contentMap = MultiValueMap.fromSerializedString(content);
				for(String key : contentMap.getKeys())
					addAll(key, contentMap.getValues(key));
			} catch(Exception e) {
				// not a serialized content
				add(CONTENT, content);
			}
		
		// if the serialization already contained destination data, it will be lost.
		resetDestination(destinationRoot, destinationElements != null ? destinationElements : new String[] {});
	}
	
	/**
	 * Creates an {@link AgentWave} with the source and destination of this wave, reversed, and with the given content,
	 * if any. No other fields are created.
	 * 
	 * @param content
	 *            - if not <code>null</code>, it is used as content for the reply.
	 * 			
	 * @return the reply to this agent wave.
	 */
	public AgentWave createReply(String content) {
		AgentWave reply = new AgentWave(content);
		reply.addSourceElements(getDestinationElements());
		reply.resetDestination(null, this.getSourceElements());
		return reply;
	}
	
	/**
	 * Appends elements to the list of source endpoint elements (the first in the list will be the first source).
	 * 
	 * @param sourceElements
	 *            - endpoint elements to add.
	 * @return the wave itself.
	 */
	public AgentWave addSourceElements(String... sourceElements) {
		if(sourceElements == null)
			throw new IllegalArgumentException("Argument is null");
		for(String elem : sourceElements)
			if(elem.length() > 0)
				add(SOURCE_ELEMENT, elem);
		return this;
	}
	
	/**
	 * /** Insert an element at the beginning of the list of source endpoint elements.
	 * 
	 * @param sourceElement
	 *            - the element that will be the first source endpoint element.
	 * @return the wave itself.
	 */
	public AgentWave addSourceElementFirst(String sourceElement) {
		if(sourceElement == null)
			throw new IllegalArgumentException("Argument is null");
		addFirst(SOURCE_ELEMENT, sourceElement);
		return this;
	}
	
	/**
	 * @return the first element of the source endpoint.
	 */
	public String getFirstSource() {
		return getValue(SOURCE_ELEMENT);
	}
	
	/**
	 * @return the complete source endpoint, obtained by joining the individual elements with the
	 *         {@link #ADDRESS_SEPARATOR}.
	 */
	public String getCompleteSource() {
		return String.join(ADDRESS_SEPARATOR, getValues(SOURCE_ELEMENT));
	}
	
	/**
	 * @return an array containing all elements of the source.
	 */
	public String[] getSourceElements() {
		return getValues(SOURCE_ELEMENT).toArray(new String[0]);
	}
	
	/**
	 * @return the first (complete) destination endpoint.
	 */
	public String getCompleteDestination() {
		return getValue(COMPLETE_DESTINATION);
	}
	
	/**
	 * @return the next element in the list of (remaining) destination endpoint elements.
	 */
	public String getFirstDestinationElement() {
		return getValue(DESTINATION_ELEMENT);
	}
	
	/**
	 * @return an array containing all elements of the destination.
	 */
	public String[] getDestinationElements() {
		return getValues(DESTINATION_ELEMENT).toArray(new String[0]);
	}
	
	/**
	 * Insert new elements of the destination endpoint, after existing elements.
	 * 
	 * @param destinationElements
	 *            - the elements to insert.
	 * @return the wave itself.
	 */
	public AgentWave appendDestination(String... destinationElements) {
		if(destinationElements == null)
			throw new IllegalArgumentException("Argument is null");
		addAll(DESTINATION_ELEMENT, Arrays.asList(destinationElements));
		String dest = get(COMPLETE_DESTINATION);
		if(isSet(COMPLETE_DESTINATION))
			removeKey(COMPLETE_DESTINATION);
		add(COMPLETE_DESTINATION, makePath(dest, destinationElements));
		return this;
	}
	
	/**
	 * Insert a new element of the destination endpoint, before existing elements.
	 * 
	 * @param destinationElement
	 *            - the element to insert.
	 * @return the wave itself.
	 */
	public AgentWave prependDestination(String destinationElement) {
		addFirst(DESTINATION_ELEMENT, destinationElement);
		String dest = get(COMPLETE_DESTINATION);
		if(isSet(COMPLETE_DESTINATION))
			removeKey(COMPLETE_DESTINATION);
		add(COMPLETE_DESTINATION, destinationElement + (dest != null ? ADDRESS_SEPARATOR + dest : ""));
		return this;
	}
	
	/**
	 * Clears all destinations and destinations elements, and sets a new destination, both in <i>complete</i> form and
	 * as the list of elements.
	 * 
	 * @param destinationRoot
	 *            - the first element of the destination endpoint.
	 * @param destinationElements
	 *            - other elements of the destination endpoint.
	 * @return the wave itself.
	 */
	public AgentWave resetDestination(String destinationRoot, String... destinationElements) {
		if(isSet(DESTINATION_ELEMENT))
			removeKey(DESTINATION_ELEMENT);
		if(destinationRoot != null)
			add(DESTINATION_ELEMENT, destinationRoot);
		addAll(DESTINATION_ELEMENT, Arrays.asList(destinationElements));
		return recomputeCompleteDestination();
	}
	
	/**
	 * Completely rewrites the {@link #COMPLETE_DESTINATION} element with the assembly of {@link #DESTINATION_ELEMENT}
	 * values.
	 * 
	 * @return the wave itself.
	 */
	public AgentWave recomputeCompleteDestination() {
		if(isSet(COMPLETE_DESTINATION))
			removeKey(COMPLETE_DESTINATION);
		add(COMPLETE_DESTINATION, String.join(ADDRESS_SEPARATOR, getValues(DESTINATION_ELEMENT)));
		return this;
	}
	
	/**
	 * Gets and removes the first element of the destination endpoint.
	 * 
	 * WARNING: the complete destination of the wave is not changed by this method, such as to preserve the original
	 * destination of the wave.
	 * 
	 * @return the next element in the list of (remaining) destination endpoint elements, which is also removed.
	 */
	public String popDestinationElement() {
		String result = get(DESTINATION_ELEMENT);
		removeFirst(DESTINATION_ELEMENT);
		return result;
	}
	
	/**
	 * Removes the first element in the list of destination endpoint elements.
	 * 
	 * WARNING: the complete destination of the wave is not changed by this method, such as to preserve the original
	 * destination of the wave.
	 * 
	 * @return the wave itself.
	 */
	public AgentWave removeFirstDestinationElement() {
		removeFirst(DESTINATION_ELEMENT);
		return this;
	}
	
	/**
	 * @return the content of the wave.
	 */
	public String getContent() {
		return getValue(CONTENT);
	}

	
	/**
	 * @return all the keys in this {@link MultiValueMap} which are not related to routing or agent event type. The
	 *         {@value #CONTENT} key is added as the first key, if it was initially present.
	 */
	public List<String> getContentElements() {
		List<String> result = new LinkedList<>(getKeys());
		for(String k : specialKeys)
			result.remove(k);
		if(result.contains(CONTENT)) {
			result.remove(CONTENT);
			result.add(0, CONTENT);
		}
		return result;
	}
	
	/**
	 * Creates a {@link String} that represents the serialization of all of the waves <b>content</b> (the keys returned
	 * by {@link #getContentElements()}). This string can be given to {@link #fromSerializedContent(String)} or to the
	 * {@link #AgentWave(String, String, String...)} constructor.
	 * 
	 * @return the {@link String} form of the content in this wave.
	 */
	public String getSerializedContent() {
		List<String> keys = getContentElements();
		if(keys.size() <= 1 && CONTENT.equals(keys.get(0)) && getValues(CONTENT).size() == 1)
			// there is only one content element
			return get(CONTENT);
		MultiValueMap contentMap = new MultiValueMap();
		for(String key : getContentElements())
			contentMap.addAll(key, getValues(key));
		return contentMap.toSerializedString();
	}
	
	/**
	 * Unimplemented.
	 * 
	 * @param serializedContent
	 * @return the wave itself.
	 */
	@SuppressWarnings({ "static-method" })
	public AgentWave fromSerializedContent(String serializedContent) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	/**
	 * Produces an endpoint description by assembling the start of the address with the rest of the elements. They will
	 * be separated by what is the value of the {@link #ADDRESS_SEPARATOR}.
	 * <p>
	 * Elements that are <code>null</code> will not be assembled in the path.
	 * <p>
	 * If the start is <code>null</code> the result will begin with a the first element.
	 * 
	 * @param start
	 *            - start of the address. Special cases:
	 *            <ul>
	 *            <li><code>null</code> - the path will start with the first element.
	 *            <li>{@link #ADDRESS_SEPARATOR} - the path will start with {@value #ADDRESS_SEPARATOR}, followed by the
	 *            first element
	 *            </ul>
	 * @param elements
	 *            - other elements in the address
	 * @return the resulting address.
	 */
	public static String makePath(String start, String... elements) {
		String ret = (start != null) ? start : "";
		for(String elem : elements)
			if(elem != null && elem.length() > 0)
				ret += ret.length() > 0 ? ADDRESS_SEPARATOR + elem : elem;
		return ret;
	}
	
	/**
	 * Splits an endpoint path into elements, using {@link #ADDRESS_SEPARATOR} as split separator.
	 * 
	 * @param path
	 *            - an endpoint path.
	 * @return the elements of the path.
	 */
	public static String[] pathToElements(String path) {
		return (path.startsWith(ADDRESS_SEPARATOR) ? path.substring(1) : path).split(ADDRESS_SEPARATOR);
	}
	
	/**
	 * Concatenates a prefix and a path (uses an {@link #ADDRESS_SEPARATOR} to separate them if they don't contain one
	 * already) and splits them by {@link #ADDRESS_SEPARATOR}.
	 * 
	 * @param path
	 *            - an endpoint path.
	 * @param prefix
	 *            - another path.
	 * @return the elements of the prefix and the path combined.
	 */
	public static String[] pathToElementsPlus(String path, String prefix) {
		
		if(path.startsWith(ADDRESS_SEPARATOR) || prefix.endsWith(ADDRESS_SEPARATOR))
			return pathToElements(prefix + path);
		return pathToElements(prefix + ADDRESS_SEPARATOR + path);
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
	 *            - the endpoint description to split.
	 * @param prefixToRemove
	 *            - the prefix to remove from the endpoint.
	 * @return the elements of the path, excluding the prefix.
	 */
	public static String[] pathToElements(String path, String prefixToRemove) {
		String barePath = prefixToRemove != null && path.startsWith(prefixToRemove)
				? path.substring(prefixToRemove.length())
				: path;
		return pathToElements(barePath);
	}
	
	/**
	 * Splits a complete endpoint (a path) into its elements. This version allows the caller to specify a prefix, which
	 * will be returned as the first element of the path, instead of being separated like the rest of the path.
	 * <p>
	 * The path is split by {@link #ADDRESS_SEPARATOR}.
	 * <p>
	 * If the path does not begin with the given prefix, the prefix will be ignored.
	 * 
	 * @param path
	 *            - the endpoint description to split.
	 * @param prefix
	 *            - the prefix of the endpoint to consider as the first element.
	 * @return the elements of the path, including the prefix.
	 */
	public static String[] pathToElementsWith(String path, String prefix) {
		String[] elements = pathToElements(path, prefix);
		if(prefix == null || !path.startsWith(prefix))
			return elements;
		String[] ret = new String[elements.length + 1];
		ret[0] = prefix;
		for(int i = 0; i < elements.length; i++)
			ret[i + 1] = elements[i];
		return ret;
	}
}
