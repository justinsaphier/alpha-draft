package com.alphadraft.skeleton.price;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Closing prices from Twelve Data's /quote endpoint. After the close, "close" is the day's closing
// price and "datetime" is the trading date. Twelve Data answers a bad key or an unknown symbol with
// { "status": "error", ... }, which counts as no price.
@Component
public class TwelveDataClosingPriceProvider implements ClosingPriceProvider {

    static final String SOURCE = "twelvedata";

    private final RestClient http;
    private final String apiKey;

    @Autowired
    public TwelveDataClosingPriceProvider(@Value("${twelvedata.api-key:}") String apiKey) {
        this(RestClient.builder(), "https://api.twelvedata.com", apiKey);
    }

    TwelveDataClosingPriceProvider(RestClient.Builder builder, String baseUrl, String apiKey) {
        this.http = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public Optional<ClosingPrice> latestClose(String ticker) {
        Map<?, ?> body = http.get()
                .uri("/quote?symbol={symbol}&apikey={key}", ticker, apiKey)
                .retrieve()
                .body(Map.class);
        if (body == null || "error".equals(body.get("status"))) {
            return Optional.empty();
        }
        Object close = body.get("close");
        Object datetime = body.get("datetime");
        if (close == null || datetime == null) {
            return Optional.empty();
        }
        BigDecimal price = new BigDecimal(String.valueOf(close));
        if (price.signum() <= 0) {
            return Optional.empty();
        }
        String day = String.valueOf(datetime);
        LocalDate tradeDate = LocalDate.parse(day.length() > 10 ? day.substring(0, 10) : day);
        return Optional.of(new ClosingPrice(tradeDate, price, SOURCE));
    }
}
