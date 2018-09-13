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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.fastjax.util.Collections;

@Mojo(name="resources")
public abstract class ResourcesMojo extends BaseMojo {
  protected class Configuration {
    private final boolean failOnNoOp;
    private final List<Resource> mainResources;
    private final List<Resource> testResources;
    private List<Resource> resources;

    public Configuration(final boolean failOnNoOp, final List<Resource> mainResources, final List<Resource> testResources) {
      this.failOnNoOp = failOnNoOp;
      this.mainResources = mainResources;
      this.testResources = testResources;
    }

    public boolean isFailOnNoOp() {
      return this.failOnNoOp;
    }

    public List<Resource> getMainResources() {
      return this.mainResources;
    }

    public List<Resource> getTestResources() {
      return this.testResources;
    }

    public List<Resource> getResources() {
      return resources == null ? Collections.concat(new ArrayList<Resource>(), mainResources, testResources) : resources;
    }
  }

  @Parameter(defaultValue="${project.resources}", required=true, readonly=true)
  private List<Resource> mainResources;

  @Parameter(defaultValue="${project.testResources}", required=true, readonly=true)
  private List<Resource> testResources;

  @Parameter(defaultValue="${project}", required=true, readonly=true)
  protected MavenProject project;

  @Override
  public final void execute(final boolean failOnNoOp) throws MojoExecutionException, MojoFailureException {
    final List<Resource> projectResources = new ArrayList<>();
    if (this.mainResources != null)
      projectResources.addAll(mainResources);

    if (testResources != null)
      projectResources.addAll(testResources);

    if (mainResources.size() == 0 && testResources.size() == 0) {
      if (failOnNoOp)
        throw new MojoExecutionException("Failing due to empty resources (failOnNoOp=true).");

      getLog().info("Skipping due to empty resources.");
      return;
    }

    execute(new Configuration(failOnNoOp, mainResources, testResources));
  }

  public abstract void execute(final Configuration configuration) throws MojoExecutionException, MojoFailureException;
}