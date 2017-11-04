package com.chromosphere.accessor.test.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.accessor.annotation.Delegator;

@Delegator(sourceClass = Test01Source.class, destinationClass = Test01Destination.class, initialize = "init", terminate = "term")
public class Test01Delegator {
	private static final Logger LOG = LoggerFactory.getLogger(Test01Delegator.class);
	private Test01Source source;

	public Test01Delegator(Test01Source source) {
		this.source = source;
	}

	public int getId() {
		return -source.getId();
	}

	public void setId(int id) {
		source.setId(-id);
	}

	public void init() {
		LOG.debug("called.");
	}

	public void term() {
		LOG.debug("called.");
	}

}
