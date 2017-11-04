package com.chromosphere.accessor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.accessor.annotation.Delegator;

abstract class AccessorFactoryBuilder {
	private static final Logger LOG = LoggerFactory.getLogger(AccessorFactoryBuilder.class);

	protected static final Set<String> couldNotDefine = new HashSet<>();
	private static final AtomicReference<String> LIB_NAME = new AtomicReference<>();
	static {
		for (Method m : Object.class.getMethods()) {
			couldNotDefine.add(m.getName());
		}
		couldNotDefine.add("finalize");
	}

	static AccessorFactoryBuilder builder() {
		if ("javassist".equals(LIB_NAME.get())) {
			return new JavassistAccessorFactoryBuilder();
		}
		if (hasClass("javassist.CtClass")) {
			LOG.debug("javassist");
			LIB_NAME.set("javassist");
			return new JavassistAccessorFactoryBuilder();
		}
		LOG.error("There is no supported bytecode libraries.");
		throw new IllegalStateException("There is no supported bytecode libraries.");
	}

	static boolean hasClass(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	abstract AccessorFactoryBuilder sourceClass(Class<?> sourceClass);

	abstract AccessorFactoryBuilder destinationClass(Class<?> destinationClass);

	abstract AccessorFactoryBuilder delegatorClass(Class<?> delegatorClass);

	abstract AccessorFactoryBuilder delegator(Delegator delegator);

	abstract <Source, Destination> AccessorFactory<Source, Destination> build();
}
