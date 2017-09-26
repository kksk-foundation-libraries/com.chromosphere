package com.chromosphere.transparent;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.transparent.test.Destination;
import com.chromosphere.transparent.test.Source;

public class TransparentizerTest {
	private static final Logger LOG = LoggerFactory.getLogger(TransparentizerTest.class);

	@Test
	public void test001() {
		TransparentizerFactoryRepository repo = new TransparentizerFactoryRepository(getClass().getPackage().getName());
		TransparentizerFactory<Source, Destination> transparentizerFactory = repo.getFactory("hogehoge");
		Source source = new Source();
		source.setId(1);
		source.setName("1-2");
		Destination destination = transparentizerFactory.create(source);
		LOG.debug("source id:[{}], name:[{}]", source.getId(), source.getName());
		LOG.debug("destination id:[{}], name:[{}]", destination.getId(), destination.getName());
		Transparentizer xx = (Transparentizer) destination;
		xx._initialize();
		xx._terminate();
	}

	@Test
	public void test002() {
		TransparentizerFactoryRepository repo = new TransparentizerFactoryRepository(getClass().getPackage().getName());
		TransparentizerFactory<Source, Destination> transparentizerFactory = repo.getFactory(Source.class, Destination.class);
		Source source = new Source();
		source.setId(1);
		source.setName("1-2");
		Destination destination = transparentizerFactory.create(source);
		LOG.debug("source id:[{}], name:[{}]", source.getId(), source.getName());
		LOG.debug("destination id:[{}], name:[{}]", destination.getId(), destination.getName());
		Transparentizer xx = (Transparentizer) destination;
		xx._initialize();
		xx._terminate();
	}

	@Test
	public void test003() {
		TransparentizerFactoryRepository repo = new TransparentizerFactoryRepository(getClass().getPackage().getName());
		TransparentizerFactory<Source, Destination> transparentizerFactory = repo.getFactory("hogehoge");
		Source source = new Source();
		source.setId(1);
		source.setName("1-2");
		long start = System.nanoTime();
		for (long i = 0; i < 10_000_000; i++) {
			Destination destination = transparentizerFactory.create(source);
			destination.setId((int)i);
		}
		long end = System.nanoTime();
		LOG.debug("elapsed:[{}]", (end - start) / 10_000_000.0d);
	}

	@Test
	public void test004() {
		TransparentizerFactoryRepository repo = new TransparentizerFactoryRepository(getClass().getPackage().getName());
		TransparentizerFactory<Source, Destination> transparentizerFactory = repo.getFactory(Source.class, Destination.class);
		Source source = new Source();
		source.setId(1);
		source.setName("1-2");
		long start = System.nanoTime();
		for (long i = 0; i < 10_000_000; i++) {
			Destination destination = transparentizerFactory.create(source);
			destination.setId((int)i);
		}
		long end = System.nanoTime();
		LOG.debug("elapsed:[{}]", (end - start) / 10_000_000.0d);
	}
}
