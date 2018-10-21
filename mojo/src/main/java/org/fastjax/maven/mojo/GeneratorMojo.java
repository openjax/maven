/* Copyright (c) 2011 FastJAX
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.fastjax.net.URLs;
import org.fastjax.util.Classes;
import org.fastjax.util.FastCollections;
import org.fastjax.util.Paths;

@Mojo(name="generator")
public abstract class GeneratorMojo extends BaseMojo {
  protected class Configuration {
    private final File destDir;
    private final boolean overwrite;
    private final Map<String,URL[]> sourceInputs;
    private final boolean failOnNoOp;

    public Configuration(final File destDir, final boolean overwrite, final Map<String,URL[]> sourceInputs, final boolean failOnNoOp) {
      this.destDir = destDir;
      this.overwrite = overwrite;
      this.sourceInputs = sourceInputs;
      this.failOnNoOp = failOnNoOp;
    }

    public File getDestDir() {
      return this.destDir;
    }

    public boolean getOverwrite() {
      return this.overwrite;
    }

    public URL[] getSourceInputs(final String name) {
      return sourceInputs.get(name);
    }

    public boolean getFailOnNoOp() {
      return this.failOnNoOp;
    }
  }

  private static URL buildURL(final File baseDir, final String path) throws MalformedURLException {
    return Paths.isAbsolute(path) ? URLs.toCanonicalURL(path) : baseDir != null ? new File(baseDir, path).toURI().toURL() : new File(path).toURI().toURL();
  }

  @Parameter(defaultValue="${project}", required=true, readonly=true)
  protected MavenProject project;

  @Parameter(property="destDir", required=true)
  private File destDir;

  @Parameter(property="overwrite")
  private boolean overwrite = true;

  @Override
  public final void execute(final boolean failOnNoOp) throws MojoExecutionException, MojoFailureException {
    MojoUtil.assertCreateDir("destination", destDir);

    final Field[] sourceInputFields = Classes.getDeclaredFieldsWithAnnotationDeep(getClass(), SourceInput.class);
    final Map<String,URL[]> sourceInputs;
    if (sourceInputFields == null || sourceInputFields.length == 0) {
      sourceInputs = null;
    }
    else {
      sourceInputs = new HashMap<>();
      try {
        for (int i = 0; i < sourceInputFields.length; ++i) {
          final Field sourceInputField = sourceInputFields[i];
          if (!List.class.isAssignableFrom(sourceInputField.getType()))
            throw new MojoFailureException("@" + SourceInput.class.getSimpleName() + " annotation can only be used on field with type that extends " + List.class.getName() + ": " + sourceInputField.getDeclaringClass().getName() + "#" + sourceInputField.getName());

          final Map<String,Object> parameterValues = AnnotationUtil.getAnnotationParameters(sourceInputField, Parameter.class);
          getLog().warn(FastCollections.toString(Arrays.asList(sourceInputField.getDeclaredAnnotations()), "\n"));
          if (parameterValues == null)
            throw new MojoFailureException("@" + SourceInput.class.getSimpleName() + " annotation can only be used on field having @" + Parameter.class.getSimpleName() + " annotation: " + sourceInputField.getDeclaringClass().getName() + "#" + sourceInputField.getName());

          final String propertyName = (String)parameterValues.get("property");
          sourceInputField.setAccessible(true);
          final List<?> sourceInput = (List<?>)sourceInputField.get(this);
          if (sourceInput == null || sourceInput.size() == 0) {
            final Object required = parameterValues.get("required");
            if (required == null || !(Boolean)required)
              continue;

            if (failOnNoOp)
              throw new MojoExecutionException("Empty " + propertyName + " (failOnNoOp=true).");

            getLog().info("Skipping due to empty " + propertyName + ".");
            return;
          }

          final URL[] sourceInputUrls = new URL[sourceInput.size()];
          sourceInputs.put(propertyName, sourceInputUrls);
          final Iterator<?> iterator = sourceInput.iterator();
          for (int j = 0; iterator.hasNext(); ++j)
            sourceInputUrls[j] = buildURL(project.getBasedir().getAbsoluteFile(), String.valueOf(iterator.next()));
        }
      }
      catch (final IllegalAccessException | IOException e) {
        throw new MojoFailureException(null, e);
      }
    }

    execute(new Configuration(destDir, overwrite, sourceInputs, failOnNoOp));

    if (isInTestPhase())
      project.addTestCompileSourceRoot(destDir.getAbsolutePath());
    else
      project.addCompileSourceRoot(destDir.getAbsolutePath());
  }

  public abstract void execute(final Configuration configuration) throws MojoExecutionException, MojoFailureException;
}