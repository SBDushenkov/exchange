package ru.dushenkov;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static ru.dushenkov.Utils.printErr;
import static ru.dushenkov.Utils.println;

public class ArtefactsOnDiscProcessor {

    public static List<Artefact> getArtifactsToDownload(Path m2Path, List<Artefact> contentArtefacts) {

        println("Check existing and prepare list to download. Started");

        List<Artefact> artifactsToDownload = new ArrayList<>();
        for (Artefact artefact : contentArtefacts) {
            Path relarivePath = Paths.get(
                    artefact.groupId.replace('.', '/'),
                    artefact.artifactId,
                    artefact.version
            );
            Path artifactPath = m2Path.resolve(relarivePath);
            if (Files.exists(artifactPath) && Files.isDirectory(artifactPath)) {
                try (Stream<Path> fileList = Files.list(artifactPath)) {
                    String expectedJarFileName = artefact.artifactId + "-" + artefact.version;
                    Optional<Path> jarPath = fileList
                            .filter(p -> p.getFileName().toString().equals(expectedJarFileName + ".jar"))
                            .findFirst();
                    if (jarPath.isEmpty()) {
                        printErr("No jar found for " + artefact + " in " + artifactPath.toAbsolutePath());
                        continue;
                    }

                    println("Jar found for " + artefact + ": " + jarPath.get().toAbsolutePath());
                    String sourceName = expectedJarFileName + "-sources.jar";
                    if (!artifactPath.resolve(sourceName).toFile().exists()) {
                        Artefact sources = new Artefact(
                                artefact,
                                relarivePath.toString(),
                                sourceName,
                                artifactPath.toAbsolutePath().toString(),
                                "sources");
                        artifactsToDownload.add(sources);
                    }
                    String javadocName = expectedJarFileName + "-javadoc.jar";
                    if (!artifactPath.resolve(sourceName).toFile().exists()) {
                        Artefact javadoc = new Artefact(
                                artefact,
                                relarivePath.toString(),
                                javadocName,
                                artifactPath.toAbsolutePath().toString(),
                                "javadoc");

                        artifactsToDownload.add(javadoc);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                printErr("Artifact path does not exist or is not a directory: " + artifactPath.toAbsolutePath());
            }
        }
        println("Artefacts to download: " + artifactsToDownload.size());
        return artifactsToDownload;
    }
}
