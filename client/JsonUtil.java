public class JsonUtil {

    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end)
                   .replace("\\\\", "\\")
                   .replace("\\\"", "\"")
                   .replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t");
    }

    /** Build JSON command with no extra params: {"command":"CMD"} */
    public static String buildCommand(String command) {
        return "{\"command\":\"" + escapeJson(command) + "\"}";
    }

    /** Build JSON command with key-value pairs: {"command":"CMD","k1":"v1",...} */
    public static String buildCommand(String command, String... keyValues) {
        StringBuilder sb = new StringBuilder("{\"command\":\"").append(escapeJson(command)).append("\"");
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            sb.append(",\"").append(escapeJson(keyValues[i]))
              .append("\":\"").append(escapeJson(keyValues[i + 1])).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
}
