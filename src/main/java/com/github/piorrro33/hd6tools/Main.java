package com.github.piorrro33.hd6tools;

import com.github.piorrro33.hd6tools.operation.OperationMode;
import com.github.piorrro33.hd6tools.operation.Operations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = Main.APPLICATION_NAME, version = Main.APPLICATION_VERSION,
        description = "Level-5 DAT/HD6 file format tool", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    public static final String APPLICATION_NAME = "HD6Tools";
    public static final String APPLICATION_VERSION = APPLICATION_NAME + " v0.2";

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
            } else if (!Files.isRegularFile(path2)) {
                // Folder path given is a valid folder path, so we'll use it
                folderPath = path2;
            } else {
                // Second path given is not a directory path, invalid usage
                System.err.println("Error: PATH_2 is not a folder!");
                CommandLine.usage(this, System.out);
                return CommandLine.ExitCode.USAGE;
            }
        } else if (Files.isDirectory(path1)) {
            // path1 is a directory --> Rebuild mode
            mode = OperationMode.REBUILD;
            folderPath = path1;
            if (path2 == null) {
                // No HD6 file given, will use the folder name as DAT/HD6 file names
                hd6Path = Utils.folderPathToHD6Path(folderPath);
                datPath = Utils.hd6PathToDatPath(hd6Path);
            } else if (Utils.isPathToHD6(path2)) {
                // Path given is a path to an HD6 file, it will be used to make the DAT file path
                hd6Path = path2;
                datPath = Utils.hd6PathToDatPath(hd6Path);
            } else {
                // Second path is not a path to an HD6 file, invalid usage
                System.err.println("Error: PATH_2 is not an HD6 file!");
                CommandLine.usage(this, System.out);
                return CommandLine.ExitCode.USAGE;
            }
        } else {
            // path1 is neither a path to a folder nor an HD6 file, invalid usage
            System.err.println("Error: PATH_1 is not an HD6 file or a folder!");
            CommandLine.usage(this, System.out);
            return CommandLine.ExitCode.USAGE;
        }

        boolean isSuccessful = Operations.perform(mode, datPath, hd6Path, folderPath);
        if (isSuccessful) {
            System.out.println("Operation completed.");
            return 0;
        } else {
            System.err.println("Operation failed.");
            return 1;
        }
    }
}
