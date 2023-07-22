package org.aphronatus.msbt;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static Executor executor;
    private static boolean verbose = false;

    public static void main(String[] args) {
        var parser = new DefaultParser();
        var options = new Options();

        options.addOption("i", "input", true, "input file");
        options.addOption("o", "output", true, "output file");
        options.addOption("p", "threads", false, "number of threads to use");
        options.addOption("v", "verbose", false, "verbose output");

        try {
            var cmdLine = parser.parse(options, args);

            if (!cmdLine.hasOption("i")) {
                System.err.println("No input file specified");
                return;
            }

            if (!cmdLine.hasOption("o")) {
                System.err.println("No output file specified");
                return;
            }

            if (cmdLine.hasOption("p")) {
                var threads = Integer.parseInt(cmdLine.getOptionValue("p"));
                executor = Executors.newFixedThreadPool(threads);
            }
            else {
                executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            }

            if (cmdLine.hasOption("v")) {
                verbose = true;
            }

            var inputFile = cmdLine.getOptionValue("i");
            var outputFile = cmdLine.getOptionValue("o");

            convert(inputFile, outputFile);
        }
        catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (executor instanceof ExecutorService) {
                ((ExecutorService) executor).shutdown();
            }

            System.exit(0);
        }
    }

    public static void convert(String inputFile, String outputFile) throws IOException {
        File fileInput = new File(inputFile);
        File fileOutput = new File(outputFile);


        if (fileInput.isDirectory() && (fileOutput.exists() && !fileOutput.isDirectory())) {
            throw new RuntimeException("Input is a directory but output is not");
        }

        if (!fileInput.isDirectory() && fileOutput.isDirectory()) {
            throw new RuntimeException("Input is not a directory but output is");
        }

        if (fileInput.isDirectory()) {
            convertDirectory(fileInput, fileOutput);
        }
        else {
            convertFile(fileInput, fileOutput);
        }
    }

    private static void convertFile(File fileInput, File fileOutput) throws IOException {
        String extensionInput = getExtension(fileInput);
        String extensionOutput = getExtension(fileOutput);

        if (extensionInput.equals(".msbt") && extensionOutput.equals(".yaml")) {
            MSBT msbtOriginal = MSBTUtils.readFromMSBTFile(fileInput);
            MSBTUtils.writeMSBTYAMLFile(msbtOriginal, fileOutput);
        }
        else if (extensionInput.equals(".yaml") && extensionOutput.equals(".msbt")) {
            MSBT msbtOriginal = MSBTUtils.readFromMSBTYAMLFile(fileInput);
            MSBTUtils.writeMSBTFile(msbtOriginal, fileOutput);
        }
        else {
            throw new RuntimeException("Unsupported file type");
        }
    }

    private static void convertDirectory(File fileInput, File fileOutput) throws IOException {
        List<File> inputFiles = readAllFilesSubdirectory(fileInput);

        log("Found " + inputFiles.size() + " files");

        if (!fileOutput.exists()) {
            fileOutput.mkdirs();
        }

        inputFiles.stream().map(inputFile -> CompletableFuture.runAsync(() -> {
            try {
                convertFile(fileInput, fileOutput, inputFile);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor)).forEach(CompletableFuture::join);

    }

    private static void convertFile(File fileInput, File fileOutput, File inputFile) throws IOException {
        String extensionInput = getExtension(inputFile);
        File outputFile = getRelativeDirectory(fileInput, fileOutput, inputFile);

        if (extensionInput.equals(".msbt")) {
            File outputFileYAML = new File(outputFile.getAbsolutePath().replace(".msbt", ".yaml"));

            if (!outputFileYAML.getParentFile().exists()) {
                outputFileYAML.getParentFile().mkdirs();
            }

            MSBT msbtOriginal = MSBTUtils.readFromMSBTFile(inputFile);
            MSBTUtils.writeMSBTYAMLFile(msbtOriginal, outputFileYAML);

            log(inputFile.getName() + " -> " + outputFileYAML.getName());
        }
        else if (extensionInput.equals(".yaml")) {
            File outputFileMSBT = new File(outputFile.getAbsolutePath().replace(".yaml", ".msbt"));

            if (!outputFileMSBT.getParentFile().exists()) {
                outputFileMSBT.getParentFile().mkdirs();
            }

            MSBT msbtOriginal = MSBTUtils.readFromMSBTYAMLFile(inputFile);
            MSBTUtils.writeMSBTFile(msbtOriginal, outputFileMSBT);

            log(inputFile.getName() + " -> " + outputFileMSBT.getName());
        }
        else {
            throw new RuntimeException("Unsupported file type");
        }
    }

    private static File getRelativeDirectory(File inputDirectory, File outputDirectory, File inputFile) {
        String inputDirectoryPath = inputDirectory.getAbsolutePath();
        String inputFileDirectory = inputFile.getAbsolutePath();

        // exclude inputDirectoryPath from inputFileDirectory
        String relativePath = inputFileDirectory.substring(inputDirectoryPath.length());

        return new File(outputDirectory, relativePath);
    }

    private static List<File> readAllFilesSubdirectory(File directory) {
        List<File> filesResult = new ArrayList<>();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".msbt") || name.endsWith(".yaml") || new File(dir, name).isDirectory());

        if (files == null) {
            return filesResult;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                filesResult.addAll(readAllFilesSubdirectory(file));
            }
            else {
                filesResult.add(file);
            }
        }

        return filesResult;
    }

    private static String getExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");

        if (lastIndexOf == -1) {
            return "";
        }

        return name.substring(lastIndexOf);
    }

    public static void log(String str) {
        if (verbose)
            System.out.println(str);
    }
}