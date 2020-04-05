package interfaceGenerator;

import interfaceGenerator.pylon.AndroidUiPylon;
import interfaceGenerator.pylon.SwingUiPylon;
import interfaceGenerator.pylon.WebUiPylon;
import interfaceGenerator.types.PlatformType;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.io.*;

public class PageBuilder {
    public static Object buildPage(InputStream input) throws Exception {
        Yaml yaml = new Yaml();
        Configuration data = yaml.loadAs(input, Configuration.class);

        var platformType = data.getPlatformType();
        var type = PlatformType.valueOfLabel(platformType);

        if (type != null) {
            switch (type) {
                case HTML:
                    var html = WebUiPylon.generate(data.getNode());
                    System.out.println(html);
                    FileWriter fileWriter = new FileWriter("interface-files\\model-page\\page.html");
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
                    return frame;
            }
        }
        return null;
    }

    public static InputStream buildPageFile(String path) {
        InputStream input = null;
        try {
            input = new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return input;
    }
}