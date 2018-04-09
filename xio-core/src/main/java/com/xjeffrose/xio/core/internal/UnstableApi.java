package com.xjeffrose.xio.core.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a public API that can change at any time (even in minor/bugfix releases).
 *
 * <p>Usage guidelines:
 *
 * <ol>
 *   <li>Is not needed for things located in *.internal.* packages
 *   <li>Only public accessible classes/interfaces must be annotated
 *   <li>If this annotation is not present the API is considered stable and so no backward
 *       compatibility can be broken in a non-major release!
 * </ol>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({
  ElementType.ANNOTATION_TYPE,
  ElementType.CONSTRUCTOR,
  ElementType.FIELD,
  ElementType.METHOD,
  ElementType.PACKAGE,
  ElementType.TYPE
})
@Documented
public @interface UnstableApi {}
