package com.chromosphere.transparent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.transparent.annotations.Transparent;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class TransparentizerFactoryRepository {
	private static final Logger LOG = LoggerFactory.getLogger(TransparentizerFactoryRepository.class);

	public TransparentizerFactoryRepository(String packageName) {
		initialize(packageName);
	}

	private final ConcurrentMap<String, TransparentizerFactory<?, ?>> map = new ConcurrentHashMap<>();

	private void initialize(String packageName) {
		Reflections reflections = new Reflections(packageName);
		for (Class<?> found : reflections.getTypesAnnotatedWith(Transparent.class)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("class name:[{}]", found.getName());
			}
			Transparent transparent = found.getAnnotation(Transparent.class);
			try {
				TransparentizerFactory<?, ?> transparentizerFactory = new TransparentizerFactory(transparent.sourceClass(), transparent.destinationClass(), found, transparent.initialize(), transparent.terminate());
				String key = transparent.key();
				if (!"".equals(key)) {
					map.putIfAbsent(key, transparentizerFactory);
				}
				String[] keys = { transparent.sourceClass().getName(), transparent.destinationClass().getName() };
				key = String.join("-", keys);
				if (!map.containsKey(key)) {
					map.putIfAbsent(key, transparentizerFactory);
				}

			} catch (Exception e) {
				LOG.error("unknown error.", e);
			}
		}
	}

	public <Source, Destination> TransparentizerFactory<Source, Destination> getFactory(Class<Source> sourceClass, Class<Destination> distinationClass) {
		String[] keys = { sourceClass.getName(), distinationClass.getName() };
		String key = String.join("-", keys);
		return (TransparentizerFactory<Source, Destination>) map.get(key);
	}

	public <Source, Distination> TransparentizerFactory<Source, Distination> getFactory(String key) {
		return (TransparentizerFactory<Source, Distination>) map.get(key);
	}
}
