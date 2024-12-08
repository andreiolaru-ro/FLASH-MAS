package easyLog.src.main.java.configuration.configLoader;

import easyLog.src.main.java.configuration.configParser.configParserController.ConfigParserController;
import easyLog.src.main.java.configuration.configParser.yamlObject.YamlObject;
import easyLog.src.main.java.main.Main;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ConfigLoader {
    private List<YamlObject> yamlObjects;

    private ConfigParserController configParserController;

    public List<YamlObject> getYamlObjects() {
        return yamlObjects;
    }

    public void setYamlObjects(List<YamlObject> yamlObjects) {
        this.yamlObjects = yamlObjects;
    }

    public void initializeConfig(String pathToConfigFile) {
        yamlObjects = new ArrayList<>();
        Yaml yaml = new Yaml(new Constructor(YamlObject.class, new LoaderOptions()));
        InputStream inputStream = Main.class
                .getClassLoader()
                .getResourceAsStream(pathToConfigFile);

        List<LinkedHashMap<String, Object>> loaders = yaml.loadAs(inputStream, List.class);

        for (LinkedHashMap<String, Object> loader : loaders) {
            yamlObjects.add(mapToYamlObject(loader));
        }

        this.configParserController = new ConfigParserController();

        for (YamlObject yamlObject : yamlObjects) {
            this.configParserController.getEntriesList().add(yamlObject.initializeEntity());
        }
    }

    public static YamlObject mapToYamlObject(LinkedHashMap<String, Object> map) {
        return new YamlObject((String) map.get("e"), (String) map.get("level"), (List<String>) map.get("match"), (List<String>) map.get("out"), (List<String>) map.get("expect"), (String) map.get("comment"));
    }

    public ConfigParserController getConfigParserController() {
        return configParserController;
    }
}
