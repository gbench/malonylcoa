package gbench.webapps.crawler.api.model.decomposer.junit;

import static gbench.util.data.DataApp.IRecord.FT;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.io.Output.println;
import static gbench.webapps.crawler.api.model.srch.SrchUtils.*;
import static java.text.MessageFormat.format;

import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.IRecord;
import gbench.webapps.crawler.api.controller.SrchController;
import gbench.webapps.crawler.api.model.SrchModel;

/**
 * 
 */
public class JunitQuery1 {

	@Test
	public void foo() {
		final var prefix = "F:/slicef/ws/gitws/malonylcoa/src/test/";
		final var corpusDir = FT("$0/java/gbench/webapps/crawler/api/model/data/corpus", prefix);
		final var indexHome = "D:/sliced/tmp/crawler/index";
		final var snapHome = "D:/sliced/tmp/crawler/snap";
		final var srchCtrl = new SrchController();
		@SuppressWarnings("unchecked")
		final Function<IRecord, String> format = r -> r.get("result") instanceof Collection<?> us
				? ((Collection<IRecord>) us).stream().map(e -> e.filter("symbol,py0,py1")).toList().toString()
				: "none";
		srchCtrl.initialize(indexHome, corpusDir, snapHome);
		// println(srchCtrl.lookup2("权", "sess1", "agent1", 10).mutate(format));

		final var srchModel = new SrchModel(indexHome, corpusDir, snapHome).initialize();
		final var pq = REC( // dsl
				"must", REC( // must 项目
						"should", REC("py0*", "*quan*", "py1*", "*q*"), // should 项目
						"file*", "*sunzi*") //
		).mutate(e -> srchModel.getPageQuery(parseDsl(e))).initialize(); // dsl

		System.out.println(format("query:{0},{1}", pq.getClass().getSimpleName(), pq));
		final var optional = pq.getData2();
		println(optional.map(e -> REC("result", e.getHits2()).mutate(format)).orElse(null));
		println(srchCtrl.lookup2("权;sunzi", "sess1", "agent1", 10).mutate(format));
	}

	/**
	 * parseDsl
	 */
	@Test
	public void bar() {
		final var line = REC( //
				"must", REC("symbol*", "*quan*", "file*", "*sunzi*"), //
				"filter", REC("py0*", "*quan*", "py1*", "*q*") //
		); // line
		final var q = parseDsl(line);
		println(q);
	}

	/**
	 * parseDsl
	 */
	@Test
	public void quz() {
		final var line = REC("+symbol*", "*quan*", "+file*", "*sunzi*"); // line
		final var q = parseDsl(line);
		println("----------", q);
	}

}
