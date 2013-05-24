package com.pigz4j.io.stream;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

public class PigzPerformanceTest extends PigzTest {

    private static final int PAYLOAD_SIZE = 64 * ONE_MB;

    private static final int ROUNDS = 25;

    private static final int SKIP_ROUNDS = 5; /* First SKIP_ROUNDS are JVM warmup and aren't counted */

    private static final int[] THREADS = new int[] {1, 2, 4, 8, 16, 24, 32};

    private static boolean _enabled;

    @BeforeClass public static void setUp() throws Exception {
        _enabled = Boolean.parseBoolean(System.getProperty("includePerfTests", "false"));
        if ( !_enabled ) { System.out.println("Skipping Performance Tests: -DincludePerfTests=true to enable"); }
    }

    @Test public void testOutputStreamHistogram_Random() throws Exception {
        if ( !_enabled ) { return; }

        System.out.println("# Threads\tpigz4j\tJRE\tRandom");
        final Result jre = runPerformanceTestJre("Random", generateRandomBytes(PAYLOAD_SIZE));

        Result pigz = null;
        for ( final int threads : THREADS ) {
            pigz = runPerformanceTestPigz("Random", generateRandomBytes(PAYLOAD_SIZE), threads);
            System.out.println(threads + "\t" + pigz._avgDuration + "\t" + jre._avgDuration);
        }

        System.out.println("Compression\t" + pigz._avgCompressionRatio + "\t" + jre._avgCompressionRatio);
    }

    @Test public void testOutputStreamHistogram_Sequence() throws Exception {
        if ( !_enabled ) { return; }

        System.out.println("# Threads\tpigz4j\tJRE\tSequence");
        final byte[] sourceBytes = generateSequenceInput(PAYLOAD_SIZE);
        final Result jre = runPerformanceTestJre("Random", sourceBytes);

        Result pigz = null;
        for ( final int threads : THREADS ) {
            pigz = runPerformanceTestPigz("Random", sourceBytes, threads);
            System.out.println(threads + "\t" + pigz._avgDuration + "\t" + jre._avgDuration);
        }

        System.out.println("Compression\t" + pigz._avgCompressionRatio + "\t" + jre._avgCompressionRatio);
    }

    private static Result runPerformanceTestPigz(final String pType, final byte[] pSourceBytes, final int pThreads) throws IOException {
        long totalDuration = 0L;
        long totalIn = 0L;
        long totalOut = 0L;

        for (int round = 0; round < ROUNDS; round++ ) {
            final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            final long start = System.currentTimeMillis();

            final PigzOutputStream out = new PigzOutputStream(compressed,
                    PigzOutputStream.DEFAULT_BUFSZ,
                    PigzOutputStream.DEFAULT_BLOCKSZ,
                    PigzDeflaterFactory.DEFAULT,
                    Executors.newFixedThreadPool(pThreads));

            out.write(pSourceBytes);
            out.finish();
            out.flush();
            out.close();

            final long finish = System.currentTimeMillis() - start;
            if ( round >= SKIP_ROUNDS) {
                totalDuration += finish;
                totalIn += pSourceBytes.length;
                totalOut += compressed.size();
            }
        }

        final double countedRounds = (double)(ROUNDS - SKIP_ROUNDS);
        final double avgDuration = totalDuration / countedRounds;
        final double avgIn = totalIn / countedRounds;
        final double avgOut = totalOut / countedRounds;
        final double avgCompressionRatio = totalOut / (double) totalIn;
        return new Result(pType, "pigz4j", (int)countedRounds, pThreads, avgDuration, avgIn, avgOut, avgCompressionRatio);
    }

    private static Result runPerformanceTestJre(final String pType, final byte[] pSourceBytes) throws IOException {
        return runPerformanceTestJre(pType, pSourceBytes, 1);
    }

    private static Result runPerformanceTestJre(final String pType, final byte[] pSourceBytes, final int pThreads) throws IOException {
        long totalDuration = 0L;
        long totalIn = 0L;
        long totalOut = 0L;

        for (int round = 0; round < ROUNDS; round++ ) {
            final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            final long start = System.currentTimeMillis();

            final GZIPOutputStream out = new GZIPOutputStream(compressed, PigzOutputStream.DEFAULT_BUFSZ);

            out.write(pSourceBytes);
            out.finish();
            out.flush();
            out.close();

            final long finish = System.currentTimeMillis() - start;
            if ( round >= SKIP_ROUNDS ) {
                totalDuration += finish;
                totalIn += pSourceBytes.length;
                totalOut += compressed.size();
            }
        }

        final double countedRounds = (double)(ROUNDS - SKIP_ROUNDS);
        final double avgDuration = totalDuration / countedRounds;
        final double avgIn = totalIn / countedRounds;
        final double avgOut = totalOut / countedRounds;
        final double avgCompressionRatio = totalOut / (double) totalIn;
        return new Result(pType, "JRE", (int)countedRounds, pThreads, avgDuration, avgIn, avgOut, avgCompressionRatio);
    }

    private static class Result {
        private final String _sequence;
        private final String _type;
        private final int _countedRounds;
        private final int _threads;
        private final double _avgDuration;
        private final double _avgIn;
        private final double _avgOut;
        private final double _avgCompressionRatio;

        Result(final String sequence, final String type,
               final int countedRounds, final int threads,
               final double avgDuration, final double avgIn, final double avgOut, final double avgCompressionRatio) {
            _sequence = sequence;
            _type = type;
            _countedRounds = countedRounds;
            _threads = threads;
            _avgDuration = avgDuration;
            _avgIn = avgIn;
            _avgOut = avgOut;
            _avgCompressionRatio = avgCompressionRatio;
        }
    }
}
