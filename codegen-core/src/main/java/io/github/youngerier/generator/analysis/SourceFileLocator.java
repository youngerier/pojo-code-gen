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
     * 从源文件向上查找真正**包含该文件**的源码根目录（{@code src/main/java} 或 {@code src/test/java}）。
     *
     * <p>必须识别两类源根：{@link #locate} 在 classpath 未命中时会从 {@code src/test/java}
     * 定位源文件，若符号求解器只注册 {@code src/main/java}，测试源码中的实体其同包类型
     * （如同包下的枚举字段）就无法解析，生成的代码会缺少 import 而无法编译。
     *
     * <p>同一个模块下两类源根可能同时存在（例如 {@code codegen-core}），因此不能只看目录是否存在，
     * 还要判断源文件是否位于该根之下。
     */
    static File findSourceRoot(File sourceFile) {
        Path file = sourceFile.toPath().toAbsolutePath().normalize();
        Path current = file.getParent();
        while (current != null) {
            for (String sourceDir : new String[]{SRC_MAIN_JAVA, SRC_TEST_JAVA}) {
                Path candidate = current.resolve(sourceDir);
                if (Files.isDirectory(candidate) && file.startsWith(candidate)) {
                    return candidate.toFile();
                }
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "Cannot find source root directory for source file: " + sourceFile.getAbsolutePath());
    }
}
