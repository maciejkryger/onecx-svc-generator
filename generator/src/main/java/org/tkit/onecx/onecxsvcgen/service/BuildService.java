package org.tkit.onecx.onecxsvcgen.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class BuildService {

    public void runMavenBuild(Path projectPath) {
        try {
            Path normalized = projectPath.toAbsolutePath().normalize();

            List<String> command;
            Path mvnw = normalized.resolve("mvnw");

            if (Files.exists(mvnw)) {
                command = List.of("./mvnw", "clean", "package", "-DskipTests");
            } else if (isWindows()) {
                command = List.of("cmd", "/c", "mvn clean package -DskipTests");
            } else {
                command = List.of("bash", "-lc", "mvn clean package -DskipTests");
            }

            System.out.println("▶ Running Maven build in: " + normalized);
            System.out.println("▶ Command: " + String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(normalized.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("Maven build failed with exit code: " + exit);
            }

            System.out.println("✔ Maven build finished successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to run Maven build in: " + projectPath, e);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}