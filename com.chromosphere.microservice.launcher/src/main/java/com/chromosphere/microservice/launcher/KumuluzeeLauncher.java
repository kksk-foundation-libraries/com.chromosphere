package com.chromosphere.microservice.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.kumuluz.ee.EeApplication;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewConstructor;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

public class KumuluzeeLauncher {
	public static void main(String[] args) {
		KumuluzeeLauncher launcher = new KumuluzeeLauncher();
		launcher.initialize();
		launcher.scanApplication();
		launcher.scanResources();
		launcher.createApplicationImplClass();
		launcher.createResourcesImplClass();
		launcher.addFiles();
		launcher.setClassPath();
		launcher.launchKumuluzEe();
	}

	private String currentPath;
	private File classesDir;
	private File beansxmlDir;
	private File webxmlDir;
	private ClassPool classPool;
	private Class<?> applicationClass = null;
	private String applicationClassName = null;
	private Map<String, Class<?>> resourceClasses;
	private ClassLoader classLoader;

	private static final String BEANS_XML_ORIGINAL = "beans.xml.original";
	private static final String WEB_XML_ORIGINAL = "web.xml.original";

	private KumuluzeeLauncher() {
	}

	private void initialize() {
		classLoader = KumuluzeeLauncher.class.getClassLoader();
		currentPath = System.getProperty("user.dir");
		classesDir = new File(new File(currentPath), "target" + File.separator + "classes");
		beansxmlDir = new File(classesDir, "META-INF");
		webxmlDir = new File(classesDir, "webapp" + File.separator + "WEB-INF");
		classPool = ClassPool.getDefault();
		resourceClasses = new HashMap<>();

		beansxmlDir.mkdirs();
		webxmlDir.mkdirs();
	}

	private void scanApplication() {
		try {
			String packageName = System.getProperty("scan.package.name");
			ImmutableSet<ClassInfo> classes = null;
			if (packageName == null) {
				classes = ClassPath.from(classLoader).getAllClasses();
			} else {
				classes = ClassPath.from(classLoader).getTopLevelClassesRecursive(packageName);
			}
			List<Class<?>> allClasses = classes.stream().filter(info -> {
				try {
					info.load();
					return true;
				} catch (Throwable e) {
					return false;
				}
			}).map(info -> info.load()).filter(clazz -> {
				return clazz.isAnnotationPresent(ApplicationPath.class);
			}).collect(Collectors.toList());
			if (allClasses == null || allClasses.isEmpty()) {
				throw new RuntimeException("javax.ws.rs.core.Application is not found.");
			} else if (allClasses.size() > 1) {
				throw new RuntimeException("Cannot launch multi-javax.ws.rs.core.Application.");
			}
			Class<?> clazz = allClasses.get(0);
			applicationClass = clazz;
			applicationClassName = clazz.getName();
		} catch (IOException e) {
		}
	}

	private void scanResources() {
		try {
			final String packageName = applicationClass.getPackage().getName();
			List<Class<?>> allClasses = ClassPath.from(classLoader).getTopLevelClassesRecursive(packageName).stream().filter(info -> {
				try {
					info.load();
					return true;
				} catch (Throwable e) {
					return false;
				}
			}).map(info -> info.load()).filter(clazz -> {
				if (clazz.isAnnotationPresent(Path.class))
					return true;
				for (Method m : clazz.getMethods()) {
					if (m.isAnnotationPresent(Path.class))
						return true;
				}
				return false;
			}).collect(Collectors.toList());
			if (allClasses == null || allClasses.isEmpty()) {
				throw new RuntimeException("javax.ws.rs.Path is not found.");
			}
			allClasses.stream().forEach(clazz -> {
				resourceClasses.putIfAbsent(clazz.getName(), clazz);
			});
		} catch (IOException e) {
		}
	}

	private void createApplicationImplClass() {
		createImplClass(applicationClass, applicationClassName, true);
	}

	private void createResourcesImplClass() {
		resourceClasses.entrySet().stream() //
				.forEach(entry -> {
					createImplClass(entry.getValue(), entry.getKey(), false);
				});
	}

	@SuppressWarnings("unchecked")
	private void createImplClass(Class<?> clazz, String className, boolean isApp) {
		String implClassName = className + "__CHROMOSPHERE";
		classPool.appendClassPath(new ClassClassPath(clazz));
		try {
			CtClass ctOrigClass = classPool.get(className);
			CtClass ctImplClass = classPool.makeClass(implClassName, ctOrigClass);
			if (ctImplClass.isFrozen())
				ctImplClass.defrost();
			ClassFile cfOrigClass = ctOrigClass.getClassFile();
			ctImplClass.addConstructor(CtNewConstructor.make("public " + ctImplClass.getSimpleName() + "(){super();}", ctImplClass));
			AnnotationsAttribute aaOrigClass = (AnnotationsAttribute) cfOrigClass.getAttribute(AnnotationsAttribute.visibleTag);
			if (aaOrigClass.getAnnotations().length > 0) {
				ClassFile cfImplClass = ctImplClass.getClassFile();
				ConstPool cpImplClass = cfImplClass.getConstPool();
				AnnotationsAttribute aaImplClass = new AnnotationsAttribute(cpImplClass, AnnotationsAttribute.visibleTag);
				for (Annotation anOrigClass : aaOrigClass.getAnnotations()) {
					Annotation anImplClass = new Annotation(anOrigClass.getTypeName(), cpImplClass);
					if (anOrigClass.getMemberNames() != null && anOrigClass.getMemberNames().size() > 0) {
						for (String memberName : (Set<String>) anOrigClass.getMemberNames()) {
							anImplClass.addMemberValue(memberName, anOrigClass.getMemberValue(memberName));
						}
					}
					aaImplClass.addAnnotation(anImplClass);
				}
				cfImplClass.addAttribute(aaImplClass);
			}
			ctImplClass.writeFile(classesDir.getAbsolutePath());
			ctImplClass.toClass();
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		} catch (CannotCompileException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void addFiles() {
		addFile(beansxmlDir, "beans.xml", BEANS_XML_ORIGINAL);
		addFile(webxmlDir, "web.xml", WEB_XML_ORIGINAL);
	}

	private void addFile(File parentDir, String fileName, String resourceFileName) {
		try (InputStream in = input(resourceFileName); OutputStream out = output(parentDir, resourceFileName);) {
			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private InputStream input(String resourceFileName) {
		return classLoader.getResourceAsStream(resourceFileName);
	}

	private OutputStream output(File parentDir, String fileName) throws IOException {
		return new FileOutputStream(new File(parentDir, fileName));
	}

	private void setClassPath() {
		setClassPath0(classesDir);
	}

	private void setClassPath0(File file) {
		if (classLoader instanceof URLClassLoader) {
			try {
				Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				method.setAccessible(true);
				method.invoke(classLoader, file.toURI().toURL());
				method.setAccessible(false);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
	}

	private void launchKumuluzEe() {
		new EeApplication();
	}
}
