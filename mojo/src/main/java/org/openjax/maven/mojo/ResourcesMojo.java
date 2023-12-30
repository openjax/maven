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

import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.libj.util.CollectionUtil;

@Mojo(name = "resources")
public abstract class ResourcesMojo extends FilterMojo {
  public class Configuration extends FilterMojo.Configuration {
    private final LinkedHashSet<Resource> mainResources;
    private final LinkedHashSet<Resource> testResources;
    private LinkedHashSet<Resource> resources;

    public Configuration(final Configuration configuration) {
      this(configuration, configuration.mainResources, configuration.testResources);
    }

    private Configuration(final FilterMojo.Configuration configuration, final LinkedHashSet<Resource> mainResources, final LinkedHashSet<Resource> testResources) {
      super(configuration);
      this.mainResources = mainResources;
      this.testResources = testResources;
    }

    public LinkedHashSet<Resource> getMainResources() {
      return mainResources;
    }

    public LinkedHashSet<Resource> getTestResources() {
      return testResources;
    }

    public LinkedHashSet<Resource> getResources() {
      return resources == null ? CollectionUtil.concat(new LinkedHashSet<>(), mainResources, testResources) : resources;
    }
  }

  @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
  private List<Resource> mainResources;

  @Parameter(defaultValue = "${project.testResources}", required = true, readonly = true)
  private List<Resource> testResources;

  @Override
  public final void execute(final FilterMojo.Configuration configuration) throws MojoExecutionException, MojoFailureException {
    if (mainResources.size() == 0 && testResources.size() == 0) {
      if (configuration.getFailOnNoOp())
        throw new MojoExecutionException("Failing due to empty resources (failOnNoOp=true)");

      getLog().info("Skipping due to empty resources.");
      return;
    }

    execute(new Configuration(configuration, new LinkedHashSet<>(mainResources), new LinkedHashSet<>(testResources)));
  }

  public abstract void execute(Configuration configuration) throws MojoExecutionException, MojoFailureException;
}