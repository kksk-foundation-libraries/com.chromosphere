package com.chromosphere.transparent.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.transparent.annotations.Transparent;

@Transparent(sourceClass = Source.class, distinationClass = Distination.class, key = "hogehoge", initialize = "init", terminate = "term")
public class Delegate {
	private static final Logger LOG = LoggerFactory.getLogger(Delegate.class);
	private Source source;

	public Delegate(Source source) {
		this.source = source;
	}

	public void init() {
		LOG.debug("called.");
	}

	public void term() {
		LOG.debug("called.");
	}

	public int getId() {
		return -source.getId();
	}

	public void setId(int id) {
		source.setId(-id);
	}

	public String getName() {
		return source.getName();
	}

	public void setName(String name) {
		source.setName(name);
	}
}
