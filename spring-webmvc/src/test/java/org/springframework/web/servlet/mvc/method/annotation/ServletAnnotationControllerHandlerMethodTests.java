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

import java.beans.ConstructorProperties;
import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.beans.testfixture.beans.GenericBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class ServletAnnotationControllerHandlerMethodTests extends AbstractServletHandlerMethodTests {

	@Test
	public void emptyValueMapping() throws Exception {
		initServletWithControllers(ControllerWithEmptyValueMapping.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContextPath("/foo");
		request.setServletPath("");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test");
	}

	@Test
	public void errorThrownFromHandlerMethod() throws Exception {
		initServletWithControllers(ControllerWithErrorThrown.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContextPath("/foo");
		request.setServletPath("");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test");
	}

	@Test
	public void customAnnotationController() throws Exception {
		initServletWithControllers(CustomAnnotationController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void requiredParamMissing() throws Exception {
		WebApplicationContext webAppContext = initServletWithControllers(RequiredParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
		assertThat(webAppContext.isSingleton(RequiredParamController.class.getSimpleName())).isTrue();
	}

	@Test
	public void typeConversionError() throws Exception {
		initServletWithControllers(RequiredParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("id", "foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
	}

	@Test
	public void optionalParamPresent() throws Exception {
		initServletWithControllers(OptionalParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("id", "val");
		request.addParameter("flag", "true");
		request.addHeader("header", "otherVal");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("val-true-otherVal");
	}

	@Test
	public void optionalParamMissing() throws Exception {
		initServletWithControllers(OptionalParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("null-false-null");
	}

	@Test
	public void defaultParameters() throws Exception {
		initServletWithControllers(DefaultValueParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("foo--bar");
	}

	@Test
	public void defaultExpressionParameters() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition ppc = new RootBeanDefinition(PropertySourcesPlaceholderConfigurer.class);
			ppc.getPropertyValues().add("properties", "myKey=foo");
			wac.registerBeanDefinition("ppc", ppc);
		}, DefaultExpressionValueParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myPath.do");
		request.setContextPath("/myApp");
		MockHttpServletResponse response = new MockHttpServletResponse();
		System.setProperty("myHeader", "bar");
		try {
			getServlet().service(request, response);
		}
		finally {
			System.clearProperty("myHeader");
		}
		assertThat(response.getContentAsString()).isEqualTo("foo-bar-/myApp");
	}

	@Test
	public void typeNestedSetBinding() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
			csDef.getPropertyValues().add("converters", new TestBeanConverter());
			RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
			wbiDef.getPropertyValues().add("conversionService", csDef);
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, NestedSetController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("testBeanSet", "1", "2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("[1, 2]-org.springframework.beans.testfixture.beans.TestBean");
	}

	@Test  // SPR-12903
	public void pathVariableWithCustomConverter() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
			csDef.getPropertyValues().add("converters", new AnnotatedExceptionRaisingConverter());
			RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
			wbiDef.getPropertyValues().add("conversionService", csDef);
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, PathVariableWithCustomConverterController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath/1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(404);
	}

	@Test
	public void methodNotAllowed() throws Exception {
		initServletWithControllers(MethodNotAllowedController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).as("Invalid response status").isEqualTo(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		String allowHeader = response.getHeader("Allow");
		assertThat(allowHeader).as("No Allow header").isNotNull();
		Set<String> allowedMethods = new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(allowHeader, ", ")));
		assertThat(allowedMethods.size()).as("Invalid amount of supported methods").isEqualTo(6);
		assertThat(allowedMethods.contains("PUT")).as("PUT not allowed").isTrue();
		assertThat(allowedMethods.contains("DELETE")).as("DELETE not allowed").isTrue();
		assertThat(allowedMethods.contains("HEAD")).as("HEAD not allowed").isTrue();
		assertThat(allowedMethods.contains("TRACE")).as("TRACE not allowed").isTrue();
		assertThat(allowedMethods.contains("OPTIONS")).as("OPTIONS not allowed").isTrue();
		assertThat(allowedMethods.contains("POST")).as("POST not allowed").isTrue();
	}

	@Test
	public void emptyParameterListHandleMethod() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition vrDef = new RootBeanDefinition(InternalResourceViewResolver.class);
			vrDef.getPropertyValues().add("suffix", ".jsp");
			wac.registerBeanDefinition("viewResolver", vrDef);
		}, EmptyParameterListHandlerMethodController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/emptyParameterListHandler");
		MockHttpServletResponse response = new MockHttpServletResponse();

		EmptyParameterListHandlerMethodController.called = false;
		getServlet().service(request, response);
		assertThat(EmptyParameterListHandlerMethodController.called).isTrue();
		assertThat(response.getContentAsString()).isEqualTo("");
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void sessionAttributeExposure() throws Exception {
		initServlet(
				wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class)),
				MySessionAttributesController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(request.getAttribute("viewName")).isEqualTo("page1");
		HttpSession session = request.getSession();
		assertThat(session.getAttribute("object1") != null).isTrue();
		assertThat(session.getAttribute("object2") != null).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object1")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object2")).isTrue();

		request = new MockHttpServletRequest("POST", "/myPage");
		request.setSession(session);
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(request.getAttribute("viewName")).isEqualTo("page2");
		assertThat(session.getAttribute("object1") != null).isTrue();
		assertThat(session.getAttribute("object2") != null).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object1")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object2")).isTrue();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void sessionAttributeExposureWithInterface() throws Exception {
		initServlet(wac -> {
			wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class));
			DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
			autoProxyCreator.setBeanFactory(wac.getBeanFactory());
			wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
			wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		}, MySessionAttributesControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(request.getAttribute("viewName")).isEqualTo("page1");
		HttpSession session = request.getSession();
		assertThat(session.getAttribute("object1") != null).isTrue();
		assertThat(session.getAttribute("object2") != null).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object1")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object2")).isTrue();

		request = new MockHttpServletRequest("POST", "/myPage");
		request.setSession(session);
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(request.getAttribute("viewName")).isEqualTo("page2");
		assertThat(session.getAttribute("object1") != null).isTrue();
		assertThat(session.getAttribute("object2") != null).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object1")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object2")).isTrue();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void parameterizedAnnotatedInterface() throws Exception {
		initServlet(
				wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class)),
				MyParameterizedControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(request.getAttribute("viewName")).isEqualTo("page1");
		HttpSession session = request.getSession();
		assertThat(session.getAttribute("object1") != null).isTrue();
		assertThat(session.getAttribute("object2") != null).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object1")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object2")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("testBeanList")).isTrue();

		request = new MockHttpServletRequest("POST", "/myPage");
		request.setSession(session);
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(request.getAttribute("viewName")).isEqualTo("page2");
		assertThat(session.getAttribute("object1") != null).isTrue();
		assertThat(session.getAttribute("object2") != null).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object1")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object2")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("testBeanList")).isTrue();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void parameterizedAnnotatedInterfaceWithOverriddenMappingsInImpl() throws Exception {
		initServlet(
				wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class)),
				MyParameterizedControllerImplWithOverriddenMappings.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(request.getAttribute("viewName")).isEqualTo("page1");
		HttpSession session = request.getSession();
		assertThat(session.getAttribute("object1") != null).isTrue();
		assertThat(session.getAttribute("object2") != null).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object1")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object2")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("testBeanList")).isTrue();

		request = new MockHttpServletRequest("POST", "/myPage");
		request.setSession(session);
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(request.getAttribute("viewName")).isEqualTo("page2");
		assertThat(session.getAttribute("object1") != null).isTrue();
		assertThat(session.getAttribute("object2") != null).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object1")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("object2")).isTrue();
		assertThat(((Map) session.getAttribute("model")).containsKey("testBeanList")).isTrue();
	}

	@Test
	public void adaptedHandleMethods() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController.class);
	}

	@Test
	public void adaptedHandleMethods2() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController2.class);
	}

	@Test
	public void adaptedHandleMethods3() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController3.class);
	}

	private void doTestAdaptedHandleMethods(final Class<?> controllerClass) throws Exception {
		initServletWithControllers(controllerClass);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath1.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test");

		request = new MockHttpServletRequest("GET", "/myPath2.do");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		request.addHeader("header1", "10");
		request.setCookies(new Cookie("cookie1", "3"));
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test-value1-2-10-3");

		request = new MockHttpServletRequest("GET", "/myPath3.do");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		request.addParameter("name", "name1");
		request.addParameter("age", "2");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test-name1-2");

		request = new MockHttpServletRequest("GET", "/myPath4.do");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test-name1-typeMismatch");
	}

	@Test
	public void formController() throws Exception {
		initServlet(
				wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class)),
				MyFormController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
	}

	@Test
	public void modelFormController() throws Exception {
		initServlet(
				wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class)),
				MyModelFormController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myPath-name1-typeMismatch-tb1-myValue-yourValue");
	}

	@Test
	public void lateBindingFormController() throws Exception {
		initServlet(
				wac -> wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class)),
				LateBindingFormController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
	}

	@Test
	public void proxiedFormController() throws Exception {
		initServlet(wac -> {
			wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
			DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
			autoProxyCreator.setBeanFactory(wac.getBeanFactory());
			wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
			wac.getBeanFactory()
					.registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		}, MyFormController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-name1-typeMismatch-tb1-myValue");
	}

	@Test
	public void commandProvidingFormControllerWithCustomEditor() throws Exception {
		initServlet(wac -> {
			wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, MyCommandProvidingFormController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
	}

	@Test
	public void typedCommandProvidingFormController() throws Exception {
		initServlet(wac -> {
			wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
			List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
			argumentResolvers.add(new ServletWebArgumentResolverAdapter(new MySpecialArgumentResolver()));
			adapterDef.getPropertyValues().add("customArgumentResolvers", argumentResolvers);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, MyTypedCommandProvidingFormController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-Integer:10-typeMismatch-tb1-myOriginalValue");

		request = new MockHttpServletRequest("GET", "/myOtherPath.do");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-myName-typeMismatch-tb1-myOriginalValue");

		request = new MockHttpServletRequest("GET", "/myThirdPath.do");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "100");
		request.addParameter("date", "2007-10-02");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-special-99-special-99");
	}

	@Test
	public void binderInitializingCommandProvidingFormController() throws Exception {
		initServlet(wac -> wac.registerBeanDefinition("viewResolver",
				new RootBeanDefinition(TestViewResolver.class)),
				MyBinderInitializingCommandProvidingFormController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
	}

	@Test
	public void specificBinderInitializingCommandProvidingFormController() throws Exception {
		initServlet(wac -> wac.registerBeanDefinition("viewResolver",
				new RootBeanDefinition(TestViewResolver.class)),
				MySpecificBinderInitializingCommandProvidingFormController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue");
	}

	@Test
	public void parameterDispatchingController() throws Exception {
		final MockServletContext servletContext = new MockServletContext();
		final MockServletConfig servletConfig = new MockServletConfig(servletContext);

		WebApplicationContext webAppContext =
			initServlet(wac -> {
				wac.setServletContext(servletContext);
				AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);
				wac.getBeanFactory().registerResolvableDependency(ServletConfig.class, servletConfig);
			}, MyParameterDispatchingController.class);

		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpSession session = request.getSession();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView");
		assertThat(request.getAttribute("servletContext")).isSameAs(servletContext);
		assertThat(request.getAttribute("servletConfig")).isSameAs(servletConfig);
		assertThat(request.getAttribute("sessionId")).isSameAs(session.getId());
		assertThat(request.getAttribute("requestUri")).isSameAs(request.getRequestURI());
		assertThat(request.getAttribute("locale")).isSameAs(request.getLocale());

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		response = new MockHttpServletResponse();
		session = request.getSession();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView");
		assertThat(request.getAttribute("servletContext")).isSameAs(servletContext);
		assertThat(request.getAttribute("servletConfig")).isSameAs(servletConfig);
		assertThat(request.getAttribute("sessionId")).isSameAs(session.getId());
		assertThat(request.getAttribute("requestUri")).isSameAs(request.getRequestURI());

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("view", "other");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myOtherView");

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("view", "my");
		request.addParameter("lang", "de");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myLangView");

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("surprise", "!");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("mySurpriseView");

		MyParameterDispatchingController deserialized =
			(MyParameterDispatchingController) SerializationTestUtils.serializeAndDeserialize(
					webAppContext.getBean(MyParameterDispatchingController.class.getSimpleName()));
		assertThat(deserialized.request).isNotNull();
		assertThat(deserialized.session).isNotNull();
	}

	@Test
	public void relativePathDispatchingController() throws Exception {
		initServletWithControllers(MyRelativePathDispatchingController.class);
		getServlet().init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myHandle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView");

		request = new MockHttpServletRequest("GET", "/myApp/myOther");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myOtherView");

		request = new MockHttpServletRequest("GET", "/myApp/myLang");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myLangView");

		request = new MockHttpServletRequest("GET", "/myApp/surprise.do");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("mySurpriseView");
	}

	@Test
	public void relativeMethodPathDispatchingController() throws Exception {
		initServletWithControllers(MyRelativeMethodPathDispatchingController.class);
		getServlet().init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myHandle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView");

		request = new MockHttpServletRequest("GET", "/yourApp/myOther");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myOtherView");

		request = new MockHttpServletRequest("GET", "/hisApp/myLang");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myLangView");

		request = new MockHttpServletRequest("GET", "/herApp/surprise.do");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("mySurpriseView");
	}

	@Test
	public void nullCommandController() throws Exception {
		initServletWithControllers(MyNullCommandController.class);
		getServlet().init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath");
		request.setUserPrincipal(new OtherPrincipal());
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myView");
	}

	@Test
	public void equivalentMappingsWithSameMethodName() {
		assertThatThrownBy(() -> initServletWithControllers(ChildController.class))
			.isInstanceOf(BeanCreationException.class)
			.hasCauseInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Ambiguous mapping");
	}

	@Test // gh-22543
	public void unmappedPathMapping() throws Exception {
		initServletWithControllers(UnmappedPathController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bogus-unmapped");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(404);

		request = new MockHttpServletRequest("GET", "");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("get");
	}

	@Test
	public void explicitAndEmptyPathsControllerMapping() throws Exception {
		initServletWithControllers(ExplicitAndEmptyPathsController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("get");

		request = new MockHttpServletRequest("GET", "");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("get");
	}

	@Test
	public void pathOrdering() throws Exception {
		initServletWithControllers(PathOrderingController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dir/myPath1.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("method1");
	}

	@Test
	public void requestBodyResponseBody() throws Exception {
		initServletWithControllers(RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "text/*, */*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo(requestBody);
	}

	@Test
	public void httpPatch() throws Exception {
		initServletWithControllers(RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/something");
		String requestBody = "Hello world!";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "text/*, */*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo(requestBody);
	}

	@Test
	public void responseBodyNoAcceptableMediaType() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			StringHttpMessageConverter converter = new StringHttpMessageConverter();
			adapterDef.getPropertyValues().add("messageConverters", converter);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, RequestResponseBodyProducesController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "application/pdf, application/msword");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(406);
	}

	@Test
	public void responseBodyWildCardMediaType() throws Exception {
		initServletWithControllers(RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "*/*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo(requestBody);
	}

	@Test
	public void unsupportedRequestBody() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("messageConverters", new ByteArrayHttpMessageConverter());
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(415);
		assertThat(response.getHeader("Accept")).as("No Accept response header set").isNotNull();
	}

	@Test
	public void responseBodyNoAcceptHeader() throws Exception {
		initServletWithControllers(RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo(requestBody);
	}

	@Test
	public void badRequestRequestBody() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("messageConverters", new NotReadableMessageConverter());
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
	}

	@Test
	public void httpEntity() throws Exception {
		initServletWithControllers(ResponseEntityController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/foo");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "text/*, */*");
		request.addHeader("MyRequestHeader", "MyValue");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(201);
		assertThat(response.getContentAsString()).isEqualTo(requestBody);
		assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");

		request = new MockHttpServletRequest("GET", "/bar");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
		assertThat(response.getStatus()).isEqualTo(404);
	}

	@Test // SPR-16172
	public void httpEntityWithContentType() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
			messageConverters.add(new MappingJackson2HttpMessageConverter());
			messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
			adapterDef.getPropertyValues().add("messageConverters", messageConverters);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, ResponseEntityController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test-entity");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader("Content-Type")).isEqualTo("application/xml");
		assertThat(response.getContentAsString()).isEqualTo(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
						"<testEntity><name>Foo Bar</name></testEntity>"));
	}

	@Test  // SPR-6877
	public void overlappingMessageConvertersRequestBody() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
			messageConverters.add(new StringHttpMessageConverter());
			messageConverters
					.add(new SimpleMessageConverter(new MediaType("application","json"), MediaType.ALL));
			adapterDef.getPropertyValues().add("messageConverters", messageConverters);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		request.setContent("Hello World".getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "application/json, text/javascript, */*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getHeader("Content-Type")).as("Invalid content-type").isEqualTo("application/json");
	}

	@Test
	public void responseBodyVoid() throws Exception {
		initServletWithControllers(ResponseBodyVoidController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/*, */*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	public void responseBodyArgMismatch() throws Exception {
		initServlet(wac -> {
			Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
			marshaller.setClassesToBeBound(A.class, B.class);
			try {
				marshaller.afterPropertiesSet();
			}
			catch (Exception ex) {
				throw new BeanCreationException(ex.getMessage(), ex);
			}
			MarshallingHttpMessageConverter messageConverter = new MarshallingHttpMessageConverter(marshaller);

			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("messageConverters", messageConverter);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, RequestBodyArgMismatchController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "<b/>";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/xml; charset=utf-8");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(400);
	}


	@Test
	public void contentTypeHeaders() throws Exception {
		initServletWithControllers(ContentTypeHeadersController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/something");
		request.setContentType("application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("pdf");

		request = new MockHttpServletRequest("POST", "/something");
		request.setContentType("text/html");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("text");

		request = new MockHttpServletRequest("POST", "/something");
		request.setContentType("application/xml");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(415);
	}

	@Test
	public void consumes() throws Exception {
		initServletWithControllers(ConsumesController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/something");
		request.setContentType("application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("pdf");

		request = new MockHttpServletRequest("POST", "/something");
		request.setContentType("text/html");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("text");

		request = new MockHttpServletRequest("POST", "/something");
		request.setContentType("application/xml");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(415);
	}

	@Test
	public void negatedContentTypeHeaders() throws Exception {
		initServletWithControllers(NegatedContentTypeHeadersController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/something");
		request.setContentType("application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("pdf");

		request = new MockHttpServletRequest("POST", "/something");
		request.setContentType("text/html");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("non-pdf");
	}

	@Test
	public void acceptHeaders() throws Exception {
		initServletWithControllers(AcceptHeadersController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("html");

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "application/xml");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("xml");

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "application/xml, text/html");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("xml");

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/html;q=0.9, application/xml");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("xml");

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "application/msword");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(406);
	}

	@Test
	public void produces() throws Exception {
		initServlet(wac -> {
			List<HttpMessageConverter<?>> converters = new ArrayList<>();
			converters.add(new MappingJackson2HttpMessageConverter());
			converters.add(new Jaxb2RootElementHttpMessageConverter());

			RootBeanDefinition beanDef;

			beanDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			beanDef.getPropertyValues().add("messageConverters", converters);
			wac.registerBeanDefinition("handlerAdapter", beanDef);

			beanDef = new RootBeanDefinition(ExceptionHandlerExceptionResolver.class);
			beanDef.getPropertyValues().add("messageConverters", converters);
			wac.registerBeanDefinition("requestMappingResolver", beanDef);

		}, ProducesController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("html");

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "application/xml");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("xml");

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "application/xml, text/html");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("xml");

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/html;q=0.9, application/xml");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("xml");

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "application/msword");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(406);

		// SPR-16318
		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/csv,application/problem+json");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		assertThat(response.getContentAsString()).isEqualTo("{\"reason\":\"error\"}");
	}

	@Test
	public void responseStatus() throws Exception {
		initServletWithControllers(ResponseStatusController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("something");
		assertThat(response.getStatus()).isEqualTo(201);
		assertThat(response.getErrorMessage()).isEqualTo("It's alive!");
	}

	@Test
	public void mavResolver() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			ModelAndViewResolver[] mavResolvers = new ModelAndViewResolver[] {new MyModelAndViewResolver()};
			adapterDef.getPropertyValues().add("modelAndViewResolvers", mavResolvers);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, ModelAndViewResolverController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myValue");

	}

	@Test
	public void bindingCookieValue() throws Exception {
		initServletWithControllers(BindingCookieValueController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.setCookies(new Cookie("date", "2008-11-18"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test-2008");
	}

	@Test
	public void ambiguousParams() throws Exception {
		initServletWithControllers(AmbiguousParamsController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("noParams");

		request = new MockHttpServletRequest("GET", "/test");
		request.addParameter("myParam", "42");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("myParam-42");
	}

	@Test  // SPR-9062
	public void ambiguousPathAndRequestMethod() throws Exception {
		initServletWithControllers(AmbiguousPathAndRequestMethodController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bug/EXISTING");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("Pattern");
	}

	@Test
	public void bridgeMethods() throws Exception {
		initServletWithControllers(TestControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/method");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
	}

	@Test
	public void bridgeMethodsWithMultipleInterfaces() throws Exception {
		initServletWithControllers(ArticleController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/method");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
	}

	@Test
	public void requestParamMap() throws Exception {
		initServletWithControllers(RequestParamMapController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/map");
		request.addParameter("key1", "value1");
		request.addParameter("key2", new String[] {"value21", "value22"});
		MockHttpServletResponse response = new MockHttpServletResponse();

		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("key1=value1,key2=value21");

		request.setRequestURI("/multiValueMap");
		response = new MockHttpServletResponse();

		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("key1=[value1],key2=[value21,value22]");
	}

	@Test
	public void requestHeaderMap() throws Exception {
		initServletWithControllers(RequestHeaderMapController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/map");
		request.addHeader("Content-Type", "text/html");
		request.addHeader("Custom-Header", new String[] {"value21", "value22"});
		MockHttpServletResponse response = new MockHttpServletResponse();

		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Content-Type=text/html,Custom-Header=value21");

		request.setRequestURI("/multiValueMap");
		response = new MockHttpServletResponse();

		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Content-Type=[text/html],Custom-Header=[value21,value22]");

		request.setRequestURI("/httpHeaders");
		response = new MockHttpServletResponse();

		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Content-Type=[text/html],Custom-Header=[value21,value22]");
	}


	@Test
	public void requestMappingInterface() throws Exception {
		initServletWithControllers(IMyControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("handle null");

		request = new MockHttpServletRequest("GET", "/handle");
		request.addParameter("p", "value");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("handle value");
	}

	@Test
	public void requestMappingInterfaceWithProxy() throws Exception {
		initServlet(wac -> {
			DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
			autoProxyCreator.setBeanFactory(wac.getBeanFactory());
			wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
			wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		}, IMyControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("handle null");

		request = new MockHttpServletRequest("GET", "/handle");
		request.addParameter("p", "value");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("handle value");
	}

	@Test
	public void requestMappingBaseClass() throws Exception {
		initServletWithControllers(MyAbstractControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("handle");

	}

	@Test
	public void trailingSlash() throws Exception {
		initServletWithControllers(TrailingSlashController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("templatePath");
	}

	/*
	 * See SPR-6021
	 */
	@Test
	public void customMapEditor() throws Exception {
		initServletWithControllers(CustomMapEditorController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
		request.addParameter("map", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();

		getServlet().service(request, response);

		assertThat(response.getContentAsString()).isEqualTo("test-{foo=bar}");
	}

	@Test
	public void multipartFileAsSingleString() throws Exception {
		initServletWithControllers(MultipartController.class);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setRequestURI("/singleString");
		request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Juergen");
	}

	@Test
	public void regularParameterAsSingleString() throws Exception {
		initServletWithControllers(MultipartController.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/singleString");
		request.setMethod("POST");
		request.addParameter("content", "Juergen");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Juergen");
	}

	@Test
	public void multipartFileAsStringArray() throws Exception {
		initServletWithControllers(MultipartController.class);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setRequestURI("/stringArray");
		request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Juergen");
	}

	@Test
	public void regularParameterAsStringArray() throws Exception {
		initServletWithControllers(MultipartController.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/stringArray");
		request.setMethod("POST");
		request.addParameter("content", "Juergen");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Juergen");
	}

	@Test
	public void multipartFilesAsStringArray() throws Exception {
		initServletWithControllers(MultipartController.class);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setRequestURI("/stringArray");
		request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
		request.addFile(new MockMultipartFile("content", "Eva".getBytes()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Juergen-Eva");
	}

	@Test
	public void regularParametersAsStringArray() throws Exception {
		initServletWithControllers(MultipartController.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/stringArray");
		request.setMethod("POST");
		request.addParameter("content", "Juergen");
		request.addParameter("content", "Eva");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Juergen-Eva");
	}

	@Test
	public void parameterCsvAsStringArray() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
			RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
			wbiDef.getPropertyValues().add("conversionService", csDef);
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, CsvController.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/integerArray");
		request.setMethod("POST");
		request.addParameter("content", "1,2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("1-2");
	}

	@Test
	public void testMatchWithoutMethodLevelPath() throws Exception {
		initServletWithControllers(NoPathGetAndM2PostController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/t1/m2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(405);
	}

	@Test  // SPR-8536
	public void testHeadersCondition() throws Exception {
		initServletWithControllers(HeadersConditionController.class);

		// No "Accept" header
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).isEqualTo("home");

		// Accept "*/*"
		request = new MockHttpServletRequest("GET", "/");
		request.addHeader("Accept", "*/*");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).isEqualTo("home");

		// Accept "application/json"
		request = new MockHttpServletRequest("GET", "/");
		request.addHeader("Accept", "application/json");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
		assertThat(response.getContentAsString()).isEqualTo("homeJson");
	}

	@Test
	public void redirectAttribute() throws Exception {
		initServletWithControllers(RedirectAttributesController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/messages");
		HttpSession session = request.getSession();
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);

		// POST -> bind error
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getForwardedUrl()).isEqualTo("messages/new");
		assertThat(RequestContextUtils.getOutputFlashMap(request).isEmpty()).isTrue();

		// POST -> success
		request = new MockHttpServletRequest("POST", "/messages");
		request.setSession(session);
		request.addParameter("name", "Jeff");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(302);
		assertThat(response.getRedirectedUrl()).isEqualTo("/messages/1?name=value");
		assertThat(RequestContextUtils.getOutputFlashMap(request).get("successMessage")).isEqualTo("yay!");

		// GET after POST
		request = new MockHttpServletRequest("GET", "/messages/1");
		request.setQueryString("name=value");
		request.setSession(session);
		response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("Got: yay!");
		assertThat(RequestContextUtils.getOutputFlashMap(request).isEmpty()).isTrue();
	}

	@Test  // SPR-15176
	public void flashAttributesWithResponseEntity() throws Exception {
		initServletWithControllers(RedirectAttributesController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/messages-response-entity");
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpSession session = request.getSession();

		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(302);
		assertThat(response.getRedirectedUrl()).isEqualTo("/messages/1?name=value");
		assertThat(RequestContextUtils.getOutputFlashMap(request).get("successMessage")).isEqualTo("yay!");

		// GET after POST
		request = new MockHttpServletRequest("GET", "/messages/1");
		request.setQueryString("name=value");
		request.setSession(session);
		response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("Got: yay!");
		assertThat(RequestContextUtils.getOutputFlashMap(request).isEmpty()).isTrue();
	}

	@Test
	public void prototypeController() throws Exception {
		initServlet(wac -> {
			RootBeanDefinition beanDef = new RootBeanDefinition(PrototypeController.class);
			beanDef.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			wac.registerBeanDefinition("controller", beanDef);
		});

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addParameter("param", "1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getContentAsString()).isEqualTo("count:3");

		response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getContentAsString()).isEqualTo("count:3");
	}

	@Test
	public void restController() throws Exception {
		initServletWithControllers(ThisWillActuallyRun.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("Hello World!");
	}

	@Test
	public void responseAsHttpHeaders() throws Exception {
		initServletWithControllers(HttpHeadersResponseController.class);
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(new MockHttpServletRequest("POST", "/"), response);

		assertThat(response.getStatus()).as("Wrong status code").isEqualTo(MockHttpServletResponse.SC_CREATED);
		assertThat(response.getHeaderNames().size()).as("Wrong number of headers").isEqualTo(1);
		assertThat(response.getHeader("location")).as("Wrong value for 'location' header").isEqualTo("/test/items/123");
		assertThat(response.getContentLength()).as("Expected an empty content").isEqualTo(0);
	}

	@Test
	public void responseAsHttpHeadersNoHeader() throws Exception {
		initServletWithControllers(HttpHeadersResponseController.class);
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(new MockHttpServletRequest("POST", "/empty"), response);

		assertThat(response.getStatus()).as("Wrong status code").isEqualTo(MockHttpServletResponse.SC_CREATED);
		assertThat(response.getHeaderNames().size()).as("Wrong number of headers").isEqualTo(0);
		assertThat(response.getContentLength()).as("Expected an empty content").isEqualTo(0);
	}

	@Test
	public void responseBodyAsHtml() throws Exception {
		initServlet(wac -> {
			ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
			factoryBean.afterPropertiesSet();
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("contentNegotiationManager", factoryBean.getObject());
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, TextRestController.class);

		byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a1.html");
		request.setContent(content);
		MockHttpServletResponse response = new MockHttpServletResponse();

		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).isEqualTo("text/html;charset=ISO-8859-1");
		assertThat(response.getHeader("Content-Disposition")).isEqualTo("inline;filename=f.txt");
		assertThat(response.getContentAsByteArray()).isEqualTo(content);
	}

	@Test
	public void responseBodyAsHtmlWithSuffixPresent() throws Exception {
		initServlet(wac -> {
			ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
			factoryBean.afterPropertiesSet();
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("contentNegotiationManager", factoryBean.getObject());
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, TextRestController.class);

		byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a2.html");
		request.setContent(content);
		MockHttpServletResponse response = new MockHttpServletResponse();

		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).isEqualTo("text/html;charset=ISO-8859-1");
		assertThat(response.getHeader("Content-Disposition")).isNull();
		assertThat(response.getContentAsByteArray()).isEqualTo(content);
	}

	@Test
	public void responseBodyAsHtmlWithProducesCondition() throws Exception {
		initServlet(wac -> {
			ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
			factoryBean.afterPropertiesSet();
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("contentNegotiationManager", factoryBean.getObject());
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, TextRestController.class);

		byte[] content = "alert('boo')".getBytes(StandardCharsets.ISO_8859_1);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a3.html");
		request.setContent(content);
		MockHttpServletResponse response = new MockHttpServletResponse();

		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).isEqualTo("text/html;charset=ISO-8859-1");
		assertThat(response.getHeader("Content-Disposition")).isNull();
		assertThat(response.getContentAsByteArray()).isEqualTo(content);
	}

	@Test
	public void responseBodyAsTextWithCssExtension() throws Exception {
		initServlet(wac -> {
			ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
			factoryBean.afterPropertiesSet();
			RootBeanDefinition adapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
			adapterDef.getPropertyValues().add("contentNegotiationManager", factoryBean.getObject());
			wac.registerBeanDefinition("handlerAdapter", adapterDef);
		}, TextRestController.class);

		byte[] content = "body".getBytes(StandardCharsets.ISO_8859_1);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a4.css");
		request.setContent(content);
		MockHttpServletResponse response = new MockHttpServletResponse();

		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentType()).isEqualTo("text/css;charset=ISO-8859-1");
		assertThat(response.getHeader("Content-Disposition")).isNull();
		assertThat(response.getContentAsByteArray()).isEqualTo(content);
	}

	@Test
	public void modelAndViewWithStatus() throws Exception {
		initServletWithControllers(ModelAndViewController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(422);
		assertThat(response.getForwardedUrl()).isEqualTo("view");
	}

	@Test // SPR-14796
	public void modelAndViewWithStatusInExceptionHandler() throws Exception {
		initServletWithControllers(ModelAndViewController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/exception");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(422);
		assertThat(response.getForwardedUrl()).isEqualTo("view");
	}

	@Test
	public void httpHead() throws Exception {
		initServletWithControllers(ResponseEntityController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/baz");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
		assertThat(response.getContentLength()).isEqualTo(4);
		assertThat(response.getContentAsByteArray().length == 0).isTrue();

		// Now repeat with GET
		request = new MockHttpServletRequest("GET", "/baz");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader("MyResponseHeader")).isEqualTo("MyValue");
		assertThat(response.getContentLength()).isEqualTo(4);
		assertThat(response.getContentAsString()).isEqualTo("body");
	}

	@Test
	public void httpHeadExplicit() throws Exception {
		initServletWithControllers(ResponseEntityController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/stores");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader("h1")).isEqualTo("v1");
	}

	@Test
	public void httpOptions() throws Exception {
		initServletWithControllers(ResponseEntityController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/baz");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
		assertThat(response.getContentAsByteArray().length == 0).isTrue();
	}

	@Test
	public void dataClassBinding() throws Exception {
		initServletWithControllers(DataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
	}

	@Test
	public void dataClassBindingWithAdditionalSetter() throws Exception {
		initServletWithControllers(DataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "true");
		request.addParameter("param3", "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
	}

	@Test
	public void dataClassBindingWithResult() throws Exception {
		initServletWithControllers(ValidatedDataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "true");
		request.addParameter("param3", "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
	}

	@Test
	public void dataClassBindingWithOptionalParameter() throws Exception {
		initServletWithControllers(ValidatedDataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "true");
		request.addParameter("optionalParam", "8");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-true-8");
	}

	@Test
	public void dataClassBindingWithMissingParameter() throws Exception {
		initServletWithControllers(ValidatedDataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("1:value1-null-null");
	}

	@Test
	public void dataClassBindingWithConversionError() throws Exception {
		initServletWithControllers(ValidatedDataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "x");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("1:value1-x-null");
	}

	@Test
	public void dataClassBindingWithValidationError() throws Exception {
		initServletWithControllers(ValidatedDataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param2", "true");
		request.addParameter("param3", "0");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("1:null-true-0");
	}

	@Test
	public void dataClassBindingWithValidationErrorAndConversionError() throws Exception {
		initServletWithControllers(ValidatedDataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param2", "x");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("2:null-x-null");
	}

	@Test
	public void dataClassBindingWithOptional() throws Exception {
		initServletWithControllers(OptionalDataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "true");
		request.addParameter("param3", "3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-true-3");
	}

	@Test
	public void dataClassBindingWithOptionalAndConversionError() throws Exception {
		initServletWithControllers(OptionalDataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "x");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-x-null");
	}

	@Test
	public void dataClassBindingWithFieldMarker() throws Exception {
		initServletWithControllers(DataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "true");
		request.addParameter("_param2", "on");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
	}

	@Test
	public void dataClassBindingWithFieldMarkerFallback() throws Exception {
		initServletWithControllers(DataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("_param2", "on");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-false-0");
	}

	@Test
	public void dataClassBindingWithFieldDefault() throws Exception {
		initServletWithControllers(DataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "true");
		request.addParameter("!param2", "false");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-true-0");
	}

	@Test
	public void dataClassBindingWithFieldDefaultFallback() throws Exception {
		initServletWithControllers(DataClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("param1", "value1");
		request.addParameter("!param2", "false");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("value1-false-0");
	}

	@Test
	public void dataClassBindingWithLocalDate() throws Exception {
		initServletWithControllers(DateClassController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bind");
		request.addParameter("date", "2010-01-01");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("2010-01-01");
	}


	@Controller
	static class ControllerWithEmptyValueMapping {

		@RequestMapping("")
		public void myPath2(HttpServletResponse response) throws IOException {
			throw new IllegalStateException("test");
		}

		@RequestMapping("/bar")
		public void myPath3(HttpServletResponse response) throws IOException {
			response.getWriter().write("testX");
		}

		@ExceptionHandler
		public void myPath2(Exception ex, HttpServletResponse response) throws IOException {
			response.getWriter().write(ex.getMessage());
		}
	}

	@Controller
	private static class ControllerWithErrorThrown {

		@RequestMapping("")
		public void myPath2(HttpServletResponse response) throws IOException {
			throw new AssertionError("test");
		}

		@RequestMapping("/bar")
		public void myPath3(HttpServletResponse response) throws IOException {
			response.getWriter().write("testX");
		}

		@ExceptionHandler
		public void myPath2(Error err, HttpServletResponse response) throws IOException {
			response.getWriter().write(err.getMessage());
		}
	}

	@Controller
	static class MyAdaptedController {

		@RequestMapping("/myPath1.do")
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.getWriter().write("test");
		}

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") String p1, @RequestParam("param2") int p2,
				@RequestHeader("header1") long h1, @CookieValue(name = "cookie1") Cookie c1,
				HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + p2 + "-" + h1 + "-" + c1.getValue());
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.do")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}

	@Controller
	@RequestMapping("/*.do")
	static class MyAdaptedController2 {

		@RequestMapping
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.getWriter().write("test");
		}

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") String p1, int param2, HttpServletResponse response,
				@RequestHeader("header1") String h1, @CookieValue("cookie1") String c1) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + h1 + "-" + c1);
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.*")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}

	@Controller
	static class MyAdaptedControllerBase<T> {

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") T p1, int param2, @RequestHeader Integer header1,
				@CookieValue int cookie1, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + header1 + "-" + cookie1);
		}

		@InitBinder
		public void initBinder(@RequestParam("param1") String p1,
				@RequestParam(value="paramX", required=false) String px, int param2) {

			assertThat(px).isNull();
		}

		@ModelAttribute
		public void modelAttribute(@RequestParam("param1") String p1,
				@RequestParam(value="paramX", required=false) String px, int param2) {

			assertThat(px).isNull();
		}
	}

	@RequestMapping("/*.do")
	static class MyAdaptedController3 extends MyAdaptedControllerBase<String> {

		@RequestMapping
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.getWriter().write("test");
		}

		@Override
		public void myHandle(@RequestParam("param1") String p1, int param2, @RequestHeader Integer header1,
				@CookieValue int cookie1, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + header1 + "-" + cookie1);
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.*")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}

		@Override
		@InitBinder
		public void initBinder(@RequestParam("param1") String p1,
				@RequestParam(value="paramX", required=false) String px, int param2) {

			assertThat(px).isNull();
		}

		@Override
		@ModelAttribute
		public void modelAttribute(@RequestParam("param1") String p1,
				@RequestParam(value="paramX", required=false) String px, int param2) {

			assertThat(px).isNull();
		}
	}

	@Controller
	@RequestMapping(method = RequestMethod.GET)
	static class EmptyParameterListHandlerMethodController {

		static boolean called;

		@RequestMapping("/emptyParameterListHandler")
		public void emptyParameterListHandler() {
			EmptyParameterListHandlerMethodController.called = true;
		}

		@RequestMapping("/nonEmptyParameterListHandler")
		public void nonEmptyParameterListHandler(HttpServletResponse response) {
		}
	}

	@Controller
	@RequestMapping("/myPage")
	@SessionAttributes(names = { "object1", "object2" })
	public static class MySessionAttributesController {

		@RequestMapping(method = RequestMethod.GET)
		public String get(Model model) {
			model.addAttribute("object1", new Object());
			model.addAttribute("object2", new Object());
			return "page1";
		}

		@RequestMapping(method = RequestMethod.POST)
		public String post(@ModelAttribute("object1") Object object1) {
			//do something with object1
			return "page2";

		}
	}

	@RequestMapping("/myPage")
	@SessionAttributes({"object1", "object2"})
	@Controller
	public interface MySessionAttributesControllerIfc {

		@RequestMapping(method = RequestMethod.GET)
		String get(Model model);

		@RequestMapping(method = RequestMethod.POST)
		String post(@ModelAttribute("object1") Object object1);
	}

	public static class MySessionAttributesControllerImpl implements MySessionAttributesControllerIfc {

		@Override
		public String get(Model model) {
			model.addAttribute("object1", new Object());
			model.addAttribute("object2", new Object());
			return "page1";
		}

		@Override
		public String post(@ModelAttribute("object1") Object object1) {
			//do something with object1
			return "page2";
		}
	}

	@RequestMapping("/myPage")
	@SessionAttributes({"object1", "object2"})
	public interface MyParameterizedControllerIfc<T> {

		@ModelAttribute("testBeanList")
		List<TestBean> getTestBeans();

		@RequestMapping(method = RequestMethod.GET)
		String get(Model model);
	}

	public interface MyEditableParameterizedControllerIfc<T> extends MyParameterizedControllerIfc<T> {

		@RequestMapping(method = RequestMethod.POST)
		String post(@ModelAttribute("object1") T object);
	}

	@Controller
	public static class MyParameterizedControllerImpl implements MyEditableParameterizedControllerIfc<TestBean> {

		@Override
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@Override
		public String get(Model model) {
			model.addAttribute("object1", new TestBean());
			model.addAttribute("object2", new TestBean());
			return "page1";
		}

		@Override
		public String post(TestBean object) {
			//do something with object1
			return "page2";
		}
	}

	@Controller
	public static class MyParameterizedControllerImplWithOverriddenMappings
			implements MyEditableParameterizedControllerIfc<TestBean> {

		@Override
		@ModelAttribute("testBeanList")
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@Override
		@RequestMapping(method = RequestMethod.GET)
		public String get(Model model) {
			model.addAttribute("object1", new TestBean());
			model.addAttribute("object2", new TestBean());
			return "page1";
		}

		@Override
		@RequestMapping(method = RequestMethod.POST)
		public String post(@ModelAttribute("object1") TestBean object1) {
			//do something with object1
			return "page2";
		}
	}

	@Controller
	public static class MyFormController {

		@ModelAttribute("testBeanList")
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, ModelMap model) {
			FieldError error = errors.getFieldError("age");
			assertThat(error).as("Must have field error for age property").isNotNull();
			assertThat(error.getRejectedValue()).isEqualTo("value2");
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}
	}

	public static class ValidTestBean extends TestBean {

		@NotNull
		private String validCountry;

		public void setValidCountry(String validCountry) {
			this.validCountry = validCountry;
		}

		public String getValidCountry() {
			return this.validCountry;
		}
	}

	@Controller
	public static class MyModelFormController {

		@ModelAttribute
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@RequestMapping("/myPath.do")
		@ModelAttribute("yourKey")
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, Model model) {
			if (!model.containsAttribute("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "yourValue";
		}
	}

	@Controller
	public static class LateBindingFormController {

		@ModelAttribute("testBeanList")
		public List<TestBean> getTestBeans(@ModelAttribute(name="myCommand", binding=false) TestBean tb) {
			List<TestBean> list = new LinkedList<>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute(name="myCommand", binding=true) TestBean tb,
				BindingResult errors, ModelMap model) {

			FieldError error = errors.getFieldError("age");
			assertThat(error).as("Must have field error for age property").isNotNull();
			assertThat(error.getRejectedValue()).isEqualTo("value2");
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}
	}

	@Controller
	static class MyCommandProvidingFormController<T, TB, TB2> extends MyFormController {

		@ModelAttribute("myCommand")
		public ValidTestBean createTestBean(@RequestParam T defaultName, Map<String, Object> model,
				@RequestParam Date date) {

			model.put("myKey", "myOriginalValue");
			ValidTestBean tb = new ValidTestBean();
			tb.setName(defaultName.getClass().getSimpleName() + ":" + defaultName.toString());
			return tb;
		}

		@Override
		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute("myCommand") @Valid TestBean tb, BindingResult errors, ModelMap model) {
			if (!errors.hasFieldErrors("validCountry")) {
				throw new IllegalStateException("Declarative validation not applied");
			}
			return super.myHandle(tb, errors, model);
		}

		@RequestMapping("/myOtherPath.do")
		public String myOtherHandle(TB tb, BindingResult errors, ExtendedModelMap model, MySpecialArg arg) {
			TestBean tbReal = (TestBean) tb;
			tbReal.setName("myName");
			boolean condition = model.get("ITestBean") instanceof DerivedTestBean;
			assertThat(condition).isTrue();
			assertThat(arg).isNotNull();
			return super.myHandle(tbReal, errors, model);
		}

		@RequestMapping("/myThirdPath.do")
		public String myThirdHandle(TB tb, Model model) {
			model.addAttribute("testBean", new TestBean("special", 99));
			return "myView";
		}

		@SuppressWarnings("unchecked")
		@ModelAttribute
		protected TB2 getModelAttr() {
			return (TB2) new DerivedTestBean();
		}
	}

	static class MySpecialArg {

		public MySpecialArg(String value) {
		}
	}

	@Controller
	static class MyTypedCommandProvidingFormController
			extends MyCommandProvidingFormController<Integer, TestBean, ITestBean> {

	}

	@Controller
	static class MyBinderInitializingCommandProvidingFormController
			extends MyCommandProvidingFormController<String, TestBean, ITestBean> {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.initBeanPropertyAccess();
			binder.setRequiredFields("sex");
			LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
			vf.afterPropertiesSet();
			binder.setValidator(vf);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@Override
		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute("myCommand") @Valid TestBean tb, BindingResult errors, ModelMap model) {
			if (!errors.hasFieldErrors("sex")) {
				throw new IllegalStateException("requiredFields not applied");
			}
			return super.myHandle(tb, errors, model);
		}
	}

	@Controller
	static class MySpecificBinderInitializingCommandProvidingFormController
			extends MyCommandProvidingFormController<String, TestBean, ITestBean> {

		@InitBinder({"myCommand", "date"})
		public void initBinder(WebDataBinder binder, String date, @RequestParam("date") String[] date2) {
			LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
			vf.afterPropertiesSet();
			binder.setValidator(vf);
			assertThat(date).isEqualTo("2007-10-02");
			assertThat(date2.length).isEqualTo(1);
			assertThat(date2[0]).isEqualTo("2007-10-02");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}

	static class MyWebBindingInitializer implements WebBindingInitializer {

		@Override
		public void initBinder(WebDataBinder binder) {
			LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
			vf.afterPropertiesSet();
			binder.setValidator(vf);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}

	static class MySpecialArgumentResolver implements WebArgumentResolver {

		@Override
		public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) {
			if (methodParameter.getParameterType().equals(MySpecialArg.class)) {
				return new MySpecialArg("myValue");
			}
			return UNRESOLVED;
		}
	}

	@Controller
	@RequestMapping("/myPath.do")
	static class MyParameterDispatchingController implements Serializable {

		private static final long serialVersionUID = 1L;

		@Autowired
		private transient ServletContext servletContext;

		@Autowired
		private transient ServletConfig servletConfig;

		@Autowired
		private HttpSession session;

		@Autowired
		private HttpServletRequest request;

		@Autowired
		private WebRequest webRequest;

		@RequestMapping
		public void myHandle(HttpServletResponse response, HttpServletRequest request) throws IOException {
			if (this.servletContext == null || this.servletConfig == null || this.session == null ||
					this.request == null || this.webRequest == null) {
				throw new IllegalStateException();
			}
			response.getWriter().write("myView");
			request.setAttribute("servletContext", this.servletContext);
			request.setAttribute("servletConfig", this.servletConfig);
			request.setAttribute("sessionId", this.session.getId());
			request.setAttribute("requestUri", this.request.getRequestURI());
			request.setAttribute("locale", this.webRequest.getLocale());
		}

		@RequestMapping(params = {"view", "!lang"})
		public void myOtherHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping(method = RequestMethod.GET, params = {"view=my", "lang=de"})
		public void myLangHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}

		@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, params = "surprise")
		public void mySurpriseHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("mySurpriseView");
		}
	}

	@Controller
	@RequestMapping(value = "/myPath.do", params = {"active"})
	static class MyConstrainedParameterDispatchingController {

		@RequestMapping(params = {"view", "!lang"})
		public void myOtherHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping(method = RequestMethod.GET, params = {"view=my", "lang=de"})
		public void myLangHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}
	}

	@Controller
	@RequestMapping("/myApp/*")
	static class MyRelativePathDispatchingController {

		@RequestMapping
		public void myHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myView");
		}

		@RequestMapping("*Other")
		public void myOtherHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping("myLang")
		public void myLangHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}

		@RequestMapping("surprise")
		public void mySurpriseHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("mySurpriseView");
		}
	}

	@Controller
	static class MyRelativeMethodPathDispatchingController {

		@RequestMapping("*/myHandle") // was **/myHandle
		public void myHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myView");
		}

		@RequestMapping("/*/*Other") // was /**/*Other
		public void myOtherHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping("*/myLang") // was **/myLang
		public void myLangHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}

		@RequestMapping("/*/surprise") // was /**/surprise
		public void mySurpriseHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("mySurpriseView");
		}
	}

	@Controller
	static class MyNullCommandController {

		@ModelAttribute
		public TestBean getTestBean() {
			return null;
		}

		@ModelAttribute
		public Principal getPrincipal() {
			return new TestPrincipal("test");
		}

		@RequestMapping("/myPath")
		public void handle(@ModelAttribute TestBean testBean,
				Errors errors,
				@ModelAttribute TestPrincipal modelPrinc,
				OtherPrincipal requestPrinc,
				Writer writer) throws IOException {
			assertThat(testBean).isNull();
			assertThat(modelPrinc).isNotNull();
			assertThat(requestPrinc).isNotNull();
			assertThat(errors.hasErrors()).isFalse();
			errors.reject("myCode");
			writer.write("myView");
		}
	}

	static class OtherPrincipal implements Principal {

		@Override
		public String getName() {
			return "other";
		}
	}

	static class TestViewResolver implements ViewResolver {

		@Override
		public View resolveViewName(final String viewName, Locale locale) throws Exception {
			return new View() {
				@Override
				public String getContentType() {
					return null;
				}
				@Override
				@SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
				public void render(@Nullable Map model, HttpServletRequest request, HttpServletResponse response)
						throws Exception {
					TestBean tb = (TestBean) model.get("testBean");
					if (tb == null) {
						tb = (TestBean) model.get("myCommand");
					}
					if (tb.getName() != null && tb.getName().endsWith("myDefaultName")) {
						assertThat(tb.getDate().getYear()).isEqualTo(107);
					}
					Errors errors = (Errors) model.get(BindingResult.MODEL_KEY_PREFIX + "testBean");
					if (errors == null) {
						errors = (Errors) model.get(BindingResult.MODEL_KEY_PREFIX + "myCommand");
					}
					if (errors.hasFieldErrors("date")) {
						throw new IllegalStateException();
					}
					if (model.containsKey("ITestBean")) {
						boolean condition = model.get(BindingResult.MODEL_KEY_PREFIX + "ITestBean") instanceof Errors;
						assertThat(condition).isTrue();
					}
					List<TestBean> testBeans = (List<TestBean>) model.get("testBeanList");
					if (errors.hasFieldErrors("age")) {
						response.getWriter()
								.write(viewName + "-" + tb.getName() + "-" + errors.getFieldError("age").getCode() +
										"-" + testBeans.get(0).getName() + "-" + model.get("myKey") +
										(model.containsKey("yourKey") ? "-" + model.get("yourKey") : ""));
					}
					else {
						response.getWriter().write(viewName + "-" + tb.getName() + "-" + tb.getAge() + "-" +
								errors.getFieldValue("name") + "-" + errors.getFieldValue("age"));
					}
				}
			};
		}
	}

	public static class ModelExposingViewResolver implements ViewResolver {

		@Override
		public View resolveViewName(final String viewName, Locale locale) throws Exception {
			return new View() {
				@Override
				public String getContentType() {
					return null;
				}
				@Override
				public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
					request.setAttribute("viewName", viewName);
					request.getSession().setAttribute("model", model);
				}
			};
		}
	}

	public static class ParentController {

		@RequestMapping(method = RequestMethod.GET)
		public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		}
	}

	@Controller
	@RequestMapping("/child/test")
	public static class ChildController extends ParentController {

		@RequestMapping(method = RequestMethod.GET)
		public void doGet(HttpServletRequest req, HttpServletResponse resp, @RequestParam("childId") String id) {
		}
	}

	@Controller
	// @RequestMapping intentionally omitted
	static class UnmappedPathController {

		@GetMapping // path intentionally omitted
		public void get(Writer writer) throws IOException {
			writer.write("get");
		}
	}

	@Controller
	// @RequestMapping intentionally omitted
	static class ExplicitAndEmptyPathsController {

		@GetMapping({"/", ""})
		public void get(Writer writer) throws IOException {
			writer.write("get");
		}
	}

	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Controller
	public @interface MyControllerAnnotation {
	}

	@MyControllerAnnotation
	public static class CustomAnnotationController {

		@RequestMapping("/myPath.do")
		public void myHandle() {
		}
	}

	@Controller
	public static class RequiredParamController {

		@RequestMapping("/myPath.do")
		public void myHandle(@RequestParam(value = "id", required = true) int id,
				@RequestHeader(value = "header", required = true) String header) {
		}
	}

	@Controller
	public static class OptionalParamController {

		@RequestMapping("/myPath.do")
		public void myHandle(@RequestParam(required = false) String id,
				@RequestParam(required = false) boolean flag,
				@RequestHeader(value = "header", required = false) String header,
				HttpServletResponse response) throws IOException {
			response.getWriter().write(String.valueOf(id) + "-" + flag + "-" + String.valueOf(header));
		}
	}

	@Controller
	public static class DefaultValueParamController {

		@RequestMapping("/myPath.do")
		public void myHandle(@RequestParam(value = "id", defaultValue = "foo") String id,
				@RequestParam(value = "otherId", defaultValue = "") String id2,
				@RequestHeader(defaultValue = "bar") String header,
				HttpServletResponse response) throws IOException {
			response.getWriter().write(String.valueOf(id) + "-" + String.valueOf(id2) + "-" + String.valueOf(header));
		}
	}

	@Controller
	public static class DefaultExpressionValueParamController {

		@RequestMapping("/myPath.do")
		public void myHandle(@RequestParam(value = "id", defaultValue = "${myKey}") String id,
				@RequestHeader(defaultValue = "#{systemProperties.myHeader}") String header,
				@Value("#{request.contextPath}") String contextPath,
				HttpServletResponse response) throws IOException {
			response.getWriter().write(String.valueOf(id) + "-" + String.valueOf(header) + "-" + contextPath);
		}
	}

	@Controller
	public static class NestedSetController {

		@RequestMapping("/myPath.do")
		public void myHandle(GenericBean<?> gb, HttpServletResponse response) throws Exception {
			response.getWriter().write(gb.getTestBeanSet().toString() + "-" +
					gb.getTestBeanSet().iterator().next().getClass().getName());
		}
	}

	public static class TestBeanConverter implements Converter<String, ITestBean> {

		@Override
		public ITestBean convert(String source) {
			return new TestBean(source);
		}
	}

	@Controller
	public static class PathVariableWithCustomConverterController {

		@RequestMapping("/myPath/{id}")
		public void myHandle(@PathVariable("id") ITestBean bean) throws Exception {
		}
	}

	public static class AnnotatedExceptionRaisingConverter implements Converter<String, ITestBean> {

		@Override
		public ITestBean convert(String source) {
			throw new NotFoundException();
		}

		@ResponseStatus(HttpStatus.NOT_FOUND)
		@SuppressWarnings("serial")
		private static class NotFoundException extends RuntimeException {
		}
	}

	@Controller
	public static class MethodNotAllowedController {

		@RequestMapping(value = "/myPath.do", method = RequestMethod.DELETE)
		public void delete() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.HEAD)
		public void head() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.OPTIONS)
		public void options() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.POST)
		public void post() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.PUT)
		public void put() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.TRACE)
		public void trace() {
		}

		@RequestMapping(value = "/otherPath.do", method = RequestMethod.GET)
		public void get() {
		}
	}

	@Controller
	public static class PathOrderingController {

		@RequestMapping(value = {"/dir/myPath1.do", "/*/*.do"})
		public void method1(Writer writer) throws IOException {
			writer.write("method1");
		}

		@RequestMapping("/dir/*.do")
		public void method2(Writer writer) throws IOException {
			writer.write("method2");
		}
	}

	@Controller
	public static class RequestResponseBodyController {

		@RequestMapping(value = "/something", method = RequestMethod.PUT)
		@ResponseBody
		public String handle(@RequestBody String body) throws IOException {
			return body;
		}

		@RequestMapping(value = "/something", method = RequestMethod.PATCH)
		@ResponseBody
		public String handlePartialUpdate(@RequestBody String content) throws IOException {
			return content;
		}
	}

	@Controller
	public static class RequestResponseBodyProducesController {

		@RequestMapping(value = "/something", method = RequestMethod.PUT, produces = "text/plain")
		@ResponseBody
		public String handle(@RequestBody String body) throws IOException {
			return body;
		}
	}

	@Controller
	public static class ResponseBodyVoidController {

		@RequestMapping("/something")
		@ResponseBody
		public void handle() throws IOException {
		}
	}

	@Controller
	public static class RequestBodyArgMismatchController {

		@RequestMapping(value = "/something", method = RequestMethod.PUT)
		public void handle(@RequestBody A a) throws IOException {
		}
	}

	@XmlRootElement
	public static class A {
	}

	@XmlRootElement
	public static class B {
	}

	public static class NotReadableMessageConverter implements HttpMessageConverter<Object> {

		@Override
		public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
			return true;
		}

		@Override
		public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
			return true;
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return Collections.singletonList(new MediaType("application", "pdf"));
		}

		@Override
		public Object read(Class<?> clazz, HttpInputMessage inputMessage) {
			throw new HttpMessageNotReadableException("Could not read", inputMessage);
		}

		@Override
		public void write(Object o, @Nullable MediaType contentType, HttpOutputMessage outputMessage) {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

	public static class SimpleMessageConverter implements HttpMessageConverter<Object> {

		private final List<MediaType> supportedMediaTypes;

		public SimpleMessageConverter(MediaType... supportedMediaTypes) {
			this.supportedMediaTypes = Arrays.asList(supportedMediaTypes);
		}

		@Override
		public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
			return supportedMediaTypes.contains(mediaType);
		}

		@Override
		public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
			return supportedMediaTypes.contains(mediaType);
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return supportedMediaTypes;
		}

		@Override
		public Object read(Class<?> clazz, HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			return null;
		}

		@Override
		public void write(Object o, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {
			outputMessage.getHeaders().setContentType(contentType);
			outputMessage.getBody(); // force a header write
		}
	}

	@Controller
	public static class ContentTypeHeadersController {

		@RequestMapping(value = "/something", headers = "content-type=application/pdf")
		public void handlePdf(Writer writer) throws IOException {
			writer.write("pdf");
		}

		@RequestMapping(value = "/something", headers = "content-type=text/*")
		public void handleHtml(Writer writer) throws IOException {
			writer.write("text");
		}
	}

	@Controller
	public static class ConsumesController {

		@RequestMapping(value = "/something", consumes = "application/pdf")
		public void handlePdf(Writer writer) throws IOException {
			writer.write("pdf");
		}

		@RequestMapping(value = "/something", consumes = "text/*")
		public void handleHtml(Writer writer) throws IOException {
			writer.write("text");
		}
	}

	@Controller
	public static class NegatedContentTypeHeadersController {

		@RequestMapping(value = "/something", headers = "content-type=application/pdf")
		public void handlePdf(Writer writer) throws IOException {
			writer.write("pdf");
		}

		@RequestMapping(value = "/something", headers = "content-type!=application/pdf")
		public void handleNonPdf(Writer writer) throws IOException {
			writer.write("non-pdf");
		}

	}

	@Controller
	public static class AcceptHeadersController {

		@RequestMapping(value = "/something", headers = "accept=text/html")
		public void handleHtml(Writer writer) throws IOException {
			writer.write("html");
		}

		@RequestMapping(value = "/something", headers = "accept=application/xml")
		public void handleXml(Writer writer) throws IOException {
			writer.write("xml");
		}
	}

	@Controller
	public static class ProducesController {

		@GetMapping(path = "/something", produces = "text/html")
		public void handleHtml(Writer writer) throws IOException {
			writer.write("html");
		}

		@GetMapping(path = "/something", produces = "application/xml")
		public void handleXml(Writer writer) throws IOException {
			writer.write("xml");
		}

		@GetMapping(path = "/something", produces = "text/csv")
		public String handleCsv() {
			throw new IllegalArgumentException();
		}

		@ExceptionHandler
		public ResponseEntity<Map<String, String>> handle(IllegalArgumentException ex) {
			return ResponseEntity.status(500).body(Collections.singletonMap("reason", "error"));
		}
	}

	@Controller
	public static class ResponseStatusController {

		@RequestMapping("/something")
		@ResponseStatus(code = HttpStatus.CREATED, reason = "It's alive!")
		public void handle(Writer writer) throws IOException {
			writer.write("something");
		}
	}

	@Controller
	public static class ModelAndViewResolverController {

		@RequestMapping("/")
		public MySpecialArg handle() {
			return new MySpecialArg("foo");
		}
	}

	public static class MyModelAndViewResolver implements ModelAndViewResolver {

		@Override
		public ModelAndView resolveModelAndView(Method handlerMethod, Class<?> handlerType, Object returnValue,
				ExtendedModelMap implicitModel, NativeWebRequest webRequest) {

			if (returnValue instanceof MySpecialArg) {
				return new ModelAndView(new View() {
					@Override
					public String getContentType() {
						return "text/html";
					}
					@Override
					public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
							throws Exception {
						response.getWriter().write("myValue");
					}
				});
			}
			return UNRESOLVED;
		}
	}

	@Controller
	@RequestMapping("/test*")
	static class AmbiguousParamsController {

		@RequestMapping(method = RequestMethod.GET)
		public void noParams(Writer writer) throws IOException {
			writer.write("noParams");
		}

		@RequestMapping(params = "myParam")
		public void param(@RequestParam("myParam") int myParam, Writer writer) throws IOException {
			writer.write("myParam-" + myParam);
		}
	}

	@Controller
	static class AmbiguousPathAndRequestMethodController {

		@RequestMapping(value = "/bug/EXISTING", method = RequestMethod.POST)
		public void directMatch(Writer writer) throws IOException {
			writer.write("Direct");
		}

		@RequestMapping(value = "/bug/{type}", method = RequestMethod.GET)
		public void patternMatch(Writer writer) throws IOException {
			writer.write("Pattern");
		}
	}

	@Controller
	@RequestMapping("/test*")
	public static class BindingCookieValueController {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.initBeanPropertyAccess();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@RequestMapping(method = RequestMethod.GET)
		public void handle(@CookieValue("date") Date date, Writer writer) throws IOException {
			assertThat(date).as("Invalid path variable value").isEqualTo(new GregorianCalendar(2008, 10, 18).getTime());
			writer.write("test-" + new SimpleDateFormat("yyyy").format(date));
		}
	}

	public interface TestController<T> {

		ModelAndView method(T object);
	}

	public static class MyEntity {
	}

	@Controller
	public static class TestControllerImpl implements TestController<MyEntity> {

		@Override
		@RequestMapping("/method")
		public ModelAndView method(MyEntity object) {
			return new ModelAndView("/something");
		}
	}

	@RestController
	@RequestMapping(path = ApiConstants.ARTICLES_PATH)
	public static class ArticleController implements ApiConstants, ResourceEndpoint<Article, ArticlePredicate> {

		@Override
		@GetMapping(params = "page")
		public Collection<Article> find(String pageable, ArticlePredicate predicate) {
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		@GetMapping
		public List<Article> find(boolean sort, ArticlePredicate predicate) {
			throw new UnsupportedOperationException("not implemented");
		}
	}

	interface ApiConstants {

		String API_V1 = "/v1";

		String ARTICLES_PATH = API_V1 + "/articles";
	}

	public interface ResourceEndpoint<E extends Entity, P extends EntityPredicate<?>> {

		Collection<E> find(String pageable, P predicate) throws IOException;

		List<E> find(boolean sort, P predicate) throws IOException;
	}

	public static abstract class Entity {

		public UUID id;

		public String createdBy;

		public Instant createdDate;
	}

	public static class Article extends Entity {

		public String slug;

		public String title;

		public String content;
	}

	public static abstract class EntityPredicate<E extends Entity> {

		public String createdBy;

		public Instant createdBefore;

		public Instant createdAfter;

		public boolean accept(E entity) {
			return (createdBy == null || createdBy.equals(entity.createdBy)) &&
					(createdBefore == null || createdBefore.compareTo(entity.createdDate) >= 0) &&
					(createdAfter == null || createdAfter.compareTo(entity.createdDate) >= 0);
		}
	}

	public static class ArticlePredicate extends EntityPredicate<Article> {

		public String query;

		@Override
		public boolean accept(Article entity) {
			return super.accept(entity) && (query == null || (entity.title.contains(query) || entity.content.contains(query)));
		}
	}

	@Controller
	public static class RequestParamMapController {

		@RequestMapping("/map")
		public void map(@RequestParam Map<String, String> params, Writer writer) throws IOException {
			for (Iterator<Map.Entry<String, String>> it = params.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, String> entry = it.next();
				writer.write(entry.getKey() + "=" + entry.getValue());
				if (it.hasNext()) {
					writer.write(',');
				}

			}
		}

		@RequestMapping("/multiValueMap")
		public void multiValueMap(@RequestParam MultiValueMap<String, String> params, Writer writer) throws IOException {
			for (Iterator<Map.Entry<String, List<String>>> it1 = params.entrySet().iterator(); it1.hasNext();) {
				Map.Entry<String, List<String>> entry = it1.next();
				writer.write(entry.getKey() + "=[");
				for (Iterator<String> it2 = entry.getValue().iterator(); it2.hasNext();) {
					String value = it2.next();
					writer.write(value);
					if (it2.hasNext()) {
						writer.write(',');
					}
				}
				writer.write(']');
				if (it1.hasNext()) {
					writer.write(',');
				}
			}
		}
	}

	@Controller
	public static class RequestHeaderMapController {

		@RequestMapping("/map")
		public void map(@RequestHeader Map<String, String> headers, Writer writer) throws IOException {
			for (Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, String> entry = it.next();
				writer.write(entry.getKey() + "=" + entry.getValue());
				if (it.hasNext()) {
					writer.write(',');
				}

			}
		}

		@RequestMapping("/multiValueMap")
		public void multiValueMap(@RequestHeader MultiValueMap<String, String> headers, Writer writer)
				throws IOException {
			for (Iterator<Map.Entry<String, List<String>>> it1 = headers.entrySet().iterator(); it1.hasNext();) {
				Map.Entry<String, List<String>> entry = it1.next();
				writer.write(entry.getKey() + "=[");
				for (Iterator<String> it2 = entry.getValue().iterator(); it2.hasNext();) {
					String value = it2.next();
					writer.write(value);
					if (it2.hasNext()) {
						writer.write(',');
					}
				}
				writer.write(']');
				if (it1.hasNext()) {
					writer.write(',');
				}
			}
		}

		@RequestMapping("/httpHeaders")
		public void httpHeaders(@RequestHeader HttpHeaders headers, Writer writer) throws IOException {
			assertThat(headers.getContentType()).as("Invalid Content-Type").isEqualTo(new MediaType("text", "html"));
			multiValueMap(headers, writer);
		}

	}

	@Controller
	public interface IMyController {

		@RequestMapping("/handle")
		void handle(Writer writer, @RequestParam(value="p", required=false) String param) throws IOException;
	}

	@Controller
	public static class IMyControllerImpl implements IMyController {

		@Override
		public void handle(Writer writer, @RequestParam(value="p", required=false) String param) throws IOException {
			writer.write("handle " + param);
		}
	}

	public static abstract class MyAbstractController {

		@RequestMapping("/handle")
		public abstract void handle(Writer writer) throws IOException;
	}

	@Controller
	public static class MyAbstractControllerImpl extends MyAbstractController {

		@Override
		public void handle(Writer writer) throws IOException {
			writer.write("handle");
		}
	}

	@Controller
	public static class TrailingSlashController  {

		@RequestMapping(value = "/", method = RequestMethod.GET)
		public void root(Writer writer) throws IOException {
			writer.write("root");
		}

		@RequestMapping(value = "/{templatePath}/", method = RequestMethod.GET)
		public void templatePath(Writer writer) throws IOException {
			writer.write("templatePath");
		}
	}

	@Controller
	public static class ResponseEntityController {

		@PostMapping("/foo")
		public ResponseEntity<String> foo(HttpEntity<byte[]> requestEntity) throws Exception {
			assertThat(requestEntity).isNotNull();
			assertThat(requestEntity.getHeaders().getFirst("MyRequestHeader")).isEqualTo("MyValue");

			String body = new String(requestEntity.getBody(), "UTF-8");
			assertThat(body).isEqualTo("Hello World");

			URI location = new URI("/foo");
			return ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body(body);
		}

		@GetMapping("/bar")
		public ResponseEntity<Void> bar() {
			return ResponseEntity.notFound().header("MyResponseHeader", "MyValue").build();
		}

		@GetMapping("/baz")
		public ResponseEntity<String> baz() {
			return ResponseEntity.ok().header("MyResponseHeader", "MyValue").body("body");
		}

		@RequestMapping(path = "/stores", method = RequestMethod.HEAD)
		public ResponseEntity<Void> headResource() {
			return ResponseEntity.ok().header("h1", "v1").build();
		}

		@GetMapping("/stores")
		public ResponseEntity<String> getResource() {
			return ResponseEntity.ok().body("body");
		}

		@GetMapping("/test-entity")
		public ResponseEntity<TestEntity> testEntity() {
			TestEntity entity = new TestEntity();
			entity.setName("Foo Bar");
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(entity);
		}
	}

	@XmlRootElement
	static class TestEntity {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Controller
	public static class CustomMapEditorController {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.initBeanPropertyAccess();
			binder.registerCustomEditor(Map.class, new CustomMapEditor());
		}

		@SuppressWarnings("rawtypes")
		@RequestMapping("/handle")
		public void handle(@RequestParam("map") Map map, Writer writer) throws IOException {
			writer.write("test-" + map);
		}
	}

	public static class CustomMapEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			if (StringUtils.hasText(text)) {
				setValue(Collections.singletonMap("foo", text));
			}
			else {
				setValue(null);
			}
		}
	}

	@Controller
	public static class MultipartController {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.registerCustomEditor(String.class, new StringMultipartFileEditor());
		}

		@RequestMapping("/singleString")
		public void processMultipart(@RequestParam("content") String content, HttpServletResponse response)
				throws IOException {
			response.getWriter().write(content);
		}

		@RequestMapping("/stringArray")
		public void processMultipart(@RequestParam("content") String[] content, HttpServletResponse response)
				throws IOException {
			response.getWriter().write(StringUtils.arrayToDelimitedString(content, "-"));
		}
	}

	@Controller
	public static class CsvController {

		@RequestMapping("/singleInteger")
		public void processCsv(@RequestParam("content") Integer content, HttpServletResponse response) throws IOException {
			response.getWriter().write(content.toString());
		}

		@RequestMapping("/integerArray")
		public void processCsv(@RequestParam("content") Integer[] content, HttpServletResponse response) throws IOException {
			response.getWriter().write(StringUtils.arrayToDelimitedString(content, "-"));
		}
	}

	@Controller
	@RequestMapping("/t1")
	protected static class NoPathGetAndM2PostController {

		@RequestMapping(method = RequestMethod.GET)
		public void handle1(Writer writer) throws IOException {
			writer.write("handle1");
		}

		@RequestMapping(value = "/m2", method = RequestMethod.POST)
		public void handle2(Writer writer) throws IOException {
			writer.write("handle2");
		}
	}

	@Controller
	static class HeadersConditionController {

		@RequestMapping(value = "/", method = RequestMethod.GET)
		public String home() {
			return "home";
		}

		@RequestMapping(value = "/", method = RequestMethod.GET, headers="Accept=application/json")
		@ResponseBody
		public String homeJson() {
			return "homeJson";
		}
	}

	@Controller
	static class RedirectAttributesController {

		@InitBinder
		public void initBinder(WebDataBinder dataBinder) {
			dataBinder.setRequiredFields("name");
		}

		@GetMapping("/messages/{id}")
		public void message(ModelMap model, Writer writer) throws IOException {
			writer.write("Got: " + model.get("successMessage"));
		}

		@PostMapping("/messages")
		public String sendMessage(TestBean testBean, BindingResult result, RedirectAttributes attributes) {
			if (result.hasErrors()) {
				return "messages/new";
			}
			attributes.addAttribute("id", "1").addAttribute("name", "value");
			attributes.addFlashAttribute("successMessage", "yay!");
			return "redirect:/messages/{id}";
		}

		@PostMapping("/messages-response-entity")
		public ResponseEntity<Void> sendMessage(RedirectAttributes attributes) {
			attributes.addFlashAttribute("successMessage", "yay!");
			URI location = URI.create("/messages/1?name=value");
			return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
		}
	}

	@Controller
	static class PrototypeController {

		private int count;

		@InitBinder
		public void initBinder(WebDataBinder dataBinder) {
			this.count++;
		}

		@ModelAttribute
		public void populate(Model model) {
			this.count++;
		}

		@RequestMapping("/")
		public void message(int param, Writer writer) throws IOException {
			this.count++;
			writer.write("count:" + this.count);
		}
	}

	@RestController
	static class ThisWillActuallyRun {

		@RequestMapping(value = "/", method = RequestMethod.GET)
		public String home() {
			return "Hello World!";
		}
	}

	@Controller
	static class HttpHeadersResponseController {

		@RequestMapping(value = "", method = RequestMethod.POST)
		@ResponseStatus(HttpStatus.CREATED)
		public HttpHeaders create() throws URISyntaxException {
			HttpHeaders headers = new HttpHeaders();
			headers.setLocation(new URI("/test/items/123"));
			return headers;
		}

		@RequestMapping(value = "empty", method = RequestMethod.POST)
		@ResponseStatus(HttpStatus.CREATED)
		public HttpHeaders createNoHeader() {
			return new HttpHeaders();
		}
	}

	@RestController
	public static class TextRestController {

		@RequestMapping(path = "/a1", method = RequestMethod.GET)
		public String a1(@RequestBody String body) {
			return body;
		}

		@RequestMapping(path = "/a2.html", method = RequestMethod.GET)
		public String a2(@RequestBody String body) {
			return body;
		}

		@RequestMapping(path = "/a3", method = RequestMethod.GET, produces = "text/html")
		public String a3(@RequestBody String body) throws IOException {
			return body;
		}

		@RequestMapping(path = "/a4.css", method = RequestMethod.GET)
		public String a4(@RequestBody String body) {
			return body;
		}
	}

	@Controller
	public static class ModelAndViewController {

		@RequestMapping("/path")
		public ModelAndView methodWithHttpStatus(MyEntity object) {
			return new ModelAndView("view", HttpStatus.UNPROCESSABLE_ENTITY);
		}

		@RequestMapping("/exception")
		public void raiseException() throws Exception {
			throw new TestException();
		}

		@ExceptionHandler(TestException.class)
		public ModelAndView handleException() {
			return new ModelAndView("view", HttpStatus.UNPROCESSABLE_ENTITY);
		}

		@SuppressWarnings("serial")
		private static class TestException extends Exception {
		}
	}

	public static class DataClass {

		@NotNull
		public final String param1;

		public final boolean param2;

		public int param3;

		@ConstructorProperties({"param1", "param2", "optionalParam"})
		public DataClass(String param1, boolean p2, Optional<Integer> optionalParam) {
			this.param1 = param1;
			this.param2 = p2;
			Assert.notNull(optionalParam, "Optional must not be null");
			optionalParam.ifPresent(integer -> this.param3 = integer);
		}

		public void setParam3(int param3) {
			this.param3 = param3;
		}
	}

	@RestController
	public static class DataClassController {

		@RequestMapping("/bind")
		public String handle(DataClass data) {
			return data.param1 + "-" + data.param2 + "-" + data.param3;
		}
	}

	@RestController
	public static class ValidatedDataClassController {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.initDirectFieldAccess();
			binder.setConversionService(new DefaultFormattingConversionService());
			LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
			vf.afterPropertiesSet();
			binder.setValidator(vf);
		}

		@RequestMapping("/bind")
		public BindStatusView handle(@Valid DataClass data, BindingResult result) {
			if (result.hasErrors()) {
				return new BindStatusView(result.getErrorCount() + ":" + result.getFieldValue("param1") + "-" +
						result.getFieldValue("param2") + "-" + result.getFieldValue("param3"));
			}
			return new BindStatusView(data.param1 + "-" + data.param2 + "-" + data.param3);
		}
	}

	public static class BindStatusView extends AbstractView {

		private final String content;

		public BindStatusView(String content) {
			this.content = content;
		}

		@Override
		protected void renderMergedOutputModel(
				Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
			RequestContext rc = new RequestContext(request, model);
			rc.getBindStatus("dataClass");
			rc.getBindStatus("dataClass.param1");
			rc.getBindStatus("dataClass.param2");
			rc.getBindStatus("dataClass.param3");
			response.getWriter().write(this.content);
		}
	}

	@RestController
	public static class OptionalDataClassController {

		@RequestMapping("/bind")
		public String handle(Optional<DataClass> optionalData, BindingResult result) {
			if (result.hasErrors()) {
				assertThat(optionalData).isNotNull();
				assertThat(optionalData.isPresent()).isFalse();
				return result.getFieldValue("param1") + "-" + result.getFieldValue("param2") + "-" +
						result.getFieldValue("param3");
			}
			return optionalData.map(data -> data.param1 + "-" + data.param2 + "-" + data.param3).orElse("");
		}
	}

	public static class DateClass {

		@DateTimeFormat(pattern = "yyyy-MM-dd")
		public LocalDate date;

		public DateClass(LocalDate date) {
			this.date = date;
		}
	}

	@RestController
	public static class DateClassController {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.initDirectFieldAccess();
			binder.setConversionService(new DefaultFormattingConversionService());
		}

		@RequestMapping("/bind")
		public String handle(DateClass data, BindingResult result) {
			if (result.hasErrors()) {
				return result.getFieldError().toString();
			}
			assertThat(data).isNotNull();
			assertThat(data.date).isNotNull();
			assertThat(data.date.getYear()).isEqualTo(2010);
			assertThat(data.date.getMonthValue()).isEqualTo(1);
			assertThat(data.date.getDayOfMonth()).isEqualTo(1);
			return result.getFieldValue("date").toString();
		}
	}

}
