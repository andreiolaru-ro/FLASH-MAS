package httphomeserver;

public class Helper {
    
    public static String extractAgentAddress(String fullUrl) {
        if (fullUrl.lastIndexOf("/") == fullUrl.indexOf("/", 7)) {
            return fullUrl;
        }
        return fullUrl.substring(0, fullUrl.lastIndexOf("/"));
    }

    public static String getAgentName(String fullUrl) {
        if (fullUrl.lastIndexOf("/") == fullUrl.indexOf("/", 7)) {
            return fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
        }
        return fullUrl.substring(fullUrl.indexOf("/", 7) + 1, fullUrl.lastIndexOf("/"));
    }
}
