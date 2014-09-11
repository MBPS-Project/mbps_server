package ch.uzh.csg.mbps.server.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import ch.uzh.csg.mbps.server.domain.ServerAccountTasks;
import ch.uzh.csg.mbps.server.util.exceptions.ServerAccountTasksAlreadyExists;

/**
 * DatabaseAccessObject for {@link ServerAccountTasks}. Handles all DB operations
 * regarding {@link ServerAccountTasks}.
 * 
 */
@Repository
public class ServerAccountTasksDAO {
	private static Logger LOGGER = Logger.getLogger(ServerAccountTasksDAO.class);

	@PersistenceContext
	private EntityManager em;
	
	public void persistCreateNewAccount(ServerAccountTasks account){
		em.persist(account);
		em.flush();
		LOGGER.info("Server Account saved: serverAccount token: " + account.getToken() + ", url: " + account.getUrl());
	}
	
	public void saveUpdateTrustLevel(ServerAccountTasks account) {
		
	}
	
	public void saveDeleteAccount(ServerAccountTasks account){
		
	}
	
	public ServerAccountTasks getAccountTasksByUrl(String url){
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		Predicate condition = cb.equal(root.get("url"), url);
		cq.where(condition);
		
		ServerAccountTasks account = getSingle(cq, em);
		return account;
	}

	public ServerAccountTasks getAccountTasksByToken(String token){
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		Predicate condition = cb.equal(root.get("token"), token);
		cq.where(condition);
		
		ServerAccountTasks account = getSingle(cq, em);
		return account;
	}
	
	/**
	 * 
	 * @param cq
	 * @param em
	 * @return
	 */
	public static<K> K getSingle(CriteriaQuery<K> cq, EntityManager em) {
		List<K> list =  em.createQuery(cq).getResultList();
		if(list.size() == 0) {
			return null;
		}
		return list.get(0);
	}

	/**
	 * 
	 * @param code
	 * @param url
	 */
	public void delete(int type, String url) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		Predicate condition = cb.equal(root.get("url"), url);
		Predicate condition2 = cb.equal(root.get("type"), type);
		Predicate condition3 = cb.and(condition, condition2);
		cq.where(condition3);
		
		ServerAccountTasks account = getSingle(cq, em);

		em.getTransaction().begin();
		em.remove(account);
		em.getTransaction().commit();
	}

	/**
	 * 
	 * @param url
	 * @return
	 * @throws ServerAccountTasksAlreadyExists 
	 */
	public ServerAccountTasks checkIfExists(String url) throws ServerAccountTasksAlreadyExists  {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);
		
		Predicate condition = cb.equal(root.get("url"), url);
		cq.where(condition);
		
		ServerAccountTasks account = ServerAccountTasksDAO.getSingle(cq, em);
		
		if(account == null){
			throw new ServerAccountTasksAlreadyExists();
		}
		return account;
	}

	/**
	 * Gets all {@link ServerAccountTasks} by given parameter by type
	 * @param type 
	 * 
	 * @return
	 */
	public List<ServerAccountTasks> getAllAccountTasksBySubject(int type) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServerAccountTasks> cq = cb.createQuery(ServerAccountTasks.class);
		Root<ServerAccountTasks> root = cq.from(ServerAccountTasks.class);

		Predicate condition = cb.equal(root.get("type"), type);
		cq.where(condition);
		
		cq.orderBy(cb.desc(root.get("creationDate")));
		List<ServerAccountTasks> resultWithAliasedBean = em.createQuery(cq)
				.getResultList();

		return resultWithAliasedBean;
	}
	
}
