package io.github.youngerier.generator.analysis;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 源文件定位器，根据 Class 查找对应的 Java 源文件。
 *
 * <p>支持多种构建工具的输出目录映射（Maven、Gradle、IntelliJ IDEA），
 * classpath 未命中时回退到按项目目录结构搜索。
 */
@Slf4j
final class SourceFileLocator {

    static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";
    private static final String SRC_TEST_JAVA = "src" + File.separator + "test" + File.separator + "java";
    private static final int MAX_SEARCH_DEPTH = 6;

    /**
     * class 文件路径到源文件路径的转换规则：{构建输出目录, 对应源码目录}。
     */
    private static final String[][] PATH_MAPPINGS = {
            {"/target/classes", "/" + SRC_MAIN_JAVA},              // Maven
            {"/target/test-classes", "/" + SRC_TEST_JAVA},        // Maven Test
            {"/build/classes/java/main", "/" + SRC_MAIN_JAVA},    // Gradle
            {"/build/classes/java/test", "/" + SRC_TEST_JAVA},    // Gradle Test
            {"/out/production/classes", "/" + SRC_MAIN_JAVA},     // IntelliJ IDEA
            {"/out/test/classes", "/" + SRC_TEST_JAVA}            // IntelliJ IDEA Test
    };

    /**
     * 定位类对应的源文件。
     */
    File locate(Class<?> clazz, String moduleName) throws IOException {
        try {
            File sourceFile = locateFromClasspath(clazz);
            if (sourceFile != null && sourceFile.exists()) {
                return sourceFile;
            }
            return locateInProject(clazz, moduleName);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot locate source file for class: " + clazz.getName(), e);
        }
    }

    private File locateFromClasspath(Class<?> clazz) {
        try {
            URL resource = clazz.getResource(clazz.getSimpleName() + ".class");
            if (resource == null) {
                return null;
            }
            Path sourcePath = convertClassPathToSourcePath(Paths.get(resource.toURI()), clazz.getSimpleName());
            if (sourcePath != null && Files.exists(sourcePath)) {
                return sourcePath.toFile();
            }
        } catch (URISyntaxException e) {
            log.debug("Failed to convert class resource to source path for class: {}", clazz.getName(), e);
        }
        return null;
    }

    private Path convertClassPathToSourcePath(Path classPath, String className) {
        String classPathStr = classPath.toString();
        for (String[] mapping : PATH_MAPPINGS) {
            if (classPathStr.contains(mapping[0])) {
                String sourcePathStr = classPathStr
                        .replace(mapping[0], mapping[1])
                        .replace(className + ".class", className + ".java");
                return Paths.get(sourcePathStr);
            }
        }
        return null;
    }

    private File locateInProject(Class<?> clazz, String moduleName) throws IOException {
        String packageName = clazz.getPackage().getName();
        String className = clazz.getSimpleName();

        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        if (moduleName != null && !moduleName.trim().isEmpty()) {
            Path moduleDir = projectRoot.resolve(moduleName);
            if (Files.exists(moduleDir)) {
                File sourceFile = searchInModule(moduleDir, packageName, className);
                if (sourceFile != null) {
                    return sourceFile;
                }
            }
        }

        return searchInProject(projectRoot, packageName, className);
    }

    private File searchInModule(Path moduleDir, String packageName, String className) {
        Path relativePath = Paths.get(packageName.replace('.', File.separatorChar), className + ".java");
        for (String sourceDir : new String[]{SRC_MAIN_JAVA, SRC_TEST_JAVA}) {
            Path sourceFile = moduleDir.resolve(sourceDir).resolve(relativePath);
            if (Files.exists(sourceFile)) {
                return sourceFile.toFile();
            }
        }
        return null;
    }

    private File searchInProject(Path projectRoot, String packageName, String className) throws IOException {
        Path relativePath = Paths.get(packageName.replace('.', File.separatorChar), className + ".java");
        try {
            return Files.walk(projectRoot, MAX_SEARCH_DEPTH)
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equals("java"))
                    .filter(path -> path.toString().contains("src"))
                    .map(javaDir -> javaDir.resolve(relativePath))
                    .filter(Files::exists)
                    .map(Path::toFile)
                    .findFirst()
                    .orElseThrow(() -> new IOException(
                            "Source file not found for class: " + packageName + "." + className));
        } catch (IOException e) {
            throw new IOException("Error searching for source file: " + packageName + "." + className, e);
        }
    }

    /**
     * 从源文件向上查找所属模块的 src/main/java 目录。
     */
    static File findSrcMainJavaDir(File sourceFile) {
        Path current = sourceFile.toPath().toAbsolutePath().normalize().getParent();
        while (current != null) {
            File srcMainJava = current.resolve(SRC_MAIN_JAVA).toFile();
            if (srcMainJava.exists()) {
                return srcMainJava;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "Cannot find src/main/java directory for source file: " + sourceFile.getAbsolutePath());
    }
}
