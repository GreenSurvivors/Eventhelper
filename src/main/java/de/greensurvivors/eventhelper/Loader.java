package de.greensurvivors.eventhelper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"}) // paper plugin api
public class Loader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder("maven central", "default", "https://repo1.maven.org/maven2/").build());

        DefaultArtifact caffeineArtifact = new DefaultArtifact("com.github.ben-manes.caffeine:caffeine:3.1.8");
        resolver.addDependency(new Dependency(caffeineArtifact, null));
        DefaultArtifact collectionsArtifact = new DefaultArtifact("com.github.ben-manes.caffeine:caffeine:3.1.8");
        resolver.addDependency(new Dependency(collectionsArtifact, null));

        classpathBuilder.getContext().getLogger().info("loaded libraries {}, version: {} and {}, version: {}",
            caffeineArtifact.getArtifactId(), caffeineArtifact.getVersion(),
            collectionsArtifact.getArtifactId(), collectionsArtifact.getVersion());

        classpathBuilder.addLibrary(resolver);
    }
}
