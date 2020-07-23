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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.NestedServletException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link ExceptionHandlerExceptionResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Kazuki Shimizu
 * @author Brian Clozel
 * @since 3.1
 */
@SuppressWarnings("unused")
public class ExceptionHandlerExceptionResolverTests {

	private static int RESOLVER_COUNT;

	private static int HANDLER_COUNT;

	private ExceptionHandlerExceptionResolver resolver;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeAll
	public static void setupOnce() {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.afterPropertiesSet();
		RESOLVER_COUNT = resolver.getArgumentResolvers().getResolvers().size();
		HANDLER_COUNT = resolver.getReturnValueHandlers().getHandlers().size();
	}

	@BeforeEach
	public void setup() throws Exception {
		this.resolver = new ExceptionHandlerExceptionResolver();
		this.resolver.setWarnLogCategory(this.resolver.getClass().getName());
		this.request = new MockHttpServletRequest("GET", "/");
		this.request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		this.response = new MockHttpServletResponse();
	}


	@Test
	public void nullHandler() {
		Object handler = null;
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handler, null);
		assertThat(mav).as("Exception can be resolved only if there is a HandlerMethod").isNull();
	}

	@Test
	public void setCustomArgumentResolvers() {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.resolver.setCustomArgumentResolvers(Collections.singletonList(resolver));
		this.resolver.afterPropertiesSet();

		assertThat(this.resolver.getArgumentResolvers().getResolvers().contains(resolver)).isTrue();
		assertMethodProcessorCount(RESOLVER_COUNT + 1, HANDLER_COUNT);
	}

	@Test
	public void setArgumentResolvers() {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.resolver.setArgumentResolvers(Collections.singletonList(resolver));
		this.resolver.afterPropertiesSet();

		assertMethodProcessorCount(1, HANDLER_COUNT);
	}

	@Test
	public void setCustomReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ViewNameMethodReturnValueHandler();
		this.resolver.setCustomReturnValueHandlers(Collections.singletonList(handler));
		this.resolver.afterPropertiesSet();

		assertThat(this.resolver.getReturnValueHandlers().getHandlers().contains(handler)).isTrue();
		assertMethodProcessorCount(RESOLVER_COUNT, HANDLER_COUNT + 1);
	}

	@Test
	public void setReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ModelMethodProcessor();
		this.resolver.setReturnValueHandlers(Collections.singletonList(handler));
		this.resolver.afterPropertiesSet();

		assertMethodProcessorCount(RESOLVER_COUNT, 1);
	}

	@Test
	public void resolveNoExceptionHandlerForException() throws NoSuchMethodException {
		Exception npe = new NullPointerException();
		HandlerMethod handlerMethod = new HandlerMethod(new IoExceptionController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, npe);

		assertThat(mav).as("NPE should not have been handled").isNull();
	}

	@Test
	public void resolveExceptionModelAndView() throws NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException("Bad argument");
		HandlerMethod handlerMethod = new HandlerMethod(new ModelAndViewController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isFalse();
		assertThat(mav.getViewName()).isEqualTo("errorView");
		assertThat(mav.getModel().get("detail")).isEqualTo("Bad argument");
	}

	@Test
	public void resolveExceptionResponseBody() throws UnsupportedEncodingException, NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
	}

	@Test
	public void resolveExceptionResponseWriter() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseWriterController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
	}

	@Test  // SPR-13546
	public void resolveExceptionModelAtArgument() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ModelArgumentController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.getModelMap().size()).isEqualTo(1);
		assertThat(mav.getModelMap().get("exceptionClassName")).isEqualTo("IllegalArgumentException");
	}

	@Test  // SPR-14651
	public void resolveRedirectAttributesAtArgument() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new RedirectAttributesController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.getViewName()).isEqualTo("redirect:/");
		FlashMap flashMap = (FlashMap) this.request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
		assertThat((Object) flashMap).as("output FlashMap should exist").isNotNull();
		assertThat(flashMap.get("exceptionClassName")).isEqualTo("IllegalArgumentException");
	}

	@Test
	public void resolveExceptionGlobalHandler() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalAccessException ex = new IllegalAccessException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("AnotherTestExceptionResolver: IllegalAccessException");
	}

	@Test
	public void resolveExceptionGlobalHandlerOrdered() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("TestExceptionResolver: IllegalStateException");
	}

	@Test  // SPR-12605
	public void resolveExceptionWithHandlerMethodArg() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("HandlerMethod: handle");
	}

	@Test
	public void resolveExceptionWithAssertionError() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		AssertionError err = new AssertionError("argh");
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod,
				new NestedServletException("Handler dispatch failed", err));

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo(err.toString());
	}

	@Test
	public void resolveExceptionWithAssertionErrorAsRootCause() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		AssertionError err = new AssertionError("argh");
		FatalBeanException ex = new FatalBeanException("wrapped", err);
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo(err.toString());
	}

	@Test
	public void resolveExceptionControllerAdviceHandler() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
	}

	@Test
	public void resolveExceptionControllerAdviceNoHandler() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, null, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
	}

	@Test  // SPR-16496
	public void resolveExceptionControllerAdviceAgainstProxy() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ProxyFactory(new ResponseBodyController()).getProxy(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
	}


	private void assertMethodProcessorCount(int resolverCount, int handlerCount) {
		assertThat(this.resolver.getArgumentResolvers().getResolvers().size()).isEqualTo(resolverCount);
		assertThat(this.resolver.getReturnValueHandlers().getHandlers().size()).isEqualTo(handlerCount);
	}


	@Controller
	static class ModelAndViewController {

		public void handle() {}

		@ExceptionHandler
		public ModelAndView handle(Exception ex) throws IOException {
			return new ModelAndView("errorView", "detail", ex.getMessage());
		}
	}


	@Controller
	static class ResponseWriterController {

		public void handle() {}

		@ExceptionHandler
		public void handleException(Exception ex, Writer writer) throws IOException {
			writer.write(ClassUtils.getShortName(ex.getClass()));
		}
	}


	interface ResponseBodyInterface {

		void handle();

		@ExceptionHandler
		@ResponseBody
		String handleException(IllegalArgumentException ex);
	}


	@Controller
	static class ResponseBodyController extends WebApplicationObjectSupport implements ResponseBodyInterface {

		@Override
		public void handle() {}

		@Override
		@ExceptionHandler
		@ResponseBody
		public String handleException(IllegalArgumentException ex) {
			return ClassUtils.getShortName(ex.getClass());
		}
	}


	@Controller
	static class IoExceptionController {

		public void handle() {}

		@ExceptionHandler(value = IOException.class)
		public void handleException() {
		}
	}


	@Controller
	static class ModelArgumentController {

		public void handle() {}

		@ExceptionHandler
		public void handleException(Exception ex, Model model) {
			model.addAttribute("exceptionClassName", ClassUtils.getShortName(ex.getClass()));
		}
	}

	@Controller
	static class RedirectAttributesController {

		public void handle() {}

		@ExceptionHandler
		public String handleException(Exception ex, RedirectAttributes redirectAttributes) {
			redirectAttributes.addFlashAttribute("exceptionClassName", ClassUtils.getShortName(ex.getClass()));
			return "redirect:/";
		}
	}


	@RestControllerAdvice
	@Order(1)
	static class TestExceptionResolver {

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "TestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}

		@ExceptionHandler(ArrayIndexOutOfBoundsException.class)
		public String handleWithHandlerMethod(HandlerMethod handlerMethod) {
			return "HandlerMethod: " + handlerMethod.getMethod().getName();
		}

		@ExceptionHandler(AssertionError.class)
		public String handleAssertionError(Error err) {
			return err.toString();
		}
	}


	@RestControllerAdvice
	@Order(2)
	static class AnotherTestExceptionResolver {

		@ExceptionHandler({IllegalStateException.class, IllegalAccessException.class})
		public String handleException(Exception ex) {
			return "AnotherTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}
	}


	@Configuration
	static class MyConfig {

		@Bean
		public TestExceptionResolver testExceptionResolver() {
			return new TestExceptionResolver();
		}

		@Bean
		public AnotherTestExceptionResolver anotherTestExceptionResolver() {
			return new AnotherTestExceptionResolver();
		}
	}


	@RestControllerAdvice("java.lang")
	@Order(1)
	static class NotCalledTestExceptionResolver {

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "NotCalledTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}
	}


	@RestControllerAdvice(assignableTypes = WebApplicationObjectSupport.class)
	@Order(2)
	static class BasePackageTestExceptionResolver {

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "BasePackageTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}
	}


	@RestControllerAdvice
	@Order(3)
	static class DefaultTestExceptionResolver {

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "DefaultTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}
	}


	@Configuration
	static class MyControllerAdviceConfig {

		@Bean
		public NotCalledTestExceptionResolver notCalledTestExceptionResolver() {
			return new NotCalledTestExceptionResolver();
		}

		@Bean
		public BasePackageTestExceptionResolver basePackageTestExceptionResolver() {
			return new BasePackageTestExceptionResolver();
		}

		@Bean
		public DefaultTestExceptionResolver defaultTestExceptionResolver() {
			return new DefaultTestExceptionResolver();
		}
	}

}
