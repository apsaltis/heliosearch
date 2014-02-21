/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.search.function.valuesource;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.Bits;
import org.apache.solr.search.QueryContext;
import org.apache.solr.search.function.FuncValues;
import org.apache.solr.search.function.funcvalues.DoubleFuncValues;
import org.apache.solr.search.mutable.MutableValue;
import org.apache.solr.search.mutable.MutableValueDouble;

import java.io.IOException;
import java.util.Map;

/**
 * Obtains double field values from {@link FieldCache#getDoubles} and makes
 * those values available as other numeric types, casting as needed.
 */
public class DoubleFieldSource extends FieldCacheSource {

  protected final FieldCache.DoubleParser parser;

  public DoubleFieldSource(String field) {
    this(field, null);
  }

  public DoubleFieldSource(String field, FieldCache.DoubleParser parser) {
    super(field);
    this.parser = parser;
  }

  @Override
  public String description() {
    return "double(" + field + ')';
  }

  @Override
  public FuncValues getValues(QueryContext context, AtomicReaderContext readerContext) throws IOException {
    final FieldCache.Doubles arr = cache.getDoubles(readerContext.reader(), field, parser, true);
    final Bits valid = cache.getDocsWithField(readerContext.reader(), field);
    return new DoubleFuncValues(this) {
      @Override
      public double doubleVal(int doc) {
        return arr.get(doc);
      }

      @Override
      public boolean exists(int doc) {
        return arr.get(doc) != 0 || valid.get(doc);
      }

      @Override
      public ValueFiller getValueFiller() {
        return new ValueFiller() {
          private final MutableValueDouble mval = new MutableValueDouble();

          @Override
          public MutableValue getValue() {
            return mval;
          }

          @Override
          public void fillValue(int doc) {
            mval.value = arr.get(doc);
            mval.exists = mval.value != 0 || valid.get(doc);
          }
        };
      }


    };

  }

  @Override
  public boolean equals(Object o) {
    if (o.getClass() != DoubleFieldSource.class) return false;
    DoubleFieldSource other = (DoubleFieldSource) o;
    return super.equals(other)
        && (this.parser == null ? other.parser == null :
        this.parser.getClass() == other.parser.getClass());
  }

  @Override
  public int hashCode() {
    int h = parser == null ? Double.class.hashCode() : parser.getClass().hashCode();
    h += super.hashCode();
    return h;
  }
}