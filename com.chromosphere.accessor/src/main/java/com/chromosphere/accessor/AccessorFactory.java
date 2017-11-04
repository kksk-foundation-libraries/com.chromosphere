package com.chromosphere.accessor;

import java.lang.reflect.Constructor;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public final class AccessorFactory<Source, Destination> {
	private static final Logger LOG = LoggerFactory.getLogger(AccessorFactory.class);

	private final Constructor<?> destinationConstructor;
	private final Optional<Constructor<?>> delegatorConstructor;

	AccessorFactory(Constructor<?> destinationConstructor, Constructor<?> delegatorConstructor) {
		this.destinationConstructor = destinationConstructor;
		this.delegatorConstructor = Optional.ofNullable(delegatorConstructor);
	}

	public Destination create(Source source) {
		Destination destination = null;
		try {
			if (delegatorConstructor.isPresent()) {
				destination = (Destination) destinationConstructor.newInstance(source, delegatorConstructor.get().newInstance(source));
			} else {
				destination = (Destination) destinationConstructor.newInstance(source);
			}
		} catch (Exception e) {
			LOG.error("", e);
			throw new RuntimeException(e);
		}
		return destination;
	}
}
