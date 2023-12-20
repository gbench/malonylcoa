open module gbench.malonylcoa {
	requires java.base;
	requires transitive java.sql;
	requires com.fasterxml.jackson.core;
	requires transitive com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jsr310;
	requires transitive org.apache.poi.poi;
	requires transitive org.apache.poi.ooxml;
	requires pinyin4j;

	exports gbench.util.array;
	exports gbench.util.chn;
	exports gbench.util.data;
	exports gbench.util.data.xls;
	exports gbench.util.function;
	exports gbench.util.io;
	exports gbench.util.jdbc;
	exports gbench.util.jdbc.annotation;
	exports gbench.util.jdbc.function;
	exports gbench.util.jdbc.kvp;
	exports gbench.util.jdbc.sql;
	exports gbench.util.json;
	exports gbench.util.lisp;
	exports gbench.util.math;
	exports gbench.util.math.algebra;
	exports gbench.util.math.algebra.op;
	exports gbench.util.math.algebra.tuple;
	exports gbench.util.math.algebra.symbol;
	exports gbench.util.matrix;
	exports gbench.util.tree;
	exports gbench.util.type;
}