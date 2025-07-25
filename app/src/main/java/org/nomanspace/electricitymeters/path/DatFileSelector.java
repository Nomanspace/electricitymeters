package org.nomanspace.electricitymeters.path;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class DatFileSelector implements PathProvider {
    @Override
    public Path providePath() {
        /*Files.list(new TargetDirectoryProvider().providePath())
                .filter(file -> file.toString().endsWith(".dat"))
                .max(Comparator.comparing(
                        file -> Files.getLastModifiedTime(file).toMillis())).orElse(null);*/

        try (Stream<Path> paths = Files.list(new TargetDirectoryProvider().providePath())) {
            return paths
                    .filter(file -> file.toString().endsWith(".dat"))
                    .max(Comparator.comparing(
                            file -> {
                                try {
                                    return Files.getLastModifiedTime(file).toMillis();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })).orElse(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /*try {
            return Files.list(new TargetDirectoryProvider().providePath())
                    .filter(file -> file.toString().endsWith(".dat"))
                    .max(Comparator.comparing(
                            file -> {
                                try {
                                    return Files.getLastModifiedTime(file).toMillis();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })).orElse(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }
}
