package gbench.sandbox.ctp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class NcCtp {

	/**
	 * @param host
	 * @param port
	 */
	public void connect(final String host, final int port) {
		try (final Socket socket = new Socket(host, port);
				final BufferedReader sysinReader = new BufferedReader(new InputStreamReader(System.in));
				final OutputStream socketOs = socket.getOutputStream();
				final InputStream socketIs = socket.getInputStream();
				final BufferedReader socketBr = new BufferedReader(new InputStreamReader(socketIs))) {

			final AtomicBoolean flag = new AtomicBoolean(false);

			final Thread readerThread = new Thread(() -> {
				try {
					while (!flag.get()) {
						String line = socketBr.readLine();
						if (line == null) {
							// 连接已关闭，退出循环
							break;
						}
						line = line.replace("> $", "");
						println(line);
						Thread.sleep(500);
					}
				} catch (Exception e) {
					if (!flag.get()) { // 非预期异常打印堆栈
						e.printStackTrace();
					}
				}
				println("READER EXIT!");
			});

			final Thread writerThread = new Thread(() -> {
				try {
					while (!flag.get()) {
						String line = sysinReader.readLine();
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
						socketOs.write((line + "\n").getBytes());
						socketOs.flush();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				println("WRITER EXIT!");
			});

			readerThread.start();
			writerThread.start();

			writerThread.join();
			readerThread.join();

		} catch (Exception e) {
			System.err.println("I/O错误: " + e.getMessage());
		}
	}

	@Test
	public void foo() {
		println("NcCtp>\n ");
		this.connect("192.168.1.10", 9898);
	}

	private void println(String message) {
		System.out.println(message);
	}
}
