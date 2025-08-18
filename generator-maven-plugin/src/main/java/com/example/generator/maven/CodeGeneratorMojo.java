package com.example.generator.maven;

import com.example.generator.GeneratorConfig;
import com.example.generator.GeneratorEngine;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodeGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * List of full-qualified POJO class names to generate code for.
     */
    @Parameter(property = "pojo.codegen.pojoPaths", required = true)
    private List<String> pojoPaths;

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

        if (pojoPaths == null || pojoPaths.isEmpty()) {
            getLog().warn("No POJO paths configured. Skipping code generation.");
            return;
        }

        // The GeneratorEngine works inside the specified output directory.
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try {
            // 1. Create GeneratorConfig using the builder
            GeneratorConfig config = GeneratorConfig.builder()
                    .moduleName(project.getArtifactId())
                    .outputBaseDir(outputDir.getAbsolutePath())
                    .pojoPaths(pojoPaths)
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
}
