package com.example.generator.maven;

import com.example.generator.GeneratorConfig;
import com.example.generator.GeneratorEngine;
import com.example.generator.annotation.GenModel;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CodeGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * List of packages to scan for POJOs.
     */
    @Parameter(property = "pojo.codegen.scanPackages", required = true)
    private List<String> scanPackages;

    /**
     * Base directory where the generated Java files will be saved.
     * Note: The final output will be inside a 'src/main/java' subdirectory of this path.
     * Defaults to ${project.build.directory}/generated-sources/
     */
    @Parameter(property = "pojo.codegen.outputDir", defaultValue = "${project.build.directory}/generated-sources/")
    private File outputDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting POJO code generation...");
        getLog().info("Output directory: " + outputDir.getAbsolutePath());

        if (scanPackages == null || scanPackages.isEmpty()) {
            getLog().warn("No packages to scan configured. Skipping code generation.");
            return;
        }

        // 检查项目是否已编译，如果没有则尝试编译
        File outputDirectory = new File(project.getBuild().getOutputDirectory());
        if (!outputDirectory.exists() || outputDirectory.listFiles() == null || outputDirectory.listFiles().length == 0) {
            getLog().info("Project classes not found, attempting to compile project first...");
            try {
                compileProject();
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to compile project before code generation", e);
            }
        }

        // The GeneratorEngine works inside the specified output directory.
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try {
            List<String> pojoClasses = findPojoClasses();
            if (pojoClasses.isEmpty()) {
                getLog().warn("No POJOs with @GenModel annotation found in specified packages. Skipping code generation.");
                return;
            }

            // 1. Create GeneratorConfig using the builder
            GeneratorConfig config = GeneratorConfig.builder()
                    .moduleName(project.getArtifactId())
                    .outputBaseDir(outputDir.getAbsolutePath())
                    .pojoClasses(pojoClasses)
                    .build();

            // 2. Create and run the GeneratorEngine
            GeneratorEngine engine = new GeneratorEngine(config);
            engine.execute();

            // 3. Add the generated sources to the project's compile source roots
            File generatedSourcesDir = new File(outputDir, "src/main/java");
            project.addCompileSourceRoot(generatedSourcesDir.getAbsolutePath());

            getLog().info("Code generation completed successfully.");
            getLog().info("Generated sources added to project: " + generatedSourcesDir.getAbsolutePath());

        } catch (Exception e) {
            getLog().error("Error during code generation", e);
            throw new MojoExecutionException("Error during code generation", e);
        }
    }

    private List<String> findPojoClasses() throws MojoExecutionException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            List<URL> urls = new ArrayList<>();
            
            // 获取编译classpath元素并转换为URL
            List<String> classpathElements = getProjectClasspathElements();
            
            for (String element : classpathElements) {
                try {
                    File file = new File(element);
                    if (file.exists()) {
                        urls.add(file.toURI().toURL());
                    }
                } catch (Exception e) {
                    getLog().warn("Failed to convert classpath element to URL: " + element, e);
                }
            }
            
            // 如果没有找到任何URL，至少添加当前项目的输出目录
            if (urls.isEmpty()) {
                File outputDir = new File(project.getBuild().getOutputDirectory());
                if (outputDir.exists()) {
                    urls.add(outputDir.toURI().toURL());
                }
            }
            
            // 创建自定义类加载器
            URLClassLoader customClassLoader = new URLClassLoader(
                urls.toArray(new URL[0]), 
                this.getClass().getClassLoader()
            );
            
            // 设置线程上下文类加载器
            Thread.currentThread().setContextClassLoader(customClassLoader);

            // 使用Reflections扫描标注了@GenModel的类
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(urls)
                    .setScanners(Scanners.TypesAnnotated)
                    .forPackages(scanPackages.toArray(new String[0]))
                    .addClassLoaders(customClassLoader));

            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(GenModel.class);
            
            List<String> result = annotatedClasses.stream()
                    .map(Class::getName)
                    .collect(Collectors.toList());
            
            getLog().info("Found " + result.size() + " classes annotated with @GenModel: " + result);
            
            return result;
            
        } catch (Exception e) {
            throw new MojoExecutionException("Error scanning for POJO classes", e);
        } finally {
            // 恢复原始的类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
    
    /**
     * 安全地获取项目classpath元素
     */
    @SuppressWarnings("unchecked")
    private List<String> getProjectClasspathElements() throws MojoExecutionException {
        try {
            // 使用反射来调用getCompileClasspathElements方法，避免直接依赖异常类型
            Object result = project.getClass().getMethod("getCompileClasspathElements").invoke(project);
            return (List<String>) result;
        } catch (Exception e) {
            getLog().warn("Failed to get compile classpath elements, falling back to output directory", e);
            // 回退方案：只使用项目的输出目录
            List<String> fallback = new ArrayList<>();
            String outputDirectory = project.getBuild().getOutputDirectory();
            if (outputDirectory != null) {
                fallback.add(outputDirectory);
            }
            return fallback;
        }
    }

    /**
     * 编译项目以确保类文件存在
     */
    private void compileProject() throws Exception {
        getLog().info("Compiling project to ensure class files exist...");
        
        // 使用 Maven 的内部 API 来编译项目
        try {
            // 构建简单的编译命令
            ProcessBuilder pb = new ProcessBuilder("mvn", "compile");
            pb.directory(project.getBasedir());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取输出但不打印（避免干扰）
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                getLog().warn("Compilation process returned non-zero exit code: " + exitCode);
                getLog().debug("Compilation output: " + output.toString());
            } else {
                getLog().info("Project compiled successfully");
            }
        } catch (Exception e) {
            getLog().warn("Failed to compile project automatically: " + e.getMessage());
            getLog().warn("Please run 'mvn compile' manually before using this plugin");
            throw e;
        }
    }
}
