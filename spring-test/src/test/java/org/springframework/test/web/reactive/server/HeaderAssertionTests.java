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

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.MonoProcessor;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link HeaderAssertions}.
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class HeaderAssertionTests {

	@Test
	public void valueEquals() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.valueEquals("foo", "bar");

		// Missing header
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("what?!", "bar"));

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("foo", "what?!"));

		// Wrong # of values
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("foo", "bar", "what?!"));
	}

	@Test
	public void valueEqualsWithMultipleValues() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		headers.add("foo", "baz");
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.valueEquals("foo", "bar", "baz");

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("foo", "bar", "what?!"));

		// Too few values
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueEquals("foo", "bar"));

	}

	@Test
	public void valueMatches() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.valueMatches("Content-Type", ".*UTF-8.*");

		// Wrong pattern
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.valueMatches("Content-Type", ".*ISO-8859-1.*"))
			.satisfies(ex -> assertThat(ex.getCause()).hasMessage("Response header " +
					"'Content-Type'=[application/json;charset=UTF-8] does not match " +
					"[.*ISO-8859-1.*]"));
	}

	@Test
	public void valueMatcher() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		HeaderAssertions assertions = headerAssertions(headers);

		assertions.value("foo", containsString("a"));
	}

	@Test
	public void exists() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.exists("Content-Type");

		// Header should not exist
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.exists("Framework"))
			.satisfies(ex -> assertThat(ex.getCause()).hasMessage("Response header 'Framework' does not exist"));
	}

	@Test
	public void doesNotExist() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.doesNotExist("Framework");

		// Existing header
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.doesNotExist("Content-Type"))
			.satisfies(ex -> assertThat(ex.getCause()).hasMessage("Response header " +
					"'Content-Type' exists with value=[application/json;charset=UTF-8]"));
	}

	@Test
	public void contentTypeCompatibleWith() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.contentTypeCompatibleWith(MediaType.parseMediaType("application/*"));

		// MediaTypes not compatible
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.contentTypeCompatibleWith(MediaType.TEXT_XML))
			.satisfies(ex -> assertThat(ex.getCause()).hasMessage("Response header " +
					"'Content-Type'=[application/xml] is not compatible with [text/xml]"));
	}

	@Test
	public void cacheControl() {
		CacheControl control = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform();

		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(control.getHeaderValue());
		HeaderAssertions assertions = headerAssertions(headers);

		// Success
		assertions.cacheControl(control);

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.cacheControl(CacheControl.noStore()));
	}

	@Test
	public void expires() {
		HttpHeaders headers = new HttpHeaders();
		ZonedDateTime expires = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		headers.setExpires(expires);
		HeaderAssertions assertions = headerAssertions(headers);
		assertions.expires(expires.toInstant().toEpochMilli());

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.expires(expires.toInstant().toEpochMilli() + 1));
	}

	@Test
	public void lastModified() {
		HttpHeaders headers = new HttpHeaders();
		ZonedDateTime lastModified = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		headers.setLastModified(lastModified.toInstant().toEpochMilli());
		HeaderAssertions assertions = headerAssertions(headers);
		assertions.lastModified(lastModified.toInstant().toEpochMilli());

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.lastModified(lastModified.toInstant().toEpochMilli() + 1));
	}

	private HeaderAssertions headerAssertions(HttpHeaders responseHeaders) {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/"));
		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		response.getHeaders().putAll(responseHeaders);

		MonoProcessor<byte[]> emptyContent = MonoProcessor.create();
		emptyContent.onComplete();

		ExchangeResult result = new ExchangeResult(request, response, emptyContent, emptyContent, Duration.ZERO, null);
		return new HeaderAssertions(result, mock(WebTestClient.ResponseSpec.class));
	}

}
