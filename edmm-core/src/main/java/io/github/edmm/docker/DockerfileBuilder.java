package io.github.edmm.docker;

import io.github.edmm.docker.support.*;
import org.apache.commons.io.FilenameUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public final class DockerfileBuilder {

    private final List<DockerfileEntry> entries = new ArrayList<>();
    private boolean compress = false;
    private final List<AddEntry> addEntries = new ArrayList<>();
    private final List<EnvEntry> envEntries = new ArrayList<>();
    private int workdirIndex = -1;

    public DockerfileBuilder from(String baseImage) {
        entries.add(new FromEntry(baseImage));
        return this;
    }

    public DockerfileBuilder env(String name, String value) {
        envEntries.add(new EnvEntry(name, value));
        return this;
    }

    public DockerfileBuilder copy(String src, String dest) {
        CopyEntry entry = new CopyEntry(src, dest);
        if (workdirIndex < 0) {
            entries.add(entry);
        } else {
            addEntries.add(entry);
        }
        return this;
    }

    public DockerfileBuilder add(String src, String dest) {
        AddEntry entry = new AddEntry(src, dest);
        if (workdirIndex < 0) {
            entries.add(entry);
        } else {
            addEntries.add(entry);
        }
        if (FilenameUtils.getExtension(src).equals("sh")) {
            entries.add(new RunEntry("chmod +x " + dest));
        }

        return this;
    }

    public DockerfileBuilder run(String command) {
        entries.add(new RunEntry(command));
        return this;
    }

    public DockerfileBuilder volume(String path) {
        entries.add(new VolumeEntry(path));
        return this;
    }

    public DockerfileBuilder expose(Integer port) {
        entries.add(new ExposeEntry(port));
        return this;
    }

    public DockerfileBuilder workdir(String directory) {
        populateAddEntries();
        entries.add(new WorkdirEntry(directory));
        workdirIndex = entries.size() - 1;
        return this;
    }

    public DockerfileBuilder entrypoint(String... args) {
        entries.add(new EntrypointEntry(args));
        return this;
    }

    public DockerfileBuilder cmd(String... args) {
        entries.add(new CmdEntry(args));
        return this;
    }

    public DockerfileBuilder compress() {
        compress = true;
        return this;
    }

    public String build() {
        populateAddEntries();
        populateEnvEntries();
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        for (int i = 0; i < entries.size(); i++) {
            DockerfileEntry prev = i > 0 ? entries.get(i - 1) : null;
            DockerfileEntry current = entries.get(i);
            DockerfileEntry next = i + 1 < entries.size() ? entries.get(i + 1) : null;
            current.append(pw, prev, next, compress);
        }
        return writer.toString();
    }

    private void populateEnvEntries() {
        entries.addAll(1, envEntries);
        envEntries.clear();
    }

    private void populateAddEntries() {
        if (entries.size() <= 1) {
            return;
        }
        if (workdirIndex < 0) {
            entries.addAll(addEntries);
        } else {
            int index = (workdirIndex == 0) ? 1 : workdirIndex;
            entries.addAll(++index, addEntries);
        }
        addEntries.clear();
    }
}
