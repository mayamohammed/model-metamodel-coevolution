
package com.coevolution.core.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Exports data to CSV files.
 */
public class CsvExporter {

    private static final String SEP = ",";
    private static final String NL  = "\n";

    public static void export(List<String> headers,
                               List<List<Object>> rows,
                               String outputPath) {
        FileUtils.ensureDir(
            new java.io.File(outputPath).getParent()
        );
        try (FileWriter writer =
                 new FileWriter(outputPath)) {

            writer.write(String.join(SEP, headers));
            writer.write(NL);

            for (List<Object> row : rows) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.size(); i++) {
                    Object val = row.get(i);
                    sb.append(
                        val != null ? val.toString() : ""
                    );
                    if (i < row.size()-1)
                        sb.append(SEP);
                }
                writer.write(sb.toString());
                writer.write(NL);
            }

            System.out.println(
                "[CsvExporter] "
                + rows.size()
                + " rows → " + outputPath
            );
        } catch (IOException e) {
            throw new RuntimeException(
                "Export failed : " + outputPath, e
            );
        }
    }
}