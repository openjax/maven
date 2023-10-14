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
 * An abstract class extending {@link AbstractMojo} that provides the following convenience parameters:
 * <ul>
 * <li>execution: The {@link MojoExecution}.</li>
 * <li>failOnNoOp: Whether the {@link Mojo} should fail on no-op. Default: true.</li>
 * <li>skip: Whether the {@link Mojo}'s execution should be skipped. Default: false.</li>
 * </ul>
 */
@Mojo(name = "base")
public abstract class BaseMojo extends AbstractMojo {
  public static class Configuration {
    private final boolean failOnNoOp;

    public Configuration(final Configuration configuration) {
      this.failOnNoOp = configuration.failOnNoOp;
    }

    private Configuration(final boolean failOnNoOp) {
      this.failOnNoOp = failOnNoOp;
    }

    public boolean getFailOnNoOp() {
      return failOnNoOp;
    }
  }

  @Parameter(defaultValue = "${mojoExecution}", required = true, readonly = true)
  private MojoExecution execution;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${settings.offline}", required = true, readonly = true)
  private boolean offline;

  @Parameter(property = "failOnNoOp")
  private boolean failOnNoOp = true;

  @Parameter(property = "skipTests")
  private boolean skipTests = false;

  @Parameter(property = "maven.test.skip.exec")
  private boolean mavenTestSkipExec = false;

  @Parameter(property = "maven.test.skip")
  private boolean mavenTestSkip = false;

  @Parameter(property = "skip")
  private boolean skip = false;

  private Boolean inTestPhase;

  protected MojoExecution getExecution() {
    return execution;
  }

  protected MavenSession getSession() {
    return session;
  }

  protected MavenProject getProject() {
    return project;
  }

  protected boolean getOffline() {
    return offline;
  }

  protected boolean getFailOnNoOp() {
    return failOnNoOp;
  }

  protected boolean getMavenTestSkipExec() {
    return mavenTestSkipExec;
  }

  protected boolean getMavenTestSkip() {
    return mavenTestSkip;
  }

  protected boolean getSkip() {
    return skip;
  }

  protected Boolean getInTestPhase() {
    return inTestPhase;
  }

  /**
   * Specifies whether the current execution is in a test phase, which includes any phase whose name contains "test".
   *
   * @return Whether the current execution is in a test phase, which includes any phase whose name contains "test".
   */
  protected final boolean isInTestPhase() {
    return inTestPhase == null ? inTestPhase = MojoUtil.isInTestPhase(getExecution()) : inTestPhase;
  }

  /**
   * Specifies whether the current execution is run with {@code -DskipTests} or {@code -Dmaven.test.skip.exec}.
   *
   * @return Whether the current execution is run with {@code -DskipTests} or {@code -Dmaven.test.skip.exec}.
   */
  protected final boolean isSkipTests() {
    return skipTests || mavenTestSkipExec;
  }

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipped (skip=true)");
      return;
    }

    if (MojoUtil.shouldSkip(getExecution(), mavenTestSkip)) {
      getLog().info("Tests are skipped (maven.test.skip=true)");
      return;
    }

    execute(new Configuration(failOnNoOp));
  }

  /**
   * Perform whatever build-process behavior this {@link Mojo} implements.
   * <p>
   * This is the main trigger for the {@link Mojo} inside the Maven system, and allows the {@link Mojo} to communicate errors.
   *
   * @param configuration The {@link Configuration}.
   * @throws MojoExecutionException If an unexpected problem occurs. Throwing this exception causes a "BUILD ERROR" message to be
   *           displayed.
   * @throws MojoFailureException If an expected problem (such as a compilation failure) occurs. Throwing this exception causes a
   *           "BUILD FAILURE" message to be displayed.
   */
  public abstract void execute(Configuration configuration) throws MojoExecutionException, MojoFailureException;
}