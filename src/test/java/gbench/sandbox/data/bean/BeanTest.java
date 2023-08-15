package gbench.sandbox.data.bean;

import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.JSON;
import gbench.util.io.Output;

@SuppressWarnings("all")
public class BeanTest {

	@Test
	public void foo() {
		println("------------foo---------------");
		class User {
			public User(String name, int height) {
				super();
				this.name = name;
				this.height = height;
			}

			private String name;
			private int height;
		}

		final var user = new User("zhangsan", 175);
		println(JSON.obj2lhm(user));
		println(JSON.toJson(user));
		println(gbench.util.lisp.IRecord.REC(user));
		println(gbench.util.data.DataApp.IRecord.REC(user));

	}

	@Test
	public void bar() {
		println("------------bar---------------");
		class User {
			public User(String name, int height) {
				super();
				this.name = name;
				this.height = height;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public int getHeight() {
				return height;
			}

			public void setHeight(int height) {
				this.height = height;
			}

			private String name;
			private int height;
		}

		final var user = new User("zhangsan", 175);
		println(JSON.obj2lhm(user));
		println(JSON.toJson(user));
		println(gbench.util.lisp.IRecord.REC(user));
		println(gbench.util.data.DataApp.IRecord.REC(user));

	}

	@Test
	public void quz() {
		println("------------quz---------------");
		final var user = new User("zhangsan", 175);
		println(JSON.obj2lhm(user));
		println(JSON.toJson(user));
		println(gbench.util.lisp.IRecord.REC(user));
		println(gbench.util.data.DataApp.IRecord.REC(user));
	}
}

class User {
	public User(String name, int height) {
		super();
		this.name = name;
		this.height = height;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	private String name;
	private int height;
}
