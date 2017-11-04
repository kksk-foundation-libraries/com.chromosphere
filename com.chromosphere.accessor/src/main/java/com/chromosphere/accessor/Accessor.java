package com.chromosphere.accessor;

public interface Accessor<Source> {
	Source _getSource();

	void _initialize();

	void _terminate();
}
