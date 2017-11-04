package com.chromosphere.accessor.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chromosphere.accessor.Accessor;
import com.chromosphere.accessor.AccessorFactory;
import com.chromosphere.accessor.AccessorFactoryRepository;
import com.chromosphere.accessor.test.dto.Test00Destination;
import com.chromosphere.accessor.test.dto.Test00Source;
import com.chromosphere.accessor.test.dto.Test01Destination;
import com.chromosphere.accessor.test.dto.Test01Source;

public class AccessorTest {
	private static final Logger LOG = LoggerFactory.getLogger(AccessorTest.class);

	private static AccessorFactoryRepository accessorFactoryRepository;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		accessorFactoryRepository = new AccessorFactoryRepository();
	}

	@Test
	public void test00() {
		Test00Source source = new Test00Source();
		source.setId(1);
		source.setName("name");
		LOG.debug("before source id:[{}]", source.getId());
		LOG.debug("before source name:[{}]", source.getName());
		AccessorFactory<Test00Source, Test00Destination> accessorFactory = accessorFactoryRepository.getOrCreate(Test00Source.class, Test00Destination.class);
		Test00Destination destination = accessorFactory.create(source);
		LOG.debug("after source id:[{}]", source.getId());
		LOG.debug("after source name:[{}]", source.getName());
		LOG.debug("after destination id:[{}]", destination.getId());
		LOG.debug("after destination name:[{}]", destination.getName());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test01() {
		Test01Source source = new Test01Source();
		source.setId(1);
		source.setName("name");
		LOG.debug("before source id:[{}]", source.getId());
		LOG.debug("before source name:[{}]", source.getName());
		AccessorFactory<Test01Source, Test01Destination> accessorFactory = accessorFactoryRepository.getOrCreate(Test01Source.class, Test01Destination.class);
		Test01Destination destination = accessorFactory.create(source);
		LOG.debug("after source id:[{}]", source.getId());
		LOG.debug("after source name:[{}]", source.getName());
		LOG.debug("after destination id:[{}]", destination.getId());
		LOG.debug("after destination name:[{}]", destination.getName());
		Accessor<Test01Source> accessor = (Accessor<Test01Source>) destination;
		accessor._initialize();
		accessor._terminate();
		Test01Source source2 = accessor._getSource();
		LOG.debug("---[{}]", source2.getId());
	}
}
