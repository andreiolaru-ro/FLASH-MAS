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
import java.util.Timer;
import java.util.TimerTask;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import easyLog.configuration.entry.Entry;
import easyLog.configuration.entry.Entry.OutputBlock;
import easyLog.configuration.yamlObject.YamlObject;

public class EngineController {
	
	protected List<Entry> entriesList = new ArrayList<>(); // list of entries in the configuration file that
																	// needs to be processed
	protected Set<ParserEngine>	engineSet;
	protected int				nLinesParsed	= 0;
	protected boolean			changed			= false;

	public EngineController(String pathToConfigFile) throws IOException {
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
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(changed)
					printBlock();
				changed = false;
			}
		}, 5000, 5000);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while((line = reader.readLine()) != null) {
				if(line.matches("^[>.*#] \\[.*")) {
					// ( . [ > [ # [ ) match pe primele 3// caractere dintr-un log obisnuit
					changed = true;
					for(ParserEngine engine : engineSet) {
						engine.process(line);
					}
					OutputBlock oneLineOutput = new OutputBlock(" ");
					OutputBlock blockoutput = new OutputBlock(" ");
					for(Entry entry : entriesList) {
						if(entry.getOutputItem() != null) {
							entry.getOutputItem().getOutput(oneLineOutput.getAccesor(), blockoutput.getAccesor());
						}
						if(!entry.showGraph)
						{
							if(entry.getFsm() != null)
							{
								entry.getFsm().displayGraph();
								entry.showGraph = true;
							}
						}


					}
					String pad = " ".repeat(20);
					System.out.print(pad + oneLineOutput + "\r");
					if(nLinesParsed++ % 50 == 0) {
						printBlock();
					}
				}
			}
			System.out.println("----------------------------- EXIT");
			timer.cancel();
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
	
	protected void printBlock() {
		OutputBlock oneLineOutput = new OutputBlock(" ");
		OutputBlock blockoutput = new OutputBlock(" ");
		for(Entry entry : entriesList) {
			if(entry.getOutputItem() != null) {
				entry.getOutputItem().getOutput(oneLineOutput.getAccesor(), blockoutput.getAccesor());
			}
		}
		String tab = " ".repeat(150);
		System.out.println(tab + "----------------------------- Log Lines processed: " + nLinesParsed);
		System.out.println(blockoutput.toString(tab));
		System.out.println(tab + " ----------------------------- ^");
		// System.out.println(oneLineOutput);
		// System.out.println(tab + "----------------------------- ^");

	}

	private void initializeParserEngineSet() {
		this.engineSet = new HashSet<>();
		for(Entry entry : entriesList) {
			this.engineSet.add(new ParserEngine(entry));
		}
	}
}
