package gbench.webapps.crawler.api.model.decomposer.junit;

import static gbench.util.data.DataApp.IRecord.FT;

import java.io.File;

import org.junit.jupiter.api.Test;

import gbench.webapps.crawler.api.model.decomposer.RulesDecomposer;
import gbench.webapps.crawler.api.model.srch.SrchEngineAdapter;

public class JunitRule1 {

	@Test
	public void foo() {
		final var rd = new RulesDecomposer();
		final var prefix = "F:/slicef/ws/gitws/malonylcoa/src/test/";
        final var fileHome = FT("$0/java/gbench/webapps/crawler/api/model/data/docs/rules", prefix);
        SrchEngineAdapter.traverse(new File(fileHome), file -> {
			rd.processFile(file).forEach(line -> {
				System.out.println("-------------");
				System.out.println(line);
			});
		});// traverse
	}

}
