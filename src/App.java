package ru.dushenkov;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static ru.dushenkov.Utils.println;

public class App {

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        Path pomDir = Paths.get(".");
        Path m2Path = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");
        Path mvnSettingsPath = null;

        for (String arg : args) {
            if (arg.startsWith("-pom=")) {
                pomDir = Paths.get(arg.substring(5));
            } else if (arg.startsWith("-m2=")) {
                m2Path = Paths.get(arg.substring(4));
            } else if (arg.startsWith("-settings=")) {
                mvnSettingsPath = Paths.get(arg.substring(10));
            } else if (arg.startsWith("-h")) {
                println("Usage: java -jar app.jar [-pom=path_to_pom] [-m2=path_to_m2] [-settings=path_to_maven_settings]");
                return;
            } else {
                println("Unknown argument: " + arg);
                println("Usage: java -jar app.jar [-pom=path_to_pom] [-m2=path_to_m2] [-settings=path_to_maven_settings]");
                return;
            }
        }
        Path pomPath = pomDir.resolve("pom.xml");

        List<Artefact> artefacts = MvnProcessor.getArtifacts(pomPath, mvnSettingsPath);

        List<Artefact> artifactsToDownload = ArtefactsOnDiscProcessor.getArtifactsToDownload(m2Path, artefacts);

        DesktopProcessor.startDownloadingAndReturnWatchList(artifactsToDownload);
        DesktopProcessor.waitAndProcessDownloads(artifactsToDownload);
        MvnProcessor.importArtefacts(artifactsToDownload);

    }
}