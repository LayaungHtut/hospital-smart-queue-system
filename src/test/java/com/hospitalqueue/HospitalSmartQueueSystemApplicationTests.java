package com.hospitalqueue;

import com.hospitalqueue.util.QueueNumberGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HospitalSmartQueueSystemApplicationTests {

    @Test
    void queueNumberGeneratorGeneratesSequentialNumbers() {
        String next = QueueNumberGenerator.generateQueueNumber("CAR1", 4);
        assertEquals("QCAR1" + QueueNumberGenerator.getDatePrefix() + "0005", next);
    }

    @Test
    void queueNumberGeneratorExtractsLastSequence() {
        int seq = QueueNumberGenerator.extractLastSequence("QCAR1202608160007");
        assertEquals(7, seq);
    }
}
