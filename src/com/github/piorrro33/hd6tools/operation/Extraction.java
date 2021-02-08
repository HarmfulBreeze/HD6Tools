package com.github.piorrro33.hd6tools.operation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class Extraction {
    public static boolean perform(Path datPath, Path hd6Path, Path folderPath) {
        if (Files.notExists(datPath) || Files.notExists(hd6Path)) {
            System.err.println();
        }
        try {
            InputStream datStream = new BufferedInputStream(Files.newInputStream(datPath));
        } catch (IOException e) {
            System.err.println("Could not open DAT file! Reason: " + e.getLocalizedMessage());

        }
        try {
            InputStream hd6Stream = new BufferedInputStream(Files.newInputStream(hd6Path));
        } catch (IOException e) {
            System.err.println("Could not open HD6 file! Reason: " + e.getLocalizedMessage());
        }
        return true;
    }
}
