package com.github.piorrro33.hd6tools.operation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

class Extraction {
    public static boolean perform(Path datPath, Path hd6Path, Path folderPath) {
        if (Files.notExists(datPath)) {
            System.err.println("Data file could not be found!");
            return false;
        }
        if (Files.notExists(hd6Path)) {
            System.err.println("HD6 file could not be found!");
            return false;
        }
        InputStream datStream, hd6Stream;
        try {
            datStream = new BufferedInputStream(Files.newInputStream(datPath));
        } catch (IOException e) {
            System.err.println("Could not open DAT file! " + e.getLocalizedMessage());
            return false;
        }
        try {
            hd6Stream = new BufferedInputStream(Files.newInputStream(hd6Path));
        } catch (IOException e) {
            System.err.println("Could not open HD6 file! " + e.getLocalizedMessage());
            try {
                datStream.close();
            } catch (IOException ioe) {
                System.err.println("Could not close DAT stream! " + ioe.getLocalizedMessage());
            }
            return false;
        }
        try {
            int hd6StreamOffset = 0;
            ByteBuffer bb_nameChunkDataSize = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_filenameTableSize = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_fileCount = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_p_filenameTable = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            ByteBuffer bb_p_fileEntries = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);

            hd6StreamOffset += hd6Stream.skip(8); // magic, p_nameChunkData
            hd6StreamOffset += hd6Stream.read(bb_nameChunkDataSize.array());
            hd6StreamOffset += hd6Stream.skip(8); // nameChunkCount, unk
            hd6StreamOffset += hd6Stream.read(bb_p_filenameTable.array());
            hd6StreamOffset += hd6Stream.read(bb_filenameTableSize.array());
            hd6StreamOffset += hd6Stream.skip(8); // unk, unk
            hd6StreamOffset += hd6Stream.read(bb_fileCount.array());
            hd6StreamOffset += hd6Stream.read(bb_p_fileEntries.array());
            hd6StreamOffset += hd6Stream.skip(8); // p_lbaData, unk, fileSize. end of header

            int nameChunkDataSize = bb_nameChunkDataSize.getInt();
            int p_filenameTable = bb_p_filenameTable.getInt();
            int filenameTableSize = bb_filenameTableSize.getInt();
            int p_fileEntries = bb_p_fileEntries.getInt();
            int fileCount = bb_fileCount.getInt() - 1; // -1 is here to remove the final dummy
            ByteBuffer bb_nameChunkData = ByteBuffer.allocate(nameChunkDataSize).order(LITTLE_ENDIAN);
            ByteBuffer bb_filenameTable = ByteBuffer.allocate(filenameTableSize).order(LITTLE_ENDIAN);
            ByteBuffer[] bb_fileEntryArr = new ByteBuffer[fileCount];

            hd6StreamOffset += hd6Stream.read(bb_nameChunkData.array());
            hd6StreamOffset += hd6Stream.skip(p_filenameTable - hd6StreamOffset); // skip to filenameTable
            hd6StreamOffset += hd6Stream.read(bb_filenameTable.array());
            hd6StreamOffset += hd6Stream.skip(p_fileEntries - hd6StreamOffset); // skip to fileEntries
            for (int i = 0; i < fileCount; i++) {
                bb_fileEntryArr[i] = ByteBuffer.allocate(8).order(LITTLE_ENDIAN);
                hd6StreamOffset += hd6Stream.read(bb_fileEntryArr[i].array());
            }

            int[] startOffsetArr = new int[fileCount];
            int[] fileSizeArr = new int[fileCount];
            String[] nameChunkArr = Charset.forName("Shift_JIS").decode(bb_nameChunkData).toString().split("\0");
            for (int i = 0; i < fileCount; i++) {
                byte[] ba = new byte[3];
                for (int j = 0; j < 3; j++) {
                    ba[j] = bb_fileEntryArr[i].get();
                }
                startOffsetArr[i] = (uint24ToInt(ba) & 0xFFFFFE) << 0x9;
                for (int j = 0; j < 3; j++) {
                    ba[j] = bb_fileEntryArr[i].get();
                }
                fileSizeArr[i] = uint24ToInt(ba) << 0x4;
            }

            String[] filenameArr = new String[fileCount];
            for (int i = 0; i < fileCount; i++) {
                filenameArr[i] = "";
                byte b;
                while ((b = bb_filenameTable.get()) != 0) {
                    int finalIndex;
                    if ((b & 0x80) != 0) { // 2-byte index
                        byte b2 = bb_filenameTable.get();
                        int shortIndex = uint16ToInt(new byte[]{b, b2});
                        int factor = b2 + 0x1;
                        finalIndex = shortIndex - (factor * 0x80);
                    } else { // 1-byte index
                        finalIndex = b;
                    }
                    filenameArr[i] += nameChunkArr[finalIndex];
                }
                System.out.println(filenameArr[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                datStream.close();
            } catch (IOException e) {
                System.err.println("Could not close DAT stream! " + e.getLocalizedMessage());
                return false;
            }
            try {
                hd6Stream.close();
            } catch (IOException e) {
                System.err.println("Could not close HD6 stream! " + e.getLocalizedMessage());
                return false;
            }
        }
        return true;
    }

    private static int uint16ToInt(byte[] ba) {
        return ((ba[0] & 0xFF) | ((ba[1] & 0xFF) << 8));
    }

    private static int uint24ToInt(byte[] ba) {
        return ((ba[0] & 0xFF) | ((ba[1] & 0xFF) << 8) | ((ba[2] & 0xFF) << 16));
    }
}
