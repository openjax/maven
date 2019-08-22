/* Copyright (c) 2018 OpenJAX
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.libj.util.ArrayUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import sun.reflect.annotation.AnnotationParser;

/**
 * Utility class providing access to annotation data via bytecode.
 */
public final class AnnotationUtil {
  private static final Type[] PRIMITIVE_TYPES = {Type.BOOLEAN_TYPE, Type.BYTE_TYPE, Type.SHORT_TYPE, Type.CHAR_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE};

  private static boolean isPrimitive(final Type type) {
    return ArrayUtil.contains(PRIMITIVE_TYPES, type);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Object getInstance(final Type type, final Object value) {
    if (isPrimitive(type))
      return value;

    if (String.class.getName().equals(type.getClassName()))
      return value;

    if (Enumeration.class.getName().equals(type.getClassName()))
      throw new UnsupportedOperationException();

    try {
      if (Class.class.getName().equals(type.getClassName()))
        return Class.forName((String)value);

      final Class<?> cls = Class.forName(type.getClassName());
      if (cls.isEnum())
        return Enum.valueOf((Class<? extends Enum>)cls, (String)value);

      throw new UnsupportedOperationException();
    }
    catch (final ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Map<String,Object> getAnnotationParameters(final List<?> annotations, final String desc) {
    if (annotations == null || annotations.size() == 0)
      return null;

    for (final Object annotation : annotations) {
      final AnnotationNode annotationNode = (AnnotationNode)annotation;
      if (desc.equals(annotationNode.desc)) {
        final Map<String,Object> parameters = new HashMap<>();
        if (annotationNode.values != null)
          for (int i = 0; i < annotationNode.values.size();) {
            final String name = (String)annotationNode.values.get(i++);
            final Object rawValue = annotationNode.values.get(i++);
            final Object value;
            if (rawValue instanceof String[]) {
              final String[] data = (String[])rawValue;
              final Type type = Type.getType(data[0]);
              value = getInstance(type.getSort() == Type.ARRAY ? type.getElementType() : type, data[1]);
            }
            else {
              value = rawValue;
            }

            parameters.put(name, value);
          }

        return parameters;
      }
    }

    return null;
  }

  /**
   * Returns a map of parameters for {@code annotationType} on {@code field},
   * regardless of the annotation's retention spec. If the
   * {@code annotationType} is not found on {@code field}, this method returns
   * {@code null}.
   *
   * @param <T> Type parameter of the annotation class.
   * @param field The field.
   * @param annotationType The annotation type.
   * @return A map of parameters for {@code annotationType} on {@code field}, or
   *         {@code null} if no such annotation exists.
   * @throws IOException If an I/O error has occurred.
   * @throws NullPointerException If {@code field} or {@code annotationType} are
   *           null.
   */
  public static <T extends Annotation>T getAnnotationParameters(final Field field, final Class<T> annotationType) throws IOException {
    final ClassReader classReader = new ClassReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(field.getDeclaringClass().getName().replace('.', '/') + ".class"));
    final ClassNode classNode = new ClassNode();
    classReader.accept(classNode, 0);
    for (final Object classField : classNode.fields) {
      final FieldNode fieldNode = (FieldNode)classField;
      if (field.getName().equals(fieldNode.name)) {
        final String desc = "L" + annotationType.getName().replace('.', '/') + ";";
        final Map<String,Object> invisible = getAnnotationParameters(fieldNode.invisibleAnnotations, desc);
        if (invisible != null)
          return (T)AnnotationParser.annotationForMap(annotationType, invisible);

        final Map<String,Object> visible = getAnnotationParameters(fieldNode.visibleAnnotations, desc);
        return visible == null ? null : (T)AnnotationParser.annotationForMap(annotationType, visible);
      }
    }

    return null;
  }

  private AnnotationUtil() {
  }
}