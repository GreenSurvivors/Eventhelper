package de.greensurvivors.eventhelper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage") // paper plugin api
public class Loader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        DefaultArtifact caffeineArtifact = new DefaultArtifact("com.github.ben-manes.caffeine:caffeine:3.1.8");

        resolver.addDependency(new Dependency(caffeineArtifact, null));
        classpathBuilder.getContext().getLogger().info("loaded library {}, version: {}", caffeineArtifact.getArtifactId(), caffeineArtifact.getVersion());

        classpathBuilder.addLibrary(resolver);
    }
}
