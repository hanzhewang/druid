package com.metamx.druid.aggregation;

import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;

import com.clearspring.analytics.stream.cardinality.AdaptiveCounting;
import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.metamx.common.logger.Logger;
import com.metamx.druid.processing.ComplexMetricSelector;

import java.io.IOException;
import java.util.Comparator;

public class CardinalityAggregator implements Aggregator
{
    static final Comparator COMPARATOR = LongSumAggregator.COMPARATOR;

    static Object combineValues(Object lhs, Object rhs)
    {
        log.info("Combining values: %s, %s", lhs.toString(), rhs.toString());
        try {
            return ((ICardinality) lhs).merge((ICardinality) rhs);
        }
        catch (CardinalityMergeException e) {
            return lhs;
        }
    }

    private static final Logger log = new Logger(CardinalityAggregator.class);

    private final ComplexMetricSelector<ICardinality> selector;
    private final String name;
    ICardinality card;

    public CardinalityAggregator(String name, ComplexMetricSelector<ICardinality> selector)
    {
        this.name = name;
        this.selector = selector;
        this.card =  AdaptiveCounting.Builder.obyCount(Integer.MAX_VALUE).build();
    }

    public CardinalityAggregator(String name, ComplexMetricSelector<ICardinality> selector, ICardinality card)
    {
        this.name = name;
        this.selector = selector;
        this.card =  card;
    }

    @Override
    public void aggregate()
    {
        ICardinality valueToAgg = selector.get();
        try {
            ICardinality mergedCardinality = card.merge(valueToAgg);
            this.card = mergedCardinality;
        }
        catch (CardinalityMergeException e) {

        }
    }

    @Override
    public void reset()
    {
        this.card = AdaptiveCounting.Builder.obyCount(Integer.MAX_VALUE).build();
    }

    @Override
    public Object get()
    {
        return card;
    }

    @Override
    public float getFloat()
    {
        throw new UnsupportedOperationException("CardinalityAggregator does not support getFloat()");
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public Aggregator clone()
    {
        log.info("Will try to return clone");
        try {
            ICardinality card = new AdaptiveCounting(this.card.getBytes());
            return new CardinalityAggregator(this.name, this.selector, card);
        }
        catch (IOException e) {

        }

        return null;
    }

    @Override
    public void close()
    {
        this.card = null;
    }
}
