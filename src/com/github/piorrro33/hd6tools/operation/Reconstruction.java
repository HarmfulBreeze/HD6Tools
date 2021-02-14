package com.github.piorrro33.hd6tools.operation;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

class Reconstruction {
    private static final Charset CS_SHIFT_JIS = Charset.forName("Shift_JIS");
    private static List<String> FILENAME_DICTIONARY = null;

    public static boolean perform(Path datPath, Path hd6Path, Path sourceFolderPath) {
        // Check if source folder exists and if DAT/HD6 files do not exist
        if (Files.notExists(sourceFolderPath)) {
            System.err.println("Source folder could not be found!");
            return false;
        }
        if (Files.exists(datPath)) {
            System.out.println("Warning! The destination DAT file already exists.\n" +
                    "Do you want to proceed (yes or no)?");
            Scanner sc = new Scanner(System.in);
            final String userAnswer = sc.nextLine();
            if (!userAnswer.equalsIgnoreCase("yes") && !userAnswer.equalsIgnoreCase("y")) {
                // User did not answer yes
                System.out.println("Aborting.");
                return false;
            }
        }
        if (Files.exists(hd6Path)) {
            System.out.println("Warning! The destination HD6 file already exists.\n" +
                    "Do you want to proceed (yes or no)?");
            Scanner sc = new Scanner(System.in);
            final String userAnswer = sc.nextLine();
            if (!userAnswer.equalsIgnoreCase("yes") && !userAnswer.equalsIgnoreCase("y")) {
                // User did not answer yes
                System.out.println("Aborting.");
                return false;
            }
        }

        // Create parent dirs for DAT/HD6
        if (datPath.getParent() != null && Files.notExists(datPath.getParent())) {
            System.out.println("Creating parent folders for DAT file...");
            try {
                Files.createDirectories(datPath.getParent());
            } catch (IOException e) {
                System.err.println("Could not create parent folders for DAT file! " + e.getLocalizedMessage());
                return false;
            }
        }
        if (hd6Path.getParent() != null && Files.notExists(hd6Path.getParent())) {
            System.out.println("Creating parent folders for HD6 file...");
            try {
                Files.createDirectories(hd6Path.getParent());
            } catch (IOException e) {
                System.err.println("Could not create parent folders for HD6 file! " + e.getLocalizedMessage());
                return false;
            }
        }

        // Make a list holding paths to all the files in the source folder.
        System.out.println("Browsing source folder...");
        List<Path> filePathList = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(sourceFolderPath)) {
            List<Path> collect = walk.filter(Files::isRegularFile).collect(Collectors.toList());
            filePathList.addAll(collect);
            filePathList.add(collect.get(collect.size() - 1));
        } catch (IOException e) {
            System.err.println("Error while browsing the source folder! " + e.getLocalizedMessage());
            return false;
        }

        // Process file path list
        System.out.println("Processing file paths...");
        List<Integer> indexList = new LinkedList<>();
        for (Path path : filePathList) {
            String s_relativeFilePath = sourceFolderPath.relativize(path).toString();
            indexList.addAll(getDictionaryIndexes(s_relativeFilePath));
        }
        ByteBuffer bb_nameChunkData = CS_SHIFT_JIS.encode("\0" + String.join("\0", FILENAME_DICTIONARY) + "\0");
        int nameChunkDataSize = bb_nameChunkData.limit();
        int nameChunkDataPadding = (0x4 - (0x34 + nameChunkDataSize) % 0x4) % 0x4;
        System.out.println("Creating filename table...");
        ByteBuffer bb_filenameTable = ByteBuffer.allocate(indexList.size() * 2);
        for (Integer index : indexList) {
            if (index < 0x80) {
                bb_filenameTable.put(index.byteValue());
            } else {
                bb_filenameTable.put((byte) (0x80 + index % 0x80));
                bb_filenameTable.put((byte) (index / 0x80));
            }
        }
        int filenameTableSize = bb_filenameTable.position();
        int filenameTablePadding = (0x4 - filenameTableSize % 0x4) % 0x4;
        bb_filenameTable.rewind();

        // File entries
        System.out.println("Creating file entries...");
        int fileCount = filePathList.size();
        ByteBuffer[] bb_fileEntryArr = new ByteBuffer[fileCount + 1]; // +1 for last dummy
        int startOffset = 0;
        int[] filenameTableOffsets = getFilenameTableOffsets(bb_filenameTable.array());
        for (int i = 0; i < fileCount; i++) {
            Path curFilePath = filePathList.get(i);
            bb_fileEntryArr[i] = ByteBuffer.allocate(8).order(LITTLE_ENDIAN);
            short filenameOffset = (short) filenameTableOffsets[i];
            int curFileSize;
            try {
                curFileSize = (int) Files.size(curFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            bb_fileEntryArr[i].putShort(filenameOffset);
            bb_fileEntryArr[i].put(intToUint24(startOffset >> 0x9));
            bb_fileEntryArr[i].put(intToUint24(curFileSize >> 0x4));
            startOffset += curFileSize + (0x800 - curFileSize % 0x800) % 0x800;
        }
        bb_fileEntryArr[fileCount] = ByteBuffer.allocate(8).order(LITTLE_ENDIAN); // write dummy
        bb_fileEntryArr[fileCount].putShort((short) filenameTableOffsets[fileCount]);
        bb_fileEntryArr[fileCount].put(intToUint24(1));
        bb_fileEntryArr[fileCount].put(intToUint24(0));

        try (OutputStream datStream = new BufferedOutputStream(Files.newOutputStream(datPath));
             OutputStream hd6Stream = new BufferedOutputStream(Files.newOutputStream(hd6Path))) {
            System.out.println("Writing HD6...");
            ByteBuffer bb_header = ByteBuffer.allocate(52).order(ByteOrder.LITTLE_ENDIAN);
            byte[] magic = {0x48, 0x44, 0x36, 0x0}; // HD6\0 in little endian
            bb_header.put(magic);
            bb_header.putInt(0x34); // header size
            bb_header.putInt(nameChunkDataSize);
            bb_header.putInt(FILENAME_DICTIONARY.size() + 2); // name chunk amount (with first and last null bytes)
            bb_header.putInt(0); // unk
            bb_header.putInt(0x34 + nameChunkDataSize + nameChunkDataPadding); // p_filenameTable
            bb_header.putInt(filenameTableSize);
            bb_header.putInt(0); // unk
            bb_header.putInt(0x10); // unk
            bb_header.putInt(bb_fileEntryArr.length); // fileCount
            bb_header.putInt(0x34 + nameChunkDataSize + nameChunkDataPadding + filenameTableSize + filenameTablePadding); // p_fileEntries
            bb_header.putInt(0); // unk
            bb_header.putInt(0x34 + nameChunkDataSize + nameChunkDataPadding + filenameTableSize + filenameTablePadding + (bb_fileEntryArr.length * 0x8)); // fileSize
            hd6Stream.write(bb_header.array());

            while (bb_nameChunkData.hasRemaining()) {
                hd6Stream.write(bb_nameChunkData.get());
            }
            hd6Stream.write(new byte[nameChunkDataPadding]);

            while (bb_filenameTable.position() < filenameTableSize) {
                hd6Stream.write(bb_filenameTable.get());
            }
            for (int i = 0; i < filenameTablePadding; i++) {
                hd6Stream.write(0xCD);
            }

            for (ByteBuffer bb_fileEntry : bb_fileEntryArr) {
                hd6Stream.write(bb_fileEntry.array());
            }
            // Done writing HD6

            System.out.println("Writing DAT...");
            for (Path path : filePathList) {
                int curFileSize = (int) Files.size(path);
                datStream.write(Files.readAllBytes(path));
                datStream.write(new byte[(0x800 - curFileSize % 0x800) % 0x800]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static byte[] intToUint24(int val) {
        byte[] ba = new byte[3];
        ba[0] = (byte) (val & 0xFF);
        ba[1] = (byte) ((val >> 0x8) & 0xFF);
        ba[2] = (byte) ((val >> 0x10) & 0xFF);
        return ba;
    }

    private static List<Integer> getDictionaryIndexes(String str) {
        if (FILENAME_DICTIONARY == null) { // Initialize dictionary
            FILENAME_DICTIONARY = new ArrayList<>(4300); // DC Data0_0 has 4277
        }
        List<Integer> intList = new LinkedList<>();
        String[] split = str.split("(?=[._\\\\-])");
        for (String part : split) {
            OptionalInt dictIndex = IntStream.range(0, FILENAME_DICTIONARY.size())
                    .filter(i -> FILENAME_DICTIONARY.get(i).equals(part))
                    .findFirst();
            dictIndex.ifPresentOrElse(e -> intList.add(e + 1), () -> {
                intList.add(FILENAME_DICTIONARY.size() + 1);
                FILENAME_DICTIONARY.add(part);
            });
        }
        intList.add(0);
        return intList;
    }

    private static int[] getFilenameTableOffsets(byte[] filenameTableArr) {
        List<Integer> filenameOffsetList = new LinkedList<>();
        filenameOffsetList.add(0x00); // first filename is at offset 0x00
        for (int i = 0; i < filenameTableArr.length; i++) {
            if (filenameTableArr[i] == 0x00) {
                filenameOffsetList.add(i + 1); // +1 because next filename table entry starts after the 0x00
            }
        }
        return filenameOffsetList.stream().mapToInt(Integer::intValue).toArray();
    }
}
