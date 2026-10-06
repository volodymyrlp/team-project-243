package travelplanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import travelplanner.config.CacheConfig;

@SpringJUnitConfig(classes = {
        CacheConfig.class,
        CacheAutoConfiguration.class,
        CurrencyServiceCacheTest.TestConfig.class
})
class CurrencyServiceCacheTest {

    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";
    private static final String SAMPLE_JSON = """
            {
              "base": "USD",
              "rates": {
                "USD": 1,
                "EUR": 0.882,
                "UAH": 44.74
              }
            }
            """;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer.reset();
        Cache cache = cacheManager.getCache("exchangeRates");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void getExchangeRates_CachedOnSubsequentCalls() {
        mockServer.expect(ExpectedCount.once(), requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        // First call - should hit mockServer
        Map<String, BigDecimal> rates1 = currencyService.getExchangeRates("USD");
        assertNotNull(rates1);
        assertEquals(new BigDecimal("0.882"), rates1.get("EUR"));

        // Second call - should be retrieved from cache, not hitting mockServer
        Map<String, BigDecimal> rates2 = currencyService.getExchangeRates("USD");
        assertNotNull(rates2);
        assertEquals(new BigDecimal("0.882"), rates2.get("EUR"));

        // Verify cache manager actually holds the cached value
        Cache cache = cacheManager.getCache("exchangeRates");
        assertNotNull(cache);
        assertNotNull(cache.get("USD"));

        mockServer.verify();
    }

    @Test
    void convert_UsesCachedExchangeRates() {
        mockServer.expect(ExpectedCount.once(), requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        // First conversion - should hit mockServer
        BigDecimal eurAmount = currencyService.convert(
                new BigDecimal("100.00"), "USD", "EUR");
        assertEquals(new BigDecimal("88.20"), eurAmount);

        // Second conversion with same base currency - should use cache
        BigDecimal uahAmount = currencyService.convert(
                new BigDecimal("50.00"), "USD", "UAH");
        assertEquals(new BigDecimal("2237.00"), uahAmount);

        mockServer.verify();
    }

    @TestConfiguration
    @Import(CacheConfig.class)
    static class TestConfig {

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        @Bean
        public MockRestServiceServer mockRestServiceServer() {
            return server;
        }

        @Bean
        public CurrencyService currencyService(ObjectProvider<CurrencyService> selfProvider) {
            return new ExternalCurrencyServiceImpl(
                    builder,
                    API_URL,
                    selfProvider
            );
        }
    }
}
