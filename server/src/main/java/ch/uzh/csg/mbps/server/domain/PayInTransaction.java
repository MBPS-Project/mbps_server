package ch.uzh.csg.mbps.server.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Index;

import ch.uzh.csg.mbps.server.dao.UserAccountDAO;
import ch.uzh.csg.mbps.server.util.exceptions.UserAccountNotFoundException;

import com.azazar.bitcoin.jsonrpcclient.Bitcoin.Transaction;

@Entity(name = "PAY_IN_TRANSACTION")
public class PayInTransaction implements Serializable {
	private static final long serialVersionUID = -5777010150563320837L;
	
	@Id
	@SequenceGenerator(name="pk_sequence",sequenceName="pay_in_transaction_id_seq", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE,generator="pk_sequence")
	@Column(name="ID")
	private long id;
	@Column(name="USER_ID")
	@Index(name = "USER_ID_INDEX")
	private long userID;
	@Column(name="TIMESTAMP")
	private Date timestamp;
	@Column(name="AMOUNT", precision = 25, scale=8)
	private BigDecimal amount;
	@Column(name="TX_ID")
	@Index(name = "TX_ID_INDEX")
	private String transactionID;
	
	public PayInTransaction() {
	}
		
	public PayInTransaction(Transaction transaction) throws UserAccountNotFoundException {
		this.userID = UserAccountDAO.getByBTCAddress(transaction.address()).getId();
		this.timestamp = transaction.timeReceived();
		this.amount = BigDecimal.valueOf(transaction.amount());
		this.transactionID = transaction.txId();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getUserID() {
		return userID;
	}

	public void setUserID(long userID) {
		this.userID = userID;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getTransactionID() {
		return transactionID;
	}

	public void setTransactionID(String transactionID) {
		this.transactionID = transactionID;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id: ");
		sb.append(getId());
		sb.append(" userId: ");
		sb.append(getUserID());
		sb.append(" timestamp: ");
		sb.append(getTimestamp());
		sb.append(" amount: ");
		sb.append(getAmount());
		sb.append(" transactionID: ");
		sb.append(getTransactionID());
		return sb.toString();
	}
}
