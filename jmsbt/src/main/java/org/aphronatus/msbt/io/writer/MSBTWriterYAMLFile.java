package org.aphronatus.msbt.io.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.aphronatus.msbt.MSBT;
import org.aphronatus.msbt.utils.YAMLObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class MSBTWriterYAMLFile {
    public static void writeFile(MSBT msbt, File file) throws IOException {
        ObjectMapper mapper = YAMLObjectMapper.getMapper();
        // UTF-16LE is the default encoding for YAML
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_16LE);
        mapper.writeValue(writer, msbt);
    }
}
