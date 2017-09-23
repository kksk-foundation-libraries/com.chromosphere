package com.chromosphere.transparent;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.transparent.test.Distination;
import com.chromosphere.transparent.test.Source;

public class TransparencerTest {
	private static final Logger LOG = LoggerFactory.getLogger(TransparencerTest.class);

	@Test
	public void test001() {
		TransparencerFactoryRepository repo = new TransparencerFactoryRepository(getClass().getPackage().getName());
		TransparencerFactory<Source, Distination> transparencerFactory = repo.getFactory("hogehoge");
		Source source = new Source();
		source.setId(1);
		source.setName("1-2");
		Distination distination = transparencerFactory.create(source);
		LOG.debug("source id:[{}], name:[{}]", source.getId(), source.getName());
		LOG.debug("distination id:[{}], name:[{}]", distination.getId(), distination.getName());
		Transparencer xx = (Transparencer) distination;
		xx._initialize();
		xx._terminate();
	}

	@Test
	public void test002() {
		TransparencerFactoryRepository repo = new TransparencerFactoryRepository(getClass().getPackage().getName());
		TransparencerFactory<Source, Distination> transparencerFactory = repo.getFactory(Source.class, Distination.class);
		Source source = new Source();
		source.setId(1);
		source.setName("1-2");
		Distination distination = transparencerFactory.create(source);
		LOG.debug("source id:[{}], name:[{}]", source.getId(), source.getName());
		LOG.debug("distination id:[{}], name:[{}]", distination.getId(), distination.getName());
		Transparencer xx = (Transparencer) distination;
		xx._initialize();
		xx._terminate();
	}

	@Test
	public void test003() {
		TransparencerFactoryRepository repo = new TransparencerFactoryRepository(getClass().getPackage().getName());
		TransparencerFactory<Source, Distination> transparencerFactory = repo.getFactory("hogehoge");
		Source source = new Source();
		source.setId(1);
		source.setName("1-2");
		long start = System.nanoTime();
		for (long i = 0; i < 10_000_000; i++) {
			Distination distination = transparencerFactory.create(source);
			distination.setId((int)i);
		}
		long end = System.nanoTime();
		LOG.debug("elapsed:[{}]", (end - start) / 10_000_000.0d);
	}

	@Test
	public void test004() {
		TransparencerFactoryRepository repo = new TransparencerFactoryRepository(getClass().getPackage().getName());
		TransparencerFactory<Source, Distination> transparencerFactory = repo.getFactory(Source.class, Distination.class);
		Source source = new Source();
		source.setId(1);
		source.setName("1-2");
		long start = System.nanoTime();
		for (long i = 0; i < 10_000_000; i++) {
			Distination distination = transparencerFactory.create(source);
			distination.setId((int)i);
		}
		long end = System.nanoTime();
		LOG.debug("elapsed:[{}]", (end - start) / 10_000_000.0d);
	}
}
