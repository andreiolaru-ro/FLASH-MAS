package interfaceGenerator;

import interfaceGenerator.pylon.AndroidUiPylon;
import interfaceGenerator.pylon.SwingUiPylon;
import interfaceGenerator.pylon.WebUiPylon;
import interfaceGenerator.types.PlatformType;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.io.*;

public class PageBuilder {
    private static Element page = null;
    private static JFrame window = null;

    public static Element getPage() {
        return page;
    }

    public static JFrame getWindow() {
        // TODO: maybe create a Singleton?
        return window;
    }

    public static boolean createdSwingPage = false;

    public static GUIShard guiShard = null;

    public static Object buildPage(Configuration data) throws Exception {
        var platformType = data.getPlatformType();
        var type = PlatformType.valueOfLabel(platformType);

        // generating ids for every element in configuration
        var configuration = IdGenerator.attributeIds(data.getNode());
        page = configuration;
        // System.out.println(configuration);

        // checking the active ports, with their elements
        Element.checkActivePorts(configuration);

        if (type != null) {
            switch (type) {
                case HTML:
                    var html = WebUiPylon.generate(data.getNode());
                    FileWriter fileWriter = new FileWriter("interface-files\\generated-web-pages\\page.html");
                    PrintWriter printWriter = new PrintWriter(fileWriter);
                    printWriter.print(html);
                    printWriter.close();
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
                    PageBuilder.createdSwingPage = true;
                    return frame;
            }
        }
        return null;
    }

    public static Configuration buildPageFile(String path) {
        InputStream input = null;
        try {
            input = new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Yaml yaml = new Yaml();
        return yaml.loadAs(input, Configuration.class);
    }

    public static Configuration buildPageInline(String inline) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(inline, Configuration.class);
    }

}