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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import pl.mjaron.tinyloki.*;

public class LokiHandler extends ExtHandler {

    private TinyLoki loki;
    private LokiConfig config;
    private final Map<Labels, ILogStream> streams = new HashMap();

    public LokiHandler(Settings lokiSettings, LokiConfig config) {
        this.loki = TinyLoki.open(lokiSettings);
        this.config = config;
        setLevel(Level.INFO);
        setFilter(null);
        setFormatter(new SimpleFormatter());
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
        Map<String, String> mdcPropertyMap = record.getMdcCopy();
        Labels labels = computeLabels(record, mdcPropertyMap);
        ILogStream stream;
        synchronized (this) {
            stream = (ILogStream)this.streams.get(labels);
            if (stream == null) {
                stream = this.loki.openStream(labels);
                this.streams.put(labels, stream);
            }
        }

        Labels structuredMetadata = Labels.of();
        structuredMetadata.l("level", record.getLevel().getName());
        if(record.getLoggerName() != null)
            structuredMetadata.l("logger", record.getLoggerName());
        if(record.getSourceClassName() != null)
            structuredMetadata.l("class", record.getSourceClassName());
        if(record.getSourceMethodName() != null)
            structuredMetadata.l("method", record.getSourceMethodName());
        structuredMetadata.l("line", record.getSourceLineNumber());
        if(record.getThreadName() != null)
            structuredMetadata.l("thread", record.getThreadName());
        for (Map.Entry<String, String> entry : mdcPropertyMap.entrySet()) {
            if(!labels.getMap().containsKey(entry.getKey())) {
                if(entry.getValue() != null && !entry.getValue().isEmpty()) {
                    structuredMetadata.l(entry.getKey(), entry.getValue());
                }
            }
        }
        stream.log(record.getMillis()*1000000L, formatted, structuredMetadata);
    }

    private Labels computeLabels(ExtLogRecord record, Map<String, String> mdcPropertyMap) {
        var labels = Labels.of();
        //TRACE , DEBUG , INFO , WARN , ERROR and FATAL
        labels.l("severity", record.getLevel().getName().toUpperCase());
        //labelSerializer.appendLabel("level", logLevel);
        //labelSerializer.appendLabel("logger", record.getLoggerName());
        //labelSerializer.appendLabel("thread", record.getThreadName());
        for (Map.Entry<String, String> entry : config.labels().entrySet()) {
            var val = entry.getValue();
            if(val.startsWith("$")) {
                var mdcKey = val.substring(1);
                if(mdcPropertyMap.containsKey(mdcKey)) {
                    var value = mdcPropertyMap.get(mdcKey);
                    if(value != null && !value.isEmpty()) {
                        labels.l(entry.getKey(), value);
                    }
                }
            }
        }
        return labels;
    }

    @Override
    public void flush() {
        try {
            this.loki.sync();
        } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws SecurityException {
        try {
            this.loki.closeSync();
        } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
        }
    }

}
