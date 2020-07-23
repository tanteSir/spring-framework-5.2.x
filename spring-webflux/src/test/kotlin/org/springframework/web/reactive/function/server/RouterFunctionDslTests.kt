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

package org.springframework.web.reactive.function.server

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.*
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Tests for [RouterFunctionDsl].
 *
 * @author Sebastien Deleuze
 */
class RouterFunctionDslTests {

	@Test
	fun header() {
		val mockRequest = get("https://example.com")
				.header("bar","bar").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun accept() {
		val mockRequest = get("https://example.com/content")
				.header(ACCEPT, APPLICATION_ATOM_XML_VALUE).build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun acceptAndPOST() {
		val mockRequest = post("https://example.com/api/foo/")
				.header(ACCEPT, APPLICATION_JSON_VALUE)
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun acceptAndPOSTWithRequestPredicate() {
		val mockRequest = post("https://example.com/api/bar/")
				.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun contentType() {
		val mockRequest = get("https://example.com/content/")
				.header(CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE)
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun resourceByPath() {
		val mockRequest = get("https://example.com/org/springframework/web/reactive/function/response.txt")
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun method() {
		val mockRequest = patch("https://example.com/")
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun path() {
		val mockRequest = get("https://example.com/baz").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun resource() {
		val mockRequest = get("https://example.com/response.txt").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.expectNextCount(1)
				.verifyComplete()
	}

	@Test
	fun noRoute() {
		val mockRequest = get("https://example.com/bar")
				.header(ACCEPT, APPLICATION_PDF_VALUE)
				.header(CONTENT_TYPE, APPLICATION_PDF_VALUE)
				.build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request))
				.verifyComplete()
	}

	@Test
	fun rendering() {
		val mockRequest = get("https://example.com/rendering").build()
		val request = DefaultServerRequest(MockServerWebExchange.from(mockRequest), emptyList())
		StepVerifier.create(sampleRouter().route(request).flatMap { it.handle(request) })
				.expectNextMatches { it is RenderingResponse}
				.verifyComplete()
	}

	@Test
	fun emptyRouter() {
		assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
			router { }
		}
	}


	private fun sampleRouter() = router {
		(GET("/foo/") or GET("/foos/")) { req -> handle(req) }
		"/api".nest {
			POST("/foo/", ::handleFromClass)
			POST("/bar/", contentType(APPLICATION_JSON), ::handleFromClass)
			PUT("/foo/", :: handleFromClass)
			PATCH("/foo/") {
				ok().build()
			}
			"/foo/"  { handleFromClass(it) }
		}
		"/content".nest {
			accept(APPLICATION_ATOM_XML, ::handle)
			contentType(APPLICATION_OCTET_STREAM, ::handle)
		}
		method(PATCH, ::handle)
		headers { it.accept().contains(APPLICATION_JSON) }.nest {
			GET("/api/foo/", ::handle)
		}
		headers({ it.header("bar").isNotEmpty() }, ::handle)
		resources("/org/springframework/web/reactive/function/**",
				ClassPathResource("/org/springframework/web/reactive/function/response.txt"))
		resources {
			if (it.path() == "/response.txt") {
				Mono.just(ClassPathResource("/org/springframework/web/reactive/function/response.txt"))
			}
			else {
				Mono.empty()
			}
		}
		path("/baz", ::handle)
		GET("/rendering") { RenderingResponse.create("index").build() }
		add(otherRouter)
	}

	private val otherRouter = router {
		"/other" {
			ok().build()
		}
		filter { request, next ->
			next(request)
		}
		before {
			it
		}
		after { _, response ->
			response
		}
		onError({it is IllegalStateException}) { _, _ ->
			ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
		onError<IllegalStateException> { _, _ ->
			ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}

	@Suppress("UNUSED_PARAMETER")
	private fun handleFromClass(req: ServerRequest) = ServerResponse.ok().build()
}

@Suppress("UNUSED_PARAMETER")
private fun handle(req: ServerRequest) = ServerResponse.ok().build()
