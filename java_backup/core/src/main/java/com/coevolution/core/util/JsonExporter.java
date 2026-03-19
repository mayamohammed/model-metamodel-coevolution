package com.coevolution.core.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Exports data to JSON files.
 */
public class JsonExporter {

    public static void export(Map<String, Object> data,
                               String outputPath) {
        FileUtils.ensureDir(
            new java.io.File(outputPath).getParent()
        );
        try (FileWriter writer =
                 new FileWriter(outputPath)) {
            writer.write(toJson(data, 0));
            System.out.println(
                "[JsonExporter] → " + outputPath
            );
        } catch (IOException e) {
            throw new RuntimeException(
                "Export failed : " + outputPath, e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public static String toJson(Object obj, int indent) {
        String p  = "  ".repeat(indent);
        String p2 = "  ".repeat(indent + 1);

        if (obj == null)            return "null";
        if (obj instanceof String)  return "\"" + obj + "\"";
        if (obj instanceof Number)  return obj.toString();
        if (obj instanceof Boolean) return obj.toString();

        if (obj instanceof Map) {
            Map<String, Object> map =
                (Map<String, Object>) obj;
            if (map.isEmpty()) return "{}";
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (Map.Entry<String, Object> e
                    : map.entrySet()) {
                sb.append(p2)
                  .append("\"").append(e.getKey())
                  .append("\": ")
                  .append(toJson(e.getValue(), indent+1));
                if (++i < map.size()) sb.append(",");
                sb.append("\n");
            }
            return sb.append(p).append("}").toString();
        }

        if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            if (list.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(p2)
                  .append(toJson(list.get(i), indent+1));
                if (i < list.size()-1) sb.append(",");
                sb.append("\n");
            }
            return sb.append(p).append("]").toString();
        }
        return "\"" + obj.toString() + "\"";
    }
}