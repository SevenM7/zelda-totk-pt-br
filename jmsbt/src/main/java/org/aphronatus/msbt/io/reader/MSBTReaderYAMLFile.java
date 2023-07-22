package org.aphronatus.msbt.io.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.aphronatus.msbt.MSBT;
import org.aphronatus.msbt.utils.YAMLObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MSBTReaderYAMLFile {

    public static MSBT fromFile(File file) throws IOException {
        ObjectMapper mapper = YAMLObjectMapper.getMapper();
        // UTF-16LE
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_16LE);
        return mapper.readValue(reader, MSBT.class);
    }
}
