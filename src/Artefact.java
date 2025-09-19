package ru.dushenkov;

import java.io.Serializable;

public class Artefact implements Serializable {
    String groupId;
    String artifactId;
    String version;
    String fileName;
    String relativePath;
    String filepath;
    String downloaded;
    String classifier;

    public Artefact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public Artefact(Artefact artefact, String relativePath, String filename, String filepath, String classifier) {
        this.groupId = artefact.groupId;
        this.artifactId = artefact.artifactId;
        this.version = artefact.version;
        this.relativePath = relativePath;
        this.fileName = filename;
        this.filepath = filepath;
        this.classifier = classifier;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}