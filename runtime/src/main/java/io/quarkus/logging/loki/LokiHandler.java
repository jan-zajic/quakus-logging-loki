/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.logging.loki;

import io.quarkus.runtime.RuntimeValue;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import pl.tkowalcz.tjahzi.LabelSerializer;
import pl.tkowalcz.tjahzi.LabelSerializers;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziLogger;


/**
 * @author hrupp
 */
public class LokiHandler extends ExtHandler {
    private LoggingSystem loggingSystem;
    private TjahziLogger logger;

    public LokiHandler(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
        logger = loggingSystem.createLogger();
        loggingSystem.start();
    }

    @Override
    protected void doPublish(ExtLogRecord record) {
        final String formatted;
        final Formatter formatter = getFormatter();
        try {
            formatted = formatter.formatMessage(record); //.format format whole line
        } catch (Exception ex) {
            reportError("Formatting error", ex, ErrorManager.FORMAT_FAILURE);
            return;
        }
        if (formatted.length() == 0) {
            // nothing to write; don't bother
            return;
        }

        String logLevel = record.getLevel().getName();
        Map<String, String> mdcPropertyMap = record.getMdcCopy();

        LabelSerializer labelSerializer = LabelSerializers.threadLocal();
        labelSerializer.appendLabel("level", logLevel);
        labelSerializer.appendLabel("logger", record.getLoggerName());
        labelSerializer.appendLabel("thread", record.getThreadName());
        appendMdcLogLabels(labelSerializer, mdcPropertyMap);

        logger.log(
                record.getMillis(),
                0L,
                labelSerializer,
                ByteBuffer.wrap(formatted.getBytes(StandardCharsets.UTF_8))
        );
    }

    private void appendMdcLogLabels(LabelSerializer serializer,
                                    Map<String, String> mdcPropertyMap) {
        for(Map.Entry<String,String> entry : mdcPropertyMap.entrySet()) {
            serializer.appendLabel(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
        this.loggingSystem.close(1000, (t) -> {});
    }

}
