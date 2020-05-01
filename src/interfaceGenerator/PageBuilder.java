package interfaceGenerator;

import interfaceGenerator.pylon.AndroidUiPylon;
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
    /*
    public JFrame getWindow() {
        return window;
    }

    public void setWindow(JFrame window) {
        this.window = window;
    }*/

    public Object buildPage(Configuration data) throws Exception {
        var platformType = data.getPlatformType();
        var type = PlatformType.valueOfLabel(platformType);
        this.platformType = type;

        // generating ids for every element in configuration
        var configuration = IdGenerator.attributeIds(data.getNode());
        page = configuration;
        // System.out.println(configuration);

        // checking the active ports, with their elements
        Element.checkActivePortsWithElement(configuration);

        if (type != null) {
            switch (type) {
                case HTML:
                    var html = WebUiPylon.generate(data.getNode());
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
                    var android = AndroidUiPylon.generate(data.getNode());
                    System.out.println(android);
                    return null;
                case DESKTOP:
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (ClassNotFoundException
                            | InstantiationException
                            | IllegalAccessException
                            | UnsupportedLookAndFeelException ex) {
                        ex.printStackTrace();
                    }
                    var frame = SwingUiPylon.generateWindow(data.getNode());
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