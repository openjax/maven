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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name="fileset")
public abstract class FileSetMojo extends ResourcesMojo {
  private static List<URL> getFiles(final MavenProject project, final List<Resource> projectResources, final FileSetMojo fileSet) throws IOException {
    final List<URL> urls = new ArrayList<>();
    for (final Resource projectResource : projectResources) {
      final File dir = new File(projectResource.getDirectory());
      if (dir.exists()) {
        Files.walk(dir.toPath()).filter(FileSetMojo.filter(project.getBasedir(), fileSet)).forEach(p -> {
          try {
            urls.add(p.toUri().toURL());
          }
          catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
          }
        });
      }
    }

    return urls;
  }

  private static Predicate<Path> filter(final File dir, final FileSetMojo fileSet) {
    return t -> {
      final File file = t.toFile();
      if (!file.isFile())
        return false;

      return filter(dir, file, fileSet.getIncludes()) && !filter(dir, file, fileSet.getExcludes());
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

  public class Configuration extends ResourcesMojo.Configuration {
    private final List<URL> fileSets;
    private final List<String> includes;
    private final List<String> excludes;

    public Configuration(final Configuration configuration) {
      this(configuration, configuration.fileSets, configuration.includes, configuration.excludes);
    }

    private Configuration(final ResourcesMojo.Configuration configuration, final List<URL> fileSets, final List<String> includes, final List<String> excludes) {
      super(configuration);
      this.fileSets = Objects.requireNonNull(fileSets);
      this.includes = includes;
      this.excludes = excludes;
    }

    public List<URL> getFileSets() {
      return this.fileSets;
    }

    public List<String> getIncludes() {
      return this.includes;
    }

    public List<String> getExcludes() {
      return this.excludes;
    }
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

  private List<String> getIncludes() {
    convert();
    return this.includes;
  }

  @Parameter(property="excludes")
  private List<String> excludes;

  private List<String> getExcludes() {
    convert();
    return this.excludes;
  }

  @Override
  public final void execute(final ResourcesMojo.Configuration configuration) throws MojoExecutionException, MojoFailureException {
    try {
      final Map<String,Object> filterParameters = getFilterParameters();
      final List<URL> fileSets = getFiles(project, configuration.getResources(), this);
      if (fileSets.size() == 0 && (filterParameters == null || filterParameters.isEmpty())) {
        if (configuration.getFailOnNoOp())
          throw new MojoExecutionException("Empty input parameters (failOnNoOp=true)");

        getLog().info("Skipping for empty input parameters.");
        return;
      }

      execute(new Configuration(configuration, fileSets, getIncludes(), getExcludes()));
    }
    catch (final DependencyResolutionRequiredException | IOException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  public abstract void execute(Configuration configuration) throws MojoExecutionException, MojoFailureException;
}