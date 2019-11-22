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

import static org.junit.Assert.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.RegEx;
import javax.annotation.meta.When;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openjax.maven.mojo.AnnotationUtil;

@Nullable
@RegEx(when = When.MAYBE)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AnnotationUtilTest {
  private static void test(final Annotation annotation) throws IllegalAccessException, InvocationTargetException {
    final Map<String,Object> map = new HashMap<>();
    for (final Method method : annotation.getClass().getMethods())
      if (method.getReturnType() != Void.class && method.getParameterCount() == 0 && !"toString".equals(method.getName()) && !"hashCode".equals(method.getName()) && !"annotationType".equals(method.getName()) && !"wait".equals(method.getName()) && !"notify".equals(method.getName()) && !"notifyAll".equals(method.getName()) && !"getClass".equals(method.getName()))
        map.put(method.getName(), method.invoke(annotation));

    final Annotation copy = AnnotationUtil.annotationForMap(annotation.annotationType(), map);
    assertEquals(annotation, copy);
  }

  @Test
  public void test() throws IllegalAccessException, InvocationTargetException {
    test(AnnotationUtilTest.class.getAnnotation(RegEx.class));
    test(AnnotationUtilTest.class.getAnnotation(Nullable.class));
    test(AnnotationUtilTest.class.getAnnotation(FixMethodOrder.class));
  }
}