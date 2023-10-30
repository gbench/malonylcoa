module gbench.malonylcoa {
	requires java.sql;
	requires java.base;
	requires com.fasterxml.jackson.core;
	exports gbench.util.array;
	exports gbench.util.data;
	exports gbench.util.function;
	exports gbench.util.io;
	exports gbench.util.json;
	exports gbench.util.lisp;
	exports gbench.util.math;
	exports gbench.util.matrix;
	exports gbench.util.tree;
	exports gbench.util.type;
}