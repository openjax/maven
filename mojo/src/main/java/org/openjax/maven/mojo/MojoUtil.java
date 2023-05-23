/* Copyright (c) 2017 OpenJAX
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

package org.openjax.maven.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
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
import org.libj.lang.Strings;

/**
 * Utility functions that perform a variety of operations related to Maven projects, executions, plugins, repositories, and
 * dependencies.
 */
public final class MojoUtil {
  /**
   * Returns the {@link PluginExecution} in the {@code mojoExecution}, if a plugin is currently being executed.
   *
   * @param execution The {@link MojoExecution}.
   * @return The {@link PluginExecution} in the {@code mojoExecution}, if a plugin is currently being executed.
   * @throws NullPointerException If {@code execution} is null.
   */
  public static PluginExecution getPluginExecution(final MojoExecution execution) {
    final Plugin plugin = execution.getPlugin();
    plugin.flushExecutionMap();
    final List<PluginExecution> executions = plugin.getExecutions();
    final int i$ = executions.size();
    if (i$ > 0) {
      if (executions instanceof RandomAccess) {
        int i = 0;
        do { // [RA]
          final PluginExecution pluginExecution = executions.get(i);
          if (pluginExecution.getId().equals(execution.getExecutionId()))
            return pluginExecution;
        }
        while (++i < i$);
      }
      else {
        final Iterator<PluginExecution> i = executions.iterator();
        do { // [I]
          final PluginExecution pluginExecution = i.next();
          if (pluginExecution.getId().equals(execution.getExecutionId()))
            return pluginExecution;
        }
        while (i.hasNext());
      }
    }

    return null;
  }

  /**
   * Returns {@code true} if the specified {@link MojoExecution} is in a lifecycle phase, and the name of the lifecycle phase
   * contains "test".
   *
   * @param execution The {@link MojoExecution}.
   * @return {@code true} if the specified {@link MojoExecution} is in a lifecycle phase, and the name of the lifecycle phase
   *         contains "test".
   * @throws NullPointerException If {@code execution} is null.
   */
  public static boolean isInTestPhase(final MojoExecution execution) {
    return execution.getLifecyclePhase() != null && execution.getLifecyclePhase().contains("test");
  }

  /**
   * Returns {@code true} if a calling MOJO should skip execution due to the {@code -Dmaven.test.skip} property. If the
   * {@code -Dmaven.test.skip} property is present, this method will return {@code true} when the phase name of MOJO or plugin
   * {@code execution} contains the string "test".
   *
   * @param execution The {@link MojoExecution}.
   * @param mavenTestSkip The {@code -Dmaven.test.skip} property.
   * @return {@code true} if a calling MOJO should skip execution due to the {@code -Dmaven.test.skip} property.
   */
  public static boolean shouldSkip(final MojoExecution execution, final boolean mavenTestSkip) {
    if (!mavenTestSkip)
      return false;

    if (execution != null && isInTestPhase(execution))
      return true;

    final PluginExecution pluginExecution = getPluginExecution(execution);
    return pluginExecution != null && pluginExecution.getPhase() != null && pluginExecution.getPhase().contains("test");
  }

  /**
   * Returns an {@link Artifact} representation of {@code dependency}, qualified by {@code artifactHandler}.
   *
   * @param dependency The {@link ComponentDependency}.
   * @param handler The {@link ArtifactHandler}.
   * @return A {@link Artifact} representation of {@code dependency}, qualified by {@code artifactHandler}.
   * @throws NullPointerException If {@code dependency} or {@code handler} is null.
   */
  public static Artifact toArtifact(final ComponentDependency dependency, final ArtifactHandler handler) {
    return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null, dependency.getType(), null, handler);
  }

  /**
   * Returns an {@link Artifact} representation of {@code dependency}, qualified by {@code artifactHandler}.
   *
   * @param dependency The {@link Dependency}.
   * @param handler The {@link ArtifactHandler}.
   * @return A {@link Artifact} representation of {@code dependency}, qualified by {@code artifactHandler}.
   * @throws NullPointerException If {@code dependency} or {@code handler} is null.
   */
  public static Artifact toArtifact(final Dependency dependency, final ArtifactHandler handler) {
    return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(), dependency.getType(), dependency.getClassifier(), handler);
  }

  /**
   * Returns the classpath of dependencies for the {@code pluginDescriptor}, relative to {@code localRepository}.
   *
   * @param descriptor The {@link PluginDescriptor}.
   * @param localRepository The local {@link ArtifactRepository}.
   * @param handler The {@link ArtifactHandler}.
   * @return The classpath of dependencies for the {@code pluginDescriptor}, relative to {@code localRepository}.
   * @throws NullPointerException If {@code descriptor}, {@code localRepository} or {@code handler} is null.
   */
  public static List<String> getPluginDependencyClassPath(final PluginDescriptor descriptor, final ArtifactRepository localRepository, final ArtifactHandler handler) {
    final List<ComponentDependency> dependencies = descriptor.getDependencies();
    final int len = dependencies.size();
    final ArrayList<String> classpath = new ArrayList<>(len);
    for (final ComponentDependency dependency : dependencies) // [L]
      classpath.add(localRepository.getBasedir() + File.separator + localRepository.pathOf(toArtifact(dependency, handler)));

    return classpath;
  }

  /**
   * Returns the filesystem path of {@code dependency} located in {@code localRepository}.
   *
   * @param localRepository The local repository reference.
   * @param dependency The dependency.
   * @return The filesystem path of {@code dependency} located in {@code localRepository}.
   * @throws NullPointerException If {@code localRepository} or {@code dependency} is null.
   */
  public static String getPathOf(final ArtifactRepository localRepository, final Dependency dependency) {
    final StringBuilder builder = new StringBuilder();
    builder.append(localRepository.getBasedir());
    builder.append(File.separatorChar);
    builder.append(dependency.getGroupId().replace('.', File.separatorChar));
    builder.append(File.separatorChar);
    builder.append(dependency.getArtifactId());
    builder.append(File.separatorChar);
    builder.append(dependency.getVersion());
    builder.append(File.separatorChar);
    builder.append(dependency.getArtifactId());
    builder.append('-');
    builder.append(dependency.getVersion());
    if ("test-jar".equals(dependency.getType()))
      builder.append("-tests");

    return builder.append(".jar").toString();
  }

  /**
   * Returns a string array of dependency paths in the specified {@link MavenProject}.
   *
   * @param project The {@link MavenProject} for which to return the classpath.
   * @param localRepository The local {@link ArtifactRepository}.
   * @return A string array of dependency paths in the specified {@link MavenProject}.
   * @throws NullPointerException If {@code project} or {@code localRepository} is null.
   */
  public static String[] getProjectDependencyPaths(final MavenProject project, final ArtifactRepository localRepository) {
    final List<Dependency> dependencies = project.getExecutionProject().getDependencies();
    final int i$ = dependencies.size();
    if (i$ == 0)
      return Strings.EMPTY_ARRAY;

    final String[] classpath = new String[i$];
    final Iterator<Dependency> iterator = dependencies.iterator();
    for (int i = 0; i < i$; ++i) // [I]
      classpath[i] = getPathOf(localRepository, iterator.next());

    return classpath;
  }

  /**
   * Creates the directory specified by the {@code dir} parameter, including any necessary but nonexistent parent directories.
   *
   * @param name The label name to refer to in case a {@link MojoFailureException} is thrown.
   * @param dir The directory path to create.
   * @throws MojoFailureException If {@code dir} points to an existing path that is a file, or {@code dir} or its necessary but
   *           nonexistent parent directories could not be created.
   * @throws NullPointerException If {@code dir} is null.
   */
  public static void assertCreateDir(final String name, final File dir) throws MojoFailureException {
    if (dir.exists()) {
      if (dir.isFile())
        throw new MojoFailureException("Path at " + name + " directory is a file: " + dir.getAbsolutePath());
    }
    else if (!dir.mkdirs()) {
      throw new MojoFailureException("Unable to create " + name + " directory: " + dir.getAbsolutePath());
    }
  }

  /**
   * Returns a {@link File} array of classpath entries of the specified MOJO project and execution parameters. This method returns
   * classpath entries of "compile" and "runtime" scopes. And, if the specified execution is currently in a test phase, this method
   * also returns classpath entries of the "test" scope, as well as the path entries of the dependency paths in the specified
   * {@link MavenProject}.
   *
   * @param project The {@link MavenProject}.
   * @param execution The {@link MojoExecution}.
   * @param descriptor The {@link PluginDescriptor}.
   * @param localRepository The {@link ArtifactRepository} representing the local repository.
   * @param handler The {@link ArtifactHandler}.
   * @return A {@link File} array of classpath entries of the specified MOJO project and execution parameters.
   * @throws DependencyResolutionRequiredException If the specified {@link MavenProject} does not meet dependency resolution
   *           requirements.
   * @throws NullPointerException If {@code project}, {@code execution}, {@code descriptor}, {@code localRepository} or
   *           {@code handler} is null.
   */
  public static File[] getExecutionClasspath(final MavenProject project, final MojoExecution execution, final PluginDescriptor descriptor, final ArtifactRepository localRepository, final ArtifactHandler handler) throws DependencyResolutionRequiredException {
    final List<String> classpath = MojoUtil.getPluginDependencyClassPath(descriptor, localRepository, handler);
    classpath.addAll(project.getCompileClasspathElements());
    classpath.addAll(project.getRuntimeClasspathElements());
    if (MojoUtil.isInTestPhase(execution)) {
      classpath.addAll(project.getTestClasspathElements());
      Collections.addAll(classpath, MojoUtil.getProjectDependencyPaths(project, localRepository));
    }

    final File[] classpathFiles = new File[classpath.size()];
    for (int i = 0, i$ = classpathFiles.length; i < i$; ++i) // [A]
      classpathFiles[i] = new File(classpath.get(i));

    return classpathFiles;
  }

  private MojoUtil() {
  }
}