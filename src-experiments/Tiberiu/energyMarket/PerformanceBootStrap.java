package Tiberiu.energyMarket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import net.xqhs.flash.FlashBoot;

public class PerformanceBootStrap {

    private static final String EXP_DIR = "src-experiments/Tiberiu/energyMarket/";
    private static final String XML_FILENAME = "deployment_3microgrids.xml";

    public static void main(String[] args) {
        System.out.println("Preparing Multi-Microgrid Topology Configuration...");

        try {
            // 1. Generăm dinamic fișierul XML bazat pe cele 3 zone de rețea
            generateMultiZoneDeployment(XML_FILENAME, 33);
            System.out.println("Topology configuration xml generated successfully.");

            // 2. Pornim simularea utilizând fișierul generat dinamic
            System.out.println("Launching 102-Agent Local Energy Market Simulation (3 MarketAgents + 99 Prosumers)...");
            FlashBoot.main(new String[] { EXP_DIR + XML_FILENAME });

        } catch (IOException e) {
            System.err.println("Critical Error generating deployment XML: " + e.getMessage());
        }
    }

    /**
     * Generează o structură XML conform specificațiilor FLASH-MAS pentru 3 zone independente.
     */
    private static void generateMultiZoneDeployment(String filename, int prosumersPerZone) throws IOException {
        File dir = new File(EXP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(EXP_DIR + filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<deployment xmlns=\"http://flash.xqhs.net/deployment-schema\"\n");
            writer.write("            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            writer.write("            xsi:schemaLocation=\"http://flash.xqhs.net/deployment-schema ../../src-schema/deployment-schema.xsd\">\n\n");
            writer.write("    <package>Tiberiu.energyMarket</package>\n");
            writer.write("    <pylon kind=\"local\">\n\n");

            // Generăm structura simetrică pentru Zonele A, B și C
            String[] zones = {"A", "B", "C"};
            for (String zone : zones) {
                writer.write("        \n");
                writer.write("        \n");
                writer.write("        \n");
                writer.write("        <agent name=\"MarketAgent_" + zone + "\" classpath=\"MarketAgent\" />\n");

                for (int i = 1; i <= prosumersPerZone; i++) {
                    writer.write("        <agent name=\"Prosumer_" + zone + i + "\" classpath=\"ProsumerAgent\" />\n");
                }
                writer.write("\n");
            }

            writer.write("    </pylon>\n");
            writer.write("</deployment>\n");
        }
    }
}