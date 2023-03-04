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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.libj.util.ArrayUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

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
    if (isPrimitive(type) || String.class.getName().equals(type.getClassName()))
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
    final int i$;
    if (annotations == null || (i$ = annotations.size()) == 0)
      return null;

    for (int i = 0; i < i$; ++i) { // [RA]
      final AnnotationNode annotationNode = (AnnotationNode)annotations.get(i);
      if (desc.equals(annotationNode.desc)) {
        final Map<String,Object> parameters = new HashMap<>();
        final List<Object> values = annotationNode.values;
        if (values != null) {
          for (int j = 0, j$ = values.size(); j < j$;) { // [RA]
            final String name = (String)values.get(j++);
            final Object rawValue = values.get(j++);
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
        }

        return parameters;
      }
    }

    return null;
  }

  /**
   * Returns a map of parameters for {@code annotationType} on {@code cls}, regardless of the annotation's retention spec. If the
   * {@code annotationType} is not found on {@code cls}, this method returns {@code null}.
   *
   * @param <T> Type parameter of the annotation class.
   * @param cls The class.
   * @param annotationType The annotation type.
   * @return A map of parameters for {@code annotationType} on {@code cls}, or {@code null} if no such annotation exists.
   * @throws IOException If an I/O error has occurred.
   * @throws IllegalArgumentException If {@code cls} or {@code annotationType} are null.
   */
  public static <T extends Annotation>T getAnnotationParameters(final Class<?> cls, final Class<T> annotationType) throws IOException {
    try (final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(cls.getName().replace('.', '/') + ".class")) {
      if (in == null)
        throw new IllegalStateException("Unable to locate bytecode for class " + cls.getName() + " in context class loader " + Thread.currentThread().getContextClassLoader());

      final ClassReader classReader = new ClassReader(in);
      final ClassNode classNode = new ClassNode();
      classReader.accept(classNode, 0);
      final String desc = "L" + annotationType.getName().replace('.', '/') + ";";
      final Map<String,Object> invisible = getAnnotationParameters(classNode.invisibleAnnotations, desc);
      if (invisible != null)
        return annotationForMap(annotationType, invisible);

      final Map<String,Object> visible = getAnnotationParameters(classNode.visibleAnnotations, desc);
      return visible == null ? null : annotationForMap(annotationType, visible);
    }
  }

  /**
   * Returns a map of parameters for {@code annotationType} on {@code field}, regardless of the annotation's retention spec. If the
   * {@code annotationType} is not found on {@code field}, this method returns {@code null}.
   *
   * @param <T> Type parameter of the annotation class.
   * @param field The field.
   * @param annotationType The annotation type.
   * @return A map of parameters for {@code annotationType} on {@code field}, or {@code null} if no such annotation exists.
   * @throws IOException If an I/O error has occurred.
   * @throws IllegalArgumentException If {@code field} or {@code annotationType} are null.
   */
  public static <T extends Annotation>T getAnnotationParameters(final Field field, final Class<T> annotationType) throws IOException {
    try (final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(field.getDeclaringClass().getName().replace('.', '/') + ".class")) {
      if (in == null)
        throw new IllegalStateException("Unable to locate bytecode for class " + field.getDeclaringClass().getName() + " in context class loader " + Thread.currentThread().getContextClassLoader());

      final ClassReader classReader = new ClassReader(in);
      final ClassNode classNode = new ClassNode();
      classReader.accept(classNode, 0);
      final List<FieldNode> fields = classNode.fields;
      for (int i = 0, i$ = fields.size(); i < i$; ++i) { // [RA]
        final FieldNode fieldNode = fields.get(i);
        if (field.getName().equals(fieldNode.name)) {
          final String desc = "L" + annotationType.getName().replace('.', '/') + ";";
          final Map<String,Object> invisible = getAnnotationParameters(fieldNode.invisibleAnnotations, desc);
          if (invisible != null)
            return annotationForMap(annotationType, invisible);

          final Map<String,Object> visible = getAnnotationParameters(fieldNode.visibleAnnotations, desc);
          return visible == null ? null : annotationForMap(annotationType, visible);
        }
      }

      return null;
    }
  }

  /**
   * Creates a new instance of an annotation of the specified type and provided member values.
   *
   * @param <T> Type parameter of the annotation class.
   * @param annotationType The annotation type.
   * @param memberValues The member values.
   * @return A new instance of an annotation of the specified type and provided member values.
   * @throws IllegalArgumentException If any restrictions on the parameters are violated.
   * @throws SecurityException If a security manager, <em>s</em>, is present and any of the following conditions is met:
   *           <ul>
   *           <li>the given {@code loader} is {@code null} and the caller's class loader is not {@code null} and the invocation of
   *           {@link SecurityManager#checkPermission s.checkPermission} with {@code RuntimePermission("getClassLoader")} permission
   *           denies access;</li>
   *           <li>for each proxy interface, {@code intf}, the caller's class loader is not the same as or an ancestor of the class
   *           loader for {@code intf} and invocation of {@link SecurityManager#checkPackageAccess s.checkPackageAccess()} denies
   *           access to {@code intf};</li>
   *           <li>any of the given proxy interfaces is non-public and the caller class is not in the same {@linkplain Package
   *           runtime package} as the non-public interface and the invocation of {@link SecurityManager#checkPermission
   *           s.checkPermission} with {@code ReflectPermission("newProxyInPackage.{package name}")} permission denies access.</li>
   *           </ul>
   * @throws NullPointerException If the specified {@code annotationType} or {@code memberValues} is null.
   */
  @SuppressWarnings({"unchecked"})
  static <T extends Annotation>T annotationForMap(final Class<T> annotationType, final Map<String,Object> memberValues) {
    Objects.requireNonNull(memberValues);
    return (T)Proxy.newProxyInstance(annotationType.getClassLoader(), new Class[] {annotationType}, new InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) {
        return memberValues.get(method.getName());
      }
    });
  }

  private AnnotationUtil() {
  }
}