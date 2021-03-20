package com.github.piorrro33.hd6tools.operation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

class Extraction {
    private static final Charset CS_SHIFT_JIS = Charset.forName("Shift_JIS");

    public static boolean perform(Path datPath, Path hd6Path, Path destFolderPath) {
        // Check if dat/hd6 exist, create destination folder if needed
        if (Files.notExists(datPath)) {
            System.err.println("Data file could not be found!");
            return false;
        }
        if (Files.notExists(hd6Path)) {
            System.err.println("HD6 file could not be found!");
            return false;
        }
        if (Files.notExists(destFolderPath)) {
            System.out.println("Destination folder does not exist. Creating it...");
            try {
                Files.createDirectories(destFolderPath);
            } catch (IOException e) {
                System.err.println("Could not create directory at given folder path! " + e.getLocalizedMessage());
                return false;
            }
        }
        try (Stream<Path> walk = Files.walk(destFolderPath, 1)) {
            if (walk.count() > 1) {
                System.out.println("Warning! The destination folder is not empty. Some files may be overwritten.\n" +
                        "Do you want to proceed (yes or no)?");
                String userAnswer = new Scanner(System.in).nextLine();
                if (!userAnswer.equalsIgnoreCase("yes") && !userAnswer.equalsIgnoreCase("y")) {
                    // User did not answer yes
                    System.out.println("Aborting.");
                    return false;
                }
            }
        } catch (IOException e) {
            System.err.println("An I/O error has occurred while walking the folder path! " + e.getLocalizedMessage());
        }

        // Open stream to dat and HD6 files
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

            // Read necessary informations from the header
            System.out.println("Reading header...");
            hd6StreamOffset += hd6Stream.skip(8); // magic, p_nameChunkData
            hd6StreamOffset += hd6Stream.read(bb_nameChunkDataSize.array());
            hd6StreamOffset += hd6Stream.skip(8); // nameChunkCount, unk
            hd6StreamOffset += hd6Stream.read(bb_p_filenameTable.array());
            hd6StreamOffset += hd6Stream.read(bb_filenameTableSize.array());
            hd6StreamOffset += hd6Stream.skip(8); // unk, unk
            hd6StreamOffset += hd6Stream.read(bb_fileCount.array());
            hd6StreamOffset += hd6Stream.read(bb_p_fileEntries.array());
            hd6StreamOffset += hd6Stream.skip(8); // unk, fileSize. end of header

            // Make integers from ByteBuffers...
            int nameChunkDataSize = bb_nameChunkDataSize.getInt();
            int p_filenameTable = bb_p_filenameTable.getInt();
            int filenameTableSize = bb_filenameTableSize.getInt();
            int p_fileEntries = bb_p_fileEntries.getInt();
            int fileCount = bb_fileCount.getInt() - 1; // -1 is here to remove the final dummy
            // ...and create others for nameChunkData, filenameTable and an array for fileEntries
            ByteBuffer bb_nameChunkData = ByteBuffer.allocate(nameChunkDataSize).order(LITTLE_ENDIAN);
            ByteBuffer bb_filenameTable = ByteBuffer.allocate(filenameTableSize).order(LITTLE_ENDIAN);
            ByteBuffer[] bb_fileEntryArr = new ByteBuffer[fileCount];

            // Read nameChunkData, filenameTable and fileEntries
            System.out.println("Reading name chunk data...");
            hd6StreamOffset += hd6Stream.read(bb_nameChunkData.array());
            hd6StreamOffset += hd6Stream.skip(p_filenameTable - hd6StreamOffset); // skip to filenameTable
            System.out.println("Reading filename table...");
            hd6StreamOffset += hd6Stream.read(bb_filenameTable.array());
            hd6StreamOffset += hd6Stream.skip(p_fileEntries - hd6StreamOffset); // skip to fileEntries
            System.out.println("Reading file entries...");
            for (int i = 0; i < fileCount; i++) {
                bb_fileEntryArr[i] = ByteBuffer.allocate(8).order(LITTLE_ENDIAN);
                hd6StreamOffset += hd6Stream.read(bb_fileEntryArr[i].array());
            }

            // Create arrays for start offsets, file sizes, and filenames
            int[] startOffsetArr = new int[fileCount];
            int[] fileSizeArr = new int[fileCount];
            String[] filenameArr = new String[fileCount];

            // Decode name chunk data and make an array
            int[] nameChunkDataOffsetArr = getNameChunkDataOffsetArr(bb_nameChunkData.array());

            // Populate start offset and file size arrays
            for (int i = 0; i < fileCount; i++) {
                bb_fileEntryArr[i].position(2); // Skip filenameTableOffset
                byte[] ba = new byte[3];
                for (int j = 0; j < 3; j++) {
                    ba[j] = bb_fileEntryArr[i].get();
                }
                startOffsetArr[i] = (uint24ToInt(ba) & 0xFFFFFC) << 0x9; // bit magic
                for (int j = 0; j < 3; j++) {
                    ba[j] = bb_fileEntryArr[i].get();
                }
                fileSizeArr[i] = uint24ToInt(ba) << 0x4;
            }

            // Populate filename array
            System.out.println("Decoding filenames...");
            for (int i = 0; i < fileCount; i++) {
                byte b;
                ByteBuffer bb_filename = ByteBuffer.allocate(580); // (Windows MAX_PATH, 260) * (max bytes for a Shift JIS char, 2)
                int filenameOffset = 0;
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
                    int curOffset = nameChunkDataOffsetArr[finalIndex];
                    int nextOffset = nameChunkDataOffsetArr[finalIndex + 1];
                    int chunkSize = nextOffset - curOffset - 1; // -1 to remove trailing NUL
                    bb_nameChunkData.get(curOffset, bb_filename.array(), filenameOffset, chunkSize);
                    filenameOffset += chunkSize;
                }
                filenameArr[i] = CS_SHIFT_JIS.decode(bb_filename.slice(0, filenameOffset)).toString();
            }

            // Write the resulting files
            System.out.println("Writing files...");
            int datStreamOffset = 0;
            for (int i = 0; i < fileCount; i++) {
                byte[] fileData = new byte[fileSizeArr[i]];
                datStreamOffset += datStream.skip(startOffsetArr[i] - datStreamOffset); // skip to next file
                datStreamOffset += datStream.read(fileData);

                // Create folders and write the file
                Path destFilePath = destFolderPath.resolve(filenameArr[i]);
                Path destFileFolderPath = destFilePath.getParent();
                Files.createDirectories(destFileFolderPath);
                Files.write(destFilePath, fileData);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                datStream.close();
            } catch (IOException e) {
                System.err.println("Could not close DAT stream! " + e.getLocalizedMessage());
            }
            try {
                hd6Stream.close();
            } catch (IOException e) {
                System.err.println("Could not close HD6 stream! " + e.getLocalizedMessage());
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

    private static int[] getNameChunkDataOffsetArr(byte[] nameChunkData) {
        List<Integer> offsetList = new ArrayList<>(nameChunkData.length / 2); // safe initial capacity
        offsetList.add(0); // add first null offset
        for (int i = 0; i < nameChunkData.length; i++) {
            if (nameChunkData[i] == 0x00) {
                offsetList.add(i + 1);
            }
        }
        return offsetList.stream().mapToInt(Integer::intValue).toArray();
    }
}
