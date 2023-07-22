package org.aphronatus.msbt;


import org.aphronatus.msbt.io.reader.MSBTReaderFile;
import org.aphronatus.msbt.io.reader.MSBTReaderYAMLFile;
import org.aphronatus.msbt.io.writer.MSBTWriterFile;
import org.aphronatus.msbt.io.writer.MSBTWriterYAMLFile;

import java.io.File;
import java.io.IOException;

public class MSBTUtils {

    public static MSBT readFromMSBTFile(File file) throws IOException {
        return MSBTReaderFile.fromFile(file);
    }

    public static MSBT readFromMSBTYAMLFile(File file) throws IOException {
        return MSBTReaderYAMLFile.fromFile(file);
    }

    public static void writeMSBTFile(MSBT msbt, File file) throws IOException {
        MSBTWriterFile.writeFile(msbt, file);
    }

    public static void writeMSBTYAMLFile(MSBT msbt, File file) throws IOException {
        MSBTWriterYAMLFile.writeFile(msbt, file);
    }

}
