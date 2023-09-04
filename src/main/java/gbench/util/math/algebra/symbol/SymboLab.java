package gbench.util.math.algebra.symbol;

import static gbench.util.math.algebra.Node.PACK;
import static gbench.util.math.algebra.op.BinaryOp.bop;
import static gbench.util.math.algebra.op.BinaryOp.dbl;
import static gbench.util.math.algebra.op.Comma.COMMA_TEST;
import static gbench.util.math.algebra.op.Ops.ADD;
import static gbench.util.math.algebra.op.Ops.MUL;
import static gbench.util.math.algebra.op.Ops.POW;
import static gbench.util.math.algebra.op.Ops.TOKEN;
import static gbench.util.math.algebra.op.Ops.kvp_int;
import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import gbench.util.math.algebra.Node;
import gbench.util.math.algebra.op.BinaryOp;
import gbench.util.math.algebra.op.ConstantOp;
import gbench.util.math.algebra.op.UnaryOp;
import gbench.util.math.algebra.tuple.IRecord;
import gbench.util.math.algebra.tuple.Tuple2;

/**
 * 符号计算库
 * 
 * @author gbench
 *
 */
public class SymboLab implements ISymboLab {

	/**
	 * 符号计算
	 * 
	 * @param <T>    第一参数类型
	 * @param <U>    第二参数类型
	 * @param symbol 运算符号
	 * @return 计算结果
	 */
	@Override
	public <T, U> BinaryOp<Object, Object> simplify(final BinaryOp<T, U> symbol) {

		final var zero = PACK(0);
		final var one = PACK(1);
		final var ai_one = new AtomicInteger(-1); // 1 乘法的 幺元
		final var ai_zero = new AtomicInteger(-1); // 0 加法的 幺元，乘法的 零元
		final var args = symbol.getArgS().filter(Objects::nonNull) // 过滤掉空值
				.map(Node::PACK).map(e -> e.fmap(BinaryOp::simplify)).map(kvp_int()).peek(e -> {
					if (e._2.equals(one))
						ai_one.set(e._1);
					else if (e._2.equals(zero))
						ai_zero.set(e._1);
				}).map(e -> e._2).toArray(Node[]::new);
		final BiFunction<Double[], BinaryOp<Object, Object>, BinaryOp<Object, Object>> cascade_handler = // 级联算符
				(argdbls, op) -> { // argdbls 浮点数类型的 args, op 级联算符名称
					final var name = op.getName(); // 算符名称
					for (int i = 0; i < 2; i++) { // 外层的 * 的 参数
						if (args[i].getName().equals(name)) { // 发现存在内层*结构。
							final var coef_i = argdbls[i == 0 ? 1 : 0]; // 外层系数
							if (coef_i != null) { // 外层系数有效
								final var op_i = args[i].getOp(); // 外层的运算对象
								final var op_i_args = op_i.getArgS().map(Node::PACK).toArray(Node[]::new); // 外层运算的参数
								for (int j = 0; j < 2; j++) { // 内层运算的各个参数
									final var arg_j = op_i_args[j].isToken() // token 检测
											? op_i_args[j].getToken() // 转换为 token
											: null; //
									final var coef_j = arg_j == null ? null : arg_j.dbl(); // 内层运算的系数
									if (coef_j != null) { // 确定了内层运算的参数
										final var left = op.compose(coef_i, coef_j).evaluate(); // 左为参数，右位的系数
										final var right = op_i_args[j == 0 ? 1 : 0].unpack(); // 右位参数
										return op.compose(left, right); // 级联运算成功
									} // if coef_j
								} // for j 内层
							} // coef_i 外层
						} // if 发现存在内层*结构。
					} // for i 外层

					return null; // 返回 空 表示 级联运算尝试 失败
				}; // cascadeOp , argdbls 浮点数类型的 args, op 级联算符名称
		final var theOp = symbol.duplicate(); // 复制操作符
		final var opName = theOp.getName(); // 操作符名称
		final var left = args != null && args.length > 0 ? args[0].unpack() : null; // 左位参数
		final var right = args != null && args.length > 1 ? args[1].unpack() : null; // 右位参数
		@SuppressWarnings("unchecked")
		final var handle = Optional.of(symbol.getAry()).map(nary -> {
			switch (nary) { // 算符类型的判断
			case 1: { // 一元算符
				if (opName.equals("neg")) { //
					return MUL(-1, left);
				} else {
					return theOp.compose1(left); // 一元算符的组合，一元算符 只有一个参数 即 左位参数
				} // if
			} // case 1
			case 2: { // 二元算符
				final var zero_i = ai_zero.getAndIncrement(); // 0值 的 位置索引
				final var one_i = ai_one.getAndIncrement(); // 1值 的 位置索引
				final var dbls = Stream.of(args) // 提取参数
						.map(e -> !e.isToken() ? null : e.getToken() == null ? null : e.getToken().dbl())
						.toArray(Double[]::new); // 浮点数类型的数据值
				final var flag = (dbls[0] != null) && (dbls[1] != null); // 是否是数值计算

				if (opName.equals("+")) { // 加法
					final BinaryOp<?, ?> arg1; // 第一参数
					final BinaryOp<?, ?> arg2; // 第二参数
					final boolean share_flag; // 共享项标志
					final var _termLeft = BinaryOp.termOpt(left);
					final var _left = _termLeft.isPresent() ? left : right; // 尝试吧_left作为term项
					final var _right = _termLeft.isPresent() ? right : left; // 尝试吧_left作为term项
					final var termLeft = _termLeft.isPresent() ? _termLeft : BinaryOp.termOpt(_left);

					if (termLeft.isPresent()) { // left 是作为term项目而存在
						final var a = bop(termLeft.get()._2._2);
						final var b = bop(_right);
						if (Objects.equals(a, b)) { // 合并 2x+x
							final var x = termLeft.get();
							return MUL(dbl(x._2._1) + 1, x._2._2);
						} else { // 合并 2x+3x
							final var termRight = BinaryOp.termOpt(_right);
							if (termRight.isPresent()) {
								final var _b = bop(termRight.get()._2._2);
								if (Objects.equals(a, _b)) {
									final var x = termLeft.get();
									final var y = termRight.get();
									return MUL(dbl(x._2._1) + dbl(y._2._1), x._2._2);
								} // if
							} // if
						} // if
					} else if ((share_flag = Objects.equals((arg1 = bop(left))._1, (arg2 = bop(right))._1)
							&& Objects.equals(arg1._1, "*")) && Optional.ofNullable(arg1._2) // 类型：ax + bx -> (a+b)*x
									.flatMap(a1 -> Optional.of(arg2._2).map(a2 -> Objects.equals(a1._2, a2._2)))
									.orElse(false)) {
						return MUL(ADD(arg1._2._1, arg2._2._1).simplify(), arg1._2._2);
					} else if (share_flag && Optional.ofNullable(arg1._2) // 类型：xa + xa -> (a+b)*x
							.flatMap(a1 -> Optional.of(arg2._2).map(a2 -> Objects.equals(a1._1, a2._1)))
							.orElse(false)) {
						return MUL(ADD(arg1._2._2, arg2._2._2).simplify(), arg1._2._1);
					} else if (share_flag && Optional.ofNullable(arg1._2) // 类型：ax + xb -> (a+b)*x
							.flatMap(a1 -> Optional.of(arg2._2).map(a2 -> Objects.equals(a1._2, a2._1)))
							.orElse(false)) {
						return MUL(ADD(arg1._2._1, arg2._2._2).simplify(), arg1._2._2);
					} else if (share_flag && Optional.ofNullable(arg1._2) // 类型：xa + bx -> (a+b)*x
							.flatMap(a1 -> Optional.of(arg2._2).map(a2 -> Objects.equals(a1._1, a2._2)))
							.orElse(false)) {
						return MUL(ADD(arg1._2._2, arg2._2._1).simplify(), arg1._2._1);
					} else if (Objects.equals(left, right)) { // 合并同类项
						return MUL(2, right);
					} else if (zero_i >= 0) { // // 存在0参数，0 是 加法的 幺元 即 0 加上 任何数 的结果 仍旧是 任何数，也就是 加上 幺元 保持不变
						return zero_i == 0 ? right : left;
					} else if (flag) {
						return PACK(dbls[0] + dbls[1]).unpack();
					} else { // 连加情形 把 (+,coef_i,(+,coef_j,c)) 转成 (+,coef_i+coef_j,c) 的结构，降低一个阶层 以 提升效率
						final var h = cascade_handler.apply(dbls, ADD(null, null)); // 计算连加
						if (h != null) {
							return h;
						} // if
					} // else 连加情形
				} else if (opName.equals("*")) { // 乘法
					if (Objects.equals(right, left)) { // 合并同类项
						return POW(right, 2);
					} else if (one_i >= 0) { // 存在1参数，1 是 乘法的 幺元 即 1 乘以 任何数 的结果 仍旧是 任何数，也就是乘以幺元 保持不变
						return one_i == 0 ? right : left;
					} else if (zero_i >= 0) { // 存在 0 参数 0 是乘法的 零元 即 任何数 乘以 零元 结构都是零元
						return PACK(0).unpack();
					} else if (flag) {
						return PACK(dbls[0] * dbls[1]).unpack();
					} else { // 连乘情形 把 (*,coef_i,(*,coef_j,c)) 转成 (*,coef_i*coef_j,c) 的结构，降低一个阶层 以 提升效率
						final var h = cascade_handler.apply(dbls, MUL(null, null)); // 计算连乘
						if (h != null)
							return h;
					} // else 连乘的情形
				} else if (opName.equals("-")) { // 减法
					if (Objects.equals(right, left)) // 合并同类项
						return TOKEN(0);
					else if (zero_i == 1)
						return left;
					else if (flag)
						return PACK(dbls[0] - dbls[1]).unpack();
				} else if (opName.equals("/")) { // 除法
					if (Objects.equals(right, left)) // 合并同类项
						return TOKEN(1);
					else if (one_i == 1)
						return left;
					else if (flag)
						return PACK(dbls[0] / dbls[1]).unpack();
				} else if (opName.equals("pow")) { // 高次幂
					if (Objects.equals(1d, IRecord.obj2dbl().apply(right))) {
						return left;
					}
				} // if

				return theOp.compose(left, right); // 重新组合数据
			} // case 2
			default: {
				return theOp;
			} // default
			} // switch
		}).map(o -> (BinaryOp<Object, Object>) o) // 转换成算符类型
				.orElse(null); // handle

		return handle;
	}

	/**
	 * 二元函数的求值
	 * 
	 * @param <T>      第一参数类型
	 * @param <U>      第二参数类型
	 * @param bindings 变量参数的数据绑定
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	@Override
	public <T, U> Object evaluate(final BinaryOp<T, U> symbol, final Map<String, Object> bindings) {

		final var dataStream = symbol.getArgS().map(e -> {
			if (e instanceof BinaryOp) { // op 算符类型
				return ((BinaryOp<?, ?>) e).evaluate(bindings);
			} else if (e instanceof Node) { // node 数据糖衣类型
				try {
					/**
					 * 正常情况 生成运算对象会 进行 unpack , 需要 运算时移除糖衣的情况很少， 但是还要给予保留，<br>
					 * 以应对不测 比如 要保证 某些不经意的,没有剔除干净掉数据糖衣的代码，恶意运行 <br>
					 */
					throw new Exception("语句中出现了数据糖衣，请检查运算的数据生成逻辑,在运算执行前给予糖衣unpack\n" + e);
				} catch (Exception ex) {
					ex.printStackTrace();
				} // try

				final var node = (Node) e;
				return node.getValue().evaluate(bindings);
			} else { // 值类型
				return e;
			} // if
		}).map(e -> { // 上下文数据绑定
			if (e instanceof String) { // 字符串类型的数据 尝试 使用 绑定上下文 进行内容解析
				final var key = (String) e; // 把 参数值 解析为 变量符号(bindings 中的键名)
				final var value = bindings.getOrDefault(key, e); // 尝试从 bindings数据集合中提取符号的的具体的值
				return value; // 返回解析后的结果
			} else { // 返回原来的值
				return e;
			} // if
		}); // dataStream

		if (COMMA_TEST(symbol.getName())) { // 逗号表达式
			final var dd = dataStream.toArray();
			final var ret = P(dd[0], dd[1]);
			return ret;
		} else { // 非 逗号表达式
			final var args = new LinkedList<Object>(); // 参数值
			final var dbls = dataStream.peek(args::add).map(IRecord.obj2dbl()).toArray(Double[]::new); // 数值参数
			final var nary = symbol.getAry(); // 函数的参数数量

			// 二元函数的参数调整
			if (nary == 2 && Stream.of(dbls).filter(Objects::nonNull).count() < 2) {
				final var _dbls = args.stream().filter(Objects::nonNull)
						.flatMap(e -> e instanceof Tuple2 ? BinaryOp.flatS((Tuple2<?, ?>) e) : Stream.of(e))
						.map(IRecord.obj2dbl()).toArray(Double[]::new);
				for (int i = 0; i < Math.min(dbls.length, _dbls.length); i++) { // 拷贝数据到 dbls
					dbls[i] = _dbls[i];
				} // for
			} // if

			final var x = dbls.length < 1 ? null : dbls[0]; // 1#参数数值
			final var y = dbls.length < 2 ? null : dbls[1]; // 2#参数数值
			final var left = dbls.length < 1 ? null : args.get(0); // 1#参数
			final var right = dbls.length < 2 ? null : args.get(1); // 2#参数
			final var opName = symbol.getName(); // 提取函数名称
			final var theOp = symbol.duplicate(); // 符号复制

			if (symbol instanceof ConstantOp) { // 常量函数
				return theOp; // 常量函数
			} else if (symbol instanceof UnaryOp && x != null) { // 一元函数
				switch (opName) { // 1 元运算
				case "sinh":
					return Math.sinh(x);
				case "sin":
					return Math.sin(x);
				case "csc":
					return 1d / Math.sin(x);
				case "cosh":
					return Math.cosh(x);
				case "cos":
					return Math.cos(x);
				case "sec":
					return 1d / Math.cos(x);
				case "tan":
					return Math.tan(x);
				case "cot":
					return 1d / Math.tan(x);
				case "arcsin":
					return Math.asin(x);
				case "arccos":
					return Math.acos(x);
				case "arctan":
					return Math.atan(x);
				case "arccot":
					return Math.atan2(1, x);
				case "exp":
					return Math.exp(x);
				case "neg":
					return -x;
				case "identity":
					return x;
				case "ln":
					return Math.log(x);
				case "sqrt":
					return Math.sqrt(x);
				case "square":
					return Math.pow(x, 2);
				case "!":
				case "fact":
					return x <= 0 ? 1d
							: Stream.iterate(1d, i -> i <= x, i -> i + 1d).reduce((a, b) -> a * b).orElse(0d);
				default:
					return theOp.compose1(x);
				} // switch
			} else if (symbol instanceof BinaryOp && x != null && y != null) { // 二元运算
				switch (opName) { // 算符名称
				case "+": // 加法
					return x + y;
				case "-": // 减法
					return x - y;
				case "*": // 乘法
					return x * y;
				case "/": // 除法
					return x / y;
				case "expa":
				case "^":
				case "pow": // expa指数函数，^/pow幂函数
					return Math.pow(x, y);
				case "log": // 对数函数
					return Math.log(y) / Math.log(x);
				default: // 默认函数
					return theOp.compose(x, y);
				} // switch
			} else { // 其余情况,参数非法的情况
				switch (nary) { // 函数的参数元数
				case 1: // 一元函数
					return theOp.compose1(left);
				case 2: // 二元函数
					return theOp.compose(left, right);
				default:// 默认值,0 元函数,Token
					return theOp;
				}// switch
			} // if 其余函数
		} // if 逗号表达式
	}

}
