package com.chromosphere.transparent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.transparent.annotations.Transparent;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class TransparencerFactoryRepository {
	private static final Logger LOG = LoggerFactory.getLogger(TransparencerFactoryRepository.class);

	public TransparencerFactoryRepository(String packageName) {
		initialize(packageName);
	}

	private final ConcurrentMap<String, TransparencerFactory<?, ?>> map = new ConcurrentHashMap<>();

	private void initialize(String packageName) {
		Reflections reflections = new Reflections(packageName);
		for (Class<?> found : reflections.getTypesAnnotatedWith(Transparent.class)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("class name:[{}]", found.getName());
			}
			Transparent transparent = found.getAnnotation(Transparent.class);
			try {
				TransparencerFactory<?, ?> transparencerFactory = new TransparencerFactory(transparent.sourceClass(), transparent.distinationClass(), found, transparent.initialize(), transparent.terminate());
				String key = transparent.key();
				if (!"".equals(key)) {
					map.putIfAbsent(key, transparencerFactory);
				}
				String[] keys = { transparent.sourceClass().getName(), transparent.distinationClass().getName() };
				key = String.join("-", keys);
				if (!map.containsKey(key)) {
					map.putIfAbsent(key, transparencerFactory);
				}

			} catch (Exception e) {
				LOG.error("unknown error.", e);
			}
		}
	}

	public <Source, Distination> TransparencerFactory<Source, Distination> getFactory(Class<Source> sourceClass, Class<Distination> distinationClass) {
		String[] keys = { sourceClass.getName(), distinationClass.getName() };
		String key = String.join("-", keys);
		return (TransparencerFactory<Source, Distination>) map.get(key);
	}

	public <Source, Distination> TransparencerFactory<Source, Distination> getFactory(String key) {
		return (TransparencerFactory<Source, Distination>) map.get(key);
	}
}
