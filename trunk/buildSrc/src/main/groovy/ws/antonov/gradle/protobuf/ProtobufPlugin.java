package ws.antonov.gradle.protobuf;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ProjectPluginsContainer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.ConventionValue;
import org.gradle.api.plugins.Convention;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.Compile;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.internal.DynamicObjectAware;
import org.gradle.api.plugins.JavaPluginConvention;

/**
 * Created by IntelliJ IDEA.
 * User: aantonov
 * Date: Feb 19, 2010
 * Time: 9:43:38 AM
 * To change this template use File | Settings | File Templates.
 */

public class ProtobufPlugin implements Plugin {
    public static final String PROTOBUF_CONFIGURATION_NAME = "protobuf";

    public void use(Project project, ProjectPluginsContainer projectPluginsContainer) {
        JavaPlugin javaPlugin = projectPluginsContainer.usePlugin(JavaPlugin.class, project);

        Configuration protobufConfiguration = project.getConfigurations().add(PROTOBUF_CONFIGURATION_NAME).setVisible(false).setTransitive(false).
                setDescription("The protobuf libraries to be used for this Java project.");
        project.getConfigurations().getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(protobufConfiguration);

        configureCompileDefaults(project);
        configureSourceSetDefaults(project, javaPlugin);

    }

    private void configureCompileDefaults(final Project project) {
        project.getTasks().withType(ProtobufCompile.class).allTasks(new Action<ProtobufCompile>() {
            public void execute(ProtobufCompile compile) {
                compile.getConventionMapping().map("defaultSource", new ConventionValue() {
                    public Object getValue(Convention convention, IConventionAware conventionAwareObject) {
                        return mainProtobuf(convention).getProtobuf();
                    }
                });
            }
        });
    }

    private void configureSourceSetDefaults(final Project project, final JavaPlugin javaPlugin) {
        final ProjectInternal projectInternal = (ProjectInternal) project;
        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().allObjects(new Action<SourceSet>() {
            public void execute(SourceSet sourceSet) {
                final DefaultProtobufSourceSet protobufSourceSet = new DefaultProtobufSourceSet(((DefaultSourceSet) sourceSet).getDisplayName(), projectInternal.getFileResolver());
                ((DynamicObjectAware) sourceSet).getConvention().getPlugins().put("protobuf", protobufSourceSet);

                protobufSourceSet.getProtobuf().srcDir(String.format("src/%s/proto", sourceSet.getName()));
                sourceSet.getJava().srcDir(String.format("%s/proto-generated", project.getBuildDir()));
                sourceSet.getResources().getFilter().exclude("**/*.proto");
                //sourceSet.getAllJava().add(protobufSourceSet.getProtobuf().matching(sourceSet.getJava().getFilter()));
                //sourceSet.getAllSource().add(protobufSourceSet.getProtobuf());

                String compileTaskName = sourceSet.getCompileTaskName("proto");
                ProtobufCompile compile = project.getTasks().add(compileTaskName, ProtobufCompile.class);

                String compileJavaTaskName = sourceSet.getCompileTaskName("java");
                Task compileJava = project.getTasks().getByName(compileJavaTaskName);

                javaPlugin.configureForSourceSet(sourceSet, compile);
                compileJava.dependsOn(compile);
                compile.setDescription(String.format("Compiles the %s Protobuf source.", sourceSet.getName()));
                compile.conventionMapping("defaultSource", new ConventionValue() {
                    public Object getValue(Convention convention, IConventionAware conventionAwareObject) {
                        return protobufSourceSet.getProtobuf();
                    }
                });

                project.getTasks().getByName(sourceSet.getClassesTaskName()).dependsOn(compileTaskName);
            }
        });
    }

    private JavaPluginConvention java(Convention convention) {
        return convention.getPlugin(JavaPluginConvention.class);
    }

    private SourceSet main(Convention convention) {
        return convention.getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    }

    private DefaultProtobufSourceSet mainProtobuf(Convention convention) {
        return ((DynamicObjectAware) main(convention)).getConvention().getPlugin(DefaultProtobufSourceSet.class);
    }
}