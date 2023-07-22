package org.aphronatus.msbt.io.reader;


import org.aphronatus.msbt.Label;
import org.aphronatus.msbt.LabelGroup;
import org.aphronatus.msbt.MSBT;
import org.aphronatus.msbt.utils.ByteBufferFile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MSBTReaderFile {

    public static MSBT fromFile(File file) throws IOException {
        try {
            var bufferFile = new ByteBufferFile(file.getAbsolutePath());

            // Read Header
            var msbt = readHeader(bufferFile);

            // Read Sections
            putSections(msbt, bufferFile);

            return msbt;
        } catch (Exception e) {
            throw new IOException("Error while reading MSBT file", e);
        }
    }

    public static MSBT readHeader(ByteBufferFile bufferFile) {
        String magic = bufferFile.readString(8, StandardCharsets.US_ASCII);

        if (!magic.equals("MsgStdBn")) {
            throw new InternalError("Invalid MSBT file");
        }

        var msbt = new MSBT();

        // Read Byte Order Mark
        msbt.setByteOrderMark(bufferFile.readShort());

        // Unknown
        bufferFile.skip(2);

        // Read Version
        msbt.setVersion(bufferFile.readShort());
        msbt.setSectionSize(bufferFile.readShort());

        // Unknown
        bufferFile.skip(2);

        // Read File Size, but we don't need it
        bufferFile.skip(4);

        // Unknown
        bufferFile.skip(10);

        return msbt;
    }


    record SectionInfo(String name, int size) {}

    public static void putSections(MSBT msbt, ByteBufferFile bufferFile) {
        List<LabelGroup> labelGroups = new ArrayList<>();
        List<String> stringTable = new ArrayList<>();

        for (int i = 0; i < msbt.getSectionSize(); i++) {
            long position = bufferFile.position();

            var sectionInfo = readSectionInfo(bufferFile);
            var bufferSection = bufferFile.read(sectionInfo.size(), ByteOrder.LITTLE_ENDIAN);

            switch (sectionInfo.name()) {
                case "LBL1" -> labelGroups = readLabelGroups(bufferSection);
                case "TXT2" -> stringTable = readStringTable(bufferSection);
            }

            bufferFile.position((int) (position + sectionInfo.size() + 0x10));
            bufferFile.align(16);
        }

        msbt.setLabelGroups(labelGroups);
        msbt.setStringTable(stringTable);
    }

    private static SectionInfo readSectionInfo(ByteBufferFile bufferFile) {
        var name = bufferFile.readString(4, StandardCharsets.US_ASCII);
        var size = bufferFile.readInt();

        // Unknown
        bufferFile.skip(8);

        return new SectionInfo(name, size);
    }

    record OffsetLabel(int stringCount, int stringOffset) {}

    public static List<LabelGroup> readLabelGroups(ByteBuffer sectionData) {
        int offsetCount = sectionData.getInt();

        OffsetLabel[] offsets = new OffsetLabel[offsetCount];
        List<LabelGroup> groups = new ArrayList<>(offsetCount);


        // Read Offsets
        for (int i = 0; i < offsetCount; i++) {
            int stringCount = sectionData.getInt();
            int stringOffset = sectionData.getInt();

            offsets[i] = new OffsetLabel(stringCount, stringOffset);
        }

        // Read Labels
        for (int index = 0; index < offsetCount; index++) {
            OffsetLabel offset = offsets[index];

            // Skip to offset
            sectionData.position(offset.stringOffset());

            var group = new LabelGroup();
            group.setId(index);
            group.setLabels(new ArrayList<>(offset.stringCount()));

            for (int j = 0; j < offset.stringCount(); j++) {
                byte stringLenght = sectionData.get();

                byte[] stringBytes = new byte[stringLenght];
                sectionData.get(stringBytes);

                int tableIndex = sectionData.getInt();

                var label = new Label();
                label.setName(new String(stringBytes, StandardCharsets.UTF_8));
                label.setTableIndex(tableIndex);

                group.add(label);
            }

            groups.add(index, group);
        }

        return groups;
    }

    public static List<String> readStringTable(ByteBuffer sectionData) {
        int textCount = sectionData.getInt();

        int[] textOffsets = new int[textCount];
        List<String> texts = new ArrayList<>(textCount);

        for (int i = 0; i < textCount; i++) {
            textOffsets[i] = sectionData.getInt();
        }

        for (int i = 0; i < textCount; i++) {
            int offset = textOffsets[i];
            sectionData.position(offset);

            StringBuilder text = new StringBuilder();

            // Get next offset or section end if it's the last text
            int nextOffset = i + 1 < textCount ? textOffsets[i + 1] : sectionData.limit();

            byte[] bytes = new byte[2]; // 2 bytes = 1 character

            while (sectionData.position() < nextOffset) {
                sectionData.get(bytes);
                text.append(new String(bytes, StandardCharsets.UTF_16LE));
            }

            texts.add(i, text.toString());
        }

        return texts;
    }

}
