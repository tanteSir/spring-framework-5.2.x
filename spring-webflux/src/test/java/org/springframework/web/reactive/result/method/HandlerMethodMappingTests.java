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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link AbstractHandlerMethodMapping}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class HandlerMethodMappingTests {

	private AbstractHandlerMethodMapping<String> mapping;

	private MyHandler handler;

	private Method method1;

	private Method method2;


	@BeforeEach
	public void setup() throws Exception {
		this.mapping = new MyHandlerMethodMapping();
		this.handler = new MyHandler();
		this.method1 = handler.getClass().getMethod("handlerMethod1");
		this.method2 = handler.getClass().getMethod("handlerMethod2");
	}


	@Test
	public void registerDuplicates() {
		this.mapping.registerMapping("foo", this.handler, this.method1);
		assertThatIllegalStateException().isThrownBy(() ->
				this.mapping.registerMapping("foo", this.handler, this.method2));
	}

	@Test
	public void directMatch() throws Exception {
		String key = "foo";
		this.mapping.registerMapping(key, this.handler, this.method1);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(key));
		Mono<Object> result = this.mapping.getHandler(exchange);

		assertThat(((HandlerMethod) result.block()).getMethod()).isEqualTo(this.method1);
	}

	@Test
	public void patternMatch() throws Exception {
		this.mapping.registerMapping("/fo*", this.handler, this.method1);
		this.mapping.registerMapping("/f*", this.handler, this.method2);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo"));
		Mono<Object> result = this.mapping.getHandler(exchange);
		assertThat(((HandlerMethod) result.block()).getMethod()).isEqualTo(this.method1);
	}

	@Test
	public void ambiguousMatch() throws Exception {
		this.mapping.registerMapping("/f?o", this.handler, this.method1);
		this.mapping.registerMapping("/fo?", this.handler, this.method2);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo"));
		Mono<Object> result = this.mapping.getHandler(exchange);

		StepVerifier.create(result).expectError(IllegalStateException.class).verify();
	}

	@Test
	public void registerMapping() throws Exception {
		String key1 = "/foo";
		String key2 = "/foo*";
		this.mapping.registerMapping(key1, this.handler, this.method1);
		this.mapping.registerMapping(key2, this.handler, this.method2);

		assertThat(this.mapping.getMappingRegistry().getMappings())
				.containsKeys(key1, key2);
	}

	@Test
	public void registerMappingWithSameMethodAndTwoHandlerInstances() throws Exception {
		String key1 = "foo";
		String key2 = "bar";
		MyHandler handler1 = new MyHandler();
		MyHandler handler2 = new MyHandler();
		this.mapping.registerMapping(key1, handler1, this.method1);
		this.mapping.registerMapping(key2, handler2, this.method1);

		assertThat(this.mapping.getMappingRegistry().getMappings())
				.containsKeys(key1, key2);
	}

	@Test
	public void unregisterMapping() throws Exception {
		String key = "foo";
		this.mapping.registerMapping(key, this.handler, this.method1);
		Mono<Object> result = this.mapping.getHandler(MockServerWebExchange.from(MockServerHttpRequest.get(key)));

		assertThat(result.block()).isNotNull();

		this.mapping.unregisterMapping(key);
		result = this.mapping.getHandler(MockServerWebExchange.from(MockServerHttpRequest.get(key)));

		assertThat(result.block()).isNull();
		assertThat(this.mapping.getMappingRegistry().getMappings().keySet()).doesNotContain(key);
	}


	private static class MyHandlerMethodMapping extends AbstractHandlerMethodMapping<String> {

		private PathPatternParser parser = new PathPatternParser();

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return true;
		}

		@Override
		protected String getMappingForMethod(Method method, Class<?> handlerType) {
			String methodName = method.getName();
			return methodName.startsWith("handler") ? methodName : null;
		}

		@Override
		protected String getMatchingMapping(String pattern, ServerWebExchange exchange) {
			PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();
			PathPattern parsedPattern = this.parser.parse(pattern);
			return (parsedPattern.matches(lookupPath) ? pattern : null);
		}

		@Override
		protected Comparator<String> getMappingComparator(ServerWebExchange exchange) {
			return (o1, o2) -> PathPattern.SPECIFICITY_COMPARATOR.compare(parser.parse(o1), parser.parse(o2));
		}

	}

	@Controller
	private static class MyHandler {

		@RequestMapping
		@SuppressWarnings("unused")
		public void handlerMethod1() {
		}

		@RequestMapping
		@SuppressWarnings("unused")
		public void handlerMethod2() {
		}
	}

}
