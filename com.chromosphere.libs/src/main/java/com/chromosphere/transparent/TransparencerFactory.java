package com.chromosphere.transparent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.util.proxy.ProxyFactory;

@SuppressWarnings({ "unchecked" })
public class TransparencerFactory<Source, Distination> {
	private static final Logger LOG = LoggerFactory.getLogger(TransparencerFactory.class);

	private static final ClassPool CLASS_POOL = ClassPool.getDefault();

	private static final Set<String> couldNotDefine = new HashSet<>();
	static {
		CLASS_POOL.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
		for (Method m : Object.class.getMethods()) {
			couldNotDefine.add(m.getName());
		}
		couldNotDefine.add("finalize");
	}

	private final Constructor<?> constructor1;
	private final Constructor<?> constructor2;

	TransparencerFactory(Class<Source> sourceClass, Class<Distination> distinationClass, Class<?> delegateClass, String initialize, String terminate) throws Exception {
		final String distinationClassName = distinationClass.getName();
		final String transparencerClassName = ProxyFactory.nameGenerator.get(distinationClassName);
		final String interfaceName = Transparencer.class.getName();
		final String delegateClassName = delegateClass.getName();
		LOG.debug("\nsourceClass:[{}],\ndistinationClassName:[{}],\ndelegateClassName:[{}],\ntransparencerClassName:[{}],\ninterfaceName:[{}]", sourceClass.getName(), distinationClassName, delegateClassName, transparencerClassName, interfaceName);

		Multimap<String, CtMethod> transparentClassMethods = MultimapBuilder.linkedHashKeys().arrayListValues().build();
		CtClass distinationCtClass = CLASS_POOL.get(distinationClassName);
		CtClass delegateCtClass = CLASS_POOL.get(delegateClassName);
		for (CtMethod delegateCtMethod : delegateCtClass.getMethods()) {
			if (!couldNotDefine.contains(delegateCtMethod.getName()) && checkScope(delegateCtMethod.getModifiers())) {
				LOG.debug("terget method:[{}]", delegateCtMethod.getName());
				transparentClassMethods.put(delegateCtMethod.getName(), delegateCtMethod);
			}
		}

		CtClass transparencerCtClass = CLASS_POOL.makeClass(transparencerClassName, distinationCtClass);
		transparencerCtClass.setInterfaces(new CtClass[] { CLASS_POOL.get(interfaceName) });
		transparencerCtClass.addField(CtField.make(String.format("private final %s _delegate;", delegateClassName), transparencerCtClass));
		String shortName = transparencerClassName.substring(transparencerClassName.lastIndexOf(".") + 1);
		transparencerCtClass.addConstructor(CtNewConstructor.make(String.format("public %s(%s _delegate) {this._delegate = _delegate;}", shortName, delegateClassName), transparencerCtClass));
		if (!"".equals(initialize)) {
			CtMethod initializeCtMethod = CtNewMethod.make(String.format("public void _initialize() {_delegate.%s();}", initialize), transparencerCtClass);
			transparencerCtClass.addMethod(initializeCtMethod);
		} else {
			CtMethod initializeCtMethod = CtNewMethod.make(String.format("public void _initialize() {}"), transparencerCtClass);
			transparencerCtClass.addMethod(initializeCtMethod);
		}
		if (!"".equals(terminate)) {
			CtMethod terminateCtMethod = CtNewMethod.make(String.format("public void _terminate() {_delegate.%s();}", terminate), transparencerCtClass);
			transparencerCtClass.addMethod(terminateCtMethod);
		} else {
			CtMethod terminateCtMethod = CtNewMethod.make(String.format("public void _terminate() {}"), transparencerCtClass);
			transparencerCtClass.addMethod(terminateCtMethod);
		}
		for (CtMethod distinationCtMethod : distinationCtClass.getMethods()) {
			for (CtMethod delegateCtMethod : transparentClassMethods.get(distinationCtMethod.getName())) {
				if (checkScope(distinationCtMethod.getModifiers()) && sameParams(distinationCtMethod, delegateCtMethod)) {
					String src = getMethodSource(distinationCtMethod);
					if (LOG.isDebugEnabled()) {
						LOG.debug("add method:[{}]", src);
					}
					CtMethod transparentCtMethod = CtNewMethod.make(src, transparencerCtClass);
					transparencerCtClass.addMethod(transparentCtMethod);
				}
			}
		}
		try {
			constructor1 = transparencerCtClass.toClass().getConstructor(delegateClass);
			constructor2 = delegateClass.getConstructor(sourceClass);
		} catch (Exception e) {
			LOG.error("unknown error.", e);
			throw e;
		}
	}

	private boolean sameParams(CtMethod m1, CtMethod m2) {
		CtClass[] p1;
		CtClass[] p2;
		try {
			p1 = m1.getParameterTypes();
			p2 = m2.getParameterTypes();
		} catch (NotFoundException e) {
			return false;
		}
		if (p1.length != p2.length)
			return false;
		for (int i = 0; i < p1.length; i++) {
			if (!p1[i].equals(p2[i]))
				return false;
		}
		return true;
	}

	private boolean checkScope(int mod) {
		if (Modifier.isPrivate(mod))
			return false;
		if (Modifier.isStatic(mod))
			return false;
		if (Modifier.isVolatile(mod))
			return false;
		if (Modifier.isTransient(mod))
			return false;
		if (Modifier.isNative(mod))
			return false;
		if (Modifier.isInterface(mod))
			return false;
		if (Modifier.isAnnotation(mod))
			return false;
		if (Modifier.isEnum(mod))
			return false;
		if (Modifier.isAbstract(mod))
			return false;
		if (Modifier.isStrict(mod))
			return false;

		return true;
	}

	private String getMethodSource(CtMethod distinationCtMethod) throws Exception {
		StringBuilder sb = new StringBuilder();
		CtClass[] paramTypes = null;
		CtClass[] exceptionTypes = null;
		CtClass returnType = null;
		paramTypes = distinationCtMethod.getParameterTypes();
		exceptionTypes = distinationCtMethod.getExceptionTypes();
		returnType = distinationCtMethod.getReturnType();
		int modifiers = distinationCtMethod.getModifiers();
		sb.append(Modifier.toString(modifiers)) //
				.append(" ") //
				.append(returnType.getName()) //
				.append(" ") //
				.append(distinationCtMethod.getName()) //
				.append("(") //
		;
		int i = 0;
		if (paramTypes != null && paramTypes.length > 0) {
			for (CtClass paramType : paramTypes) {
				if (i > 0)
					sb.append(", ");
				sb.append(paramType.getName()) //
						.append(" ") //
						.append("p" + i);
				i++;
			}
		}
		sb.append(") ");
		if (exceptionTypes != null && exceptionTypes.length > 0) {
			i = 0;
			sb.append("throws ");
			for (CtClass exceptionType : exceptionTypes) {
				if (i > 0)
					sb.append(", ");
				sb.append(exceptionType.getName());
				i++;
			}
		}
		sb.append(" { ");
		if (returnType != null && !CtClass.voidType.equals(returnType)) {
			sb.append("return ");
		}
		sb.append("_delegate.").append(distinationCtMethod.getName()).append("(");
		if (paramTypes != null && paramTypes.length > 0) {
			for (int x = 0; x < paramTypes.length; x++) {
				if (x > 0)
					sb.append(", ");
				sb.append("p" + x);
			}
		}
		sb.append(");");
		sb.append(" }");
		return sb.toString();
	}

	public Distination create(Source source) {
		try {
			return (Distination) constructor1.newInstance(constructor2.newInstance(source));
		} catch (Exception e) {
			LOG.error("unknown error.", e);
			throw new RuntimeException(e);
		}
	}
}
