package org.example.utils;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class StringUtils {
    public static String sanitizeString(String string) {
        ByteBuffer bytes = StandardCharsets.UTF_16LE
            .encode(string)
            .order(ByteOrder.LITTLE_ENDIAN);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            short charUTF16 = bytes.getShort();

            // if charUTF16 is not visible in utf-8, then replace it with \\uXXXX
            if (charUTF16 < 0x20 || charUTF16 > 0x7E) {
                sb.append("\\u").append(String.format("%04X", charUTF16));
            }
            else {
                sb.append((char) charUTF16);
            }
        }

        return sb.toString();
    }
}
