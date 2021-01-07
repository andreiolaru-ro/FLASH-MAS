package net.xqhs.flash.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.gui.structure.GlobalConfiguration;

public class GUIGeneration {
	public final static String	INLINE	= "inline";
	public final static String	FILE	= "file";
	public final static String	CLAIM	= "claim";
	
	public static GlobalConfiguration loadGlobalRepresentation(MultiTreeMap configuration) {
		switch(configuration.getSimpleNames().get(0)) {
		case FILE:
			InputStream input = null;
			try {
				input = new FileInputStream(new File(configuration.get(FILE)));
			} catch(FileNotFoundException e) {
				e.printStackTrace();
			}
			Yaml yaml = new Yaml();
			GlobalConfiguration conf = yaml.loadAs(input, GlobalConfiguration.class);
			return conf;
		default:
			// TODO: error
			return null;
		}
	}
}
