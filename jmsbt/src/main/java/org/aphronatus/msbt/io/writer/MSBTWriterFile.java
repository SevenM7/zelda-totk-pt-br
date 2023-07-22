package org.aphronatus.msbt.io.writer;

import org.aphronatus.msbt.MSBT;
import org.aphronatus.msbt.io.MSBTReference;
import org.aphronatus.msbt.utils.FileWriterBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class MSBTWriterFile {
    public static void writeFile(MSBT msbt, File file) throws IOException {
        try (var fileWriter = new FileWriterBuffer(file)) {
            fileWriter.write(writeHeader(msbt).array());

            if (!msbt.getLabelGroups().isEmpty()) {
                fileWriter.write(getLBL1SectionBuffer(msbt).array());
                fileWriter.align(16, (byte) 0xAB);
            }

            if (!msbt.getStringTable().isEmpty()) {
                fileWriter.write(getTXT2SectionBuffer(msbt).array());
                fileWriter.align(16, (byte) 0xAB);
            }

            // write the size of the file
            fileWriter.position(0x12);

            var sizeFileBuffer = ByteBuffer
                    .allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt((int) fileWriter.size());

            fileWriter.write(sizeFileBuffer.array());
        }
    }

    private static ByteBuffer writeHeader(MSBT msbt) throws IOException {
        var header = ByteBuffer.allocate(0x20).order(ByteOrder.LITTLE_ENDIAN);

        header.put(MSBTReference.MSBT_FILE_MAGIC.getBytes(StandardCharsets.US_ASCII));

        header.putShort(msbt.getByteOrderMark());
        header.putShort((short) 0x0000);

        header.putShort(msbt.getVersion());
        header.putShort(msbt.getSectionSize());

        // Size section, we'll write it later
        header.putShort((short) 0x0000);

        // Unknown
        header.put(new byte[10]);

        return header;
    }

    private static ByteBuffer getLBL1SectionBuffer(MSBT msbt) throws IOException {
        var sizeOffsetLabel = msbt.getLabelGroups().size();
        var groups = msbt.getLabelGroups();

        var labelOffsets = new int[sizeOffsetLabel];

        var labelStringTableBuffer = new ByteArrayOutputStream();

        // We first write the labels to get the offsets
        for (int i = 0; i < sizeOffsetLabel; i++) {
            var labelGroup = groups.get(i);
            var sizeLabel = labelGroup.getLabels().size();

            ByteArrayOutputStream labelGroupBuffer = new ByteArrayOutputStream();

            for (int j = 0; j < sizeLabel; j++) {
                var label = labelGroup.getLabels().get(j);
                var sizeString = label.getName().length();

                var labelBuffer = ByteBuffer
                    // 1 byte for the size of the string
                    // sizeString bytes for the string
                    // 4 bytes for the table index
                    .allocate(1 + sizeString + 4)
                    .order(ByteOrder.LITTLE_ENDIAN);

                labelBuffer.put((byte) sizeString);
                labelBuffer.put(label.getName().getBytes(StandardCharsets.UTF_8));
                labelBuffer.putInt(label.getTableIndex());

                labelGroupBuffer.write(labelBuffer.array());
            }

            labelStringTableBuffer.write(labelGroupBuffer.toByteArray());
            labelOffsets[i] = labelGroupBuffer.size();
        }

        // initial offset of labels is the size of the offset labels + 4 bytes for the offset count
        int offset = sizeOffsetLabel * 8 + 4;

        // We then write the offset labels

        ByteArrayOutputStream offsetLabelBufferTable = new ByteArrayOutputStream();

        for (int i = 0; i < sizeOffsetLabel; i++) {
            var stringCount = groups.get(i).getLabels().size();
            var stringOffset = offset;

            var offsetLabelBuffer = ByteBuffer
                // 4 bytes for the string count
                // 4 bytes for the string offset
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN);

            offsetLabelBuffer.putInt(stringCount);
            offsetLabelBuffer.putInt(stringOffset);

            offsetLabelBufferTable.write(offsetLabelBuffer.array());

            offset += labelOffsets[i];
        }

        // 4 bytes for the section size
        // sizeLabelOffsetTable
        // sizeLabelStringTable
        int sizeSection = 4 + offsetLabelBufferTable.size() + labelStringTableBuffer.size();

        // now we write the complete LBL1 section
        ByteBuffer lbl1SectionBuffer = ByteBuffer
            // Label section header size
            // size section
            .allocate(sizeSection + 16)
            .order(ByteOrder.LITTLE_ENDIAN);

        // Header Section
        lbl1SectionBuffer.put(getSectionBuffer(MSBTReference.SECTION_LBL1, sizeSection).array());

        // Label Section Offset Table
        lbl1SectionBuffer.putInt(sizeOffsetLabel);
        lbl1SectionBuffer.put(offsetLabelBufferTable.toByteArray());

        // Label Section String Table
        lbl1SectionBuffer.put(labelStringTableBuffer.toByteArray());

        return lbl1SectionBuffer;
    }

    public static ByteBuffer getTXT2SectionBuffer(MSBT msbt) throws IOException {
        // Texts Offset Table
        // Texts Section String Table

        var texts = msbt.getStringTable();
        var sizeTexts = texts.size();

        int[] offsetTable = new int[sizeTexts];

        // offset size * 4 + 4 bytes for the offset count
        int offset = sizeTexts * 4 + 4;

        ByteArrayOutputStream textsBuffer = new ByteArrayOutputStream();

        for (int i = 0; i < sizeTexts; i++) {
            var text = texts.get(i);
            var textBytes = text.getBytes(StandardCharsets.UTF_16LE);
            var sizeText = textBytes.length;

            textsBuffer.write(textBytes);

            offsetTable[i] = offset;
            offset += sizeText;
        }

        // 4 bytes for offset count
        // 4 bytes for each offset entry
        // size of the string table
        var sectionSize = 4 + offsetTable.length * 4 + textsBuffer.size();

        ByteBuffer txt2SectionBuffer = ByteBuffer
            // 16 bytes for the section header
            // size of the section
            .allocate(16 + sectionSize)
            .order(ByteOrder.LITTLE_ENDIAN);

        txt2SectionBuffer.put(getSectionBuffer(MSBTReference.SECTION_TXT2, sectionSize).array());

        // Texts Section Offset Table
        txt2SectionBuffer.putInt(sizeTexts);

        for (int i = 0; i < sizeTexts; i++) {
            txt2SectionBuffer.putInt(offsetTable[i]);
        }

        // Texts Section String Table
        txt2SectionBuffer.put(textsBuffer.toByteArray());

        return txt2SectionBuffer;
    }

    private static ByteBuffer getSectionBuffer(String name, int size) {
        var buffer = ByteBuffer
            .allocate(16)
            .order(ByteOrder.LITTLE_ENDIAN);

        String nameSection = name.substring(0, 4);

        buffer.put(nameSection.getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(size);
        buffer.put(new byte[8]);

        return buffer;
    }
}
