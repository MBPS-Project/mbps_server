package ch.uzh.csg.mbps.server.service;

import java.math.BigDecimal;
import java.security.SignedObject;
import java.util.ArrayList;

import org.hibernate.HibernateException;

import ch.uzh.csg.mbps.model.HistoryTransaction;
import ch.uzh.csg.mbps.model.Transaction;
import ch.uzh.csg.mbps.server.clientinterface.ITransaction;
import ch.uzh.csg.mbps.server.dao.TransactionDAO;
import ch.uzh.csg.mbps.server.domain.DbTransaction;
import ch.uzh.csg.mbps.server.domain.UserAccount;
import ch.uzh.csg.mbps.server.util.Constants;
import ch.uzh.csg.mbps.server.util.exceptions.PayOutRuleNotFoundException;
import ch.uzh.csg.mbps.server.util.exceptions.TransactionException;
import ch.uzh.csg.mbps.server.util.exceptions.UserAccountNotFoundException;
import ch.uzh.csg.mbps.util.KeyHandler;
import ch.uzh.csg.mbps.util.Pair;

import com.azazar.bitcoin.jsonrpcclient.BitcoinException;

/**
 * Service class for {@link DbTransaction} between two {@link UserAccount}s.
 *
 */
public class TransactionService implements ITransaction {

	public static final String BALANCE = "Not sufficient funds. Payment rejected.";
	public static final String NEGATIVE_AMOUNT = "The transaction amount can't be negative or equals 0.";
	public static final String HIBERNATE_ERROR = "An error occured while persisting the data. Please try again later.";
	public static final String INTERNAL_ERROR = "An internal error occured. Please try again later.";
	public static final String PAYMENT_REFUSE = "The server refused the payment.";
	
	
	private static TransactionService transactionService;
	
	private TransactionService() {
	}
	
	/**
	 * Returns new or existing instance of {@link TransactionService}
	 * 
	 * @return instance of TransactionService
	 */
	public static TransactionService getInstance() {
		if (transactionService == null)
			transactionService = new TransactionService();
		
		return transactionService;
	}
	
	@Override
	public ArrayList<HistoryTransaction> getHistory(String username, int page) throws UserAccountNotFoundException {
		return TransactionDAO.getHistory(username, page);
	}
	
	@Override
	public ArrayList<HistoryTransaction> getLast3Transactions(String username) throws UserAccountNotFoundException {
		 return  TransactionDAO.getLast3Transactions(username);
	}
	
	
	/**
	 * Counts and returns number of {@link DbTransaction}s which are saved in the DB for
	 * {@link UserAccount} with username.
	 * 
	 * @param username
	 *            of UserAccount
	 * @return number of PayInTrasactions
	 * @throws UserAccountNotFoundException
	 */
	public long getHistoryCount(String username) throws UserAccountNotFoundException {
		return TransactionDAO.getHistoryCount(username);
	}
	
	@Override
	public SignedObject createTransaction(Pair<SignedObject> signedObjectPair) throws TransactionException, UserAccountNotFoundException {
		if (signedObjectPair == null || signedObjectPair.getFirst() == null || signedObjectPair.getSecond() == null)
			throw new TransactionException(PAYMENT_REFUSE);
		
		Transaction buyerTransaction = null;
		Transaction sellerTransaction = null;
		try {
			buyerTransaction = KeyHandler.retrieveTransaction(signedObjectPair.getFirst());
			sellerTransaction = KeyHandler.retrieveTransaction(signedObjectPair.getSecond());;
		} catch (Exception e) {
			throw new TransactionException(INTERNAL_ERROR);
		}
		
		if (buyerTransaction == null || sellerTransaction == null)
			throw new TransactionException(PAYMENT_REFUSE);
		
		if (buyerTransaction.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			throw new TransactionException(NEGATIVE_AMOUNT);
		
		String buyerUsername = buyerTransaction.getBuyerUsername();
		String sellerUsername = buyerTransaction.getSellerUsername();
		
		if (buyerUsername == sellerUsername)
			throw new TransactionException(PAYMENT_REFUSE);
			
		UserAccount buyerAccount = null;
		UserAccount sellerAccount = null;
		try {
			buyerAccount = UserAccountService.getInstance().getByUsername(buyerUsername);
			sellerAccount = UserAccountService.getInstance().getByUsername(sellerUsername);
		} catch (UserAccountNotFoundException e) {
			throw new TransactionException(PAYMENT_REFUSE);
		}
		
		if (!transactionNumbersValid(buyerTransaction, buyerAccount.getTransactionNumber(), sellerTransaction, sellerAccount.getTransactionNumber()))
			throw new TransactionException(PAYMENT_REFUSE);
		
		if (!transactionRequestsIdentic(buyerTransaction, sellerTransaction))
			throw new TransactionException(PAYMENT_REFUSE);
		
		boolean signaturesNotValid;
		try {
			signaturesNotValid = !userRequestValid(buyerAccount, signedObjectPair.getFirst()) || !userRequestValid(sellerAccount, signedObjectPair.getSecond());
		} catch (Exception e) {
			throw new TransactionException(INTERNAL_ERROR);
		}
		if (signaturesNotValid)
			throw new TransactionException(PAYMENT_REFUSE);
		
		if ((buyerAccount.getBalance().subtract(buyerTransaction.getAmount())).compareTo(BigDecimal.ZERO) < 0)
			throw new TransactionException(BALANCE);
		
		try {
			DbTransaction dbTransaction = new DbTransaction(buyerTransaction);
			TransactionDAO.createTransaction(dbTransaction, buyerAccount, sellerAccount);
		} catch (HibernateException e) {
			throw new TransactionException(HIBERNATE_ERROR);
		}
		
		//check if user account balance limit has been exceeded (according to PayOutRules)
		try {
			PayOutRuleService.getInstance().checkBalanceLimitRules(sellerAccount);
		} catch (PayOutRuleNotFoundException | BitcoinException e) {
			// do nothing as user requests actually a transaction and not a payout
		}
		
		SignedObject signedTransaction = null;
		try {
			signedTransaction = KeyHandler.signTransaction(sellerTransaction, Constants.PRIVATEKEY);
		} catch (Exception e) {
			throw new TransactionException(INTERNAL_ERROR);
		}
		
		return signedTransaction;
	}
	
	private boolean transactionNumbersValid(Transaction buyerTx, long buyerTxNr, Transaction sellerTx, long sellerTxNr) {
		return ((buyerTx.getTransactionNrBuyer() == buyerTxNr)
				&& (sellerTx.getTransactionNrBuyer() == buyerTxNr))
				&& (buyerTx.getTransactionNrSeller() == sellerTxNr)
				&& (sellerTx.getTransactionNrSeller() == sellerTxNr);
	}
	
	private boolean transactionRequestsIdentic(Transaction buyerTransaction, Transaction sellerTransaction) {
		return ((buyerTransaction.getBuyerUsername().equals(sellerTransaction.getBuyerUsername())) 
				&& (buyerTransaction.getSellerUsername().equals(sellerTransaction.getSellerUsername()))
				&& (buyerTransaction.getTransactionNrBuyer() == sellerTransaction.getTransactionNrBuyer())
				&& (buyerTransaction.getTransactionNrSeller() == sellerTransaction.getTransactionNrSeller())
				&& (buyerTransaction.getAmount().equals(sellerTransaction.getAmount())));
	}
	
	private boolean userRequestValid(UserAccount userAccount, SignedObject signedObject) throws Exception {
		return KeyHandler.verifyObject(signedObject, userAccount.getPublicKey());
	}
	
}
