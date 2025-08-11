package org.nomanspace.electricitymeters.util;

import java.util.Arrays;

public class BinDataDecoder {

    private static byte[] hexStringToByteArray(String string) {
        byte[] binDataByte = new byte[string.length() / 2];
        int z = 0;
        for (int i = 0; i < string.length(); i = i + 2) {

            int oneByteUnit = Integer.parseInt(string.substring(i, i + 2), 16);
            binDataByte[z] = (byte) oneByteUnit;
            z++;
        }
        return binDataByte;
    }

    private static int bcdByteToInt(Byte hexPair) {
        int leftPairChunk = (hexPair >> 4) & 0x0F;
        int rightPairChunk = hexPair & 0x0F;
        return leftPairChunk * 10 + rightPairChunk;
    }

    private static int bcdBytesToInt(byte[] bcdBytes, int offset, int lengthForExecute) {
        int result = 0;
        for (int i = offset + lengthForExecute - 1; i >= offset; i--) {
            int currentChunk = bcdByteToInt(bcdBytes[i]);
            result = result * 100;
            result = result + currentChunk;
        }

        return result;
    }

    private static long littleEndianBytesToLong(byte[] hexByte, int offset, int lengthForExecute) {
        long result = 0L;
        for (int i = offset + lengthForExecute - 1; i >= offset; i--) {
            result = (result << 8);
            result = result | (hexByte[i] & 0xFF);
        }
        return result;
    }

    public static void main(String[] args) {
        String stringExample = "8523AF";

        System.out.println(Arrays.toString(hexStringToByteArray(stringExample)));
    }
}
