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

import org.junit.Test;

public class PatternSetMojoTest {
  private static void test(final String pattern, final String[] pass, final String[] fail) {
    final String regex = PatternSetMojo.convertToRegex(pattern);
    for (int i = 0; i < pass.length; ++i) // [A]
      assertTrue("Expected pass: " + regex + " " + pass[i], pass[i].matches(regex));

    for (int i = 0; i < fail.length; ++i) // [A]
      assertFalse("Expected fail: " + fail[i], fail[i].matches(regex));
  }

  @Test
  public void testConvertToRegex() {
    try {
      PatternSetMojo.convertToRegex(null);
      fail("Expected NullPointerException");
    }
    catch (final NullPointerException e) {
    }

    test("foo", new String[] {"foo"}, new String[] {"bar"});
    test("*he?lo*.xml", new String[] {"hello.xml"}, new String[] {"hi.xml"});
    test("/?abc/*/*.java", new String[] {"/xabc/foobar/test.java"}, new String[] {"/xxabc/foobar/test.java"});
    test("mypackage/test/", new String[] {"mypackage/test/file.xml"}, new String[] {"oops/mypackage/test/file.xml"});
    test("mypackage\\test\\", new String[] {"mypackage\\test\\file.xml"}, new String[] {"oops\\mypackage\\test\\file.xml"});

    test("**/CVS/*", new String[] {"CVS/Repository", "org/apache/CVS/Entries", "org/apache/jakarta/tools/ant/CVS/Entries"}, new String[] {"org/apache/CVS/foo/bar/Entries"});
    test("org/apache/jakarta/**", new String[] {"org/apache/jakarta/tools/ant/docs/index.html", "org/apache/jakarta/test.xml"}, new String[] {"org/apache/xyz.java"});
    test("org/apache/**/CVS/*", new String[] {"org/apache/CVS/Entries", "org/apache/jakarta/tools/ant/CVS/Entries"}, new String[] {"org/apache/CVS/foo/bar/Entries"});
    test("**/test/**", new String[] {"test", "path/to/test/file", "test/path.xml", "path/test"}, new String[] {"file.xml"});
  }
}