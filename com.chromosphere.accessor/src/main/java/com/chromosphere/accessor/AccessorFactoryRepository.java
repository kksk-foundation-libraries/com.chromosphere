package com.chromosphere.accessor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.accessor.annotation.Delegator;

@SuppressWarnings("unchecked")
public class AccessorFactoryRepository {
	private static final Logger LOG = LoggerFactory.getLogger(AccessorFactoryRepository.class);

	private final ConcurrentMap<String, AccessorFactory<?, ?>> repository = new ConcurrentHashMap<>();

	public AccessorFactoryRepository() {
		String className = Thread.currentThread().getStackTrace()[1].getClassName();
		try {
			String packageName = Class.forName(className).getPackage().getName();
			scan(packageName);
		} catch (ClassNotFoundException e) {
		}
	}

	public AccessorFactoryRepository(String... packageNames) {
		scan(packageNames);
	}

	public void scan(String... packageNames) {
		for (String packageName : packageNames) {

			Reflections reflections = new Reflections(packageName);
			for (Class<?> found : reflections.getTypesAnnotatedWith(Delegator.class)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("class name:[{}]", found.getName());
				}
				Delegator delegator = found.getAnnotation(Delegator.class);
				try {
					AccessorFactoryBuilder builder = AccessorFactoryBuilder.builder();
					builder.sourceClass(delegator.sourceClass());
					builder.destinationClass(delegator.destinationClass());
					builder.delegatorClass(found);
					builder.delegator(delegator);

					AccessorFactory<?, ?> accessorFactory = builder.build();
					String[] keys = { delegator.sourceClass().getName(), delegator.destinationClass().getName() };
					String key = String.join("-", keys);
					if (!repository.containsKey(key)) {
						repository.putIfAbsent(key, accessorFactory);
					}
				} catch (Exception e) {
					LOG.error("unknown error.", e);
				}
			}
		}
	}

	public <Source, Destination> AccessorFactory<Source, Destination> getOrCreate(Class<?> sourceClass, Class<?> destinationClass) {
		String[] keys = { sourceClass.getName(), destinationClass.getName() };
		String key = String.join("-", keys);
		if (repository.containsKey(key))
			return (AccessorFactory<Source, Destination>) repository.get(key);
		AccessorFactoryBuilder builder = AccessorFactoryBuilder.builder();
		builder.sourceClass(sourceClass);
		builder.destinationClass(destinationClass);

		AccessorFactory<?, ?> accessorFactory = builder.build();
		if (!repository.containsKey(key)) {
			repository.putIfAbsent(key, accessorFactory);
		}
		return (AccessorFactory<Source, Destination>) repository.get(key);
	}

	public <Source, Destination> AccessorFactory<Source, Destination> get(Class<?> sourceClass, Class<?> destinationClass) {
		String[] keys = { sourceClass.getName(), destinationClass.getName() };
		String key = String.join("-", keys);
		return (AccessorFactory<Source, Destination>) repository.get(key);
	}
}
