package com.github.piorrro33.hd6tools;

import com.github.piorrro33.hd6tools.operation.OperationMode;
import com.github.piorrro33.hd6tools.operation.Operations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "HD6Tools", version = Main.APPLICATION_VERSION, description = "Level-5 DAT/HD6 file format tool",
        mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    public static final String APPLICATION_NAME = "HD6Tools";
    public static final String APPLICATION_VERSION = APPLICATION_NAME + " v0.1";

    @Parameters(index = "0", arity = "1", paramLabel = "PATH_1", description = """
            Extract: path to HD6 file.
            Rebuild: path to source folder.""")
    private Path path1;

    @Parameters(index = "1", arity = "0..1", paramLabel = "PATH_2", description = """
            Extract: path to destination folder.
            Rebuild: path to HD6 file.""")
    private Path path2;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println(APPLICATION_VERSION);

        boolean isSuccessful;
        OperationMode mode;
        Path datPath, hd6Path, folderPath;
        if (Utils.isPathToHD6(path1) && Files.isRegularFile(path1)) {
            // path1 is an HD6 file --> Extraction mode
            mode = OperationMode.EXTRACT;
            hd6Path = path1;
            datPath = Utils.hd6PathToDatPath(hd6Path);
            if (path2 == null) {
                // No folder path given, will create a folder after the file name of the HD6 file
                folderPath = Utils.createFolderAfterHD6FileName(hd6Path);
            } else if (Files.isDirectory(path2)) {
                // Folder path given is a valid directory, so we'll use it
                folderPath = path2;
            } else {
                // Second path given is not a directory path, invalid usage
                System.err.println("Error: PATH_2 is not a folder!");
                CommandLine.usage(this, System.out);
                return CommandLine.ExitCode.USAGE;
            }
        } else {
            // TODO: write rebuild
            mode = null;
            datPath = null;
            hd6Path = null;
            folderPath = null;
        }
        try {
            // Get mode and check path validity
            isSuccessful = Operations.perform(mode, datPath, hd6Path, folderPath);
        } catch (InvalidPathException e) {
            System.err.println("Invalid path: " + e.getInput());
            isSuccessful = false;
        }
        if (isSuccessful) {
            System.out.println("Operation completed.");
        } else {
            System.err.println("Operation failed.");
        }
        return CommandLine.ExitCode.OK;
    }
}
