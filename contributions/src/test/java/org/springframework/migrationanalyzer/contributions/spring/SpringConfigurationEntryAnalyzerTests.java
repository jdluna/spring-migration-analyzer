/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.migrationanalyzer.contributions.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.migrationanalyzer.analyze.fs.FileSystemEntry;
import org.springframework.migrationanalyzer.analyze.fs.FileSystemEntry.Callback;
import org.springframework.migrationanalyzer.analyze.fs.FileSystemEntry.ExceptionCallback;
import org.springframework.migrationanalyzer.util.IoUtils;

public class SpringConfigurationEntryAnalyzerTests {

    private final StubSpringConfigurationClassValueAnalyzer classValueAnalyzer = new StubSpringConfigurationClassValueAnalyzer();

    @SuppressWarnings({ "rawtypes" })
    private final SpringConfigurationEntryAnalyzer analyzer = new SpringConfigurationEntryAnalyzer(
        new HashSet<SpringConfigurationClassValueAnalyzer>(Arrays.asList(this.classValueAnalyzer)));

    @Test
    public void springConfigurationFile() throws Exception {
        Set<Object> results = this.analyzer.analyze(createFileSystemEntry("spring-jndi.xml"));
        assertNotNull(results);
        assertEquals(4, results.size());

        assertTrue(this.classValueAnalyzer.classNamesAnalyzed.contains("org.springframework.jndi.JndiObjectFactoryBean"));
        assertTrue(this.classValueAnalyzer.classNamesAnalyzed.contains("javax.sql.DataSource"));
    }

    @Test
    public void nonSpringConfigurationXmlFile() throws Exception {
        Set<Object> results = this.analyzer.analyze(createFileSystemEntry("web.xml"));
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FileSystemEntry createFileSystemEntry(final String name) throws Exception {
        FileSystemEntry fileSystemEntry = mock(FileSystemEntry.class);
        when(fileSystemEntry.getName()).thenReturn(name);
        Answer answer = new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                InputStream input = new FileInputStream("src/test/resources/org/springframework/migrationanalyzer/contributions/spring/" + name);
                try {
                    Object callback = invocation.getArguments()[0];
                    if (callback instanceof Callback) {
                        return ((Callback) callback).perform(input);
                    } else {
                        return ((ExceptionCallback) callback).perform(input);
                    }
                } finally {
                    IoUtils.closeQuietly(input);
                }
            }
        };

        when(fileSystemEntry.doWithInputStream(any(ExceptionCallback.class))).thenAnswer(answer);
        when(fileSystemEntry.doWithInputStream(any(Callback.class))).thenAnswer(answer);

        return fileSystemEntry;
    }

    private static final class StubSpringConfigurationClassValueAnalyzer implements SpringConfigurationClassValueAnalyzer<String> {

        private final Set<String> classNamesAnalyzed = new HashSet<String>();

        @Override
        public String analyze(String className, FileSystemEntry fse) {
            this.classNamesAnalyzed.add(className);
            return null;
        }
    }
}