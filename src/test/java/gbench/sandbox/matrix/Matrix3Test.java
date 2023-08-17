package gbench.sandbox.matrix;

import static gbench.util.lisp.IRecord.A;
import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.matrix.Matrices;
import gbench.util.matrix.Matrix;
import gbench.util.type.Types;
import gbench.util.lisp.IRecord;

import static gbench.util.type.Types.*;

import java.util.function.Function;

public class Matrix3Test {
	
	@SuppressWarnings("unchecked")
	public Function<IRecord,Matrices<Double>> K(final String key){
		return rec->(Matrices<Double>)rec.get(key);
	}

	@Test
	public void foo() {
		final var A = Matrix.of(A(3.3, 1, 2, 6, 3, 4, 3, 1, 5), 3);
		println(A);
		final var palu = Matrices.lu(A).toArray(cast((Matrices<Double>) null));
		
		final Matrices<Double> l = palu[0];
		final Matrices<Double> u = palu[1];
		final Matrices<Double> p = palu[2];
		println();
		println(l.mmult(u));
		println();
		println(p.mmult(A));
	}

	/**
	 * 
	 */
	@Test
	public void bar() {
		final var A = Matrix.of(A(2, 1, 5, 4, 4, -4, 1, 3, 1), 3, 3).fmap(Number::doubleValue);
		println(A);
		println("-------------------------");
		final var palu = Matrices.lu(A).toArray(cast((Matrices<Double>) null));
		final Matrices<Double> l = palu[0];
		final Matrices<Double> u = palu[1];
		final Matrices<Double> p = palu[2];

		println(l.mmult(u));
		println();
		println(p.mmult(A));
		println("----------------");
		println(l.mmult(l.fmap((index, v) -> index._1 > index._2 ? -v : v)));
	}

	@Test
	public void quz() {
		final var a = (Types.getClassHierarchy(Integer.class));
		final var b = (Types.getClassHierarchy(String.class));
		println(a);
		println(b);
		final var p = Types.getSharedSuperClasses(Integer.class, Float.class, String.class);
		println(p);
		println(Matrix.of(A(1,2,3,4,5),2));

	}
}
