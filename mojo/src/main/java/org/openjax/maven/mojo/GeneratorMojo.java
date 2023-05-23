/* Copyright (c) 2011 OpenJAX
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="generator")
public abstract class GeneratorMojo extends FilterMojo {
  public class Configuration extends FilterMojo.Configuration {
    private final File destDir;
    private final boolean overwrite;

    public Configuration(final Configuration configuration) {
      this(configuration, configuration.destDir, configuration.overwrite);
    }

    private Configuration(final FilterMojo.Configuration configuration, final File destDir, final boolean overwrite) {
      super(configuration);
      this.destDir = destDir;
      this.overwrite = overwrite;
    }

    public File getDestDir() {
      return destDir;
    }

    public boolean getOverwrite() {
      return overwrite;
    }
  }

  @Parameter(property="destDir", required=true)
  private File destDir;

  @Parameter(property="overwrite")
  private boolean overwrite = true;

  @Override
  public final void execute(final FilterMojo.Configuration configuration) throws MojoExecutionException, MojoFailureException {
    MojoUtil.assertCreateDir("destination", destDir);

    getLog().info("Writing files to: " + new File("").getAbsoluteFile().toPath().relativize(destDir.getAbsoluteFile().toPath()).toString());
    execute(new Configuration(configuration, destDir, overwrite));

    if (isInTestPhase())
      project.addTestCompileSourceRoot(destDir.getAbsolutePath());
    else
      project.addCompileSourceRoot(destDir.getAbsolutePath());
  }

  public abstract void execute(Configuration configuration) throws MojoExecutionException, MojoFailureException;
}