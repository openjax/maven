/* Copyright (c) 2019 OpenJAX
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
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.libj.lang.Classes;
import org.libj.net.URLs;
import org.libj.util.StringPaths;
import org.libj.util.function.Throwing;

/**
 * An abstract class extending {@link BaseMojo} that parameter filtering for
 * MOJOs via {@link FilterParameter}.
 */
public abstract class FilterMojo extends BaseMojo {
  public static class Configuration extends BaseMojo.Configuration {
    public Configuration(final BaseMojo.Configuration configuration) {
      super(configuration);
    }
  }

  private boolean wasFiltered;
  private Map<String,Object> filteredParameters;

  private static URL filterURL(final String value, final File baseDir) throws MalformedURLException {
    return StringPaths.isAbsolute(value) ? URLs.toCanonicalURL(value) : baseDir != null ? new File(baseDir, value).toURI().toURL() : new File(value).toURI().toURL();
  }

  private static URL filterResource(final String value, final ClassLoader classLoader) throws MojoExecutionException {
    final URL url = classLoader.getResource(value);
    if (url == null)
      throw new MojoExecutionException("Resource not found in context ClassLoader: " + value);

    return url;
  }

  /**
   * Filters parameters declared with the {@link FilterParameter} annotation,
   * and replaces each field's value with the filtered value.
   * <p>
   * <b>NOTE:</b> This method is not thread safe.
   *
   * @return A map of parameter name to a list of the filtered parameter values,
   *         or {@code null} if no fields were found with the
   *         {@link FilterParameter} annotation.
   * @throws DependencyResolutionRequiredException If an artifact file is used,
   *           but has not been resolved.
   * @throws MojoExecutionException If a source input property is declared with
   *           {@code required=true}, and no property values are declared in the
   *           POM
   * @throws MojoFailureException If no fields are found with the
   *           {@link FilterParameter} annotation in the specified class, or if
   *           a field with the {@link FilterParameter} annotation is declared
   *           with a type other than {@link List}, or if a field with the
   *           {@link FilterParameter} annotation does not declare the
   *           {@link Parameter} annotation.
   */
  @SuppressWarnings("unchecked")
  protected Map<String,Object> getFilterParameters() throws DependencyResolutionRequiredException, MojoExecutionException, MojoFailureException {
    if (wasFiltered)
      return filteredParameters;

    wasFiltered = true;
    final Field[] fields = Classes.getDeclaredFieldsDeep(getClass(), Throwing.<Field>rethrow(f -> AnnotationUtil.getAnnotationParameters(f, FilterParameter.class) != null));
    if (fields == null || fields.length == 0)
      return null;

    final MavenProject project = (MavenProject)getPluginContext().get("project");
    final Map<String,Object> nameToInputs = new HashMap<String,Object>() {
      private static final long serialVersionUID = -6518007948977059826L;

      @Override
      public boolean isEmpty() {
        if (super.isEmpty())
          return true;

        for (final Object value : values())
          if (value != null && (!(value instanceof List) || ((List<?>)value).size() > 0))
            return false;

        return true;
      }
    };

    try {
      for (final Field field : fields) {
        final Parameter parameter = AnnotationUtil.getAnnotationParameters(field, Parameter.class);
        if (parameter == null)
          throw new MojoFailureException("@" + FilterParameter.class.getSimpleName() + " annotation can only be used on field having @" + Parameter.class.getSimpleName() + " annotation: " + field.getDeclaringClass().getName() + "#" + field.getName());

        final boolean isList = List.class.isAssignableFrom(field.getType());
        final Type typeArgument;
        if (isList) {
          final ParameterizedType genericType = (ParameterizedType)field.getGenericType();
          typeArgument = genericType.getActualTypeArguments().length == 0 ? null : genericType.getActualTypeArguments()[0];
        }
        else {
          typeArgument = field.getType();
        }

        field.setAccessible(true);
        final Object value = field.get(this);
        final Object filteredValue;

        final FilterParameter filterParameter = AnnotationUtil.getAnnotationParameters(field, FilterParameter.class);
        final FilterType filterType = filterParameter.value();
        if (filterType == FilterType.FILE) {
          if (typeArgument != File.class)
            throw new IllegalArgumentException("Field specified with @" + FilterParameter.class.getSimpleName() + "(" + FilterType.class.getSimpleName() + ".FILE) must be of type " + File.class.getName() + " or " + List.class.getName() + "<" + File.class.getName() + ">, but found " + typeArgument);

          filteredValue = value;
        }
        else {
          if (typeArgument != String.class)
            throw new IllegalArgumentException("Field specified with @" + FilterParameter.class.getSimpleName() + "(" + FilterType.class.getSimpleName() + ".URL) must be of type String or " + List.class.getName() + "<String>, but found " + typeArgument);

          if (filterType == FilterType.URL) {
            final File baseDir = project.getBasedir().getAbsoluteFile();
            if (isList) {
              final List<String> values = (List<String>)(filteredValue = value);
              for (int i = 0; i < values.size(); ++i) {
                values.set(i, filterURL(values.get(i), baseDir).toString());
              }
            }
            else {
              filteredValue = filterURL((String)value, baseDir);
              field.set(this, value);
            }
          }
          else if (filterType == FilterType.RESOURCE) {
            final List<String> classPaths = new ArrayList<>();
            for (final Resource resource : project.getResources())
              classPaths.add(resource.getDirectory());

            final ArtifactRepository localRepository = session.getLocalRepository();
            classPaths.addAll(MojoUtil.getPluginDependencyClassPath((PluginDescriptor)getPluginContext().get("pluginDescriptor"), localRepository, new DefaultArtifactHandler("jar")));
            classPaths.addAll(project.getRuntimeClasspathElements());
            classPaths.addAll(project.getCompileClasspathElements());
            if (isInTestPhase()) {
              for (final Resource resource : project.getTestResources())
                classPaths.add(resource.getDirectory());

              Collections.addAll(classPaths, MojoUtil.getProjectDependencyPaths(project, localRepository));
              classPaths.addAll(project.getTestClasspathElements());
            }

            final URL[] classPathURLs = new URL[classPaths.size()];
            for (int i = 0; i < classPathURLs.length; ++i) {
              final String path = classPaths.get(i);
              classPathURLs[i] = new URL("file", "", path.endsWith(".jar") ? path : (path + "/"));
            }

            try (final URLClassLoader classLoader = new URLClassLoader(classPathURLs, Thread.currentThread().getContextClassLoader())) {
              if (isList) {
                final List<String> values = (List<String>)(filteredValue = value);
                for (int i = 0; i < values.size(); ++i) {
                  values.set(i, filterResource(values.get(i), classLoader).toString());
                }
              }
              else {
                filteredValue = filterResource((String)value, classLoader).toString();
                field.set(this, value);
              }
            }
          }
          else {
            throw new UnsupportedOperationException("Unsupported @" + FilterType.class.getSimpleName() + ": " + filterType);
          }
        }

        nameToInputs.put(parameter.property(), filteredValue);
      }

      return filteredParameters = nameToInputs;
    }
    catch (final IllegalAccessException | IOException e) {
      throw new MojoFailureException(null, e);
    }
  }

  @Override
  public final void execute(final BaseMojo.Configuration configuration) throws MojoExecutionException, MojoFailureException {
    try {
      getFilterParameters();
      execute(new Configuration(configuration));
    }
    catch (final DependencyResolutionRequiredException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  public abstract void execute(Configuration configuration) throws MojoExecutionException, MojoFailureException;
}