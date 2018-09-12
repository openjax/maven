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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;

import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.fastjax.net.URLs;
import org.fastjax.util.Paths;

public class Specification {
  public static Specification parse(final MavenProject project, final MojoExecution mojoExecution) throws MojoFailureException {
    final Plugin plugin = mojoExecution.getPlugin();
    final PluginExecution pluginExecution = MojoUtil.getPluginExecution(mojoExecution);

    final Build build = project.getBuild();
    if (build == null || build.getPlugins() == null)
      throw new MojoFailureException("Configuration is required");

    final Xpp3Dom configuration = plugin.getConfiguration() == null ? pluginExecution == null ? null : (Xpp3Dom)pluginExecution.getConfiguration() : pluginExecution.getConfiguration() == null ? (Xpp3Dom)plugin.getConfiguration() : Xpp3Dom.mergeXpp3Dom((Xpp3Dom)plugin.getConfiguration(), (Xpp3Dom)pluginExecution.getConfiguration());
    return configuration == null ? null : parse(configuration.getChild("manifest"), project);
  }

  private static Specification parse(final Xpp3Dom manifest, final MavenProject project) throws MojoFailureException {
    if (manifest == null)
      throw new MojoFailureException("Manifest is required");

    File destDir = null;
    final LinkedHashSet<URL> resources = new LinkedHashSet<>();
    boolean overwrite = true;

    try {
      for (int j = 0; j < manifest.getChildCount(); j++) {
        final Xpp3Dom element = manifest.getChild(j);
        if ("destDir".equals(element.getName())) {
          destDir = Paths.isAbsolute(element.getValue()) ? new File(element.getValue()) : new File(project.getBasedir(), element.getValue());
          for (final String attribute : element.getAttributeNames()) {
            if (attribute.endsWith("overwrite"))
              overwrite = Boolean.parseBoolean(element.getAttribute(attribute));
          }
        }
        else if ("resources".equals(element.getName())) {
          for (int k = 0; k < element.getChildCount(); k++) {
            final Xpp3Dom schema = element.getChild(k);
            if ("resource".equals(schema.getName())) {
              resources.add(buildURL(project.getFile().getParentFile().getAbsoluteFile(), schema.getValue()));
            }
          }
        }
      }
    }
    catch (final IOException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }

    if (destDir == null)
      throw new MojoFailureException("Manifest.destDir is required");

    return new Specification(overwrite, destDir, resources);
  }

  private static URL buildURL(final File baseDir, final String path) throws MalformedURLException {
    return URLs.isAbsolute(path) ? URLs.makeCanonicalUrlFromPath(path) : baseDir == null ? new File(path).toURI().toURL() : new File(baseDir, path).toURI().toURL();
  }

  private final boolean overwrite;
  private final File destDir;
  private final LinkedHashSet<URL> schemas;

  public Specification(final boolean overwrite, final File destDir, final LinkedHashSet<URL> schemas) {
    this.overwrite = overwrite;
    this.destDir = destDir;
    this.schemas = schemas;
  }

  public boolean getOverwrite() {
    return overwrite;
  }

  public File getDestDir() {
    return destDir;
  }

  public LinkedHashSet<URL> getSchemas() {
    return schemas;
  }
}