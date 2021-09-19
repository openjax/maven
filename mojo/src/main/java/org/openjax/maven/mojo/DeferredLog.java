/* Copyright (c) 2021 OpenJAX
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

import java.util.ArrayList;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.libj.lang.Assertions;

/**
 * A {@link Log} that defers the output of log messages to the time of the
 * invocation of {@link #flush(Level, CharSequence, Throwable)}.
 */
public abstract class DeferredLog implements Log {
  private final ArrayList<Entry> entries = new ArrayList<>();
  private final Log log;

  /**
   * Creates a new {@link DeferredLog} with the specified target {@link Log}.
   *
   * @param target The target {@link Log}.
   * @throws IllegalArgumentException If the target {@link Log} is null.
   */
  public DeferredLog(final Log target) {
    this.log = Assertions.assertNotNull(target);
  }

  /**
   * The log level.
   */
  public enum Level {
    DEBUG() {
      @Override
      public void flush(final Log log, final CharSequence content, final Throwable error) {
        log.debug(content, error);
      }
    },
    INFO() {
      @Override
      public void flush(final Log log, final CharSequence content, final Throwable error) {
        log.info(content, error);
      }
    },
    WARN() {
      @Override
      public void flush(final Log log, final CharSequence content, final Throwable error) {
        log.warn(content, error);
      }
    },
    ERROR() {
      @Override
      public void flush(final Log log, final CharSequence content, final Throwable error) {
        log.error(content, error);
      }
    };

    /**
     * Flushes the provided {@code content} and {@code error} to the specified
     * {@link Log}.
     *
     * @param log The {@link Log} to which the {@code content} and {@code error}
     *          are to be flushed.
     * @param content The {@link CharSequence} to be flushed.
     * @param error The {@link Throwable} to be flushed.
     */
    public abstract void flush(Log log, CharSequence content, Throwable error);
  }

  private class Entry {
    private final Level level;
    private final CharSequence content;
    private final Throwable error;

    private Entry(final Level level, final CharSequence content, final Throwable error) {
      this.level = level;
      this.content = content;
      this.error = error;
    }
  }

  protected abstract void flush(Level level, CharSequence content, Throwable error) throws MojoExecutionException;

  public Log getTarget() {
    return log;
  }

  public void flush(final Level level) throws MojoExecutionException {
    for (final Entry entry : entries)
      if (level == null || entry.level.ordinal() <= level.ordinal())
        flush(entry.level, entry.content, entry.error);
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public void debug(final CharSequence content) {
    entries.add(new Entry(Level.DEBUG, content, null));
  }

  @Override
  public void debug(final CharSequence content, final Throwable error) {
    entries.add(new Entry(Level.DEBUG, content, error));
  }

  @Override
  public void debug(final Throwable error) {
    entries.add(new Entry(Level.DEBUG, null, error));
  }

  @Override
  public boolean isInfoEnabled() {
    return log.isInfoEnabled();
  }

  @Override
  public void info(final CharSequence content) {
    entries.add(new Entry(Level.INFO, content, null));
  }

  @Override
  public void info(final CharSequence content, final Throwable error) {
    entries.add(new Entry(Level.INFO, content, error));
  }

  @Override
  public void info(final Throwable error) {
    entries.add(new Entry(Level.INFO, null, error));
  }

  @Override
  public boolean isWarnEnabled() {
    return log.isWarnEnabled();
  }

  @Override
  public void warn(final CharSequence content) {
    entries.add(new Entry(Level.WARN, content, null));
  }

  @Override
  public void warn(final CharSequence content, final Throwable error) {
    entries.add(new Entry(Level.WARN, content, error));
  }

  @Override
  public void warn(final Throwable error) {
    entries.add(new Entry(Level.WARN, null, error));
  }

  @Override
  public boolean isErrorEnabled() {
    return log.isErrorEnabled();
  }

  @Override
  public void error(final CharSequence content) {
    entries.add(new Entry(Level.ERROR, content, null));
  }

  @Override
  public void error(final CharSequence content, final Throwable error) {
    entries.add(new Entry(Level.ERROR, content, error));
  }

  @Override
  public void error(final Throwable error) {
    entries.add(new Entry(Level.ERROR, null, error));
  }
}