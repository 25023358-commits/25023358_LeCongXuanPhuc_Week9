package org.example;

import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class PathUtilsTest {

    @Test
    void testPath() {

        PathUtils utils = new PathUtils();

        // Linux/macOS dùng /
        assertEquals("data/test.txt", utils.buildPath());
    }
}