package easyLog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import easyLog.configuration.entry.Entry;
import easyLog.configuration.yamlObject.YamlObject;

public class EngineController {
	
	private List<Entry>			entriesList	= new ArrayList<>();	// list of entries in the configuration file that
																	// needs to be processed
	private Set<ParserEngine>	engineSet;
	private boolean				matched		= false;
	
	public EngineController(String pathToConfigFile) {
		List<YamlObject> yamlObjects = new ArrayList<>();
		Yaml yaml = new Yaml(new Constructor(YamlObject.class, new LoaderOptions()));
		InputStream inputStream = EasyLog.class.getClassLoader().getResourceAsStream(pathToConfigFile);
		
		List<LinkedHashMap<String, Object>> loaders = yaml.loadAs(inputStream, List.class);
		
		for(LinkedHashMap<String, Object> loader : loaders) {
			yamlObjects.add(mapToYamlObject(loader));
		}
		
		for(YamlObject yamlObject : yamlObjects) {
			entriesList.add(yamlObject.initializeEntity());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static YamlObject mapToYamlObject(LinkedHashMap<String, Object> map) {
		return new YamlObject((String) map.get("e"), (String) map.get("level"), (List<String>) map.get("match"), (String) map.get("fsm-from"), (String) map.get("fsm-start"), (String) map.get("fsm-expect"),
				(List<String>) map.get("out"), (List<String>) map.get("expect"), (String) map.get("comment"));
	}
	
	public void parseStream(InputStream in) throws FileNotFoundException, InterruptedException {
		// method that activates the parser engine for the configuration objects
		initializeParserEngineSet();
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			int n = 0;
			while((line = reader.readLine()) != null) {
				if(line.matches("^[>.*#] \\[.*"))// ( . [ > [ # [ ) match pe primele 3// caractere dintr-un log obisnuit
				{
					this.matched = true;
				}
				if(this.matched) {
					for(ParserEngine engine : engineSet) {
						engine.process(line);
					}
//					System.out.print("\r" + (n++) + " Lines processed");
					if(n % 10 == 0) {
						for(Entry entry : entriesList) {
							if(entry.getOutputItem() != null) {
								entry.getOutputItem().getOutput();
							}
							System.out.println();
							System.out.println("-----------------------------");
						}
					}
				}
			}
			reader.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		
		// for(Entry entry: entriesList)
		// {
		// if (entry.getOutputItem() != null) {
		// entry.getOutputItem().getOutput();
		// }
		// System.out.println();
		// System.out.println("-----------------------------");
		// }
	}
	
	private void initializeParserEngineSet() {
		this.engineSet = new HashSet<>();
		for(Entry entry : entriesList) {
			this.engineSet.add(new ParserEngine(entry));
		}
	}
}
