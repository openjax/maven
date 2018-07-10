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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.lib4j.net.URLs;

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

  public static List<String> getProjectExecutionArtifactClassPath(final MavenProject project, final ArtifactRepository localRepository) {
    final List<String> classPath = new ArrayList<String>();
    for (final Dependency dependency : project.getExecutionProject().getDependencies())
      classPath.add(getPathOf(localRepository, dependency));

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

  public static File[] getExecutionClasspash(final MojoExecution execution, final PluginDescriptor pluginDescriptor, final MavenProject project, final ArtifactRepository localRepository, final ArtifactHandler artifactHandler) throws DependencyResolutionRequiredException {
    final List<String> classpath = MojoUtil.getPluginDependencyClassPath(pluginDescriptor, localRepository, artifactHandler);
    classpath.addAll(project.getCompileClasspathElements());
    classpath.addAll(project.getRuntimeClasspathElements());
    if (MojoUtil.isInTestPhase(execution)) {
      classpath.addAll(project.getTestClasspathElements());
      classpath.addAll(MojoUtil.getProjectExecutionArtifactClassPath(project, localRepository));
    }

    final File[] classpathFiles = new File[classpath.size()];
    for (int i = 0; i < classpathFiles.length; i++)
      classpathFiles[i] = new File(classpath.get(i));

    return classpathFiles;
  }

  private static final Pattern replacePattern = Pattern.compile("^\\/((([^\\/])|(\\\\/))+)\\/((([^\\/])|(\\\\/))+)\\/$");

  public static String getRenamedFileName(final URL url, final String rename) throws MojoExecutionException {
    if (rename == null)
      return URLs.getName(url);;

    final Matcher matcher = replacePattern.matcher(rename);
    if (!matcher.matches())
      throw new MojoExecutionException("<rename> tag must have a RegEx in the form: /<search>/<replace>/");

    return URLs.getName(url).replaceAll(matcher.group(1), matcher.group(5));
  }

  private MojoUtil() {
  }
}