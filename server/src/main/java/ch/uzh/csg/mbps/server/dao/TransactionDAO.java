package ch.uzh.csg.mbps.server.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.springframework.stereotype.Repository;

import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.mbps.model.HistoryPayInTransaction;
import ch.uzh.csg.mbps.model.HistoryTransaction;
import ch.uzh.csg.mbps.server.domain.DbTransaction;
import ch.uzh.csg.mbps.server.domain.PayInTransaction;
import ch.uzh.csg.mbps.server.domain.UserAccount;
import ch.uzh.csg.mbps.server.util.Config;
import ch.uzh.csg.mbps.server.util.exceptions.UserAccountNotFoundException;
import ch.uzh.csg.mbps.util.Converter;

/**
 * DatabaseAccessObject for {@link DbTransaction}. Handles all DB operations regarding
 * {@link DbTransaction}s between two {@link UserAccount}s.
 * 
 */
@Repository
public class TransactionDAO {
	private static Logger LOGGER = Logger.getLogger(TransactionDAO.class);

	@PersistenceContext
	private EntityManager em;

	/**
	 * Returns defined amount of {@link DbTransaction}s assigned to the given
	 * username as an ArrayList. Number of Transactions and selection is defined
	 * in the Config-File and by the parameter "page".
	 * 
	 * @param username
	 *            for which history is requested
	 * @param page
	 *            which defines which page of Transactions shall be returned
	 *            (NrX to NrY)
	 * @return ArrayList with requested amount of HistoryTransactions
	 * @throws UserAccountNotFoundException
	 */
	public List<HistoryTransaction> getHistory(UserAccount userAccount, int page) throws UserAccountNotFoundException {
		if (page < 0) {
			return null;
		}
		
		@SuppressWarnings("unchecked")
        List<HistoryTransaction> resultWithAliasedBean = em.createQuery(
				  "SELECT NEW ch.uzh.csg.mbps.model.HistoryTransaction(dbt.timestamp, dbt.usernamePayer, dbt.usernamePayee, dbt.amount, dbt.inputCurrency, dbt.inputCurrencyAmount) "
				+ "FROM DB_TRANSACTION dbt "
				+ "WHERE (dbt.usernamePayer = :username OR dbt.usernamePayee = :username) "
				+ "ORDER BY dbt.timestamp DESC")
				.setParameter("username", userAccount.getUsername())
				.setFirstResult(page * Config.TRANSACTIONS_MAX_RESULTS)
				.setMaxResults(Config.TRANSACTIONS_MAX_RESULTS)
				.getResultList();
		
		return resultWithAliasedBean;
		
		//TODO: why so many joins?
		//UserAccount userAccount = UserAccountService.getInstance().getByUsername(username);
		
		/*List<HistoryTransaction> resultWithAliasedBean = session
				.createSQLQuery(
						"SELECT transaction.timestamp, u2.username as buyer, u1.username as seller, transaction.amount, transaction.input_currency as inputCurrency, transaction.input_currency_amount as inputCurrencyAmount "
								+ "FROM DB_TRANSACTION transaction "
								+ "INNER JOIN user_account u1 on transaction.username_payee = u1.username "
								+ "INNER JOIN user_account u2 on transaction.username_payer = u2.username "
								+ "WHERE transaction.username_payer = :username OR transaction.username_payee = :username "
								+ "ORDER BY transaction.timestamp DESC")
				.addScalar("timestamp")
				.addScalar("buyer")
				.addScalar("seller")
				.addScalar("amount")
				.addScalar("inputCurrency")
				.addScalar("inputCurrencyAmount")
				.setString("username", userAccount.getUsername())
				.setFirstResult(page * Config.TRANSACTIONS_MAX_RESULTS)
				.setMaxResults(Config.TRANSACTIONS_MAX_RESULTS)
				.setFetchSize(Config.TRANSACTIONS_MAX_RESULTS)
				.setResultTransformer(Transformers.aliasToBean(HistoryTransaction.class))
				.list();

		List<HistoryTransaction> results = resultWithAliasedBean;*/
		//TODO: rewrite with JPA, not Hibernate!
		
		//return new ArrayList<HistoryTransaction>();
	}

	/**
	 * Counts number of {@link DbTransaction}-entries for given username and
	 * returns number as long.
	 * 
	 * @param username
	 *            for which Transactions shall be counted.
	 * @return number of Transactions assigned to username.
	 * @throws UserAccountNotFoundException
	 */
	public long getHistoryCount(UserAccount userAccount) throws UserAccountNotFoundException {
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<DbTransaction> root = cq.from(DbTransaction.class);
		cq.select(cb.count(root));
		
		Predicate condition1 = cb.equal(root.get("usernamePayer"), userAccount.getUsername());
		Predicate condition2 = cb.equal(root.get("usernamePayee"), userAccount.getUsername());
		Predicate condition3 = cb.or(condition1, condition2);
		cq.where(condition3);
		return em.createQuery(cq).getSingleResult();
	}

	/**
	 * Saves a new {@link DbTransaction} in the database.
	 * 
	 * @param tx
	 *            to save in the DB
	 * @param buyerAccount
	 *            UserAccount from which transaction-amount is subtracted.
	 * @param sellerAccount
	 *            UserAccount to which transaction-amount is added.
	 * @throws HibernateException
	 */
	public void createTransaction(DbTransaction tx, UserAccount buyerAccount, UserAccount sellerAccount) {
		
		buyerAccount.setBalance(buyerAccount.getBalance().subtract(tx.getAmount()));
		em.merge(buyerAccount);
		sellerAccount.setBalance(sellerAccount.getBalance().add(tx.getAmount()));
		em.merge(sellerAccount);
		em.persist(tx);
		LOGGER.info("Transaction created: " + tx.toString());
	}
	
	/**
	 * Checks if a Transaction with the given parameters does already exist.
	 * 
	 * @param usernamePayer
	 *            the payer's username
	 * @param usernamePayee
	 *            the payee's username
	 * @param currency
	 *            the currency
	 * @param amount
	 *            the amount
	 * @param timestamp
	 *            the payer's timestamp
	 * @return
	 */
	public boolean exists(String usernamePayer, String usernamePayee, Currency currency, long amount, long timestampPayer) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		
		Root<DbTransaction> root = cq.from(DbTransaction.class);
		cq.select(cb.count(root));
		
		
		Predicate condition1 = cb.equal(root.get("usernamePayer"), usernamePayer);
		Predicate condition2 = cb.equal(root.get("usernamePayee"), usernamePayee);
		Predicate condition3= cb.equal(root.get("currency"), currency.getCurrencyCode());
		Predicate condition4 = cb.equal(root.get("amount"), Converter.getBigDecimalFromLong(amount));
		Predicate condition5= cb.equal(root.get("timestampPayer"), timestampPayer);
		
		Predicate condition6 = cb.and(condition1, condition2, condition3, condition4, condition5);
		
		cq.where(condition6);
		Long count = em.createQuery(cq).getSingleResult();
		
		return count > 0;
	}

	/**
	 * Returns 5 newest Transactions as {@link HistoryTransaction}s in
	 * descending order.
	 * 
	 * @param username
	 * @return ArrayList<HistoryTransaction>
	 * @throws UserAccountNotFoundException
	 */
	public List<HistoryTransaction> getLast5Transactions(UserAccount userAccount) throws UserAccountNotFoundException {
		
		@SuppressWarnings("unchecked")
        List<HistoryTransaction> resultWithAliasedBean = em.createQuery(
				  "SELECT NEW ch.uzh.csg.mbps.model.HistoryTransaction(dbt.timestamp, dbt.usernamePayer, dbt.usernamePayee, dbt.amount, dbt.inputCurrency, dbt.inputCurrencyAmount) "
				+ "FROM DB_TRANSACTION dbt "
				+ "WHERE (dbt.usernamePayer = :username OR dbt.usernamePayee = :username) "
				+ "ORDER BY dbt.timestamp DESC")
				.setParameter("username", userAccount.getUsername())
				.setMaxResults(5)
				.getResultList();
		
		return resultWithAliasedBean;
	}

	public List<HistoryTransaction> getAll() {		
		@SuppressWarnings("unchecked")
        List<HistoryTransaction> resultWithAliasedBean = em.createQuery(
				  "SELECT NEW ch.uzh.csg.mbps.model.HistoryTransaction(dbt.timestamp,  dbt.usernamePayer, dbt.usernamePayee, dbt.amount, dbt.inputCurrency, dbt.inputCurrencyAmount) "
				+ "FROM DB_TRANSACTION dbt")
				.getResultList();
		
		return resultWithAliasedBean;
    }
}
