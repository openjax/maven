/* Copyright (c) 2018 FastJAX
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * Utility class providing access to annotation data via bytecode.
 */
public final class AnnotationUtil {
  /**
   * Returns a map of parameters for {@code annotationType} on {@code field},
   * regardless of the annotation's retention spec. This method returns
   * {@code null} if the {@code annotationType} is not found on {@code field}.
   *
   * @param field The field.
   * @param annotationType The annotation type.
   * @return A map of parameters for {@code annotationType} on {@code field}.
   * @throws IOException If an I/O error occurs.
   * @throws NullPointerException If {@code field} or {@code annotationType} are
   *           null.
   */
  public static Map<String,Object> getAnnotationParameters(final Field field, final Class<? extends Annotation> annotationType) throws IOException {
    final ClassReader classReader = new ClassReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(field.getDeclaringClass().getName().replace('.', '/') + ".class"));
    final ClassNode classNode = new ClassNode();
    classReader.accept(classNode, 0);
    for (final Object classField : classNode.fields) {
      final FieldNode fieldNode = (FieldNode)classField;
      if (field.getName().equals(fieldNode.name)) {
        final String desc = "L" + annotationType.getName().replace('.', '/') + ";";
        final Map<String,Object> parameters = getAnnotationParameters(fieldNode.invisibleAnnotations, desc);
        return parameters != null ? parameters : getAnnotationParameters(fieldNode.visibleAnnotations, desc);
      }
    }

    return null;
  }

  private static Map<String,Object> getAnnotationParameters(final List<?> annotations, final String desc) {
    if (annotations == null || annotations.size() == 0)
      return null;

    for (final Object annotation : annotations) {
      final AnnotationNode annotationNode = (AnnotationNode)annotation;
      if (desc.equals(annotationNode.desc)) {
        final Map<String,Object> parameters = new HashMap<>();
        for (int i = 0; i < annotationNode.values.size();)
          parameters.put((String)annotationNode.values.get(i++), annotationNode.values.get(i++));

        return parameters;
      }
    }

    return null;
  }

  private AnnotationUtil() {
  }
}