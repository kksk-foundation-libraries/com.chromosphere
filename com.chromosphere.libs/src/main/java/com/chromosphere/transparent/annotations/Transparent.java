package com.chromosphere.transparent.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Transparent {
	int priority() default Integer.MAX_VALUE;

	String key() default "";

	String initialize() default "";

	String terminate() default "";

	Class<?> sourceClass();

	Class<?> distinationClass();
}
