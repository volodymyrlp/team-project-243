package travelplanner.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import travelplanner.dto.currency.ExchangeRateResponseDto;
import travelplanner.exception.CurrencyExchangeException;

@Service
public class ExternalCurrencyServiceImpl implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(ExternalCurrencyServiceImpl.class);

    private final RestClient restClient;
    private final String apiUrl;
    private final ObjectProvider<CurrencyService> selfProvider;

    @Autowired
    public ExternalCurrencyServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${exchange.api.url}") String apiUrl,
            ObjectProvider<CurrencyService> selfProvider
    ) {
        this(restClientBuilder.build(), apiUrl, selfProvider);
    }

    public ExternalCurrencyServiceImpl(
            RestClient.Builder restClientBuilder,
            String apiUrl
    ) {
        this(restClientBuilder.build(), apiUrl, null);
    }

    public ExternalCurrencyServiceImpl(
            RestClient restClient,
            String apiUrl
    ) {
        this(restClient, apiUrl, null);
    }

    public ExternalCurrencyServiceImpl(
            RestClient restClient,
            String apiUrl,
            ObjectProvider<CurrencyService> selfProvider
    ) {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalArgumentException("API URL must not be null or blank");
        }
        this.restClient = restClient;
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        this.selfProvider = selfProvider;
    }

    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (fromCurrency == null || fromCurrency.isBlank()) {
            throw new IllegalArgumentException("Source currency must not be null or blank");
        }
        if (toCurrency == null || toCurrency.isBlank()) {
            throw new IllegalArgumentException("Target currency must not be null or blank");
        }

        String normalizedFrom = fromCurrency.trim().toUpperCase();
        String normalizedTo = toCurrency.trim().toUpperCase();

        Map<String, BigDecimal> rates = getSelf().getExchangeRates(normalizedFrom);
        BigDecimal multiplier = rates != null ? rates.get(normalizedTo) : null;
        if (multiplier == null) {
            throw new CurrencyExchangeException(
                    "Exchange rate not found for currency: " + normalizedTo);
        }

        return amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Cacheable("exchangeRates")
    public Map<String, BigDecimal> getExchangeRates(String baseCurrency) {
        if (baseCurrency == null || baseCurrency.isBlank()) {
            throw new IllegalArgumentException("Base currency must not be null or blank");
        }

        String normalizedBase = baseCurrency.trim().toUpperCase();
        try {
            ExchangeRateResponseDto response = restClient.get()
                    .uri(apiUrl + "{baseCurrency}", normalizedBase)
                    .retrieve()
                    .body(ExchangeRateResponseDto.class);

            if (response == null || response.getRates() == null || response.getRates().isEmpty()) {
                throw new CurrencyExchangeException(
                        "No exchange rates returned for currency: " + normalizedBase);
            }
            return response.getRates();
        } catch (RestClientException ex) {
            log.warn("Failed to fetch exchange rates for currency: {}", normalizedBase, ex);
            throw new CurrencyExchangeException(
                    "Failed to fetch exchange rates for currency: " + normalizedBase, ex);
        }
    }

    private CurrencyService getSelf() {
        if (selfProvider != null) {
            CurrencyService self = selfProvider.getIfAvailable();
            if (self != null) {
                return self;
            }
        }
        return this;
    }
}
