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
public class TransparentizerFactory<Source, Destination> {
	private static final Logger LOG = LoggerFactory.getLogger(TransparentizerFactory.class);

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

	TransparentizerFactory(Class<Source> sourceClass, Class<Destination> destinationClass, Class<?> delegateClass, String initialize, String terminate) throws Exception {
		final String destinationClassName = destinationClass.getName();
		final String transparentizerClassName = ProxyFactory.nameGenerator.get(destinationClassName);
		final String interfaceName = Transparentizer.class.getName();
		final String delegateClassName = delegateClass.getName();
		LOG.debug("\nsourceClass:[{}],\ndestinationClassName:[{}],\ndelegateClassName:[{}],\ntransparencerClassName:[{}],\ninterfaceName:[{}]", sourceClass.getName(), destinationClassName, delegateClassName, transparentizerClassName, interfaceName);

		Multimap<String, CtMethod> transparentClassMethods = MultimapBuilder.linkedHashKeys().arrayListValues().build();
		CtClass destinationCtClass = CLASS_POOL.get(destinationClassName);
		CtClass delegateCtClass = CLASS_POOL.get(delegateClassName);
		for (CtMethod delegateCtMethod : delegateCtClass.getMethods()) {
			if (!couldNotDefine.contains(delegateCtMethod.getName()) && checkScope(delegateCtMethod.getModifiers())) {
				LOG.debug("target method:[{}]", delegateCtMethod.getName());
				transparentClassMethods.put(delegateCtMethod.getName(), delegateCtMethod);
			}
		}

		CtClass transparentizerCtClass = CLASS_POOL.makeClass(transparentizerClassName, destinationCtClass);
		transparentizerCtClass.setInterfaces(new CtClass[] { CLASS_POOL.get(interfaceName) });
		transparentizerCtClass.addField(CtField.make(String.format("private final %s _delegate;", delegateClassName), transparentizerCtClass));
		String shortName = transparentizerClassName.substring(transparentizerClassName.lastIndexOf(".") + 1);
		transparentizerCtClass.addConstructor(CtNewConstructor.make(String.format("public %s(%s _delegate) {this._delegate = _delegate;}", shortName, delegateClassName), transparentizerCtClass));
		if (!"".equals(initialize)) {
			CtMethod initializeCtMethod = CtNewMethod.make(String.format("public void _initialize() {_delegate.%s();}", initialize), transparentizerCtClass);
			transparentizerCtClass.addMethod(initializeCtMethod);
		} else {
			CtMethod initializeCtMethod = CtNewMethod.make(String.format("public void _initialize() {}"), transparentizerCtClass);
			transparentizerCtClass.addMethod(initializeCtMethod);
		}
		if (!"".equals(terminate)) {
			CtMethod terminateCtMethod = CtNewMethod.make(String.format("public void _terminate() {_delegate.%s();}", terminate), transparentizerCtClass);
			transparentizerCtClass.addMethod(terminateCtMethod);
		} else {
			CtMethod terminateCtMethod = CtNewMethod.make(String.format("public void _terminate() {}"), transparentizerCtClass);
			transparentizerCtClass.addMethod(terminateCtMethod);
		}
		for (CtMethod destinationCtMethod : destinationCtClass.getMethods()) {
			for (CtMethod delegateCtMethod : transparentClassMethods.get(destinationCtMethod.getName())) {
				if (checkScope(destinationCtMethod.getModifiers()) && sameParams(destinationCtMethod, delegateCtMethod)) {
					String src = getMethodSource(destinationCtMethod);
					if (LOG.isDebugEnabled()) {
						LOG.debug("add method:[{}]", src);
					}
					CtMethod transparentCtMethod = CtNewMethod.make(src, transparentizerCtClass);
					transparentizerCtClass.addMethod(transparentCtMethod);
				}
			}
		}
		try {
			constructor1 = transparentizerCtClass.toClass().getConstructor(delegateClass);
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

	private String getMethodSource(CtMethod destinationCtMethod) throws Exception {
		StringBuilder sb = new StringBuilder();
		CtClass[] paramTypes = null;
		CtClass[] exceptionTypes = null;
		CtClass returnType = null;
		paramTypes = destinationCtMethod.getParameterTypes();
		exceptionTypes = destinationCtMethod.getExceptionTypes();
		returnType = destinationCtMethod.getReturnType();
		int modifiers = destinationCtMethod.getModifiers();
		sb.append(Modifier.toString(modifiers)) //
				.append(" ") //
				.append(returnType.getName()) //
				.append(" ") //
				.append(destinationCtMethod.getName()) //
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
		sb.append("_delegate.").append(destinationCtMethod.getName()).append("(");
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

	public Destination create(Source source) {
		try {
			return (Destination) constructor1.newInstance(constructor2.newInstance(source));
		} catch (Exception e) {
			LOG.error("unknown error.", e);
			throw new RuntimeException(e);
		}
	}
}
