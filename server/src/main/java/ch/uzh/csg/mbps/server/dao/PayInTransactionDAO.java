package ch.uzh.csg.mbps.server.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ch.uzh.csg.mbps.model.HistoryPayInTransaction;
import ch.uzh.csg.mbps.server.domain.PayInTransaction;
import ch.uzh.csg.mbps.server.domain.UserAccount;
import ch.uzh.csg.mbps.server.util.Config;
import ch.uzh.csg.mbps.server.util.exceptions.UserAccountNotFoundException;

/**
 * DatabaseAccessObject for {@link PayInTransaction}. Handles all DB operations
 * regarding PayInTransactions.
 * 
 */
@Repository
public class PayInTransactionDAO {
	private static Logger LOGGER = Logger.getLogger(PayInTransactionDAO.class);
	
	@PersistenceContext
	private EntityManager em;
	
	@Autowired
	private UserAccountDAO userAccountDAO;

	/**
	 * Saves a new {@link PayInTransaction} in the database.
	 * 
	 * @param tx (PayInTransaction)
	 * @throws HibernateException
	 * @throws UserAccountNotFoundException
	 */
	public void createPayInTransaction(PayInTransaction tx) throws UserAccountNotFoundException {
		em.persist(tx);
		//update UserAccountBalance
		UserAccount userAccount = userAccountDAO.getById(tx.getUserID());
		userAccount.setBalance(userAccount.getBalance().add(tx.getAmount()));
		em.merge(userAccount);
		LOGGER.info("PayInTransaction created and balance for user with ID " + tx.getUserID() + " updated: " + tx.toString());
	}

	/**
	 * Checks if {@link PayInTransaction} is already saved in the database and returns
	 * true (false) if pit doesn't exist yet (already exists).
	 * 
	 * @param pit
	 * @return boolean if PayInTransaction is new (not in DB yet).
	 */
	public boolean isNew(PayInTransaction pit) {
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		
		Root<PayInTransaction> root = cq.from(PayInTransaction.class);
		cq.select(cb.count(root));
		
		Predicate condition1 = cb.equal(root.get("userID"), pit.getUserID());
		Predicate condition2 = cb.equal(root.get("transactionID"), pit.getTransactionID());
		Predicate condition3 = cb.and(condition1, condition2);
		cq.where(condition3);
		
		Long count = em.createQuery(cq).getSingleResult();
		
		return count == 0;
	}

	/**
	 * Returns defined amount of {@link PayInTransaction}s assigned to the given
	 * username as an ArrayList. Number of PITs and selection is defined in the
	 * Config-File and by the parameter "page".
	 * 
	 * @param username
	 * @param page which defines which page of PayInTransaction shall be returned (NrX to NrY)
	 * @return ArrayList<HistoryPayInTransaction>
	 * @throws UserAccountNotFoundException
	 */
	public List<HistoryPayInTransaction> getHistory(String username, int page) throws UserAccountNotFoundException {
		if (page < 0) {
			return null;
		}
		
		UserAccount userAccount = userAccountDAO.getByUsername(username);
		
		@SuppressWarnings("unchecked")
        List<HistoryPayInTransaction> resultWithAliasedBean = em.createQuery(""
				+ "SELECT NEW ch.uzh.csg.mbps.model.HistoryPayInTransaction(pit.timestamp,  pit.amount) "
				+ "FROM PayInTransaction pit "
				+ "WHERE pit.userID = :userid "
				+ "ORDER BY pit.timestamp DESC")
				.setFirstResult(page * Config.PAY_INS_MAX_RESULTS)
				.setMaxResults(Config.PAY_INS_MAX_RESULTS)
				.setParameter("userid", userAccount.getId())
				.getResultList();
		
		return resultWithAliasedBean;
	}
	
	/**
	 * Counts number of {@link PayInTransaction}-entries for given username and returns
	 * number as long.
	 * 
	 * @param username
	 * @return long number of PayInTransactions
	 * @throws UserAccountNotFoundException  if no PITs are assigned to given UserAccount
	 */
	public long getHistoryCount(UserAccount userAccount) throws UserAccountNotFoundException {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<PayInTransaction> root = cq.from(PayInTransaction.class);
		cq.select(cb.count(root));
		
		Predicate condition = cb.equal(root.get("userID"), userAccount.getId());
		cq.where(condition);
		return em.createQuery(cq).getSingleResult();
	}
	
	/**
	 * Returns five newest {@link PayInTransaction}s assigned to the given
	 * username as an ArrayList.
	 * 
	 * @param username
	 * @return ArrayList<HistoryPayInTransaction>
	 * @throws UserAccountNotFoundException
	 */
	public List<HistoryPayInTransaction> getLast5Transactions(UserAccount userAccount) throws UserAccountNotFoundException {
		
		
		@SuppressWarnings("unchecked")
        List<HistoryPayInTransaction> resultWithAliasedBean = em.createQuery(""
				+ "SELECT NEW ch.uzh.csg.mbps.model.HistoryPayInTransaction(pit.timestamp,  pit.amount) "
				+ "FROM PayInTransaction pit "
				+ "WHERE pit.userID = :userid "
				+ "ORDER BY pit.timestamp DESC")
				.setMaxResults(5)
				.setParameter("userid", userAccount.getId())
				.getResultList();
		
		return resultWithAliasedBean;
	}
}
