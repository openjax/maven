/* Copyright (c) 2017 lib4j
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.lib4j.maven.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.ComponentDependency;

public final class MojoUtil {
  public static PluginExecution getPluginExecution(final MojoExecution mojoExecution) {
    final Plugin plugin = mojoExecution.getPlugin();
    for (final PluginExecution pluginExecution : plugin.getExecutions())
      if (pluginExecution.getId().equals(mojoExecution.getExecutionId()))
        return pluginExecution;

    return null;
  }

  public static boolean shouldSkip(final MojoExecution execution, final boolean mavenTestSkip) {
    if (!mavenTestSkip)
      return false;

    if (execution != null && execution.getLifecyclePhase() != null && execution.getLifecyclePhase().contains("test"))
      return true;

    final Plugin plugin = execution.getPlugin();
    plugin.flushExecutionMap();
    final PluginExecution pluginExecution = getPluginExecution(execution);
    return pluginExecution != null && pluginExecution.getPhase() != null && pluginExecution.getPhase().contains("test");
  }

  public static Artifact toArtifact(final ComponentDependency dependency, final ArtifactHandler artifactHandler) {
    return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null, dependency.getType(), null, artifactHandler);
  }

  public static Artifact toArtifact(final Dependency dependency, final ArtifactHandler artifactHandler) {
    return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(), dependency.getType(), dependency.getClassifier(), artifactHandler);
  }

  public static List<String> getPluginDependencyClassPath(final PluginDescriptor pluginDescriptor, final ArtifactRepository localRepository, final ArtifactHandler artifactHandler) {
    final List<String> classPath = new ArrayList<String>();
    for (final ComponentDependency dependency : pluginDescriptor.getDependencies())
      classPath.add(localRepository.getBasedir() + File.separator + localRepository.pathOf(toArtifact(dependency, artifactHandler)));

    return classPath;
  }

  public static String getPathOf(final ArtifactRepository localRepository, final ArtifactHandler artifactHandler, final Dependency dependency) {
    final StringBuilder string = new StringBuilder();
    string.append(localRepository.getBasedir());
    string.append(File.separatorChar);
    string.append(dependency.getGroupId().replace('.', File.separatorChar));
    string.append(File.separatorChar);
    string.append(dependency.getArtifactId());
    string.append(File.separatorChar);
    string.append(dependency.getVersion());
    string.append(File.separatorChar);
    string.append(dependency.getArtifactId());
    string.append('-');
    string.append(dependency.getVersion());
    if ("test-jar".equals(dependency.getType()))
      string.append("-tests");

    return string.append(".jar").toString();
  }

  public static List<String> getProjectExecutionArtifactClassPath(final MavenProject project, final ArtifactRepository localRepository, final ArtifactHandler artifactHandler) {
    final List<String> classPath = new ArrayList<String>();
    for (final Dependency dependency : project.getExecutionProject().getDependencies())
      classPath.add(getPathOf(localRepository, artifactHandler, dependency));

    return classPath;
  }

  public static boolean isInTestPhase(final MojoExecution execution) {
    return execution.getLifecyclePhase() != null && execution.getLifecyclePhase().contains("test");
  }

  public static void assertCreateDir(final String name, final File dir) throws MojoFailureException {
    if (dir.exists()) {
      if (dir.isFile())
        throw new MojoFailureException("Path at " + name + " directory is a file: " + dir.getAbsolutePath());
    }
    else if (!dir.mkdirs()) {
      throw new MojoFailureException("Unable to create " + name + " directory: " + dir.getAbsolutePath());
    }
  }

  private MojoUtil() {
  }
}