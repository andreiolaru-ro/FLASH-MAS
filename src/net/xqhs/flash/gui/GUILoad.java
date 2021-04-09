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

public class GUILoad {
	public static Element load(MultiTreeMap configuration, Logger log) {
		String config = configuration.getFirstValue("from");
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
	
	public static Element fromYaml(FileInputStream input) {
		return new Yaml().loadAs(input, Element.class);
	}
	
	public static Element fromYaml(String input) {
		return new Yaml().loadAs(input, Element.class);
	}
}
