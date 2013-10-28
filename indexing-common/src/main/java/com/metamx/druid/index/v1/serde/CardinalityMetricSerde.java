package com.metamx.druid.index.v1.serde;


import com.clearspring.analytics.stream.cardinality.AdaptiveCounting;
import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.google.common.collect.Ordering;
import com.metamx.druid.index.column.ColumnBuilder;
import com.metamx.druid.index.serde.ColumnPartSerde;
import com.metamx.druid.index.serde.ComplexColumnPartSerde;
import com.metamx.druid.index.serde.ComplexColumnPartSupplier;
import com.metamx.druid.input.InputRow;
import com.metamx.druid.kv.GenericIndexed;
import com.metamx.druid.kv.ObjectStrategy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

public class CardinalityMetricSerde extends ComplexMetricSerde {

    private final ObjectStrategy<ICardinality> cardinalityStrategy = new ObjectStrategy<ICardinality>() {
        @Override
        public Class<? extends ICardinality> getClazz() {
            return ICardinality.class;
        }

        @Override
        public ICardinality fromByteBuffer(ByteBuffer buffer, int numBytes) {
            return new AdaptiveCounting(buffer.array());
        }

        @Override
        public byte[] toBytes(ICardinality val) {
            try {
                final byte[] bytes = val.getBytes();
                return bytes;
            }
            catch (IOException e) {

            }

            return null;
        }

        @Override
        public int compare(ICardinality o1, ICardinality o2) {
            return Ordering.natural().nullsFirst().compare(o1.cardinality(), o2.cardinality());
        }
    };

    private final ComplexMetricExtractor cardinalityMetricExtractor = new ComplexMetricExtractor() {
        @Override
        public Class<?> extractedClass() {
            return ICardinality.class;
        }

        @Override
        public Object extractValue(InputRow inputRow, String metricName) {
            ICardinality cardinalityCounter = AdaptiveCounting.Builder.obyCount(Integer.MAX_VALUE).build();
            final List<String> dimension = inputRow.getDimension(metricName);
            Iterator<String> iterator = dimension.iterator();
            while (iterator.hasNext()) {
                cardinalityCounter.offer(iterator.next());
            }

            return cardinalityCounter;
        }
    };

    @Override
    public String getTypeName() {
        return "cardinality";
    }

    @Override
    public ComplexMetricExtractor getExtractor() {
        return cardinalityMetricExtractor;
    }

    @Override
    public ColumnPartSerde deserializeColumn(ByteBuffer buffer, ColumnBuilder builder) {
        GenericIndexed column = GenericIndexed.read(buffer, cardinalityStrategy);
        builder.setComplexColumn(new ComplexColumnPartSupplier(getTypeName(), column));
        return new ComplexColumnPartSerde(column, getTypeName());
    }

    @Override
    public ObjectStrategy getObjectStrategy() {
        return cardinalityStrategy;
    }
}
