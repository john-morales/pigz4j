package com.pigz4j.io.stream;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

public class PigzPerformanceTest extends PigzTest {

    private static final int PAYLOAD_SIZE = 64 * ONE_MB;
    private static final int ROUNDS = 24;
    private static final int[] THREADS = new int[] {1, 2, 4, 8, 16, 24, 32};

    private static boolean _enabled;

    @BeforeClass public static void setUp() throws Exception {
        _enabled = Boolean.parseBoolean(System.getProperty("includePerfTests", "false"));
        if ( !_enabled ) { System.out.println("Skipping Performance Tests: -DincludePerfTests=true to enable"); }
    }

    @Test public void testPigzOutputStreamHistogram_Sequence() throws Exception {
        if ( !_enabled ) { return; }

        for ( final int threads : THREADS ) {
            final byte[] sourceBytes = generateSequenceInput(PAYLOAD_SIZE);
            runPerformanceTestPigz("type=sequence threads=" + threads + " impl=pigz", sourceBytes, threads);
        }
    }

    @Test public void testPigzOutputStreamHistogram_Random() throws Exception {
        if ( !_enabled ) { return; }

        for ( final int threads : THREADS ) {
            final byte[] sourceBytes = generateRandomBytes(PAYLOAD_SIZE);
            runPerformanceTestPigz("type=random threads=" + threads + " impl=pigz", sourceBytes, threads);
        }
    }

    @Test public void testJreOutputStreamHistogram_Sequence() throws Exception {
        if ( !_enabled ) { return; }

        for ( final int threads : THREADS ) {
            final byte[] sourceBytes = generateSequenceInput(PAYLOAD_SIZE);
            runPerformanceTestJre("type=sequence threads=" + threads + " impl=jre", sourceBytes, threads);
        }
    }

    @Test public void testJreOutputStreamHistogram_Random() throws Exception {
        if ( !_enabled ) { return; }

        for ( final int threads : THREADS ) {
            final byte[] sourceBytes = generateRandomBytes(PAYLOAD_SIZE);
            runPerformanceTestJre("type=random threads=" + threads + " impl=jre", sourceBytes, threads);
        }
    }

    private static void runPerformanceTestPigz(final String pType, final byte[] pSourceBytes, final int pThreads) throws IOException {
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
            totalDuration += finish;
            totalIn += out.getTotalIn();
            totalOut += out.getTotalOut();
        }

        final double avgDuration = totalDuration / (double) ROUNDS;
        final double avgIn = totalIn / (double) ROUNDS;
        final double avgOut = totalOut / (double) ROUNDS;
        final double avgCompressionRatio = totalOut / (double) totalIn;
        System.out.println(String.format("%s : avgDuration=%f avgIn=%f avgOut=%f avgCompressionRatio=%f",
                pType, avgDuration, avgIn, avgOut, avgCompressionRatio));
    }

    private static void runPerformanceTestJre(final String pType, final byte[] pSourceBytes, final int pThreads) throws IOException {
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
            totalDuration += finish;
            totalIn += pSourceBytes.length;
            totalOut += compressed.size();
        }

        final double avgDuration = totalDuration / (double) ROUNDS;
        final double avgIn = totalIn / (double) ROUNDS;
        final double avgOut = totalOut / (double) ROUNDS;
        final double avgCompressionRatio = totalOut / (double) totalIn;
        System.out.println(String.format("%s : avgDuration=%f avgIn=%f avgOut=%f avgCompressionRatio=%f",
                pType, avgDuration, avgIn, avgOut, avgCompressionRatio));
    }
}
