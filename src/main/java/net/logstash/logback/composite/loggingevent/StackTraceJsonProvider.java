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
package net.logstash.logback.composite.loggingevent;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;

public class StackTraceJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public static final String FIELD_STACK_TRACE = "stack_trace";

    /**
     * Used to format throwables as Strings.
     */
    private ThrowableHandlingConverter throwableConverter = new ExtendedThrowableProxyConverter();
    
    public StackTraceJsonProvider() {
        setFieldName(FIELD_STACK_TRACE);
    }
    
    @Override
    public void start() {
        this.throwableConverter.start();
        super.start();
    }
    
    @Override
    public void stop() {
        this.throwableConverter.stop();
        super.stop();
    }
    
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            JsonWritingUtils.writeStringField(generator, getFieldName(), throwableConverter.convert(event));
        }
    }
    
    public ThrowableHandlingConverter getThrowableConverter() {
        return throwableConverter;
    }
    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }
}
