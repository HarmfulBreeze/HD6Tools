package com.github.piorrro33.hd6tools;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void isPathToHD6() {
        // Valid paths
        Path basic = Path.of("myWonderfulHd6.hd6");
        assertTrue(Utils.isPathToHD6(basic), basic + " is a valid path");
        Path nestedWithSpaces = Path.of("a/b/this incredible file.hd6");
        assertTrue(Utils.isPathToHD6(nestedWithSpaces), nestedWithSpaces + " is a valid path");
        Path oddCapitalization = Path.of("hello.hD6");
        assertTrue(Utils.isPathToHD6(oddCapitalization), oddCapitalization + " is a valid path");
        Path allCaps = Path.of("CAPSLOCK.HD6");
        assertTrue(Utils.isPathToHD6(allCaps), allCaps + " is a valid path");

        // Invalid paths
        Path noExt = Path.of("myFile");
        assertFalse(Utils.isPathToHD6(noExt), noExt + " is not a path to a HD6 file");
        Path datFile = Path.of("aDatFile.dat");
        assertFalse(Utils.isPathToHD6(datFile), datFile + " is not a path to a HD6 file");
        Path hd6InFileName = Path.of("my.hd6.dat");
        assertFalse(Utils.isPathToHD6(hd6InFileName), hd6InFileName + " is not a path to a HD6 file");
        Path hd6AtEndOfFileName = Path.of("myhd6");
        assertFalse(Utils.isPathToHD6(hd6AtEndOfFileName), hd6AtEndOfFileName + " is not a path to a HD6 file");
    }
}