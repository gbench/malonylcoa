package gbench.webapps.world.api.controller;

import static gbench.util.lisp.IRecord.REC;
import static java.time.LocalDateTime.now;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gbench.util.lisp.IRecord;

@RestController
@RequestMapping("api")
public class ApiController {
	
	/**
	 * http://localhost:8010/world/api/hello
	 * @return
	 */
	@RequestMapping("hello")
	public IRecord hello() {
		return REC("code","0","message","hello","time",now());
	}
}
