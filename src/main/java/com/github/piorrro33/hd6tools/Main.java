package com.github.piorrro33.hd6tools;

import com.github.piorrro33.hd6tools.operation.OperationMode;
import com.github.piorrro33.hd6tools.operation.Operations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
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
    private Optional<Path> optPath2;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    private static Path hd6PathToDatPath(Path hd6Path) {
        String hd6FileName = hd6Path.getFileName().toString();
        String hd6FileNameNoExt = hd6FileName.substring(0, hd6FileName.length() - 4);
        return hd6Path.getParent().resolve(hd6FileNameNoExt + ".dat");
    }

    private static Path createFolderAfterHD6FileName(Path hd6Path) throws IOException {
        String hd6FileName = hd6Path.getFileName().toString();
        String hd6FileNameNoExt = hd6FileName.substring(0, hd6FileName.length() - 4);
        return Files.createDirectories(hd6Path.getParent().resolve(hd6FileNameNoExt));
    }

    @Override
    public Integer call() throws Exception {
        System.out.println(APPLICATION_VERSION);

        boolean isSuccessful;
        OperationMode mode;
        Path datPath, hd6Path, folderPath;
        // TODO: make ".HD6" be recognized by pattern
        if (path1.toString().matches(".*.hd6$") && Files.isRegularFile(path1)) {
            // path1 is an HD6 file, so we want to extract it
            mode = OperationMode.EXTRACT;
            hd6Path = path1;
            datPath = hd6PathToDatPath(hd6Path);
            if (optPath2.isEmpty()) {
                folderPath = createFolderAfterHD6FileName(hd6Path);
            } else if (!Files.isDirectory(optPath2.get())) {
                // TODO: folder path is not a folder
                return CommandLine.ExitCode.USAGE;
            } else {
                folderPath = optPath2.get();
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
