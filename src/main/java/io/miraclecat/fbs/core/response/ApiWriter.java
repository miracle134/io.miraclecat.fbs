package io.miraclecat.fbs.core.response;

import io.miraclecat.fbs.core.utils.data.ObjectMapperEnum;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * packageName    : io.miraclecat.fbs.core.response
 * fileName       : ApiWriter
 * author         : MiracleCat 
 * date           : 2025-04-18
 * description    :
 */
@Slf4j
public class ApiWriter {
	
	public static void writeJson(HttpServletResponse response, ApiDto<?> body) throws IOException {
		String json = ObjectMapperEnum.JSON_DEFAULT.toJsonSafely(body);

		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(body.getStatus());
		response.getWriter().write(json);
	}
	
}