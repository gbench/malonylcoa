module gbench.malonylcoa {
	requires java.sql;
	requires java.base;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jsr310;
	requires org.apache.poi.poi;
	requires org.apache.poi.ooxml;
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