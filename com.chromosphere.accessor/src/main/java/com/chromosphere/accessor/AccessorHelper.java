package com.chromosphere.accessor;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

class AccessorHelper {
	public static boolean sameParams(CtMethod m1, CtMethod m2) {
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

	public static boolean checkReadScope(int mod) {
		if (Modifier.isPrivate(mod))
			return false;
		if (Modifier.isStatic(mod))
			return false;
		if (Modifier.isVolatile(mod))
			return false;
		if (Modifier.isTransient(mod))
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

	public static boolean checkWriteScope(int mod) {
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

	public static String getDecolatorMethodSourceBySourceClass(CtMethod ctMethod) throws Exception {
		return getDecolatorMethodSource("_source", ctMethod);
	}

	public static String getDecolatorMethodSourceByDelegatorClass(CtMethod ctMethod) throws Exception {
		return getDecolatorMethodSource("_delegator", ctMethod);
	}

	private static String getDecolatorMethodSource(String delegated, CtMethod ctMethod) throws Exception {
		StringBuilder sb = new StringBuilder();
		CtClass[] paramTypes = null;
		CtClass[] exceptionTypes = null;
		CtClass returnType = null;
		paramTypes = ctMethod.getParameterTypes();
		exceptionTypes = ctMethod.getExceptionTypes();
		returnType = ctMethod.getReturnType();
		int modifiers = ctMethod.getModifiers();
		sb.append(Modifier.toString(modifiers)) //
				.append(" ") //
				.append(returnType.getName()) //
				.append(" ") //
				.append(ctMethod.getName()) //
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
		sb.append(delegated).append(".").append(ctMethod.getName()).append("(");
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

	public static String getMethodLongName(CtMethod ctMethod) {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append(ctMethod.getReturnType().getName()) //
					.append(" ") //
					.append(ctMethod.getName()) //
					.append("(");
		} catch (NotFoundException e) {
		}

		boolean isFirst = true;
		try {
			for (CtClass parameterType : ctMethod.getParameterTypes()) {
				if (!isFirst) {
					sb.append(", ");
					isFirst = false;
				}
				sb.append(parameterType.getName());
			}
		} catch (NotFoundException e) {
		}
		sb.append(")");
		return sb.toString();
	}
}
