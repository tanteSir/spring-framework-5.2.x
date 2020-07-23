/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.config;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with a {@link CorsRegistry}.
 *
 * @author Sebastien Deleuze
 */
public class CorsRegistryTests {

	private final CorsRegistry registry = new CorsRegistry();


	@Test
	public void noMapping() {
		assertThat(this.registry.getCorsConfigurations().isEmpty()).isTrue();
	}

	@Test
	public void multipleMappings() {
		this.registry.addMapping("/foo");
		this.registry.addMapping("/bar");
		assertThat(this.registry.getCorsConfigurations().size()).isEqualTo(2);
	}

	@Test
	public void customizedMapping() {
		this.registry.addMapping("/foo").allowedOrigins("https://domain2.com", "https://domain2.com")
				.allowedMethods("DELETE").allowCredentials(false).allowedHeaders("header1", "header2")
				.exposedHeaders("header3", "header4").maxAge(3600);
		Map<String, CorsConfiguration> configs = this.registry.getCorsConfigurations();
		assertThat(configs.size()).isEqualTo(1);
		CorsConfiguration config = configs.get("/foo");
		assertThat(config.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain2.com", "https://domain2.com"));
		assertThat(config.getAllowedMethods()).isEqualTo(Arrays.asList("DELETE"));
		assertThat(config.getAllowedHeaders()).isEqualTo(Arrays.asList("header1", "header2"));
		assertThat(config.getExposedHeaders()).isEqualTo(Arrays.asList("header3", "header4"));
		assertThat(config.getAllowCredentials()).isEqualTo(false);
		assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(3600));
	}

}
