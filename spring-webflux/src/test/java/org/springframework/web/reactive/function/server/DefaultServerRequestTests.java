/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * @author Arjen Poutsma
 */
public class DefaultServerRequestTests {

	private final List<HttpMessageReader<?>> messageReaders = Arrays.asList(
			new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()),
			new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));


	@Test
	public void method() {
		HttpMethod method = HttpMethod.HEAD;
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(method, "https://example.com")),
				this.messageReaders);

		assertThat(request.method()).isEqualTo(method);
	}

	@Test
	public void uri() {
		URI uri = URI.create("https://example.com");

		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, uri)),
				this.messageReaders);

		assertThat(request.uri()).isEqualTo(uri);
	}

	@Test
	public void uriBuilder() throws URISyntaxException {
		URI uri = new URI("http", "localhost", "/path", "a=1", null);
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, uri)),
				this.messageReaders);


		URI result = request.uriBuilder().build();
		assertThat(result.getScheme()).isEqualTo("http");
		assertThat(result.getHost()).isEqualTo("localhost");
		assertThat(result.getPort()).isEqualTo(-1);
		assertThat(result.getPath()).isEqualTo("/path");
		assertThat(result.getQuery()).isEqualTo("a=1");
	}

	@Test
	public void attribute() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.method(HttpMethod.GET, "https://example.com"));
		exchange.getAttributes().put("foo", "bar");

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		assertThat(request.attribute("foo")).isEqualTo(Optional.of("bar"));
	}

	@Test
	public void queryParams() {
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "https://example.com?foo=bar")),
				this.messageReaders);

		assertThat(request.queryParam("foo")).isEqualTo(Optional.of("bar"));
	}

	@Test
	public void emptyQueryParam() {
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "https://example.com?foo")),
				this.messageReaders);

		assertThat(request.queryParam("foo")).isEqualTo(Optional.of(""));
	}

	@Test
	public void absentQueryParam() {
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "https://example.com?foo")),
				this.messageReaders);

		assertThat(request.queryParam("bar")).isEqualTo(Optional.empty());
	}

	@Test
	public void pathVariable() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://example.com"));
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		exchange.getAttributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		assertThat(request.pathVariable("foo")).isEqualTo("bar");
	}


	@Test
	public void pathVariableNotFound() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://example.com"));
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		exchange.getAttributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		assertThatIllegalArgumentException().isThrownBy(() ->
				request.pathVariable("baz"));
	}

	@Test
	public void pathVariables() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://example.com"));
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		exchange.getAttributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		assertThat(request.pathVariables()).isEqualTo(pathVariables);
	}

	@Test
	public void header() {
		HttpHeaders httpHeaders = new HttpHeaders();
		List<MediaType> accept =
				Collections.singletonList(MediaType.APPLICATION_JSON);
		httpHeaders.setAccept(accept);
		List<Charset> acceptCharset = Collections.singletonList(StandardCharsets.UTF_8);
		httpHeaders.setAcceptCharset(acceptCharset);
		long contentLength = 42L;
		httpHeaders.setContentLength(contentLength);
		MediaType contentType = MediaType.TEXT_PLAIN;
		httpHeaders.setContentType(contentType);
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
		httpHeaders.setHost(host);
		List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(0, 42));
		httpHeaders.setRange(range);

		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest
						.method(HttpMethod.GET, "https://example.com?foo=bar")
						.headers(httpHeaders)),
				this.messageReaders);

		ServerRequest.Headers headers = request.headers();
		assertThat(headers.accept()).isEqualTo(accept);
		assertThat(headers.acceptCharset()).isEqualTo(acceptCharset);
		assertThat(headers.contentLength()).isEqualTo(OptionalLong.of(contentLength));
		assertThat(headers.contentType()).isEqualTo(Optional.of(contentType));
		assertThat(headers.header(HttpHeaders.CONTENT_TYPE)).containsExactly(MediaType.TEXT_PLAIN_VALUE);
		assertThat(headers.firstHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(headers.asHttpHeaders()).isEqualTo(httpHeaders);
	}

	@Test
	public void cookies() {
		HttpCookie cookie = new HttpCookie("foo", "bar");
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.method(HttpMethod.GET, "https://example.com").cookie(cookie));

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		MultiValueMap<String, HttpCookie> expected = new LinkedMultiValueMap<>();
		expected.add("foo", cookie);

		assertThat(request.cookies()).isEqualTo(expected);

	}

	@Test
	public void body() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);

		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "https://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		Mono<String> resultMono = request.body(toMono(String.class));
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	public void bodyToMono() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "https://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		Mono<String> resultMono = request.bodyToMono(String.class);
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	public void bodyToMonoParameterizedTypeReference() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "https://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<String>() {};
		Mono<String> resultMono = request.bodyToMono(typeReference);
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	public void bodyToMonoDecodingException() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("{\"invalid\":\"json\" ".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.POST, "https://example.com/invalid")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		Mono<Map<String, String>> resultMono = request.bodyToMono(
				new ParameterizedTypeReference<Map<String, String>>() {});
		StepVerifier.create(resultMono)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void bodyToFlux() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "https://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		Flux<String> resultFlux = request.bodyToFlux(String.class);
		assertThat(resultFlux.collectList().block()).isEqualTo(Collections.singletonList("foo"));
	}

	@Test
	public void bodyToFluxParameterizedTypeReference() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "https://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<String>() {};
		Flux<String> resultFlux = request.bodyToFlux(typeReference);
		assertThat(resultFlux.collectList().block()).isEqualTo(Collections.singletonList("foo"));
	}

	@Test
	public void bodyUnacceptable() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "https://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Flux<String> resultFlux = request.bodyToFlux(String.class);
		StepVerifier.create(resultFlux)
				.expectError(UnsupportedMediaTypeStatusException.class)
				.verify();
	}

	@Test
	public void formData() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo=bar&baz=qux".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "https://example.com")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<MultiValueMap<String, String>> resultData = request.formData();
		StepVerifier.create(resultData)
				.consumeNextWith(formData -> {
					assertThat(formData.size()).isEqualTo(2);
					assertThat(formData.getFirst("foo")).isEqualTo("bar");
					assertThat(formData.getFirst("baz")).isEqualTo("qux");
				})
				.verifyComplete();
	}

	@Test
	public void multipartData() {
		String data = "--12345\r\n" +
				"Content-Disposition: form-data; name=\"foo\"\r\n" +
				"\r\n" +
				"bar\r\n" +
				"--12345\r\n" +
				"Content-Disposition: form-data; name=\"baz\"\r\n" +
				"\r\n" +
				"qux\r\n" +
				"--12345--\r\n";
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=12345");
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "https://example.com")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<MultiValueMap<String, Part>> resultData = request.multipartData();
		StepVerifier.create(resultData)
				.consumeNextWith(formData -> {
					assertThat(formData.size()).isEqualTo(2);

					Part part = formData.getFirst("foo");
					boolean condition1 = part instanceof FormFieldPart;
					assertThat(condition1).isTrue();
					FormFieldPart formFieldPart = (FormFieldPart) part;
					assertThat(formFieldPart.value()).isEqualTo("bar");

					part = formData.getFirst("baz");
					boolean condition = part instanceof FormFieldPart;
					assertThat(condition).isTrue();
					formFieldPart = (FormFieldPart) part;
					assertThat(formFieldPart.value()).isEqualTo("qux");
				})
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedTimestamp(String method) throws Exception {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		HttpHeaders headers = new HttpHeaders();
		headers.setIfModifiedSince(now);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(now);

		StepVerifier.create(result)
				.assertNext(serverResponse -> {
					assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
					assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
				})
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkModifiedTimestamp(String method) {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		HttpHeaders headers = new HttpHeaders();
		headers.setIfModifiedSince(oneMinuteAgo);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(now);

		StepVerifier.create(result)
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETag(String method) {
		String eTag = "\"Foo\"";
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch(eTag);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(eTag);

		StepVerifier.create(result)
				.assertNext(serverResponse -> {
					assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
					assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
				})
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagWithSeparatorChars(String method) {
		String eTag = "\"Foo, Bar\"";
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch(eTag);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(eTag);

		StepVerifier.create(result)
				.assertNext(serverResponse -> {
					assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
					assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
				})
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkModifiedETag(String method) {
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch(oldEtag);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(currentETag);

		StepVerifier.create(result)
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedUnpaddedETag(String method) {
		String eTag = "Foo";
		String paddedEtag = String.format("\"%s\"", eTag);
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch(paddedEtag);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(eTag);

		StepVerifier.create(result)
				.assertNext(serverResponse -> {
					assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
					assertThat(serverResponse.headers().getETag()).isEqualTo(paddedEtag);
				})
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkModifiedUnpaddedETag(String method) {
		String currentETag = "Foo";
		String oldEtag = "Bar";
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch(oldEtag);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(currentETag);

		StepVerifier.create(result)
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedWildcardIsIgnored(String method) {
		String eTag = "\"Foo\"";
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch("*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(eTag);

		StepVerifier.create(result)
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagAndTimestamp(String method) {
		String eTag = "\"Foo\"";
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch(eTag);
		headers.setIfModifiedSince(now);

		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(now, eTag);

		StepVerifier.create(result)
				.assertNext(serverResponse -> {
					assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
					assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
					assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
				})
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagAndModifiedTimestamp(String method) {
		String eTag = "\"Foo\"";
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch(eTag);
		headers.setIfModifiedSince(oneMinuteAgo);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(now, eTag);

		StepVerifier.create(result)
				.assertNext(serverResponse -> {
					assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
					assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
					assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
				})
				.verifyComplete();
	}

	@ParameterizedHttpMethodTest
	void checkModifiedETagAndNotModifiedTimestamp(String method) throws Exception {
		String currentETag = "\"Foo\"";
		String oldEtag = "\"Bar\"";
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch(oldEtag);
		headers.setIfModifiedSince(now);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), "/")
				.headers(headers)
				.build();

		DefaultServerRequest request =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> result = request.checkNotModified(now, currentETag);

		StepVerifier.create(result)
				.verifyComplete();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(strings = {"GET", "HEAD"})
	@interface ParameterizedHttpMethodTest {

	}

}
