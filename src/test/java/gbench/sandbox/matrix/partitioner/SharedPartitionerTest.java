package gbench.sandbox.matrix.partitioner;

import static gbench.util.array.Partitioner.P2;

import java.util.Arrays;

import gbench.util.SharedPartitioner;
import org.junit.jupiter.api.Test;

/**
 * 
 */
public class SharedPartitionerTest {

	/**
	 * 创建记账分录结构
	 */
	public static SharedPartitioner acct_entries(String name) {
		int RECORD_COUNT = 128;
		int INT64_SIZE = 8;
		int FLOAT64_SIZE = 8;
		int STRING32_SIZE = 32 * 2; // UTF-16

		final var schema = P2("root", P2(// 根节点的 的值列表
				"int64", P2(// int64 的值列表
						"id", INT64_SIZE * RECORD_COUNT, //
						"drcr", INT64_SIZE * RECORD_COUNT //
				), "float64", P2(// float64 的值列表
						"price", FLOAT64_SIZE * RECORD_COUNT, //
						"quantity", FLOAT64_SIZE * RECORD_COUNT //
				), "string32", P2( // string32 的值列表
						"name", STRING32_SIZE * RECORD_COUNT, //
						"description", STRING32_SIZE * RECORD_COUNT //
				) //
		));

		return SharedPartitioner.of(schema, name);
	}

	@Test
	public void foo(String[] args) {
		final var sp = acct_entries("/acct_entries");
		sp.write();

		// 读取验证
		System.out.println("字段元数据:");
		sp.metadata().forEach((k, v) -> System.out.println(k + ": " + v));

		// 读取id字段
		long[] ids = sp.fld("root.int64.id").lng(sp.buffer);
		System.out.println("\n前10个ID: " + Arrays.toString(Arrays.copyOf(ids, 10)));

		// 保持共享内存运行
		System.out.println("\n共享内存已创建: /dev/shm/acct_entries");
		System.out.println("按任意键退出...");
		try {
			System.in.read();
		} catch (Exception e) {
		}

		sp.close();
	}

}
