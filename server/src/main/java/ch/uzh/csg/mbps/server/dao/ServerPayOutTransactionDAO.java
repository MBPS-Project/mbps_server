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

import ch.uzh.csg.mbps.server.domain.ServerAccount;
import ch.uzh.csg.mbps.server.domain.ServerPayOutTransaction;
import ch.uzh.csg.mbps.server.service.ServerAccountService;
import ch.uzh.csg.mbps.server.util.Config;
import ch.uzh.csg.mbps.server.util.exceptions.ServerAccountNotFoundException;
import ch.uzh.csg.mbps.server.util.exceptions.TransactionException;
import ch.uzh.csg.mbps.server.util.web.model.HistoryServerPayOutTransaction;

/**
 * DatabaseAccessObject for {@link ServerPayOutTransaction}s. Handles all DB operations
 * regarding {@link ServerPayOutTransaction}s.
 * 
 */
@Repository
public class ServerPayOutTransactionDAO {
	private static Logger LOGGER = Logger.getLogger(ServerPayOutTransactionDAO.class);
	
	@PersistenceContext
	private EntityManager eManager;
	
	@Autowired
	private ServerAccountDAO serverAccountDAO;
	
	@Autowired
	private ServerAccountService serverAccountService;
	
	/**
	 * Returns defined amount of {@link ServerPayOutTransaction}s as an ArrayList. 
	 * Number of POTs and selection is defined in the Config-File and by the parameter "page".
	 * 
	 * @param page which defines which page of ServerPayOutTransaction shall be returned (NrX to NrY)
	 * @return ArrayList<HistoryServerPayOutTransaction> (an array list with the requested amount of PayOutTransactions)
	 */
	public List<HistoryServerPayOutTransaction> getHistory(int page){
		if (page < 0) {
			return null;
		}

		@SuppressWarnings("unchecked")
		List<HistoryServerPayOutTransaction> resultWithAliasedBean = eManager.createQuery(""
				  + "SELECT NEW ch.uzh.csg.mbps.server.util.web.model.HistoryServerPayOutTransaction(pot.timestamp, pot.amount, pot.payout_address, pot.server_account_id) "
				  + "FROM server_pay_out_transaction pot "
				  + "ORDER BY pot.timestamp DESC")
				  .setFirstResult(page * Config.PAY_OUTS_MAX_RESULTS)
				  .setMaxResults(Config.PAY_OUTS_MAX_RESULTS)
				  .getResultList();
		
		return resultWithAliasedBean;
	}
	
	/**
	 * Counts number of {@link ServerPayOutTransaction}-entries for given url and returns
	 * number as long.
	 * 
	 * @param url
	 * @return number of ServerPayOutTransaction
	 * @throws ServerAccountNotFoundException if no POTs are assigned to given ServerAccount
	 */
	public long getHistoryCount(String url) throws ServerAccountNotFoundException {
		CriteriaBuilder cb = eManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ServerPayOutTransaction> root = cq.from(ServerPayOutTransaction.class);
		cq.select(cb.count(root));
		
		Predicate condition = cb.equal(root.get("serverUrl"), url);
		cq.where(condition);
		return eManager.createQuery(cq).getSingleResult();
	}
	
	/**
	 * Verifies a {@link ServerPayOutTransaction} in the DB.
	 * 
	 * @param spot ServerPayOutTransation to be verified.
	 * @throws TransactionException if ServerPayOutTransaction with specified TransactionId does not exist in the database.
	 * @throws HibernateException
	 */
	public void verify(ServerPayOutTransaction spot) throws TransactionException {
		
		
		CriteriaBuilder cb = eManager.getCriteriaBuilder();
		CriteriaQuery<ServerPayOutTransaction> cq = cb.createQuery(ServerPayOutTransaction.class);
		Root<ServerPayOutTransaction> root = cq.from(ServerPayOutTransaction.class);
		
		Predicate condition = cb.equal(root.get("serverTransactionID"), spot.getTransactionID());
		cq.where(condition);
		
		ServerPayOutTransaction existingSPOT = ServerAccountDAO.getSingle(cq, eManager);
		
		if(existingSPOT == null){
			throw new TransactionException("Problem verifying ServerPayOutTransaction. This transaction does not exists.");
		}
				
		if(!existingSPOT.isVerified()){
			existingSPOT.setVerified(true);
			eManager.merge(existingSPOT);
			LOGGER.info("Successfully verified ServerPayOutTransaction with ID: " + existingSPOT.getId());
		}
	}
	
	/**
	 * Saves a new {@link ServerPayOutTransaction} in the database.
	 * 
	 * @param spot ServerPayOutTransaction to save
	 * @throws ServerAccountNotFoundException if defined ServerAccount is not found
	 * @throws HibernateException
	 */
	public void createPayOutTransaction(ServerPayOutTransaction spot) throws ServerAccountNotFoundException {
		eManager.persist(spot);
		//update UserAccountBalance
		ServerAccount serverAccount = serverAccountDAO.getById(spot.getServerAccountID());
		serverAccount.setActiveBalance(serverAccount.getActiveBalance().subtract(spot.getAmount()));
		eManager.merge(serverAccount);
		LOGGER.info("ServerPayOutTransaction created for serverAccount with ID: " + serverAccount.getId() + " Transaction: " + spot.toString());
	}
	
	/**
	 * Returns history of 5 newest {@link ServerPayOutTransaction}s assigned to the
	 * 
	 * @return ArrayList<HistoryServerPayOutTransaction> (an array list with the last 5 PayOutTransactions)
	 * @throws ServerAccountNotFoundException 
	 */
	public List<HistoryServerPayOutTransaction> getLast5ServerAccountTransactions(String url) throws ServerAccountNotFoundException {
		serverAccountService.getByUrl(url);
		
		@SuppressWarnings("unchecked")
		List<HistoryServerPayOutTransaction> resultWithAliasedBean = eManager.createQuery(""
				  + "SELECT NEW ch.uzh.csg.mbps.server.util.web.model.HistoryServerPayOutTransaction((pot.timestamp, pot.amount, pot.payout_address, pot.server_account_id) "
				  + "FROM server_pay_out_transaction pot "
				  + "WHERE pot.server_url == :serverUrl "
				  + "ORDER BY pot.timestamp DESC")
				  .setMaxResults(5)
				  .setParameter("serverUrl", url)
				  .getResultList();

		return resultWithAliasedBean;
	}
}