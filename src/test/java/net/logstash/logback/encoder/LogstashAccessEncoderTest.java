/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.logback.encoder;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.time.FastDateFormat;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogstashAccessEncoderTest {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private LogstashAccessEncoder encoder;
    private ByteArrayOutputStream outputStream;
    
    @Before
    public void before() throws Exception {
        outputStream = new ByteArrayOutputStream();
        encoder = new LogstashAccessEncoder();
        encoder.init(outputStream);
    }
    
    @Test
    public void basicsAreIncluded() throws Exception {
        final long timestamp = System.currentTimeMillis();
        
        IAccessEvent event = mockBasicILoggingEvent();
        when(event.getTimeStamp()).thenReturn(timestamp);
        
        encoder.getFieldNames().setTimestamp("timestamp");
        
        encoder.start();
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(node.get("timestamp").textValue(), is(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").format
                (timestamp)));
        assertThat(node.get("@version").intValue(), is(1));
        assertThat(node.get("@message").textValue(), is(String.format("%s - %s [%s] \"%s\" %s %s", event.getRemoteHost(), event.getRemoteUser(),
                FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").format
                        (event.getTimeStamp()), event.getRequestURL(), event.getStatusCode(),
                event.getContentLength())));
        
        assertThat(node.get("@fields.method").textValue(), is(event.getMethod()));
        assertThat(node.get("@fields.protocol").textValue(), is(event.getProtocol()));
        assertThat(node.get("@fields.status_code").asInt(), is(event.getStatusCode()));
        assertThat(node.get("@fields.requested_url").textValue(), is(event.getRequestURL()));
        assertThat(node.get("@fields.requested_uri").textValue(), is(event.getRequestURI()));
        assertThat(node.get("@fields.remote_host").textValue(), is(event.getRemoteHost()));
        assertThat(node.get("@fields.HOSTNAME").textValue(), is(event.getRemoteHost()));
        assertThat(node.get("@fields.remote_user").textValue(), is(event.getRemoteUser()));
        assertThat(node.get("@fields.content_length").asLong(), is(event.getContentLength()));
        assertThat(node.get("@fields.elapsed_time").asLong(), is(event.getElapsedTime()));
        assertThat(node.get("@fields.request_headers"), is(nullValue()));
        assertThat(node.get("@fields.response_headers"), is(nullValue()));
        
    }
    
    @Test
    public void closePutsSeparatorAtTheEnd() throws Exception {
        IAccessEvent event = mockBasicILoggingEvent();
        
        encoder.start();
        encoder.doEncode(event);
        encoder.close();
        closeQuietly(outputStream);
        
        assertThat(outputStream.toString(), Matchers.endsWith(LINE_SEPARATOR));
    }
    
    @Test
    public void propertiesInContextAreIncluded() throws Exception {
        Map<String, String> propertyMap = new HashMap<String, String>();
        propertyMap.put("thing_one", "One");
        propertyMap.put("thing_two", "Three");
        
        final Context context = mock(Context.class);
        when(context.getCopyOfPropertyMap()).thenReturn(propertyMap);
        
        IAccessEvent event = mockBasicILoggingEvent();
        
        encoder.setContext(context);
        encoder.start();
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());
        
        assertThat(node.get("thing_one").textValue(), is("One"));
        assertThat(node.get("thing_two").textValue(), is("Three"));
    }
    
    @Test
    public void requestAndResponseHeadersAreIncluded() throws Exception {
        IAccessEvent event = mockBasicILoggingEvent();
        
        encoder.getFieldNames().setFieldsRequestHeaders("@fields.request_headers");
        encoder.getFieldNames().setFieldsResponseHeaders("@fields.response_headers");
        encoder.start();
        encoder.doEncode(event);
        closeQuietly(outputStream);
        
        JsonNode node = MAPPER.readTree(outputStream.toByteArray());

        assertThat(node.get("@fields.request_headers").size(), is(2));
        assertThat(node.get("@fields.request_headers").get("User-Agent").textValue(), is("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36"));
        assertThat(node.get("@fields.request_headers").get("Accept").textValue(), is("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
        assertThat(node.get("@fields.request_headers").get("Unknown"), is(nullValue()));
        
        assertThat(node.get("@fields.response_headers").size(), is(2));
        assertThat(node.get("@fields.response_headers").get("Content-Type").textValue(), is("text/html; charset=UTF-8"));
        assertThat(node.get("@fields.response_headers").get("Content-Length").textValue(), is("42"));
        assertThat(node.get("@fields.response_headers").get("Unknown"), is(nullValue()));
    }
    
    @Test
    public void immediateFlushIsSane() {
        encoder.setImmediateFlush(true);
        assertThat(encoder.isImmediateFlush(), is(true));
        
        encoder.setImmediateFlush(false);
        assertThat(encoder.isImmediateFlush(), is(false));
    }
    
    private IAccessEvent mockBasicILoggingEvent() {
        IAccessEvent event = mock(IAccessEvent.class);
        when(event.getContentLength()).thenReturn(123L);
        when(event.getMethod()).thenReturn("GET");
        when(event.getProtocol()).thenReturn("HTTPS");
        when(event.getRemoteHost()).thenReturn("123.123.123.123");
        when(event.getRemoteUser()).thenReturn("remote-user");
        when(event.getRequestURI()).thenReturn("/my/uri");
        when(event.getRequestURL()).thenReturn("https://123.123.123.123/my/uri");
        when(event.getStatusCode()).thenReturn(200);
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getElapsedTime()).thenReturn(246L);
        when(event.getRequestHeaderMap()).thenReturn(new HashMap<String, String>() {{
            put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
            put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        }});
        when(event.getResponseHeaderMap()).thenReturn(new HashMap<String, String>() {{
            put("Content-Type", "text/html; charset=UTF-8");
            put("Content-Length", "42");
        }});
        return event;
    }
    
}
