package ch.uzh.csg.mbps.server.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import net.minidev.json.parser.ParseException;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeException;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.NotAvailableFromExchangeException;
import com.xeiam.xchange.NotYetImplementedForExchangeException;
import com.xeiam.xchange.bitstamp.BitstampExchange;
import com.xeiam.xchange.currency.Currencies;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.dto.trade.OpenOrders;
import com.xeiam.xchange.service.polling.PollingAccountService;
import com.xeiam.xchange.service.polling.PollingMarketDataService;
import com.xeiam.xchange.service.polling.PollingTradeService;

public class BitstampController {

	private static Exchange bitstamp =  getExchange();

	private static Exchange getExchange() {
		ExchangeSpecification exSpec = new BitstampExchange()
		.getDefaultExchangeSpecification();
		exSpec.setUserName(Config.BITSTAMP_USERNAME);
		exSpec.setApiKey(Config.BITSTAMP_APIKEY);
		exSpec.setSecretKey(Config.BITSTAMP_SECRETKEY);
		return ExchangeFactory.INSTANCE.createExchange(exSpec);
	}

	/**
	 * Buy bitcoins at Bistamp for the defined amount. Places immediately an
	 * LimitOrder with maxPrice = currentExchangerate*1.05
	 * 
	 * @param amount
	 * @return String with bistamp order confirmation number.
	 * @throws ExchangeException
	 * @throws NotAvailableFromExchangeException
	 * @throws NotYetImplementedForExchangeException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static String buyBTC(BigDecimal amount) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException, ParseException {
		PollingTradeService tradeService = bitstamp.getPollingTradeService();

		OrderType orderType = (OrderType.BID);
		BigDecimal tradeableAmount = amount;
		String tradableIdentifier = "BTC";
		String transactionCurrency = "USD";
		String id = null;
		Date timestamp = new Date();
		
		BigDecimal exchangeLimitPrice = getExchangeRate().multiply(Config.BITSTAMP_BUY_EXCHANGE_RATE_LIMIT).setScale(2, RoundingMode.HALF_DOWN);
		BigMoney limitPrice = BigMoney.of(CurrencyUnit.USD, exchangeLimitPrice);

		LimitOrder limitOrder = new LimitOrder(orderType, tradeableAmount, tradableIdentifier, transactionCurrency, id, timestamp, limitPrice);

		String orderID = tradeService.placeLimitOrder(limitOrder);

		if(orderID.equals("0")){
			throw new ExchangeException("Couldnt process exchange, limitOrderID=0. Probably amount lower than 1$");
		}
		
		return orderID;
	}

	/**
	 * Sell bitcoins at Bistamp for the defined amount. Places immediately an
	 * LimitOrder with min = currentExchangerate*0.95
	 * 
	 * @param amount
	 * @return
	 * @throws ExchangeException
	 * @throws NotAvailableFromExchangeException
	 * @throws NotYetImplementedForExchangeException
	 * @throws IOException
	 */
	public static String sellBTC(BigDecimal amount) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		
		PollingTradeService tradeService = bitstamp.getPollingTradeService();

		OrderType orderType = (OrderType.ASK);
		BigDecimal tradeableAmount = amount;
		String tradableIdentifier = "BTC";
		String transactionCurrency = "USD";
		String id = null;
		Date timestamp = new Date();

		BigDecimal exchangeLimitPrice = getExchangeRate().multiply(Config.BITSTAMP_SELL_EXCHANGE_RATE_LIMIT).setScale(2, RoundingMode.HALF_UP);
		
		BigMoney limitPrice = BigMoney.of(CurrencyUnit.USD, exchangeLimitPrice);

		LimitOrder limitOrder = new LimitOrder(orderType, tradeableAmount, tradableIdentifier, transactionCurrency, id, timestamp, limitPrice);	 

		String orderID = tradeService.placeLimitOrder(limitOrder);

		if(orderID.equals("0")){
			throw new ExchangeException("Couldnt process exchange, limitOrderID=0. Probably amount lower than 1$");
		}
		
		return orderID;
	}
	
	/**
	 * Returns all open Limitorders which are placed at Bistamp but not yet
	 * executed because the defined min/max price is not reached.
	 * 
	 * @return {@link OpenOrders}
	 * @throws ExchangeException
	 * @throws NotAvailableFromExchangeException
	 * @throws NotYetImplementedForExchangeException
	 * @throws IOException
	 */
	public static OpenOrders getOpenOrders() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		PollingTradeService tradeService = bitstamp.getPollingTradeService();
		OpenOrders openOrders = tradeService.getOpenOrders();

		return openOrders;
	}

	/**
	 * Returns String with Bistamp-Account Information. Account-ID, USD-Balance and BTC-Balance.
	 * 
	 * @return String with Account-Information
	 * @throws ExchangeException
	 * @throws NotAvailableFromExchangeException
	 * @throws NotYetImplementedForExchangeException
	 * @throws IOException
	 */
	public static String getAccountInfo() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		PollingAccountService accountService = bitstamp.getPollingAccountService();
		AccountInfo accountInfo = accountService.getAccountInfo();
		return accountInfo.toString();
	}

	/**
	 * Returns current Bitstamp Exchange Rate BTC/USD
	 * 
	 * @return exchangerate BTC/USD
	 * @throws ExchangeException
	 * @throws NotAvailableFromExchangeException
	 * @throws NotYetImplementedForExchangeException
	 * @throws IOException
	 */
	public static BigDecimal getExchangeRate() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException{
		PollingMarketDataService marketDataService = bitstamp.getPollingMarketDataService();

		Ticker ticker = marketDataService.getTicker(Currencies.BTC, Currencies.USD);
		BigDecimal btcusd = ticker.getLast().getAmount();
		return btcusd;
	}
}
