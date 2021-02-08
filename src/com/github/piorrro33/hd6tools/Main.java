package com.github.piorrro33.hd6tools;

import com.github.piorrro33.hd6tools.operation.OperationMode;
import com.github.piorrro33.hd6tools.operation.Operations;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class Main {
    public static final String APPLICATION_VERSION = "v0.1";

    public static void usage() {
        // extract/rebuild datfile hd6file sourceDir/destDir
        System.out.println("Usage:");
        System.out.println("./HD6Tools <mode> <datfile> <hd6file> <folder>");
        System.out.println("<mode>: extract, rebuild");
        System.out.println("<datfile>: path to your data file");
        System.out.println("<hd6file>: path to your HD6 file");
        System.out.println("<folder>: path to your destination folder or source folder, depending on the chosen mode");
    }

    public static void main(String[] args) {
        System.out.println("HD6Tools " + APPLICATION_VERSION);
        if (args.length < 4) {
            usage();
            System.exit(1);
        }

        boolean isSuccessful;
        OperationMode mode;
        Path datPath, hd6Path, folderPath;
        try {
            // Get mode and check path validity
            mode = OperationMode.valueOf(args[0].toUpperCase(Locale.ROOT));
            datPath = Paths.get(args[1]);
            hd6Path = Paths.get(args[2]);
            folderPath = Paths.get(args[3]);
            isSuccessful = Operations.perform(mode, datPath, hd6Path, folderPath);
        } catch (InvalidPathException e) {
            System.err.println("Invalid path: " + e.getInput());
            isSuccessful = false;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid operation mode: " + args[0]);
            usage();
            isSuccessful = false;
        }
        if (isSuccessful) {
            System.out.println("Operation completed.");
        } else {
            System.err.println("Operation failed.");
        }
    }
}
