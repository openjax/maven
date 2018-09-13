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

package org.fastjax.maven.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="base")
public abstract class BaseMojo extends AbstractMojo {
  @Parameter(property="failOnNoOp")
  private boolean failOnNoOp = true;

  @Parameter(property="maven.test.skip")
  private boolean mavenTestSkip = false;

  @Parameter(property="skip")
  private boolean skip = false;

  @Parameter(defaultValue="${mojoExecution}", required=true, readonly=true)
  protected MojoExecution execution;

  private Boolean inTestPhase;

  public boolean isInTestPhase() {
    return inTestPhase == null ? inTestPhase = MojoUtil.isInTestPhase(execution) : inTestPhase;
  }

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipped (skip=true)");
      return;
    }

    if (MojoUtil.shouldSkip(execution, mavenTestSkip)) {
      getLog().info("Tests are skipped (maven.test.skip=true)");
      return;
    }

    execute(failOnNoOp);
  }

  public abstract void execute(final boolean failOnNoOp) throws MojoExecutionException, MojoFailureException;
}