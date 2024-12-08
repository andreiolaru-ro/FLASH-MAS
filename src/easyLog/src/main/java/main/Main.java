package easyLog.src.main.java.main;

import easyLog.src.main.java.configuration.configLoader.ConfigLoader;
import net.xqhs.flash.FlashBoot;
import net.xqhs.util.logging.MasterLog;
import net.xqhs.util.logging.output.StreamLogOutput;

import java.io.*;


public class Main {
    public static void main(InputStream in) throws IOException, InterruptedException {
        startEasyLog(in);
    }

    public static void startEasyLog(InputStream in) throws FileNotFoundException, InterruptedException {
//        try(BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
//        {
//            String line;
//            while((line = reader.readLine())!= null){
//                System.out.println("[easyLog] " + line);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

//        String filePathOfFlashmasLogs = "G:\\FLASH-MAS\\src\\easyLog\\src\\main\\resources\\flashMasALL_GOOD.txt";
        ConfigLoader configLoader = new ConfigLoader();
        configLoader.initializeConfig("easyLog/src/main/resources/test.yml");
        configLoader.getConfigParserController().activateParserEngine(in);


    }
}
