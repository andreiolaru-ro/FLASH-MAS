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
package net.xqhs.flash.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.util.logging.Logger;

/**
 * Class with static methods to load an interface specification (represented as an {@link Element} instance) from a YAML
 * file or directly from a YAML-formatted {@link String}.
 * 
 * @author Florin Mihalache
 * @author andreiolaru
 */
public class GUILoad {
	/**
	 * Parameter in the configuration specifying the file from which to load the UI structure.
	 */
	public static final String FILE_SOURCE_PARAMETER = "from";
	
	/**
	 * Loads a GUI from a configuration from a .yml or .yaml file, and associates it with a logger.
	 *
	 * @param configuration
	 *            - the configuration.
	 * @param log
	 *            - the logger.
	 * @return the structure of the interface.
	 */
	public static Element load(MultiTreeMap configuration, Logger log) {
		String config = configuration.getFirstValue(FILE_SOURCE_PARAMETER);
		if(config == null)
			log.le("Configuration is null");
		if(config != null && (config.endsWith(".yml") || config.endsWith(".yaml"))) { // file
			List<String> checked = new LinkedList<>();
			String path = Loader.autoFind(configuration.getValues(CategoryName.PACKAGE.s()), config, null, null, null,
					checked);
			if(path == null)
				log.le("Cannot find file []. Check paths: ", config, checked);
			else
				try (FileInputStream input = new FileInputStream(new File(path))) {
					return fromYaml(input);
				} catch(FileNotFoundException e) {
					log.le("Cannot load file [].", config);
				} catch(IOException e1) {
					log.le("File close error for file [].", config);
				} catch(Exception e) {
					log.le("Interface load failed from [] with []", config, e);
					e.printStackTrace();
				}
		}
		else // inline
			try {
				return fromYaml(config);
			} catch(Exception e) {
				log.le("Interface load failed from [] with []", config, e);
				e.printStackTrace();
			}
		return null;
	}

	/**
	 *  Parses a YAML file from a FileInputStream and converts it into an instance of the Element class.
	 *
	 * @param input
	 *          - A FileInputStream object representing the YAML file to be parsed.
	 *
	 * @return
	 * 		- An instance of the Element class representing the parsed YAML file.
	 */
	public static Element fromYaml(FileInputStream input) {
		return new Yaml().loadAs(input, Element.class);
	}

	/**
	 * Parses YAML data from a String and converts it into an instance of the Element class.
	 *
	 * @param input
	 * 		- A String object representing the YAML data to be parsed.
	 *
	 * @return
	 * 		- An instance of the Element class representing the parsed YAML data.
	 */
	public static Element fromYaml(String input) {
		return new Yaml().loadAs(input, Element.class);
	}
}
