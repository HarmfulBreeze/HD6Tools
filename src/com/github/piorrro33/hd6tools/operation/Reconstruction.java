package com.github.piorrro33.hd6tools.operation;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

class Reconstruction {
    private static final Charset CS_SHIFT_JIS = Charset.forName("Shift_JIS");

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
        if (datPath.getParent() != null) {
            try {
                Files.createDirectories(datPath.getParent());
            } catch (IOException e) {
                System.err.println("Could not create parent folders for DAT file! " + e.getLocalizedMessage());
                return false;
            }
        }
        if (hd6Path.getParent() != null) {
            try {
                Files.createDirectories(hd6Path.getParent());
            } catch (IOException e) {
                System.err.println("Could not create parent folders for HD6 file! " + e.getLocalizedMessage());
                return false;
            }
        }

        /*
         * Make a list holding paths to all the files in the source folder.
         */
        List<Path> filePathList = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(sourceFolderPath)) {
            walk.filter(Files::isRegularFile).forEach(filePathList::add);
        } catch (IOException e) {
            System.err.println("Error while browsing the source folder! " + e.getLocalizedMessage());
            return false;
        }

        String[] strArr = new String[filePathList.size()];
        for (int i = 0; i < filePathList.size(); i++) {
            strArr[i] = sourceFolderPath.relativize(filePathList.get(i)).toString();
        }
        String s_nameChunkData = String.join("\0", strArr) + "\0";
        ByteBuffer bb_nameChunkData = CS_SHIFT_JIS.encode(s_nameChunkData);
        int nameChunkDataSize = bb_nameChunkData.remaining();
        int nameChunkDataPadding = 0x10 - ((0x34 + nameChunkDataSize) % 0x10);

        byte[] ba;
        short[] sa = null;
        int filenameTableSize;
        if (filePathList.size() < 0x80) {
            ba = new byte[filePathList.size()];
            for (byte i = 0; i < filePathList.size(); i++) {
                ba[i] = i;
            }
            filenameTableSize = ba.length * Byte.BYTES;
        } else {
            ba = new byte[0x80];
            sa = new short[filePathList.size() - 0x80];
            for (int i = 0; i < filePathList.size(); i++) {
                if (i < 0x80) {
                    ba[i] = (byte) i;
                } else {
                    sa[i - 0x80] = (short) ((i / 0x80 << 8) | (0x80 + i % 0x80));
                }
            }
            filenameTableSize = (ba.length * Byte.BYTES) + (sa.length * Short.BYTES);
        }
        int filenameTablePadding = 0x10 - (nameChunkDataSize % 0x10);

        try (OutputStream datStream = new BufferedOutputStream(Files.newOutputStream(datPath));
             OutputStream hd6Stream = new BufferedOutputStream(Files.newOutputStream(hd6Path))) {
            ByteBuffer bb_header = ByteBuffer.allocate(52).order(ByteOrder.LITTLE_ENDIAN);
            byte[] magic = {0x48, 0x44, 0x36, 0x0}; // HD6\0 in little endian
            bb_header.put(magic);
            bb_header.putInt(0x34); // header size
            bb_header.putInt(nameChunkDataSize);
            bb_header.putInt(filePathList.size()); // name chunk amount (I'm lazy so it's file path size)
            bb_header.putInt(0); // unk
            bb_header.putInt(0x34 + nameChunkDataSize + nameChunkDataPadding); // p_filenameTable
            bb_header.putInt(filenameTableSize);
            bb_header.putInt(0); // unk
            bb_header.putInt(0x10); // unk
            bb_header.putInt(filePathList.size()); // fileCount
            bb_header.putInt(0x34 + nameChunkDataSize + nameChunkDataPadding + filenameTableSize + filenameTablePadding); // p_fileEntries
            bb_header.putInt(0); // unk
            bb_header.putInt(0x34 + nameChunkDataSize + nameChunkDataPadding + filenameTableSize + filenameTablePadding + (filePathList.size() * 0x8)); // fileSize
            hd6Stream.write(bb_header.array());

            while (bb_nameChunkData.hasRemaining()) {
                hd6Stream.write(bb_nameChunkData.get());
            }
            hd6Stream.write(new byte[nameChunkDataPadding]);

            ByteBuffer bb_filenameTable = ByteBuffer.allocate(filenameTableSize).order(LITTLE_ENDIAN);
            bb_filenameTable.put(ba);
            if (sa != null) {
                bb_filenameTable.asShortBuffer().put(sa);
            }
            hd6Stream.write(bb_filenameTable.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}
