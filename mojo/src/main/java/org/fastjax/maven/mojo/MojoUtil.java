/* Copyright (c) 2017 FastJAX
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

package org.fastjax.maven.mojo;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.fastjax.net.URLs;

/**
 * Utility functions that perform a variety of operations related to Maven
 * projects, executions, plugins, repositories, and dependencies.
 */
public final class MojoUtil {
  /**
   * Returns the {@code PluginExecution} in the {@code mojoExecution}, if a
   * plugin is currently being executed.
   *
   * @param mojoExecution The {@code MojoExecution}.
   * @return The {@code PluginExecution} in the {@code mojoExecution}, if a
   *         plugin is currently being executed.
   */
  public static PluginExecution getPluginExecution(final MojoExecution mojoExecution) {
    final Plugin plugin = mojoExecution.getPlugin();
    plugin.flushExecutionMap();
    for (final PluginExecution pluginExecution : plugin.getExecutions())
      if (pluginExecution.getId().equals(mojoExecution.getExecutionId()))
        return pluginExecution;

    return null;
  }

  /**
   * Returns {@code true} if the specified {@link MojoExecution} is in a
   * lifecycle phase, and the name of the lifecycle phase contains "test".
   *
   * @param execution The {@code MojoExecution}.
   * @return {@code true} if the specified {@link MojoExecution} is in a
   *         lifecycle phase, and the name of the lifecycle phase contains
   *         "test".
   */
  public static boolean isInTestPhase(final MojoExecution execution) {
    return execution.getLifecyclePhase() != null && execution.getLifecyclePhase().contains("test");
  }

  /**
   * Returns {@code true} if a calling MOJO should skip execution due to the
   * {@code -Dmaven.test.skip} property. If the {@code -Dmaven.test.skip}
   * property is present, this method will return {@code true} when the phase
   * name of MOJO or plugin {@code execution} contains the string "test".
   *
   * @param execution The {@code MojoExecution}.
   * @param mavenTestSkip The {@code -Dmaven.test.skip} property.
   * @return {@code true} if a calling MOJO should skip execution due to the
   *         {@code -Dmaven.test.skip} property.
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
   * Returns an {@code Artifact} representation of {@code dependency}, qualified
   * by {@code artifactHandler}.
   *
   * @param dependency The {@code ComponentDependency}.
   * @param artifactHandler The {@code ArtifactHandler}.
   * @return A {@code Artifact} representation of {@code dependency}, qualified
   *         by {@code artifactHandler}.
   */
  public static Artifact toArtifact(final ComponentDependency dependency, final ArtifactHandler artifactHandler) {
    return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null, dependency.getType(), null, artifactHandler);
  }

  /**
   * Returns an {@code Artifact} representation of {@code dependency}, qualified
   * by {@code artifactHandler}.
   *
   * @param dependency The {@code Dependency}.
   * @param artifactHandler The {@code ArtifactHandler}.
   * @return A {@code Artifact} representation of {@code dependency}, qualified
   *         by {@code artifactHandler}.
   */
  public static Artifact toArtifact(final Dependency dependency, final ArtifactHandler artifactHandler) {
    return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(), dependency.getType(), dependency.getClassifier(), artifactHandler);
  }

  /**
   * Returns the classpath of dependencies for the {@code pluginDescriptor},
   * relative to {@code localRepository}.
   *
   * @param pluginDescriptor The {@code PluginDescriptor}.
   * @param localRepository The local {@code ArtifactRepository}.
   * @param artifactHandler The {@code ArtifactHandler}.
   * @return The classpath of dependencies for the {@code pluginDescriptor},
   *         relative to {@code localRepository}.
   */
  public static List<String> getPluginDependencyClassPath(final PluginDescriptor pluginDescriptor, final ArtifactRepository localRepository, final ArtifactHandler artifactHandler) {
    final List<String> classpath = new ArrayList<>(pluginDescriptor.getDependencies().size());
    for (final ComponentDependency dependency : pluginDescriptor.getDependencies())
      classpath.add(localRepository.getBasedir() + File.separator + localRepository.pathOf(toArtifact(dependency, artifactHandler)));

    return classpath;
  }

  /**
   * Returns the filesystem path of {@code dependency} located in
   * {@code localRepository}.
   *
   * @param localRepository The local repository reference.
   * @param dependency The dependency.
   * @return The filesystem path of {@code dependency} located in
   *         {@code localRepository}.
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
   * Returns a list of dependency paths in the specified {@link MavenProject}.
   *
   * @param project The {@link MavenProject} for which to return the classpath.
   * @param localRepository The local {@link ArtifactRepository}.
   * @return A list of dependency paths in the specified {@link MavenProject}.
   */
  public static List<String> getProjectDependencyPaths(final MavenProject project, final ArtifactRepository localRepository) {
    final List<String> classpath = new ArrayList<>(project.getExecutionProject().getDependencies().size());
    for (final Dependency dependency : project.getExecutionProject().getDependencies())
      classpath.add(getPathOf(localRepository, dependency));

    return classpath;
  }

  /**
   * Creates the directory specified by the {@code dir} parameter, including any
   * necessary but nonexistent parent directories.
   *
   * @param name The label name to refer to in case a
   *          {@code MojoFailureException} is thrown.
   * @param dir The directory path to create.
   * @throws MojoFailureException If {@code dir} points to an existing path that
   *           is a file, or {@code dir} or its necessary but nonexistent parent
   *           directories could not be created.
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
   * Returns a {@link File} array of classpath entries of the specified MOJO
   * project and execution parameters. This method returns classpath entries of
   * "compile" and "runtime" scopes. And, if the specified execution is
   * currently in a test phase, this method also returns classpath entries of
   * the "test" scope, as well as the path entries of the dependency paths in
   * the specified {@link MavenProject}.
   *
   * @param project The {@link MavenProject}.
   * @param execution The {@link MojoExecution}.
   * @param pluginDescriptor The {@link PluginDescriptor}.
   * @param localRepository The {@link ArtifactRepository} representing the
   *          local repository.
   * @param artifactHandler The {@link ArtifactHandler}.
   * @return A {@link File} array of classpath entries of the specified MOJO
   *         project and execution parameters.
   * @throws DependencyResolutionRequiredException If the specified
   *           {@link MavenProject} does not meet dependency resolution
   *           requirements.
   */
  public static File[] getExecutionClasspash(final MavenProject project, final MojoExecution execution, final PluginDescriptor pluginDescriptor, final ArtifactRepository localRepository, final ArtifactHandler artifactHandler) throws DependencyResolutionRequiredException {
    final List<String> classpath = MojoUtil.getPluginDependencyClassPath(pluginDescriptor, localRepository, artifactHandler);
    classpath.addAll(project.getCompileClasspathElements());
    classpath.addAll(project.getRuntimeClasspathElements());
    if (MojoUtil.isInTestPhase(execution)) {
      classpath.addAll(project.getTestClasspathElements());
      classpath.addAll(MojoUtil.getProjectDependencyPaths(project, localRepository));
    }

    final File[] classpathFiles = new File[classpath.size()];
    for (int i = 0; i < classpathFiles.length; ++i)
      classpathFiles[i] = new File(classpath.get(i));

    return classpathFiles;
  }

  private static final Pattern replacePattern = Pattern.compile("^\\/((([^\\/])|(\\\\/))+)\\/((([^\\/])|(\\\\/))+)\\/$");

  /**
   * Returns the renamed file name in the specified {@link URL} as per the
   * regular expression specified by {@code rename}, or the original file name
   * if {@code rename} is null. The RegEx pattern specified by {@code rename}
   * must be in the form: <blockquote>{@code /<search>/<replace>/}</blockquote>
   *
   * @param url The {@link URL} whose file name to rename.
   * @param rename The RegEx pattern by which the file name of {@code url}
   *          should be renamed.
   * @return The renamed file name in the specified {@link URL} as per the
   *         regular expression specified by {@code rename}, or the original
   *         file name if {@code rename} is null.
   * @throws IllegalArgumentException If {@code rename} is malformed.
   * @see URLs#getName(URL)
   */
  public static String getRenamedFileName(final URL url, final String rename) {
    if (rename == null)
      return URLs.getName(url);

    final Matcher matcher = replacePattern.matcher(rename);
    if (!matcher.matches())
      throw new IllegalArgumentException("<rename> tag must have a RegEx in the form: /<search>/<replace>/");

    return URLs.getName(url).replaceAll(matcher.group(1), matcher.group(5));
  }

  private MojoUtil() {
  }
}