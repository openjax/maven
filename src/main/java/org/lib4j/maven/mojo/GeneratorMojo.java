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

package org.lib4j.maven.mojo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.fastjax.net.URLs;

@Mojo(name="generator")
public abstract class GeneratorMojo extends BaseMojo {
  protected class Configuration {
    private final File destDir;
    private final boolean overwrite;
    private final URL[][] resources;
    private final boolean failOnNoOp;

    public Configuration(final File destDir, final boolean overwrite, final URL[][] resources, final boolean failOnNoOp) {
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

    public URL[] getResources(int index) {
      return this.resources[index];
    }

    public boolean isFailOnNoOp() {
      return this.failOnNoOp;
    }
  }

  private static URL buildURL(final File baseDir, final String path) throws MalformedURLException {
    return URLs.isAbsolute(path) ? URLs.makeCanonicalUrlFromPath(path) : baseDir == null ? new File(path).toURI().toURL() : new File(baseDir, path).toURI().toURL();
  }

  @Parameter(defaultValue="${project}", required=true, readonly=true)
  protected MavenProject project;

  @Parameter(property="destDir", required=true)
  private File destDir;

  @Parameter(property="overwrite")
  private boolean overwrite = true;

  protected abstract List<String>[] getResources();

  @Override
  public final void execute(final boolean failOnNoOp) throws MojoExecutionException, MojoFailureException {
    MojoUtil.assertCreateDir("destination", destDir);

    final List<String>[] resources = getResources();
    final URL[][] resourceUrls;
    if (resources == null) {
      resourceUrls = null;
    }
    else {
      resourceUrls = new URL[resources.length][];
      try {
        final ResourceLabel resourceLabel = getClass().getDeclaredMethod("getResources").getAnnotation(ResourceLabel.class);
        if (resourceLabel == null)
          throw new MojoFailureException("getResources() must have a @ResourceLabel annotation");

        if (resourceLabel.label().length != resources.length)
          throw new MojoFailureException("@ResourceLabel annotation must have the same length 'label' array as number of resources");

        if (resourceLabel.nonEmpty().length != resources.length)
          throw new MojoFailureException("@ResourceLabel annotation must have the same length 'required' array as number of resources");

        for (int i = 0; i < resources.length; i++) {
          final List<String> resource = resources[i];
          if (resource == null || resource.size() == 0) {
            final String resourcesLabel = resourceLabel.label()[i];
            if (!resourceLabel.nonEmpty()[i])
              continue;

            if (failOnNoOp)
              throw new MojoExecutionException("Empty " + resourcesLabel + " (failOnNoOp=true).");

            getLog().info("Skipping due to empty " + resourcesLabel + ".");
            return;
          }

          resourceUrls[i] = new URL[resource.size()];
          final Iterator<String> iterator = resource.iterator();
          for (int j = 0; j < 10 && iterator.hasNext(); j++)
            resourceUrls[i][j] = buildURL(project.getBasedir().getAbsoluteFile(), iterator.next());
        }
      }
      catch (final MalformedURLException | NoSuchMethodException e) {
        throw new MojoFailureException(null, e);
      }
    }

    execute(new Configuration(destDir, overwrite, resourceUrls, failOnNoOp));

    if (isInTestPhase())
      project.addTestCompileSourceRoot(destDir.getAbsolutePath());
    else
      project.addCompileSourceRoot(destDir.getAbsolutePath());
  }

  public abstract void execute(final Configuration configuration) throws MojoExecutionException, MojoFailureException;
}