package gbench.sandbox.ctp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * cat - | nc localhost 9898
 */
public class NcCtp {

	private static String join(final String... ss) {
		return Stream.of(ss).collect(Collectors.joining("\t"));
	}

	private static void println(final String... message) {
		System.out.println(join(message));
	}

	private static void print(final String... message) {
		System.out.print(join(message));
	}

	/**
	 * CTP 连接
	 * 
	 * @param host 主机地址（IP)
	 * @param port 连接端口
	 */
	public void connect(final String host, final int port) {
		try (final Socket socket = new Socket(host, port);
				final BufferedReader sysin_reader = new BufferedReader(new InputStreamReader(System.in));
				final OutputStream socket_os = socket.getOutputStream();
				final InputStream socket_is = socket.getInputStream();
				final BufferedReader socket_br = new BufferedReader(new InputStreamReader(socket_is))) {

			final AtomicBoolean flag = new AtomicBoolean(false);

			final Thread readerThread = new Thread(() -> {
				try {
					while (!flag.get()) {
						final var c = socket_br.read(); // 读取字符
						if (c < 0) {
							// 连接已关闭，退出循环
							break;
						} else {
							print(String.valueOf((char) c));
						}
					}
				} catch (Exception e) {
					if (!flag.get()) { // 非预期异常打印堆栈
						e.printStackTrace();
					}
				}
				println("READER EXIT!");
			});
			readerThread.setName("readerThread");

			final Thread writerThread = new Thread(() -> {
				try {
					while (!flag.get()) {
						String line = sysin_reader.readLine();
						if (line.equals("quit")) {
							flag.set(true);
							// 关闭套接字以中断readerThread的阻塞
							try {
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							break;
						}
						socket_os.write((line + "\n").getBytes());
						socket_os.flush();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				println("WRITER EXIT!");
			});
			readerThread.setName("writerThread");

			// 读写线程
			readerThread.start();
			writerThread.start();

			// 等待线程运行完毕
			writerThread.join();
			readerThread.join();

		} catch (Exception e) {
			System.err.println("I/O错误: " + e.getMessage());
		}
	}

	@Test
	public void foo() {
		this.connect("192.168.1.10", 9898);
	}
}
