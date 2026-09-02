package com.cristiannustes.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class TestRunner {

    private static final String FRAMEWORK_PACKAGE = "com.cristiannustes.testing.";

    private final List<String> failures = new ArrayList<>();
    private int executed;
    private int passed;

    private TestRunner() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "out/test-classes");
        String filter = args.length > 1 ? args[1] : "";

        TestRunner runner = new TestRunner();
        long startedAt = System.nanoTime();
        for (Class<?> testClass : runner.discover(root, filter)) {
            runner.runClass(testClass);
        }
        runner.report(Duration.ofNanos(System.nanoTime() - startedAt));

        System.exit(runner.failures.isEmpty() ? 0 : 1);
    }

    private List<Class<?>> discover(Path root, String filter) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("compiled tests not found at " + root.toAbsolutePath());
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files
                .filter(path -> path.toString().endsWith(".class"))
                .map(path -> toClassName(root, path))
                .filter(name -> !name.startsWith(FRAMEWORK_PACKAGE))
                .filter(name -> !name.contains("$"))
                .filter(name -> filter.isEmpty() || name.contains(filter))
                .sorted()
                .map(TestRunner::load)
                .filter(TestRunner::hasTests)
                .toList();
        }
    }

    private static String toClassName(Path root, Path classFile) {
        String relative = root.relativize(classFile).toString();
        return relative
            .substring(0, relative.length() - ".class".length())
            .replace(java.io.File.separatorChar, '.');
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, TestRunner.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("cannot load test class " + name, e);
        }
    }

    private static boolean hasTests(Class<?> candidate) {
        if (Modifier.isAbstract(candidate.getModifiers())) {
            return false;
        }
        return Stream.of(candidate.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(Test.class));
    }

    private void runClass(Class<?> testClass) {
        System.out.println();
        System.out.println(testClass.getSimpleName());

        List<Method> methods = Stream.of(testClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Test.class))
            .sorted(Comparator.comparing(Method::getName))
            .toList();

        for (Method method : methods) {
            runMethod(testClass, method);
        }
    }

    private void runMethod(Class<?> testClass, Method method) {
        Test annotation = method.getAnnotation(Test.class);
        String name = annotation.value().isEmpty() ? method.getName() : annotation.value();
        executed++;
        try {
            Object instance = testClass.getDeclaredConstructor().newInstance();
            method.setAccessible(true);
            method.invoke(instance);
            passed++;
            System.out.printf("  [ ok ] %s%n", name);
        } catch (InvocationTargetException e) {
            recordFailure(testClass, name, e.getCause());
        } catch (ReflectiveOperationException e) {
            recordFailure(testClass, name, e);
        }
    }

    private void recordFailure(Class<?> testClass, String name, Throwable cause) {
        System.out.printf("  [FAIL] %s%n", name);
        String detail = cause instanceof AssertionFailure
            ? cause.getMessage()
            : describeUnexpected(cause);
        System.out.println(indent(detail));
        failures.add("%s > %s".formatted(testClass.getSimpleName(), name));
    }

    private static String describeUnexpected(Throwable cause) {
        Throwable root = cause instanceof UncheckedIOException wrapped ? wrapped.getCause() : cause;
        StringBuilder detail = new StringBuilder(root.getClass().getName() + ": " + root.getMessage());
        StackTraceElement[] trace = root.getStackTrace();
        for (int i = 0; i < Math.min(6, trace.length); i++) {
            detail.append(System.lineSeparator()).append("at ").append(trace[i]);
        }
        return detail.toString();
    }

    private static String indent(String text) {
        return text.lines().map(line -> "         " + line).reduce((a, b) -> a + System.lineSeparator() + b).orElse("");
    }

    private void report(Duration elapsed) {
        System.out.println();
        System.out.println("-".repeat(60));
        System.out.printf("%d tests, %d passed, %d failed in %d ms%n",
            executed, passed, failures.size(), elapsed.toMillis());
        if (!failures.isEmpty()) {
            System.out.println();
            System.out.println("Failed:");
            failures.forEach(failure -> System.out.println("  - " + failure));
        }
    }
}
