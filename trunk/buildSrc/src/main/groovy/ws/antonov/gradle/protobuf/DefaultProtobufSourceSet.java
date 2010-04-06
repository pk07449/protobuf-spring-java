package ws.antonov.gradle.protobuf;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.UnionFileTree;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.file.FileTree;
import org.gradle.util.ConfigureUtil;
import groovy.lang.Closure;

/**
 * 
 */

public class DefaultProtobufSourceSet {
    private final SourceDirectorySet protobuf;
    private final UnionFileTree allProtobuf;
    private final PatternFilterable protobufPatterns = new PatternSet();
    private final String type;

    public DefaultProtobufSourceSet(String displayName, FileResolver fileResolver) {
        type = displayName;
        protobuf = new DefaultSourceDirectorySet(String.format("%s Protobuf source", displayName), fileResolver);
        protobuf.getFilter().include("**/*.proto");
        protobufPatterns.include("**/*.proto");
        allProtobuf = new UnionFileTree(String.format("%s Protobuf source", displayName), protobuf.matching(protobufPatterns));
    }

    public String getType() {
        return type;
    }

    public SourceDirectorySet getProtobuf() {
        return protobuf;
    }

    public DefaultProtobufSourceSet protobuf(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getProtobuf());
        return this;
    }

    public PatternFilterable getProtobufSourcePatterns() {
        return protobufPatterns;
    }

    public FileTree getAllProtobuf() {
        return allProtobuf;
    }
}