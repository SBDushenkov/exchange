package ru.dushenkov;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static ru.dushenkov.Utils.println;

public class DesktopProcessor {

    public static void startDownloadingAndReturnWatchList(List<Artefact> artefacts) throws IOException {
        println("Initiating downloading. Starting browser magic");

        String os = System.getProperty("os.name").toLowerCase();

        Path downloadsDir = Paths.get(getDownloadsDir());

        int alreadyDone = 0;
        for (Artefact artefact : artefacts) {
            Path checkPath = downloadsDir.resolve(artefact.fileName);
            if (checkPath.toFile().exists()) {
                artefact.downloaded = checkPath.toAbsolutePath().toString();
                alreadyDone++;
                continue;
            }

            String url = "https://repo1.maven.org/maven2/" + artefact.relativePath + "/" + artefact.fileName;
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", url);
            } else {
                pb = new ProcessBuilder("xdg-open", url);
            }

            pb.inheritIO(); // optional: attach output/error to your console
            pb.start();
        }
        println("Found " + alreadyDone + " already downloaded. Browser magic done");
    }

    private static String getDownloadsDir() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return System.getProperty("user.home") + "\\Downloads";
        } else {
            return System.getProperty("user.home") + "/Downloads";
        }
    }

    private static boolean checkDownloads(List<Artefact> artefacts) {
        println("Checking downloads. Started");

        String os = System.getProperty("os.name").toLowerCase();

        Path downloadsDir = Paths.get(getDownloadsDir());

        int foundDone = 0;
        int totalDone = 0;
        for (Artefact artefact : artefacts) {
            if (artefact.downloaded != null) {
                totalDone++;
                continue;
            }
            Path checkPath = downloadsDir.resolve(artefact.fileName);
            if (checkPath.toFile().exists()) {
                artefact.downloaded = checkPath.toAbsolutePath().toString();
                foundDone++;
            }

        }
        println("Found " + foundDone + " new downloaded. Ready " + (totalDone + foundDone) + " of " + artefacts.size());
        return (totalDone + foundDone) == artefacts.size();
    }

    public static void waitAndProcessDownloads(List<Artefact> artefacts) throws InterruptedException {
        AtomicBoolean stop = new AtomicBoolean(false);
        CompletableFuture<Void> checker = CompletableFuture.runAsync(() -> {

            while (!stop.get() && !DesktopProcessor.checkDownloads(artefacts)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Non-blocking input thread
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                System.out.println("Press x + ENTER to stop waiting...");
                while (!checker.isDone()) {
                    if (reader.ready()) {                   // check if input is available
                        if (reader.read() == 'x') {       // read the input;
                            stop.set(true);
                            break;
                        }
                    } else {
                        Thread.sleep(200);                // avoid busy-wait
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        checker.join();
        println("All downloads processed or stopped by user.");

        println("Missing downloads:" + artefacts.stream().filter(a -> a.downloaded == null).count());
    }
}


