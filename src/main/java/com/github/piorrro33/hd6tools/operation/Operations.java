package com.github.piorrro33.hd6tools.operation;

import java.nio.file.Path;

public class Operations {
    public static boolean perform(OperationMode mode, Path datPath, Path hd6Path, Path folderPath) {
        switch (mode) {
            case EXTRACT -> {
                return Extraction.perform(datPath, hd6Path, folderPath);
            }
            case REBUILD -> {
                return Reconstruction.perform(datPath, hd6Path, folderPath);
            }
            default -> {
                System.err.println("Warning: no handler for OperationMode" + mode.toString());
                return false;
            }
        }
    }
}
