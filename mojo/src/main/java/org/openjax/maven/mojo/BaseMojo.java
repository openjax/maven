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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * An abstract class extending {@link AbstractMojo} that provides the following
 * convenience parameters:
 * <ul>
 * <li>execution: The {@link MojoExecution}.</li>
 * <li>failOnNoOp: Whether the {@link Mojo} should fail on no-op. Default:
 * true.</li>
 * <li>skip: Whether the {@link Mojo}'s execution should be skipped. Default:
 * false.</li>
 * </ul>
 */
@Mojo(name="base")
public abstract class BaseMojo extends AbstractMojo {
  public class Configuration {
    private final boolean failOnNoOp;

    public Configuration(final Configuration configuration) {
      this.failOnNoOp = configuration.failOnNoOp;
    }

    private Configuration(final boolean failOnNoOp) {
      this.failOnNoOp = failOnNoOp;
    }

    public boolean getFailOnNoOp() {
      return this.failOnNoOp;
    }
  }

  @Parameter(defaultValue="${mojoExecution}", required=true, readonly=true)
  protected MojoExecution execution;

  @Parameter(defaultValue="${session}", readonly=true, required=true)
  protected MavenSession session;

  @Parameter(defaultValue="${project}", readonly=true, required=true)
  protected MavenProject project;

  @Parameter(property="failOnNoOp")
  private boolean failOnNoOp = true;

  @Parameter(property="maven.test.skip")
  private boolean mavenTestSkip = false;

  @Parameter(property="skipTests")
  private boolean skipTests = false;

  @Parameter(property="skip")
  private boolean skip = false;

  private Boolean inTestPhase;

  /**
   * Returns whether the current execution is in a test phase, which includes
   * any phase whose name contains "test".
   *
   * @return Whether the current execution is in a test phase, which includes
   *         any phase whose name contains "test".
   */
  public boolean isInTestPhase() {
    return inTestPhase == null ? inTestPhase = MojoUtil.isInTestPhase(execution) : inTestPhase;
  }

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipped (skip=true)");
      return;
    }

    if (MojoUtil.shouldSkip(execution, mavenTestSkip | skipTests)) {
      getLog().info("Tests are skipped (" + (mavenTestSkip ? "maven.test.skip" : "skipTests") + "=true)");
      return;
    }

    execute(new Configuration(failOnNoOp));
  }

  /**
   * Perform whatever build-process behavior this {@link Mojo} implements.
   * <p>
   * This is the main trigger for the {@link Mojo} inside the Maven system, and
   * allows the {@link Mojo} to communicate errors.
   *
   * @param configuration The {@link Configuration}.
   * @throws MojoExecutionException If an unexpected problem occurs. Throwing
   *           this exception causes a "BUILD ERROR" message to be displayed.
   * @throws MojoFailureException If an expected problem (such as a compilation
   *           failure) occurs. Throwing this exception causes a "BUILD FAILURE"
   *           message to be displayed.
   */
  public abstract void execute(Configuration configuration) throws MojoExecutionException, MojoFailureException;
}