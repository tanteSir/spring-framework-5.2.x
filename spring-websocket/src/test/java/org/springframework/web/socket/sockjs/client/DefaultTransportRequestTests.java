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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link DefaultTransportRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultTransportRequestTests {

	private static final Jackson2SockJsMessageCodec CODEC = new Jackson2SockJsMessageCodec();


	private SettableListenableFuture<WebSocketSession> connectFuture;

	private ListenableFutureCallback<WebSocketSession> connectCallback;

	private TestTransport webSocketTransport;

	private TestTransport xhrTransport;


	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setup() throws Exception {
		this.connectCallback = mock(ListenableFutureCallback.class);
		this.connectFuture = new SettableListenableFuture<>();
		this.connectFuture.addCallback(this.connectCallback);
		this.webSocketTransport = new TestTransport("WebSocketTestTransport");
		this.xhrTransport = new TestTransport("XhrTestTransport");
	}


	@Test
	public void connect() throws Exception {
		DefaultTransportRequest request = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		request.connect(null, this.connectFuture);
		WebSocketSession session = mock(WebSocketSession.class);
		this.webSocketTransport.getConnectCallback().onSuccess(session);
		assertThat(this.connectFuture.get()).isSameAs(session);
	}

	@Test
	public void fallbackAfterTransportError() throws Exception {
		DefaultTransportRequest request1 = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		DefaultTransportRequest request2 = createTransportRequest(this.xhrTransport, TransportType.XHR_STREAMING);
		request1.setFallbackRequest(request2);
		request1.connect(null, this.connectFuture);

		// Transport error => fallback
		this.webSocketTransport.getConnectCallback().onFailure(new IOException("Fake exception 1"));
		assertThat(this.connectFuture.isDone()).isFalse();
		assertThat(this.xhrTransport.invoked()).isTrue();

		// Transport error => no more fallback
		this.xhrTransport.getConnectCallback().onFailure(new IOException("Fake exception 2"));
		assertThat(this.connectFuture.isDone()).isTrue();
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(
				this.connectFuture::get)
			.withMessageContaining("Fake exception 2");
	}

	@Test
	public void fallbackAfterTimeout() throws Exception {
		TaskScheduler scheduler = mock(TaskScheduler.class);
		Runnable sessionCleanupTask = mock(Runnable.class);
		DefaultTransportRequest request1 = createTransportRequest(this.webSocketTransport, TransportType.WEBSOCKET);
		DefaultTransportRequest request2 = createTransportRequest(this.xhrTransport, TransportType.XHR_STREAMING);
		request1.setFallbackRequest(request2);
		request1.setTimeoutScheduler(scheduler);
		request1.addTimeoutTask(sessionCleanupTask);
		request1.connect(null, this.connectFuture);

		assertThat(this.webSocketTransport.invoked()).isTrue();
		assertThat(this.xhrTransport.invoked()).isFalse();

		// Get and invoke the scheduled timeout task
		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(scheduler).schedule(taskCaptor.capture(), any(Date.class));
		verifyNoMoreInteractions(scheduler);
		taskCaptor.getValue().run();

		assertThat(this.xhrTransport.invoked()).isTrue();
		verify(sessionCleanupTask).run();
	}

	protected DefaultTransportRequest createTransportRequest(Transport transport, TransportType type) throws Exception {
		SockJsUrlInfo urlInfo = new SockJsUrlInfo(new URI("https://example.com"));
		return new DefaultTransportRequest(urlInfo, new HttpHeaders(), new HttpHeaders(), transport, type, CODEC);
	}

}
