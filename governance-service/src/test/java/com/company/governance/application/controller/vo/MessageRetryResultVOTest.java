package com.company.governance.application.controller.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessageRetryResultVOTest {

    @Test
    void shouldExposeConstructorValues() {
        MessageRetryResultVO response = new MessageRetryResultVO(3, "SUCCESS");

        assertEquals(3, response.getRetriedCount());
        assertEquals("SUCCESS", response.getStatus());
    }
}
