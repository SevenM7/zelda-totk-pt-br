package org.example;

import com.google.gson.JsonObject;
import kotlin.Pair;
import org.aphronatus.msbt.LabelGroup;
import org.aphronatus.msbt.MSBTUtils;
import org.example.database.EasyDatabaseManager;
import org.example.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WriteDatabase {


    private static final EasyDatabaseManager easyDatabaseManager = new EasyDatabaseManager();


    /**
     *
     *
     *
     * Struct table
         CREATE TABLE labels_text (
             "filename" TEXT,
             "id_label" INTEGER,
             "label" TEXT,
             "table_index" INTEGER,
             "original_text" TEXT,
             "translated_text" TEXT,
             "translated" BOOLEAN
         )
     *
     * @param folder
     * @throws IOException
     */
    public void insertFilesLinesByFolder(String folder) throws IOException {
        List<File> files = readAllFiles(new File(folder));

        List<JsonObject> jsonObjects = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            String fileName = file.getName();
            String filenamecolumn = fileName.substring(0, fileName.lastIndexOf('.'));

            List<LabelGroup> labelsFilled = MSBTUtils.readFromMSBTYAMLFile(file).getLabelsFilled();

            for (int j = 0; j < labelsFilled.size(); j++) {
                var labelGroup = labelsFilled.get(j);

                for (int k = 0; k < labelGroup.getLabels().size(); k++) {
                    var label = labelGroup.getLabels().get(k);

                    String sanitizeValue = StringUtils.sanitizeString(label.getValue());

                    var object = new JsonObject();
                    object.addProperty("filename", filenamecolumn);
                    object.addProperty("id_label", labelGroup.getId());
                    object.addProperty("label", label.getName());
                    object.addProperty("table_index", label.getTableIndex());
                    object.addProperty("original_text", sanitizeValue);
                    object.addProperty("translated_text", sanitizeValue);
                    object.addProperty("translated", false);

                    jsonObjects.add(object);
                }
            }
        }

        String query = """
                INSERT INTO "msbt_text"."labels_text" ("filename", "id_label", "label", "table_index", "original_text", "translated_text", "translated")
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;


        easyDatabaseManager.batchUpdate(query, (statement -> {
            for (int i = 0; i < jsonObjects.size(); i++) {
                var object = jsonObjects.get(i);
                statement.setString(1, object.get("filename").getAsString());
                statement.setInt(2, object.get("id_label").getAsInt());
                statement.setString(3, object.get("label").getAsString());
                statement.setInt(4, object.get("table_index").getAsInt());
                statement.setString(5, object.get("original_text").getAsString());
                statement.setString(6, object.get("translated_text").getAsString());
                statement.setBoolean(7, object.get("translated").getAsBoolean());
                statement.addBatch();
            }
        }));

    }


    public void updateFiles(String folder) throws IOException {
        File folderFile = new File(folder);
        List<File> files = readAllFiles(folderFile);

        Map<String, File> fileByName = new HashMap<>();

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            var filename = file.getName();
            var filenameColumn = filename.substring(0, filename.lastIndexOf('.'));

            fileByName.put(filenameColumn, file);
        }

        System.out.println("Files loaded");

        String query = """
            SELECT * FROM "msbt_text"."labels_text"
            """;

        List<JsonObject> jsonObjects = easyDatabaseManager.queryList(query, (resultSet -> {
            var object = new JsonObject();
            object.addProperty("filename", resultSet.getString("filename"));
            object.addProperty("id_label", resultSet.getInt("id_label"));
            object.addProperty("label", resultSet.getString("label"));
            object.addProperty("table_index", resultSet.getInt("table_index"));
            object.addProperty("original_text", resultSet.getString("original_text"));
            object.addProperty("translated_text", resultSet.getString("translated_text"));
            object.addProperty("translated", resultSet.getBoolean("translated"));

            return object;

        }));

        System.out.println("Data loaded");

        Map<String, List<Pair<Integer, String>>> mapByFile = new HashMap<>();

        for (JsonObject object : jsonObjects) {
            var filename = object.get("filename").getAsString();
            var tableIndex = object.get("table_index").getAsInt();

            if (!mapByFile.containsKey(filename)) {
                mapByFile.put(filename, new ArrayList<>());
            }

            var modify = object.get("translated_text").getAsString();
            var text = sanitizeStringToUTF16LE(modify);

            mapByFile.get(filename).add(new Pair<>(tableIndex, text));
        }

        System.out.println("Data organized");

        // rewrite file in target folder
        int count = 0;

        for (var entry : mapByFile.entrySet()) {
            var filename = entry.getKey();
            var list = entry.getValue();

            list.sort(Comparator.comparingInt(Pair::getFirst));

            var orderedList = list.stream().map(Pair::getSecond).collect(Collectors.toList());

            var file = fileByName.get(filename);

            var msbt = MSBTUtils.readFromMSBTYAMLFile(file);
            msbt.setStringTable(orderedList);

            // get name and folder
            var subfolderName = file.getParentFile().getName();
            var fullname = subfolderName + "/" + file.getName();

            var targetFolder = new File(folderFile.getParentFile(), "target_pt_br");

            if (!targetFolder.exists()) {
                targetFolder.mkdir();
            }

            var fileTarget = new File(targetFolder, fullname);

            if (!fileTarget.getParentFile().exists()) {
                fileTarget.getParentFile().mkdirs();
            }

            MSBTUtils.writeMSBTYAMLFile(msbt, fileTarget);

            if (count++ % 100 == 0) {
                System.out.println("Files updated: " + count);
            }
        }

        System.out.println("Files updated");
    }

    private static String sanitizeStringToUTF16LE(String string) {
        // convert to UTF-16LE
        ByteBuffer encode = StandardCharsets.UTF_16LE.encode(string);
        var convertedString = new String(encode.array(), StandardCharsets.UTF_16LE);

        // replace \\uXXXX unicode json to represetation byte[2]
        var matcher = Pattern.compile("\\\\u([0-9A-Fa-f]{4})").matcher(convertedString);
        var builder = new StringBuilder();
        var lastEnd = 0;

        while (matcher.find()) {
            var start = matcher.start();
            var end = matcher.end();
            var group = matcher.group(1);

            builder.append(convertedString, lastEnd, start);
            builder.append((char) Integer.parseInt(group, 16));

            lastEnd = end;
        }

        builder.append(convertedString, lastEnd, convertedString.length());

        return builder.toString();
    }


    public static List<File> readAllFiles(File directory) {
        List<File> files = new ArrayList<>();
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                files.addAll(readAllFiles(file));
            } else {
                files.add(file);
            }
        }
        return files;
    }

}