package org.apache.solr.search.function;

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
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SortField;
import org.apache.solr.search.QueryContext;

import java.io.IOException;


/**
 * Instantiates {@link FuncValues} for a particular reader.
 * <br>
 * Often used when creating a {@link FunctionQuery}.
 */
public abstract class ValueSource {

  /**
   * Gets the values for this reader and the context that was previously
   * passed to createWeight()
   */
  public abstract FuncValues getValues(QueryContext context, AtomicReaderContext readerContext) throws IOException;

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  /**
   * description of field, used in explain()
   */
  public abstract String description();

  @Override
  public String toString() {
    return description();
  }


  /**
   * Implementations should propagate createWeight to sub-ValueSources which can optionally store
   * weight info in the context. The context object will be passed to getValues()
   * where this info can be retrieved.
   */
  public void createWeight(QueryContext context) throws IOException {
  }


  //
  // Sorting by function
  //

  /**
   * EXPERIMENTAL: This method is subject to change.
   * <p/>
   * Get the SortField for this ValueSource.  Uses the {@link #getValues(org.apache.solr.search.QueryContext, org.apache.lucene.index.AtomicReaderContext)}
   * to populate the SortField.
   *
   * @param reverse true if this is a reverse sort.
   * @return The {@link org.apache.lucene.search.SortField} for the ValueSource
   */
  public SortField getSortField(boolean reverse) {
    return new ValueSourceSortField(reverse);
  }

  class ValueSourceSortField extends SortField {
    public ValueSourceSortField(boolean reverse) {
      super(description(), SortField.Type.REWRITEABLE, reverse);
    }

    @Override
    public SortField rewrite(IndexSearcher searcher) throws IOException {
      QueryContext context = QueryContext.newContext(searcher);
      createWeight(context);
      return new SortField(getField(), new ValueSourceComparatorSource(context), getReverse());
    }
  }

  class ValueSourceComparatorSource extends FieldComparatorSource {
    private final QueryContext context;

    public ValueSourceComparatorSource(QueryContext context) {
      this.context = context;
    }

    @Override
    public FieldComparator<Double> newComparator(String fieldname, int numHits,
                                                 int sortPos, boolean reversed) throws IOException {
      return new ValueSourceComparator(context, numHits);
    }
  }

  /**
   * Implement a {@link org.apache.lucene.search.FieldComparator} that works
   * off of the {@link FuncValues} for a ValueSource
   * instead of the normal Lucene FieldComparator that works off of a FieldCache.
   */
  class ValueSourceComparator extends FieldComparator<Double> {
    private final double[] values;
    private FuncValues docVals;
    private double bottom;
    private final QueryContext fcontext;
    private double topValue;

    ValueSourceComparator(QueryContext fcontext, int numHits) {
      this.fcontext = fcontext;
      values = new double[numHits];
    }

    @Override
    public int compare(int slot1, int slot2) {
      return Double.compare(values[slot1], values[slot2]);
    }

    @Override
    public int compareBottom(int doc) {
      return Double.compare(bottom, docVals.doubleVal(doc));
    }

    @Override
    public void copy(int slot, int doc) {
      values[slot] = docVals.doubleVal(doc);
    }

    @Override
    public FieldComparator setNextReader(AtomicReaderContext context) throws IOException {
      docVals = getValues(fcontext, context);
      return this;
    }

    @Override
    public void setBottom(final int bottom) {
      this.bottom = values[bottom];
    }

    @Override
    public void setTopValue(final Double value) {
      this.topValue = value.doubleValue();
    }

    @Override
    public Double value(int slot) {
      return values[slot];
    }

    @Override
    public int compareTop(int doc) {
      final double docValue = docVals.doubleVal(doc);
      return Double.compare(topValue, docValue);
    }
  }
}
