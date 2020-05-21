package interfaceGenerator;

import interfaceGenerator.gui.GUIShard;
import interfaceGenerator.io.IOShard;
import interfaceGenerator.pylon.AndroidUiPylon;
import interfaceGenerator.pylon.GUIPylonProxy;
import interfaceGenerator.pylon.SwingUiPylon;
import interfaceGenerator.pylon.WebUiPylon;
import interfaceGenerator.types.PlatformType;
import interfaceGenerator.web.Input;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;

public class PageBuilder {
    private static PageBuilder instance = null;
    public PlatformType platformType = null;
    public boolean createdSwingPage = false;
    public boolean createdWebPage = false;
    public GUIShard guiShard = null;
    public IOShard ioShard = null;
    private Element page = null;
    public static JFrame window = null;

    public static PageBuilder getInstance() {
        if (instance == null) {
            instance = new PageBuilder();
        }
        return instance;
    }

    public Element getPage() {
        return page;
    }

    public Object buildPage(Configuration data) throws Exception {
        return buildPage(data.getNode());
    }

    public Object buildPage(Element data) throws Exception {
        // generating ids for every element in configuration
        page = IdGenerator.attributeIds(data);
        // System.out.println(configuration);

        // checking the active ports, with their elements
        Utils.checkActivePortsWithElement(page);
        GUIPylonProxy guiPylonProxy;

        if (platformType != null) {
            switch (platformType) {
                case WEB:
                    guiPylonProxy = new WebUiPylon();
                    String html = (String) guiPylonProxy.generate(data);
                    FileWriter fileWriter = new FileWriter("interface-files\\generated-web-pages\\page.html");
                    PrintWriter printWriter = new PrintWriter(fileWriter);
                    printWriter.print(html);
                    printWriter.close();
                    Input.main(new String[]{});
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI("http://localhost:8080/"));
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
        return yaml.loadAs(input, Configuration.class);
    }

    public Configuration buildPageInline(String inline) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(inline, Configuration.class);
    }

}