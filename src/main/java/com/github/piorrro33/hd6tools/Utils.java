package com.github.piorrro33.hd6tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static Path hd6PathToDatPath(Path hd6Path) {
        String hd6FileName = hd6Path.getFileName().toString();
        String hd6FileNameNoExt = hd6FileName.substring(0, hd6FileName.length() - 4);
        Path hd6Parent = hd6Path.getParent();
        if (hd6Parent != null) {
            return hd6Parent.resolve(hd6FileNameNoExt + ".dat");
        } else {
            return Path.of(hd6FileNameNoExt + ".dat");
        }
    }

    public static Path createFolderAfterHD6FileName(Path hd6Path) throws IOException {
        String hd6FileName = hd6Path.getFileName().toString();
        String hd6FileNameNoExt = hd6FileName.substring(0, hd6FileName.length() - 4);
        Path absHD6Path = hd6Path.toAbsolutePath();
        return Files.createDirectories(absHD6Path.getParent().resolve(hd6FileNameNoExt));
    }

    public static Path folderPathToHD6Path(Path folderPath) {
        String folderName = folderPath.getFileName().toString();
        Path folderParent = folderPath.getParent();
        if (folderParent != null) {
            return folderParent.resolve(folderName + ".hd6");
        } else {
            return Path.of(folderName + ".hd6");
        }
    }

    public static boolean isPathToHD6(Path path) {
        // Case-insensitive regex to check for
        Pattern pattern = Pattern.compile(".*\\.hd6$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(path.toString());
        boolean pathMatches = matcher.matches();
        boolean isDirectory = Files.isDirectory(path);
        return pathMatches && !isDirectory;
    }
}
