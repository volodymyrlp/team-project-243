package travelplanner.service;

import java.math.BigDecimal;
import java.util.Map;

public interface CurrencyService {

    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);

    Map<String, BigDecimal> getExchangeRates(String baseCurrency);
}
