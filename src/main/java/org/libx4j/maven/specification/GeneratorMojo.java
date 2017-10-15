/* Copyright (c) 2011 lib4j
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

package org.libx4j.maven.specification;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.lib4j.net.URLs;

@Mojo(name = "generator")
public abstract class GeneratorMojo extends BaseMojo {
  protected class Configuration {
    private final File destDir;
    private final boolean overwrite;
    private final URL[] resources;
    private final boolean failOnNoOp;

    public Configuration(final File destDir, final boolean overwrite, final URL[] resources, final boolean failOnNoOp) {
      this.destDir = destDir;
      this.overwrite = overwrite;
      this.resources = resources;
      this.failOnNoOp = failOnNoOp;
    }

    public File getDestDir() {
      return this.destDir;
    }

    public boolean isOverwrite() {
      return this.overwrite;
    }

    public URL[] getResources() {
      return this.resources;
    }

    public boolean isFailOnNoOp() {
      return this.failOnNoOp;
    }
  }

  private static URL buildURL(final File baseDir, final String path) throws MalformedURLException {
    return URLs.isAbsolute(path) ? URLs.makeUrlFromPath(path) : baseDir == null ? new File(path).toURI().toURL() : new File(baseDir, path).toURI().toURL();
  }

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject project;

  @Parameter(property = "destDir", required = true)
  private File destDir;

  @Parameter(property = "overwrite")
  private boolean overwrite = true;

  @Parameter(property = "resources", required = true)
  private List<String> resources;

  @Override
  public final void execute(final boolean failOnNoOp) throws MojoExecutionException, MojoFailureException {
    if (destDir.exists()) {
      if (destDir.isFile())
        throw new MojoFailureException("destDir points to a file");
    }
    else if (!destDir.mkdirs()) {
      throw new MojoFailureException("Unable to create destination directory: " + destDir.getAbsolutePath());
    }

    if (resources.size() == 0) {
      if (failOnNoOp)
        throw new MojoExecutionException("Failing due to empty resources (failOnNoOp=true).");

      getLog().info("Skipping due to empty resources.");
      return;
    }

    if (isInTestPhase())
      project.addTestCompileSourceRoot(destDir.getAbsolutePath());
    else
      project.addCompileSourceRoot(destDir.getAbsolutePath());

    try {
      final URL[] urls = new URL[resources.size()];
      for (int i = 0; i < urls.length; i++)
        urls[i] = buildURL(project.getBasedir().getAbsoluteFile(), resources.get(i));

      execute(new Configuration(destDir, overwrite, urls, failOnNoOp));
    }
    catch (final MalformedURLException e) {
      throw new MojoExecutionException(null, e);
    }
  }

  public abstract void execute(final Configuration configuration) throws MojoExecutionException, MojoFailureException;
}