package ru.dushenkov;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ru.dushenkov.Utils.*;

public class MvnProcessor {

    private static String clearGroupFromNonPrintableChars(String groupId) {
        String reversedGroup = new StringBuffer(groupId).reverse().toString().split("\\s")[0];
        return new StringBuilder(reversedGroup).reverse().toString();
    }

    public static List<Artefact> getArtifacts(Path pomPath, Path settingPath) throws IOException, InterruptedException {
        if (!Files.exists(pomPath)) {
            throw new FileNotFoundException("No pom file found: " + pomPath);
        }
        println("mvn dependency:list started");

        Process process = getProcess(pomPath, settingPath);
        List<Artefact> artefacts = logAndExtract(process);
        process.waitFor(20L, java.util.concurrent.TimeUnit.SECONDS);
        println("mvn dependency:list finished. " + artefacts.size() + " artifacts found.");
        return artefacts;
    }

    private static List<Artefact> logAndExtract(Process process) throws IOException {
        List<Artefact> artefacts = new ArrayList<>();
        boolean artifactsSectionEnd = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {

                String[] parts = line.split(":");
                if (line.contains("BUILD SUCCESS")) {
                    artifactsSectionEnd = true;
                }
                if (!artifactsSectionEnd &&
                        line.startsWith("[") &&
                        parts.length > 4) {

                    Artefact artefact = new Artefact(
                            clearGroupFromNonPrintableChars(parts[0]),
                            parts[1],
                            parts[3]
                    );
                    artefacts.add(artefact);
                    print("> " + i++ + " => ");
                }
                println(line); // send to your app console
            }
        }
        return artefacts;
    }

    private static Process getProcess(Path pomPath, Path settingPath) throws IOException {
        List<String> mvnParams = new ArrayList<>(List.of(
                "mvn",
                "dependency:list",
                "-f", pomPath.toAbsolutePath().toString(), //file
                "-DoutputType=text",
                "-Dverbose",
                "-ff"
        ));
        if (settingPath != null) {
            mvnParams.add("-s");
            mvnParams.add(settingPath.toAbsolutePath().toString());
        }
        ProcessBuilder pb = new ProcessBuilder(mvnParams);

        pb.redirectErrorStream(true);
        Process process = pb.start();
        return process;
    }

    public static void importArtefacts(List<Artefact> artefacts) throws IOException, InterruptedException {
        for (Artefact artefact : artefacts) {

            ProcessBuilder pb = new ProcessBuilder(
                    "mvn",
                    "install:install-file",
                    "-Dfile=" + artefact.downloaded,
                    "-DgroupId=" + artefact.groupId,
                    "-DartifactId=" + artefact.artifactId,
                    "-Dversion=" + artefact.version,
                    "-Dpackaging=jar",
                    "-Dclassifier=" + artefact.classifier
            );

            pb.inheritIO(); // show output
            Process process = pb.start();
            int exitCode = process.waitFor();
            System.out.println(artefact.fileName + " installed with exit code: " + exitCode);
        }
    }

}
