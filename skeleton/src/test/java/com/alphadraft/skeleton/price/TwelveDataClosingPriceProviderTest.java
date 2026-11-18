package com.alphadraft.skeleton.price;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TwelveDataClosingPriceProviderTest {

    private static final String BASE = "https://api.twelvedata.com";

    private static MockRestServiceServer server(RestClient.Builder builder) {
        return MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void readsTheClosePriceAndTheTradingDate() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = server(builder);
        server.expect(requestTo(BASE + "/quote?symbol=AAPL&apikey=secret"))
                .andRespond(withSuccess("{\"symbol\":\"AAPL\",\"datetime\":\"2026-11-13\",\"close\":\"230.50\",\"previous_close\":\"228.00\"}",
                        MediaType.APPLICATION_JSON));

        Optional<ClosingPrice> close = new TwelveDataClosingPriceProvider(builder, BASE, "secret").latestClose("AAPL");

        assertTrue(close.isPresent());
        assertEquals(LocalDate.of(2026, 11, 13), close.get().tradeDate());
        assertEquals(new BigDecimal("230.50"), close.get().price());
        assertEquals("twelvedata", close.get().source());
        server.verify();
    }

    @Test
    void anErrorResponseHasNoPrice() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = server(builder);
        server.expect(requestTo(BASE + "/quote?symbol=ZZZZ&apikey=secret"))
                .andRespond(withSuccess("{\"code\":400,\"message\":\"symbol not found\",\"status\":\"error\"}",
                        MediaType.APPLICATION_JSON));

        assertFalse(new TwelveDataClosingPriceProvider(builder, BASE, "secret").latestClose("ZZZZ").isPresent());
    }

    @Test
    void aZeroPriceHasNoPrice() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = server(builder);
        server.expect(requestTo(BASE + "/quote?symbol=AAPL&apikey=secret"))
                .andRespond(withSuccess("{\"symbol\":\"AAPL\",\"datetime\":\"2026-11-13\",\"close\":\"0\"}",
                        MediaType.APPLICATION_JSON));

        assertFalse(new TwelveDataClosingPriceProvider(builder, BASE, "secret").latestClose("AAPL").isPresent());
    }

    @Test
    void isNotConfiguredWithoutAKey() {
        RestClient.Builder builder = RestClient.builder();

        assertFalse(new TwelveDataClosingPriceProvider(builder, BASE, "").isConfigured());
        assertFalse(new TwelveDataClosingPriceProvider(builder, BASE, "   ").isConfigured());
        assertTrue(new TwelveDataClosingPriceProvider(builder, BASE, "secret").isConfigured());
    }
}
