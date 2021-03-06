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
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Terms;
import org.apache.solr.search.QueryContext;
import org.apache.solr.search.function.FuncValues;
import org.apache.solr.search.function.ValueSource;
import org.apache.solr.search.function.funcvalues.LongDocValues;

import java.io.IOException;

/**
 * <code>SumTotalTermFreqValueSource</code> returns the number of tokens.
 * (sum of term freqs across all documents, across all terms).
 * Returns -1 if frequencies were omitted for the field, or if
 * the codec doesn't support this statistic.
 *
 * @lucene.internal
 */
public class SumTotalTermFreqValueSource extends ValueSource {
  protected final String indexedField;

  public SumTotalTermFreqValueSource(String indexedField) {
    this.indexedField = indexedField;
  }

  public String name() {
    return "sumtotaltermfreq";
  }

  @Override
  public String description() {
    return name() + '(' + indexedField + ')';
  }

  @Override
  public FuncValues getValues(QueryContext context, AtomicReaderContext readerContext) throws IOException {
    return (FuncValues) context.get(this);
  }

  @Override
  public void createWeight(QueryContext context) throws IOException {
    long sumTotalTermFreq = 0;
    for (AtomicReaderContext readerContext : context.indexSearcher().getTopReaderContext().leaves()) {
      Fields fields = readerContext.reader().fields();
      if (fields == null) continue;
      Terms terms = fields.terms(indexedField);
      if (terms == null) continue;
      long v = terms.getSumTotalTermFreq();
      if (v == -1) {
        sumTotalTermFreq = -1;
        break;
      } else {
        sumTotalTermFreq += v;
      }
    }
    final long ttf = sumTotalTermFreq;
    context.put(this, new LongDocValues(this) {
      @Override
      public long longVal(int doc) {
        return ttf;
      }
    });
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() + indexedField.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this.getClass() != o.getClass()) return false;
    SumTotalTermFreqValueSource other = (SumTotalTermFreqValueSource) o;
    return this.indexedField.equals(other.indexedField);
  }
}
