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
package one.cafebabe.samurai.gc;

import one.cafebabe.samurai.util.LineGraphDataSourceParser;

public class GCParser implements LineGraphDataSourceParser {
    private final LineGraphDataSourceParser[] gcParsers = new LineGraphDataSourceParser[]{new BEAGCParser(), 
            new SunGCParser(), new IBMGCParser(), new OpenJDKGCParser(), new OpenJDKZGCParser()};
    private LineGraphDataSourceParser finalParser = null;

    public GCParser() {
    }

    public boolean parse(String line, LineGraphRenderer renderer) {
        if (null != finalParser) {
            return finalParser.parse(line, renderer);
        } else {
            for (LineGraphDataSourceParser gcParser : gcParsers) {
                if (gcParser.parse(line, renderer)) {
                    finalParser = gcParser;
                    return true;
                }
            }
            return false;
        }
    }
}
