package com.cristiannustes.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PathPattern {

    private static final Pattern VARIABLE = Pattern.compile("^\\{([a-zA-Z][a-zA-Z0-9_]*)}$");

    private final String template;
    private final Pattern compiled;
    private final List<String> variableNames;

    private PathPattern(String template, Pattern compiled, List<String> variableNames) {
        this.template = template;
        this.compiled = compiled;
        this.variableNames = List.copyOf(variableNames);
    }

    public static PathPattern of(String template) {
        if (!template.startsWith("/")) {
            throw new IllegalArgumentException("route template must start with '/': " + template);
        }
        List<String> names = new ArrayList<>();
        StringBuilder regex = new StringBuilder();
        for (String segment : template.substring(1).split("/", -1)) {
            regex.append('/');
            Matcher variable = VARIABLE.matcher(segment);
            if (variable.matches()) {
                names.add(variable.group(1));
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(segment));
            }
        }
        return new PathPattern(template, Pattern.compile(regex.toString()), names);
    }

    public Optional<Map<String, String>> match(String path) {
        Matcher matcher = compiled.matcher(normalise(path));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Map<String, String> variables = new LinkedHashMap<>();
        for (int i = 0; i < variableNames.size(); i++) {
            variables.put(variableNames.get(i), matcher.group(i + 1));
        }
        return Optional.of(variables);
    }

    private static String normalise(String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    public String template() {
        return template;
    }

    @Override
    public String toString() {
        return template;
    }
}
