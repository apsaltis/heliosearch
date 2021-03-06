package org.apache.solr.search.field;

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

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.core.HS;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.BitDocSetNative;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;

public class DoubleTopValues extends TopValues {

  public DoubleTopValues(DoubleFieldValues doubleFieldValues) {
    super(doubleFieldValues);
  }

  protected static class Uninvert extends LeafValues.Uninvert {
    private boolean first = true;
    DoubleFieldStats stats = new DoubleFieldStats();

    double currentValue;
    private final FieldCache.DoubleParser parser = FieldCache.NUMERIC_UTILS_DOUBLE_PARSER;

    long arr;

    public Uninvert(AtomicReaderContext readerContext, SchemaField field) {
      super(readerContext, field);
    }

    @Override
    public void visitTerm(BytesRef term) {
      currentValue = parser.parseDouble(term);
      if (first) {
        first = false;
        stats.firstValue = currentValue;
        arr = HS.allocArray(maxDoc, 8, true);
      }
    }

    @Override
    public void visitDoc(int docID) {
      HS.setDouble(arr, docID, currentValue);
    }

    @Override
    protected TermsEnum termsEnum(Terms terms) throws IOException {
      return parser.termsEnum(terms);
    }

    protected void done() {
      stats.lastValue = currentValue;
      stats.numUniqueValues = super.termNum;
      stats.numDocsWithField = super.termsDocCount;
    }

    @Override
    public void close() throws IOException {
      if (arr != 0) {
        HS.freeArray(arr);
        arr = 0;
      }
      super.close();
    }
  }

  @Override
  public DoubleLeafValues createValue(TopValues topValues, CreationLeafValue create, AtomicReaderContext readerContext) throws IOException {
    try (
        Uninvert u = new Uninvert(readerContext, fieldValues.field);
    ) {
      u.uninvert();

      long arr = u.arr;
      BitDocSetNative docsWithField = u.docsWithField;
      u.docsWithField = null;

      if (arr == 0) {
        return new Double0LeafValues(fieldValues);
      }

      // steal the array
      u.arr = 0;
      return new Double64LeafValues(fieldValues, arr, docsWithField, u.stats);
    }

  }

  @Override
  public DoubleTopValues create(SolrIndexSearcher.WarmContext warmContext) {
    DoubleTopValues tv = new DoubleTopValues((DoubleFieldValues)fieldValues);
    tv.create(warmContext, this);
    return tv;
  }
}
