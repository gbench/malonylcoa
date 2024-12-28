package gbench.webapps.crawler.api.model.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {

	void init();

	String store(final MultipartFile file);

	String store(final InputStream inputStream, final String filename);

	Stream<Path> loadAll();

	Path load(final String filename);

	Resource loadAsResource(final String filename);

	void deleteAll();

}