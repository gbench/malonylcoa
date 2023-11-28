package gbench.webapps.crawler.api.model.decomposer.junit;

import static gbench.util.data.DataApp.IRecord.FT;
import static gbench.util.io.Output.println;

import org.junit.jupiter.api.Test;

import gbench.webapps.crawler.api.controller.SrchController;
import gbench.webapps.crawler.api.model.SrchModel;

public class JunitRule2 {

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
		println("----------------------");
		println(srchCtrl.lookup("存货"));
	}

}
