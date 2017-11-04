package com.chromosphere.accessor;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.accessor.annotation.Delegator;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;

@SuppressWarnings("unchecked")
class JavassistAccessorFactoryBuilder extends AccessorFactoryBuilder {
	private static final Logger LOG = LoggerFactory.getLogger(JavassistAccessorFactoryBuilder.class);

	private static final ClassPool CLASS_POOL = ClassPool.getDefault();
	private static final AtomicInteger counter = new AtomicInteger();

	private static final String INTERFACE_NAME;
	static {
		CLASS_POOL.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
		INTERFACE_NAME = Accessor.class.getName();
	}

	private Class<?> sourceClass;
	private Class<?> delegatorClass;
	private String sourceClassName;
	private String destinationClassName;
	private String accessorClassName;
	private String delegatorClassName;
	private Delegator delegator = null;

	JavassistAccessorFactoryBuilder() {
	}

	@Override
	AccessorFactoryBuilder sourceClass(Class<?> sourceClass) {
		this.sourceClass = sourceClass;
		this.sourceClassName = sourceClass.getName();
		return this;
	}

	@Override
	AccessorFactoryBuilder destinationClass(Class<?> destinationClass) {
		this.destinationClassName = destinationClass.getName();
		this.accessorClassName = destinationClassName + "_$$_" + counter.getAndIncrement();
		return this;
	}

	@Override
	AccessorFactoryBuilder delegatorClass(Class<?> delegatorClass) {
		this.delegatorClass = delegatorClass;
		this.delegatorClassName = delegatorClass.getName();
		return this;
	}

	@Override
	AccessorFactoryBuilder delegator(Delegator delegator) {
		this.delegator = delegator;
		return this;
	}

	@Override
	<Source, Destination> AccessorFactory<Source, Destination> build() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("sourceClassName:[{}]", sourceClassName);
			LOG.debug("destinationClassName:[{}]", destinationClassName);
			LOG.debug("accessorClassName:[{}]", accessorClassName);
			LOG.debug("delegatorClassName:[{}]", delegatorClassName);
		}
		if (sourceClassName == null || sourceClassName.length() == 0) {
			LOG.error("sourceClassName is empty.");
			throw new RuntimeException("sourceClassName is empty.");
		}
		if (destinationClassName == null || destinationClassName.length() == 0) {
			LOG.error("destinationClassName is empty.");
			throw new RuntimeException("destinationClassName is empty.");
		}
		if (accessorClassName == null || accessorClassName.length() == 0) {
			LOG.error("accessorClassName is empty.");
			throw new RuntimeException("accessorClassName is empty.");
		}
		Constructor<?> destinationConstructor = null;
		Constructor<?> delegatorConstructor = null;
		if (delegatorClassName != null && delegatorClassName.length() > 0) {
			if (delegator == null) {
				LOG.error("delegator is null.");
				throw new RuntimeException("delegator is null.");
			}
			try {
				destinationConstructor = createConstructorWithDelegator();
				delegatorConstructor = delegatorClass.getConstructor(sourceClass);
			} catch (Exception e) {
				LOG.error("unknown error.", e);
				throw new RuntimeException(e);
			}
		} else {
			try {
				destinationConstructor = createConstructorWithoutDelegator();
			} catch (Exception e) {
				LOG.error("unknown error.", e);
				throw new RuntimeException(e);
			}
		}
		return (AccessorFactory<Source, Destination>) new AccessorFactory<>(destinationConstructor, delegatorConstructor);
	}

	private Constructor<?> createConstructorWithDelegator() throws Exception {
		Map<String, CtMethod> delegatorClassMethods = new HashMap<>();
		Map<String, CtMethod> sourceClassMethods = new HashMap<>();
		CtClass destinationCtClass = CLASS_POOL.get(destinationClassName);
		CtClass delegatorCtClass = CLASS_POOL.get(delegatorClassName);
		for (CtMethod delegatorCtMethod : delegatorCtClass.getMethods()) {
			String key = AccessorHelper.getMethodLongName(delegatorCtMethod);
			if (!couldNotDefine.contains(delegatorCtMethod.getName()) && !delegatorClassMethods.containsKey(key) && AccessorHelper.checkReadScope(delegatorCtMethod.getModifiers())) {
				LOG.debug("target method:[{}]", delegatorCtMethod.getName());
				delegatorClassMethods.put(key, delegatorCtMethod);
			}
		}
		CtClass sourceCtClass = CLASS_POOL.get(sourceClassName);
		for (CtMethod sourceCtMethod : sourceCtClass.getMethods()) {
			String key = AccessorHelper.getMethodLongName(sourceCtMethod);
			if (!couldNotDefine.contains(sourceCtMethod.getName()) && !sourceClassMethods.containsKey(key) && AccessorHelper.checkReadScope(sourceCtMethod.getModifiers())) {
				LOG.debug("target method:[{}]", sourceCtMethod.getName());
				sourceClassMethods.put(key, sourceCtMethod);
			}
		}

		CtClass accessorCtClass = CLASS_POOL.makeClass(accessorClassName, destinationCtClass);
		accessorCtClass.addInterface(CLASS_POOL.get(INTERFACE_NAME));
		String shortName = accessorClassName.substring(accessorClassName.lastIndexOf(".") + 1);
		accessorCtClass.addField(CtField.make(String.format("private final %s _source;", sourceClassName), accessorCtClass));
		accessorCtClass.addField(CtField.make(String.format("private final %s _delegator;", delegatorClassName), accessorCtClass));
		accessorCtClass.addConstructor(CtNewConstructor.make(String.format("public %s(%s _source, %s _delegator) {this._source = _source;this._delegator = _delegator;}", shortName, sourceClassName, delegatorClassName), accessorCtClass));
		if (!"".equals(delegator.initialize())) {
			CtMethod initializeCtMethod = CtNewMethod.make(String.format("public void _initialize() {_delegator.%s();}", delegator.initialize()), accessorCtClass);
			accessorCtClass.addMethod(initializeCtMethod);
		} else {
			CtMethod initializeCtMethod = CtNewMethod.make(String.format("public void _initialize() {}"), accessorCtClass);
			accessorCtClass.addMethod(initializeCtMethod);
		}
		if (!"".equals(delegator.terminate())) {
			CtMethod terminateCtMethod = CtNewMethod.make(String.format("public void _terminate() {_delegator.%s();}", delegator.terminate()), accessorCtClass);
			accessorCtClass.addMethod(terminateCtMethod);
		} else {
			CtMethod terminateCtMethod = CtNewMethod.make(String.format("public void _terminate() {}"), accessorCtClass);
			accessorCtClass.addMethod(terminateCtMethod);
		}

		CtMethod getSourceCtMethod = CtNewMethod.make("public Object _getSource() {return this._source;}", accessorCtClass);
		accessorCtClass.addMethod(getSourceCtMethod);

		for (CtMethod destinationCtMethod : destinationCtClass.getMethods()) {
			String key = AccessorHelper.getMethodLongName(destinationCtMethod);
			if (delegatorClassMethods.containsKey(key)) {
				CtMethod ctMethod = delegatorClassMethods.get(key);
				if (AccessorHelper.checkWriteScope(destinationCtMethod.getModifiers()) && AccessorHelper.sameParams(destinationCtMethod, ctMethod) && destinationCtMethod.getReturnType().equals(ctMethod.getReturnType())) {
					String src = AccessorHelper.getDecolatorMethodSourceByDelegatorClass(destinationCtMethod);
					if (LOG.isDebugEnabled()) {
						LOG.debug("add method:[{}]", src);
					}
					CtMethod accessorCtMethod = CtNewMethod.make(src, accessorCtClass);
					accessorCtClass.addMethod(accessorCtMethod);
				}
			} else if (sourceClassMethods.containsKey(key)) {
				CtMethod ctMethod = sourceClassMethods.get(key);
				if (AccessorHelper.checkWriteScope(destinationCtMethod.getModifiers()) && AccessorHelper.sameParams(destinationCtMethod, ctMethod) && destinationCtMethod.getReturnType().equals(ctMethod.getReturnType())) {
					String src = AccessorHelper.getDecolatorMethodSourceBySourceClass(destinationCtMethod);
					if (LOG.isDebugEnabled()) {
						LOG.debug("add method:[{}]", src);
					}
					CtMethod accessorCtMethod = CtNewMethod.make(src, accessorCtClass);
					accessorCtClass.addMethod(accessorCtMethod);
				}
			}
		}
		return accessorCtClass.toClass().getConstructor(sourceClass, delegatorClass);
	}

	private Constructor<?> createConstructorWithoutDelegator() throws Exception {
		Map<String, CtMethod> sourceClassMethods = new HashMap<>();
		CtClass destinationCtClass = CLASS_POOL.get(destinationClassName);
		CtClass sourceCtClass = CLASS_POOL.get(sourceClassName);
		for (CtMethod sourceCtMethod : sourceCtClass.getMethods()) {
			String key = AccessorHelper.getMethodLongName(sourceCtMethod);
			if (!couldNotDefine.contains(sourceCtMethod.getName()) && !sourceClassMethods.containsKey(key) && AccessorHelper.checkReadScope(sourceCtMethod.getModifiers())) {
				LOG.debug("target method:[{}]", sourceCtMethod.getName());
				sourceClassMethods.put(key, sourceCtMethod);
			}
		}

		CtClass accessorCtClass = CLASS_POOL.makeClass(accessorClassName, destinationCtClass);
		accessorCtClass.addInterface(CLASS_POOL.get(INTERFACE_NAME));
		String shortName = accessorClassName.substring(accessorClassName.lastIndexOf(".") + 1);
		accessorCtClass.addField(CtField.make(String.format("private final %s _source;", sourceClassName), accessorCtClass));
		accessorCtClass.addConstructor(CtNewConstructor.make(String.format("public %s(%s _source) {this._source = _source;}", shortName, sourceClassName), accessorCtClass));

		CtMethod getSourceCtMethod = CtNewMethod.make("public Object _getSource() {return this._source;}", accessorCtClass);
		accessorCtClass.addMethod(getSourceCtMethod);

		for (CtMethod destinationCtMethod : destinationCtClass.getMethods()) {
			String key = AccessorHelper.getMethodLongName(destinationCtMethod);
			if (sourceClassMethods.containsKey(key)) {
				CtMethod ctMethod = sourceClassMethods.get(key);
				if (AccessorHelper.checkWriteScope(destinationCtMethod.getModifiers()) && AccessorHelper.sameParams(destinationCtMethod, ctMethod) && destinationCtMethod.getReturnType().equals(ctMethod.getReturnType())) {
					String src = AccessorHelper.getDecolatorMethodSourceBySourceClass(destinationCtMethod);
					if (LOG.isDebugEnabled()) {
						LOG.debug("add method:[{}]", src);
					}
					CtMethod accessorCtMethod = CtNewMethod.make(src, accessorCtClass);
					accessorCtClass.addMethod(accessorCtMethod);
				}
			}
		}
		return accessorCtClass.toClass().getConstructor(sourceClass);
	}
}
