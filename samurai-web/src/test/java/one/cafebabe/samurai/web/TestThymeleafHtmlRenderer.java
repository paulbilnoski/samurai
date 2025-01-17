/*
 * Copyright 2003-2012 Yusuke Yamamoto
 *
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
package one.cafebabe.samurai.web;

import one.cafebabe.samurai.core.ThreadDumpExtractor;
import one.cafebabe.samurai.core.ThreadStatistic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class TestThymeleafHtmlRenderer {
    final ThreadStatistic statistic = new ThreadStatistic();


    public static void main(String[] args) throws IOException {
        //do a performance test
        analyze("BEA", "testcases/BEA/910JRockit.dmp", 100);
        analyze("IBM", "testcases/IBM/1.4.2IBM/javacore.20060511.172914.516.txt", 200);
        analyze("SUN", "testcases/Sun/sun1.4.2_03stacked.dmp", 1000);
    }

    private static void analyze(String vendor, String fileName, int count) throws IOException {
        ThreadStatistic stats = new ThreadStatistic();
        ThreadDumpExtractor analyzer = new ThreadDumpExtractor(stats);
        analyzer.analyze(new File(fileName));
        ThymeleafHtmlRenderer renderer = new ThymeleafHtmlRenderer();
        //warm up
        System.out.println("Warming up " + vendor + "...");
        for (int i = 0; i < 100; i++) {
            renderer.saveTo(stats, null, (finished, all) -> {
            });
        }
        System.gc();
        System.out.println("Running " + vendor + "...");
        long before = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            renderer.saveTo(stats, null, (finished, all) -> {
            });
        }
        long timeSpent = System.currentTimeMillis() - before;
        System.out.println("Time spent " + vendor + ":" + (timeSpent / 1000d) + " secs");
    }


    @Test
    void testEscape() {
        ThymeleafHtmlRenderer.Util util = new ThymeleafHtmlRenderer.Util();
        assertEquals("foo&lt;bar",util.escape("foo<bar"));
        assertEquals("foo&gt;bar",util.escape("foo>bar"));
        assertEquals("foo&lt;&gt;bar",util.escape("foo<>bar"));
        assertEquals("&lt;foo&lt;&lt;foo&gt;&gt;bar&gt;",util.escape("<foo<<foo>>bar>"));
    }

    @Test
    void testSaveTo() throws IOException {
        ThreadDumpExtractor analyzer = new ThreadDumpExtractor(statistic);
        analyzer.analyze(TestThymeleafHtmlRenderer.class.getResourceAsStream("/Sun/1.4.2_03Sunstacked.dmp"));
        ThymeleafHtmlRenderer renderer = new ThymeleafHtmlRenderer();
        renderer.saveTo(statistic, new File("savedhtml"), (finished, all) -> assertTrue(finished <= all));
        File savedhtml = new File("savedhtml");
        //noinspection ResultOfMethodCallIgnored
        Files.walk(savedhtml.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

}
