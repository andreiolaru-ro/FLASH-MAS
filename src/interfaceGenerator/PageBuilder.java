package interfaceGenerator;

import interfaceGenerator.gui.GUIShard;
import interfaceGenerator.io.IOShard;
import interfaceGenerator.pylon.AndroidUiPylon;
import interfaceGenerator.pylon.GUIPylonProxy;
import interfaceGenerator.pylon.SwingUiPylon;
import interfaceGenerator.pylon.WebUiPylon;
import interfaceGenerator.types.*;
import interfaceGenerator.web.Input;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class PageBuilder {
    private static PageBuilder instance = null;
    public PlatformType platformType = null;
    public boolean createdSwingPage = false;
    public boolean createdWebPage = false;
    public GUIShard guiShard = null;
    public IOShard ioShard = null;
    private Element page = null;
    public static JFrame window = null;
    public LayoutType layoutType;
    public ArrayList<Element> defaultEntitiesElements = new ArrayList<>();
    public ArrayList<Element> defaultExtendedInterfacesElement = new ArrayList<>();

    public static PageBuilder getInstance() {
        if (instance == null) {
            instance = new PageBuilder();
        }
        return instance;
    }

    public Element getPage() {
        return page;
    }

    private void createDefaultEntitiesElements() {
        // predefined elements for entities
        Element startButton = new Element();
        startButton.setType(ElementType.BUTTON.type);
        startButton.setRole(PortType.ACTIVE.type);
        startButton.setPort("start-entity");
        startButton.setValue("Start");
        startButton.setBlockType(BlockType.GLOBAL.type);

        Element stopButton = new Element();
        stopButton.setType(ElementType.BUTTON.type);
        stopButton.setRole(PortType.ACTIVE.type);
        stopButton.setPort("stop-entity");
        stopButton.setValue("Stop");
        stopButton.setBlockType(BlockType.GLOBAL.type);

        Element pauseButton = new Element();
        pauseButton.setType(ElementType.BUTTON.type);
        pauseButton.setRole(PortType.ACTIVE.type);
        pauseButton.setPort("pause-entity");
        pauseButton.setValue("Pause");
        pauseButton.setBlockType(BlockType.GLOBAL.type);

        defaultEntitiesElements.add(startButton);
        defaultEntitiesElements.add(stopButton);
        defaultEntitiesElements.add(pauseButton);
    }

    private void createDefaultExtendedInterfacesElements() {
        Element entityType = new Element();
        entityType.setType(ElementType.OUTPUT.type);
        entityType.setBlockType(BlockType.INTERFACES.type);
        entityType.setRole("entity-type");
        entityType.setPort("interface-entity-info");

        Element entityStatus = new Element();
        entityStatus.setType(ElementType.OUTPUT.type);
        entityStatus.setBlockType(BlockType.INTERFACES.type);
        entityStatus.setRole("entity-status");
        entityStatus.setPort("interface-entity-info");

        Element startButton = new Element();
        startButton.setType(ElementType.BUTTON.type);
        startButton.setBlockType(BlockType.INTERFACES.type);
        startButton.setRole("interface-entity-start");
        startButton.setPort("interface-entity-control");

        Element stopButton = new Element();
        stopButton.setType(ElementType.BUTTON.type);
        stopButton.setBlockType(BlockType.INTERFACES.type);
        stopButton.setRole("interface-entity-stop");
        stopButton.setPort("interface-entity-control");

        Element actionList = new Element();
        actionList.setType(ElementType.LIST.type);
        actionList.setBlockType(BlockType.INTERFACES.type);
        actionList.setRole("interface-entity-action");
        actionList.setPort("interface-entity-operations");

        Element executeAction = new Element();
        executeAction.setType(ElementType.BUTTON.type);
        executeAction.setBlockType(BlockType.INTERFACES.type);
        executeAction.setRole("interface-entity-action");
        executeAction.setPort("interface-entity-operations");

        Element inputAction = new Element();
        inputAction.setType(ElementType.FORM.type);
        inputAction.setBlockType(BlockType.INTERFACES.type);
        inputAction.setRole("interface-entity-action");
        inputAction.setPort("interface-entity-operations");

        defaultExtendedInterfacesElement.add(entityType);
        defaultExtendedInterfacesElement.add(entityStatus);
        defaultExtendedInterfacesElement.add(startButton);
        defaultExtendedInterfacesElement.add(stopButton);
        defaultExtendedInterfacesElement.add(actionList);
        defaultExtendedInterfacesElement.add(executeAction);
        defaultExtendedInterfacesElement.add(inputAction);
    }

    public Object buildPage(Configuration data) throws Exception {
        layoutType = LayoutType.valueOfLabel(data.getLayout());
        if (data.getNode() != null) {
            return buildPage(data.getNode());
        } else {
            // TODO with global and interfaces
            Element node = new Element();
            node.setType(ElementType.BLOCK.type);
            node.setRole("page-root");
            ArrayList<Element> globals = (ArrayList<Element>) data.getGlobal();
            ArrayList<Element> interfaces = (ArrayList<Element>) data.getInterfaces();

            ArrayList<Element> globalsWithType = new ArrayList<>();
            ArrayList<Element> interfacesWithType = new ArrayList<>();

            createDefaultEntitiesElements();
            createDefaultExtendedInterfacesElements();

            for (Element elem : globals) {
                if (elem.getPort() != null && elem.getPort().equals("entities")) {
                    if (elem.getChildren() == null || elem.getChildren().isEmpty()) {
                        elem.addAllChildren(defaultEntitiesElements);
                    }
                }
                globalsWithType.add(Utils.attributeBlockType(elem, BlockType.GLOBAL));
            }

            // "interfaces" key from specification can have no value
            if (interfaces != null) {
                for (Element elem : interfaces) {
                    interfacesWithType.add(Utils.attributeBlockType(elem, BlockType.INTERFACES));
                }
            }

            Element globalContainer = new Element();
            globalContainer.setType(ElementType.BLOCK.type);
            globalContainer.setRole("global");
            globalContainer.setChildren(globalsWithType);

            Element interfacesContainer = new Element();
            interfacesContainer.setType(ElementType.BLOCK.type);
            interfacesContainer.setRole("interfaces");
            interfacesContainer.setChildren(interfacesWithType);

            ArrayList<Element> nodeChildren = new ArrayList<>();
            nodeChildren.add(globalContainer);
            nodeChildren.add(interfacesContainer);

            node.setChildren(nodeChildren);

            return buildPage(node);
        }
    }

    public Object buildPage(Element data) throws Exception {
        // generating ids for every element in configuration
        // System.out.println(data);
        page = IdGenerator.attributeIds(data);
        System.out.println(page);
        platformType = PlatformType.WEB;

        // checking the active ports, with their elements
        Utils.checkActivePortsWithElement(page);
        GUIPylonProxy guiPylonProxy;
        if (platformType != null) {
            switch (platformType) {
                case WEB:
                    guiPylonProxy = new WebUiPylon();
                    String html = (String) guiPylonProxy.generate(data);
                    FileWriter fileWriter = new FileWriter("src/web/page.html");
                    //FileWriter fileWriter = new FileWriter("interface-files/generated-web-pages/page.html");
                    PrintWriter printWriter = new PrintWriter(fileWriter);
                    printWriter.print(html);
                    printWriter.close();
                    Input.main(new String[]{});
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        //Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
                    }
                    createdWebPage = true;
                    return null;
                case ANDROID:
                    Object android = AndroidUiPylon.generate(data);
                    System.out.println(android);
                    return null;
                case DESKTOP:
                    guiPylonProxy = new SwingUiPylon();
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (ClassNotFoundException
                            | InstantiationException
                            | IllegalAccessException
                            | UnsupportedLookAndFeelException ex) {
                        ex.printStackTrace();
                    }
                    JFrame frame = (JFrame) guiPylonProxy.generate(data);
                    frame.setVisible(true);
                    if (window == null) {
                        window = frame;
                    }
                    createdSwingPage = true;
                    return frame;
            }
        }
        return null;
    }

    public Configuration buildPageFile(String path) {
        InputStream input = null;
        try {
            input = new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Yaml yaml = new Yaml();
        Configuration conf = yaml.loadAs(input, Configuration.class);
        return conf;
    }

    public Configuration buildPageInline(String inline) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(inline, Configuration.class);
    }

}