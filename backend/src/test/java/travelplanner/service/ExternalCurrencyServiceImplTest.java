package travelplanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import travelplanner.exception.CurrencyExchangeException;

class ExternalCurrencyServiceImplTest {

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

    private MockRestServiceServer mockServer;
    private ExternalCurrencyServiceImpl currencyService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        currencyService = new ExternalCurrencyServiceImpl(builder, API_URL);
    }

    @Test
    void convert_Success_UsdToEur() {
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        BigDecimal converted = currencyService.convert(
                new BigDecimal("100.00"), "USD", "EUR");

        assertEquals(new BigDecimal("88.20"), converted);
        mockServer.verify();
    }

    @Test
    void convert_Success_UsdToUah() {
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        BigDecimal converted = currencyService.convert(
                new BigDecimal("50.00"), "USD", "UAH");

        assertEquals(new BigDecimal("2237.00"), converted);
        mockServer.verify();
    }

    @Test
    void convert_Success_RoundingHalfUp() {
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        BigDecimal converted = currencyService.convert(
                new BigDecimal("10.555"), "USD", "EUR");

        // 10.555 * 0.882 = 9.30951 -> 9.31
        assertEquals(new BigDecimal("9.31"), converted);
        mockServer.verify();
    }

    @Test
    void convert_Success_SameCurrency() {
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        BigDecimal converted = currencyService.convert(
                new BigDecimal("100.00"), "USD", "USD");

        assertEquals(new BigDecimal("100.00"), converted);
        mockServer.verify();
    }

    @Test
    void convert_TargetCurrencyNotFound_ThrowsCurrencyExchangeException() {
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        CurrencyExchangeException ex = assertThrows(
                CurrencyExchangeException.class,
                () -> currencyService.convert(new BigDecimal("100.00"), "USD", "GBP")
        );

        assertTrue(ex.getMessage().contains("GBP"));
        mockServer.verify();
    }

    @Test
    void convert_NullAmount_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyService.convert(null, "USD", "EUR")
        );
    }

    @Test
    void convert_NullOrBlankFromCurrency_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyService.convert(BigDecimal.TEN, null, "EUR")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyService.convert(BigDecimal.TEN, "   ", "EUR")
        );
    }

    @Test
    void convert_NullOrBlankToCurrency_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyService.convert(BigDecimal.TEN, "USD", null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyService.convert(BigDecimal.TEN, "USD", "   ")
        );
    }

    @Test
    void getExchangeRates_Success_ReturnsRatesMap() {
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        Map<String, BigDecimal> rates = currencyService.getExchangeRates("usd");

        assertNotNull(rates);
        assertEquals(new BigDecimal("1"), rates.get("USD"));
        assertEquals(new BigDecimal("0.882"), rates.get("EUR"));
        assertEquals(new BigDecimal("44.74"), rates.get("UAH"));
        mockServer.verify();
    }

    @Test
    void getExchangeRates_NullOrBlankBaseCurrency_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyService.getExchangeRates(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> currencyService.getExchangeRates("   ")
        );
    }

    @Test
    void getExchangeRates_ApiReturnsEmptyRates_ThrowsCurrencyExchangeException() {
        String emptyJson = "{\"base\":\"USD\",\"rates\":{}}";
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(emptyJson, MediaType.APPLICATION_JSON));

        assertThrows(
                CurrencyExchangeException.class,
                () -> currencyService.getExchangeRates("USD")
        );
        mockServer.verify();
    }

    @Test
    void getExchangeRates_ApiError500_ThrowsCurrencyExchangeException() {
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(
                CurrencyExchangeException.class,
                () -> currencyService.getExchangeRates("USD")
        );
        mockServer.verify();
    }

    @Test
    void getExchangeRates_ApiError404_ThrowsCurrencyExchangeException() {
        mockServer.expect(requestTo(API_URL + "UNKNOWN"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(
                CurrencyExchangeException.class,
                () -> currencyService.getExchangeRates("UNKNOWN")
        );
        mockServer.verify();
    }

    @Test
    void constructor_NullOrBlankApiUrl_ThrowsIllegalArgumentException() {
        RestClient.Builder builder = RestClient.builder();
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExternalCurrencyServiceImpl(builder, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExternalCurrencyServiceImpl(builder, "  ")
        );
    }

    @Test
    void getExchangeRates_ApiReturnsNullRates_ThrowsCurrencyExchangeException() {
        String nullRatesJson = "{\"base\":\"USD\",\"rates\":null}";
        mockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(nullRatesJson, MediaType.APPLICATION_JSON));

        assertThrows(
                CurrencyExchangeException.class,
                () -> currencyService.getExchangeRates("USD")
        );
        mockServer.verify();
    }

    @Test
    void convert_WithSelfProvider_CallsSelfProvider() {
        @SuppressWarnings("unchecked")
        ObjectProvider<CurrencyService> selfProvider = mock(ObjectProvider.class);
        CurrencyService mockProxy = mock(CurrencyService.class);
        when(selfProvider.getIfAvailable()).thenReturn(mockProxy);
        when(mockProxy.getExchangeRates("USD")).thenReturn(Map.of("EUR", new BigDecimal("0.85")));

        RestClient.Builder builder = RestClient.builder();
        ExternalCurrencyServiceImpl serviceWithSelf =
                new ExternalCurrencyServiceImpl(builder.build(), API_URL, selfProvider);

        BigDecimal result = serviceWithSelf.convert(new BigDecimal("100"), "usd", "eur");
        assertEquals(new BigDecimal("85.00"), result);
    }

    @Test
    void convert_WithSelfProviderReturningNull_FallsBackToThis() {
        @SuppressWarnings("unchecked")
        ObjectProvider<CurrencyService> selfProvider = mock(ObjectProvider.class);
        when(selfProvider.getIfAvailable()).thenReturn(null);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer customMockServer = MockRestServiceServer.bindTo(builder).build();
        ExternalCurrencyServiceImpl serviceWithNullSelf =
                new ExternalCurrencyServiceImpl(builder.build(), API_URL, selfProvider);

        customMockServer.expect(requestTo(API_URL + "USD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));

        BigDecimal result = serviceWithNullSelf.convert(new BigDecimal("100"), "USD", "EUR");
        assertEquals(new BigDecimal("88.20"), result);
        customMockServer.verify();
    }

    @Test
    void constructor_ApiUrlFormatting() {
        RestClient.Builder builder = RestClient.builder();
        ExternalCurrencyServiceImpl serviceWithSlash =
                new ExternalCurrencyServiceImpl(builder.build(), "https://api.example.com/");
        assertNotNull(serviceWithSlash);

        ExternalCurrencyServiceImpl serviceWithoutSlash =
                new ExternalCurrencyServiceImpl(builder.build(), "https://api.example.com");
        assertNotNull(serviceWithoutSlash);
    }
}
