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
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.fastjax.io.Files;
import org.fastjax.net.URLs;

@Mojo(name="fileset")
public abstract class FileSetMojo extends ResourcesMojo {
  private static LinkedHashSet<URL> getFiles(final MavenProject project, final List<Resource> projectResources, final FileSetMojo fileSet) throws MalformedURLException {
    final LinkedHashSet<URL> urls = new LinkedHashSet<>();
    for (final Resource projectResource : projectResources) {
      final File dir = new File(projectResource.getDirectory());
      final List<File> xmlFiles = Files.listAll(dir, FileSetMojo.filter(project.getBasedir(), fileSet));
      if (xmlFiles != null)
        for (final File file : xmlFiles)
          urls.add(file.toURI().toURL());
    }

    return urls;
  }

  private static FileFilter filter(final File dir, final FileSetMojo fileSet) {
    return new FileFilter() {
      @Override
      public boolean accept(final File pathname) {
        if (!pathname.isFile())
          return false;

        if (fileSet == null)
          return pathname.getName().endsWith(".xml") || pathname.getName().endsWith(".xsd") || pathname.getName().endsWith(".xsl");

        return filter(dir, pathname, fileSet.getIncludes()) && !filter(dir, pathname, fileSet.getExcludes());
      }
    };
  }

  private static boolean filter(final File dir, final File pathname, final List<String> filters) {
    if (filters == null)
      return false;

    for (final String filter : filters)
      if (pathname.getAbsolutePath().substring(dir.getAbsolutePath().length() + 1).matches(filter))
        return true;

    return false;
  }

  private static void convertToRegex(final List<String> list) {
    if (list != null)
      for (int i = 0; i < list.size(); ++i)
        list.set(i, list.get(i).replace(".", "\\.").replace("**/", ".*").replace("/", "\\/").replace("*", ".*"));
  }

  private boolean converted = false;

  private void convert() {
    if (!converted) {
      convertToRegex(includes);
      convertToRegex(excludes);
      converted = true;
    }
  }

  @Parameter(property="includes")
  private List<String> includes;

  public List<String> getIncludes() {
    convert();
    return this.includes;
  }

  @Parameter(property="excludes")
  private List<String> excludes;

  public List<String> getExcludes() {
    convert();
    return this.excludes;
  }

  @Parameter(property="resources")
  private List<String> resources;

  @Parameter(defaultValue="${localRepository}", readonly=true)
  private ArtifactRepository localRepository;

  @Override
  public void execute(final Configuration configuration) throws MojoExecutionException, MojoFailureException {
    try {
      final LinkedHashSet<URL> urls = getFiles(project, configuration.getResources(), this);
      if (resources != null && resources.size() > 0) {
        final ArtifactHandler artifactHandler = new DefaultArtifactHandler("jar");
        final List<String> classPaths = new ArrayList<>();
        project.getResources().stream().forEach(r -> classPaths.add(r.getDirectory()));
        classPaths.addAll(MojoUtil.getPluginDependencyClassPath((PluginDescriptor)this.getPluginContext().get("pluginDescriptor"), localRepository, artifactHandler));
        classPaths.addAll(project.getRuntimeClasspathElements());
        classPaths.addAll(project.getCompileClasspathElements());
        if (isInTestPhase()) {
          project.getTestResources().stream().forEach(r -> classPaths.add(r.getDirectory()));
          classPaths.addAll(MojoUtil.getProjectExecutionArtifactClassPath(project, localRepository));
          classPaths.addAll(project.getTestClasspathElements());
        }

        final URL[] classPathURLs = new URL[classPaths.size()];
        for (int i = 0; i < classPathURLs.length; ++i) {
          final String path = classPaths.get(i);
          classPathURLs[i] = URLs.makeUrlFromPath(path + (path.endsWith(".jar") ? "" : "/"));
        }

        try (final URLClassLoader classLoader = new URLClassLoader(classPathURLs, Thread.currentThread().getContextClassLoader())) {
          for (final String resource : resources) {
            final URL url = classLoader.getResource(resource);
            if (url == null)
              throw new MojoExecutionException("Resource not found in context classLoader: " + resource);

            urls.add(url);
          }
        }
      }

      if (urls.size() == 0) {
        if (configuration.isFailOnNoOp())
          throw new MojoExecutionException("Failing due to empty resources (failOnNoOp=true).");

        getLog().info("Skipping due to empty resources.");
        return;
      }

      execute(urls);
    }
    catch (final DependencyResolutionRequiredException | IOException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  public abstract void execute(final LinkedHashSet<URL> urls) throws MojoExecutionException, MojoFailureException;
}