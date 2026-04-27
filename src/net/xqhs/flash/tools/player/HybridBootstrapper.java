package net.xqhs.flash.tools.player;

import java.util.Map;

/**
 * HybridBootstrapper - Deployment Factory for Flash-MAS.
 * <p>
 * Translates the user's Live/Mocked preferences into a valid Flash-MAS XML deployment
 * configuration dynamically before launching the engine.
 * </p>
 */
public class HybridBootstrapper {

    /**
     * Generates the XML deployment content based on agent execution modes.
     *
     * @param agentModes A map denoting whether an agent is "live" or "mocked".
     * @param registry A map containing the fully qualified class names of the agents.
     * @return The raw XML string representing the deployment file.
     */
    public static String prepareDeployment(Map<String, String> agentModes, Map<String, String> registry) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<deployment xmlns=\"http://flash.xqhs.net/deployment-schema\"\n");
        xml.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("    xsi:schemaLocation=\"http://flash.xqhs.net/deployment-schema ../../src-schema/deployment-schema.xsd\">\n\n");

        // Define the local communication Pylon
        xml.append("    <pylon kind=\"local\">\n");

        for (String agentName : agentModes.keySet()) {
            // Protect against instantiating infrastructure components as generic agents
            if (agentName == null || agentName.trim().isEmpty() ||
                    agentName.toLowerCase().contains("pylon") || agentName.equals("local")) {
                continue;
            }

            String mode = agentModes.get(agentName);

            // By default, everyone acts as a Mocked agent (playback shell)
            String classPath = "net.xqhs.flash.tools.player.MockAgent";

            // If marked as LIVE, inject the actual Java class so Flash-MAS runs real logic
            if ("live".equals(mode)) {
                String realClass = registry.get(agentName);
                if (realClass != null && !realClass.trim().isEmpty()) {
                    classPath = realClass;
                } else {
                    System.err.println("[Bootstrapper] Warning: Missing class path for " + agentName + " in registry. Defaulting to Mock.");
                }
            }

            xml.append(String.format("       <agent name=\"%s\" classpath=\"%s\" />\n", agentName, classPath));
        }

        xml.append("    </pylon>\n");
        xml.append("</deployment>\n");

        return xml.toString();
    }
}