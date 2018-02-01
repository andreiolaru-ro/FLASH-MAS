/*******************************************************************************
 * Copyright (C) 2013 Andrei Olaru, Marius-Tudor Benea, Nguyen Thi Thuy Nga, Amal El Fallah Seghrouchni, Cedric Herpson.
 * 
 * This file is part of tATAmI-PC.
 * 
 * tATAmI-PC is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * tATAmI-PC is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with tATAmI-PC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.util;

/**
 * A simple class that can hold a value of type T, which can be changed while keeping the same ContentHolder instance.
 * 
 * @author andreiolaru
 *
 * @param <T>
 *            the type of the value stored in the ContentHolder instance.
 */
public class ContentHolder<T>
{
	/**
	 * the content to hold.
	 */
	T content = null;
	
	/**
	 * @param _content the content to hold
	 */
	public ContentHolder(T _content)
	{
		content = _content;
	}
	
	/**
	 * @return the content held.
	 */
	public T get()
	{
		return content;
	}
	
	/**
	 * @param _content the content to hold from this point on.
	 * @return the {@link ContentHolder} instance itself.
	 */
	public ContentHolder<T> set(T _content)
	{
		content = _content;
		return this;
	}
	
	/**
	 * Returns the string redition of the held content.
	 */
	@Override
	public String toString()
	{
		return content.toString();
	}
}
