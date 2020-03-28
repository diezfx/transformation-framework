package io.github.edmm.plugins.multi.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


public class Plan {
    public List<PlanStep> steps;

    public Plan() {
        steps = new ArrayList<>();
    }

    public String toYaml() {
        DumperOptions options = new DumperOptions();
        Yaml yaml = new Yaml(options);
        StringWriter writer = new StringWriter();
        yaml.dump(this, writer);
        return writer.toString();
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}




