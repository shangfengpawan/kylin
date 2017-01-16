/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.apache.kylin.measure.percentile;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.tdunning.math.stats.AVLTreeDigest;
import com.tdunning.math.stats.TDigest;

public class PercentileCounter implements Serializable {
    private static final double INVALID_QUANTILE_RATIO = -1;

    double compression;
    double quantileRatio;

    TDigest registers;

    public PercentileCounter(double compression) {
        this(compression, INVALID_QUANTILE_RATIO);
    }

    public PercentileCounter(PercentileCounter another) {
        this(another.compression, another.quantileRatio);
        merge(another);
    }

    public PercentileCounter(double compression, double quantileRatio) {
        this.compression = compression;
        this.quantileRatio = quantileRatio;
        reInitRegisters();
    }

    private void reInitRegisters() {
        this.registers = TDigest.createAvlTreeDigest(this.compression);
    }

    public void add(double v) {
        registers.add(v);
    }

    public void merge(PercentileCounter counter) {
        assert this.compression == counter.compression;
        registers.add(counter.registers);
    }

    public double getResultEstimate() {
        return registers.quantile(quantileRatio);
    }

    public void writeRegisters(ByteBuffer out) {
        registers.compress();
        registers.asSmallBytes(out);
    }

    public void readRegisters(ByteBuffer in) {
        registers = AVLTreeDigest.fromBytes(in);
        compression = registers.compression();
    }

    public int getBytesEstimate() {
        return maxLength();
    }

    public int maxLength() {
        // 10KB for max length
        return 10 * 1024;
    }

    public int peekLength(ByteBuffer in) {
        int mark = in.position();
        AVLTreeDigest.fromBytes(in);
        int total = in.position() - mark;
        in.position(mark);
        return total;
    }

    public void clear() {
        reInitRegisters();
    }
}
