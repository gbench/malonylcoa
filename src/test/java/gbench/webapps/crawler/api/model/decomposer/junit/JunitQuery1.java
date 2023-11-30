package gbench.webapps.crawler.api.model.decomposer.junit;

import static gbench.util.data.DataApp.IRecord.FT;
import static gbench.util.io.Output.println;

import java.util.*;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.IRecord;
import gbench.webapps.crawler.api.controller.SrchController;

public class JunitQuery1 {

	@Test
	public void foo() {
		final var prefix = "F:/slicef/ws/gitws/malonylcoa/src/test/";
		final var corpusDir = FT("$0/java/gbench/webapps/crawler/api/model/data/corpus", prefix);
		final var indexHome = "D:/sliced/tmp/crawler/index";
		final var snapHome = "D:/sliced/tmp/crawler/snap";
		final var srchCtrl = new SrchController();
		@SuppressWarnings("unchecked")
		final Function<IRecord, String> format = r -> r.get("result") instanceof List<?> us
				? ((List<IRecord>) us).stream().map(e -> e.filter("symbol,py0,py1")).toList().toString()
				: "none";
		srchCtrl.initialize(indexHome, corpusDir, snapHome);
		println(srchCtrl.lookup2("权", "sess1", "agent1", 10).mutate(format));
	}

}
