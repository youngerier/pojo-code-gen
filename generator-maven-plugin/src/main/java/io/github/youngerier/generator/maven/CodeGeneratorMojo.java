package io.github.youngerier.generator.maven;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import io.github.youngerier.generator.GeneratorConfig;
import io.github.youngerier.generator.GeneratorEngine;
import io.github.youngerier.generator.analysis.ModelInput;
import io.github.youngerier.generator.analysis.PojoSourceScanner;
import io.github.youngerier.generator.analysis.TypeSolverFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 从标注 {@code @GenModel} 的 POJO 源码生成 DTO、Service、Repository 等代码。
 *
 * <p>绑定 {@code generate-sources} 阶段，直接扫描 {@code .java} 源文件，生成产物在
 * <strong>同一次构建</strong>中随主代码一起编译——不需要先编译，也不再递归启动
 * {@code mvn compile} 子进程。
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class CodeGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * 待扫描的包名白名单：只有包名等于或位于这些包之下的 {@code @GenModel} 类才会生成，
     * 依赖 jar 中的类永远不会被当作来源。
     */
    @Parameter(property = "pojo.codegen.scanPackages")
    private List<String> scanPackages;

    /**
     * 生成代码的根输出目录，产物直接写入该目录下对应包路径。
     */
    @Parameter(property = "pojo.codegen.outputDir",
            defaultValue = "${project.build.directory}/generated-sources/pojo-codegen")
    private File outputDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (scanPackages == null || scanPackages.isEmpty()) {
            getLog().warn("未配置 scanPackages，跳过代码生成。"
                    + "示例: <scanPackages><scanPackage>com.acme.entity</scanPackage></scanPackages>");
            return;
        }

        List<Path> sourceRoots = project.getCompileSourceRoots().stream()
                .map(Path::of)
                .filter(Files::isDirectory)
                .toList();
        if (sourceRoots.isEmpty()) {
            getLog().warn("项目没有可扫描的源码根目录，跳过代码生成。");
            return;
        }

        URLClassLoader dependencyClassLoader = buildDependencyClassLoader();
        try {
            ParserConfiguration parserConfiguration = new ParserConfiguration();
            parserConfiguration.setSymbolResolver(new JavaSymbolSolver(
                    TypeSolverFactory.combined(sourceRoots, dependencyClassLoader)));
            JavaParser javaParser = new JavaParser(parserConfiguration);

            List<ModelInput> models = new PojoSourceScanner(scanPackages, javaParser)
                    .scan(sourceRoots);
            if (models.isEmpty()) {
                getLog().warn("扫描包 " + scanPackages + " 内未找到标注 @GenModel 的类，跳过代码生成。");
                return;
            }

            Files.createDirectories(outputDir.toPath());
            GeneratorConfig config = GeneratorConfig.builder()
                    .outputBaseDir(outputDir.getAbsolutePath())
                    .models(models)
                    .build();
            new GeneratorEngine(config).execute();

            project.addCompileSourceRoot(outputDir.getAbsolutePath());
            getLog().info("代码生成完成，产物目录已加入编译源: " + outputDir.getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("代码生成失败", e);
        } finally {
            closeQuietly(dependencyClassLoader);
        }
    }

    /**
     * 用项目编译期 classpath 构建依赖类加载器，供符号求解器解析第三方库中的字段类型；
     * 父加载器使用插件自身的 ClassLoader，结束后在 finally 中关闭。
     */
    private URLClassLoader buildDependencyClassLoader() throws MojoExecutionException {
        try {
            List<URL> urls = new ArrayList<>();
            for (String element : project.getCompileClasspathElements()) {
                File file = new File(element);
                if (file.exists()) {
                    urls.add(file.toURI().toURL());
                }
            }
            return new URLClassLoader(urls.toArray(new URL[0]), this.getClass().getClassLoader());
        } catch (Exception e) {
            throw new MojoExecutionException("无法构建项目依赖类加载器", e);
        }
    }

    private static void closeQuietly(URLClassLoader classLoader) {
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                // 关闭失败不影响构建结果
            }
        }
    }
}
