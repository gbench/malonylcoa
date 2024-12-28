package gbench.webapps.crawler.api.model.decomposer.junit;

import static gbench.util.data.DataApp.IRecord.FT;
import static gbench.util.io.Output.println;

import java.util.*;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.IRecord;
import gbench.webapps.crawler.api.controller.SrchController;
import gbench.webapps.crawler.api.model.SrchModel;

//import java.io.File;
//import java.util.stream.Collectors;
//import static java.text.MessageFormat.format;
//import gbench.util.io.FileSystem;

/**
 * 
 */
public class JunitSrch {

	@Test
	public void foo() {
		final var prefix = "F:/slicef/ws/gitws/malonylcoa/src/test/";
		final var fileHome = FT("$0/java/gbench/webapps/crawler/api/model/data/docs", prefix);
		final var corpusDir = FT("$0/java/gbench/webapps/crawler/api/model/data/corpus", prefix);
		final var indexHome = "D:/sliced/tmp/crawler/index";
		final var snapHome = "D:/sliced/tmp/crawler/snap";
		final var srchModel = new SrchModel(indexHome, corpusDir, snapHome);
		srchModel.indexFiles(fileHome, e -> {
			println(e);
		});
		final var srchCtrl = new SrchController();
		srchCtrl.initialize(indexHome, corpusDir, snapHome);

		final var keys = "docid,symbol,py0,py1";
		@SuppressWarnings("unchecked")
		final Function<IRecord, String> format = r -> r.get("result") instanceof List<?> us
				? ((List<IRecord>) us).stream().map(e -> e.filter("symbol,py0,py1")).toList().toString()
				: "none";
		println("srchCtrl.lookup(\"权\")");
		println(srchCtrl.lookup("权").mutate(format));

		println("srchCtrl.lookup(\"quan\")");
		println(srchCtrl.lookup("quan").filter(keys).mutate(format));

		println("srchCtrl.lookup2(\"权\",\"sess1\",\"agent1\",10)");
		println(srchCtrl.lookup2("权", "sess1", "agent1", 10).mutate(format));

		println("srchCtrl.lookup2(\"quan\",\"sess2\",\"agent1\",10)");
		println(srchCtrl.lookup2("quan;sunzi", "sess2", "agent1", 10).mutate(format));
	}

	/**
	 * 修改把gb2312转换成utf8
	 */
//	@Test
//	public void bar() {
//		final var prefix = "F:/slicef/ws/gitws/malonylcoa/src/test/";
//		final var fileHome = FT("$0/java/gbench/webapps/crawler/api/model/data/docs/maoxuan", prefix);
//		final var output = fileHome;
//		final var home = new File(fileHome);
//		FileSystem.tranverse(home, file -> {
//			if (file.isFile()) {
//				println("write file", file);
//				final var name = file.getName();
//				final var lines = FileSystem.readLineS(file, "gb2312").collect(Collectors.joining("\n"));
//				FileSystem.write(new File(format("{0}/{1}", output, name)), "utf8", () -> lines);
//			} // if
//		});
//	}

}
