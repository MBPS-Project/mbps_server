package ch.uzh.csg.mbps.server.domain;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Index;

import com.azazar.bitcoin.jsonrpcclient.Bitcoin.Transaction;

@Entity(name="SERVER_TRANSACTION")
public class ServerTransaction {

	@Id
	@SequenceGenerator(name="pk_sequence",sequenceName="server_transaction_id_seq", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE,generator="pk_sequence")
	@Column(name="ID")
	private long id;
	@Column(name="TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp;
	@Column(name="AMOUNT", precision = 25, scale=8)
	private BigDecimal amount;
	@Column(name="BTC_ADDRESS")
	@Index(name = "BTC_ADDRESS_INDEX")
	private String btcAddress;
	@Column(name="TRANSACTION_ID")
	@Index(name = "TRANSACTION_ID_INDEX")
	private String transactionID;
	@Column(name="VERIFIED")
	private boolean verified;
	@Column(name="RECEIVED")
	private boolean received;

	public ServerTransaction(){
	}
	
	public ServerTransaction(Transaction tx) {
		this.timestamp = tx.time();
		this.amount = new BigDecimal(tx.amount());
		this.btcAddress = tx.address();
		this.verified = false;
		this.transactionID = tx.txId();
//		this.received = ;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

	public String getBtcAddress() {
		return btcAddress;
	}

	public void setBtcAddress(String btcAddress) {
		this.btcAddress = btcAddress;
	}

	public String getTransactionID() {
		return transactionID;
	}

	public void setTransactionID(String transactionID) {
		this.transactionID = transactionID;
	}

	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	public boolean isReceived() {
		return received;
	}

	public void setReceived(boolean received) {
		this.received = received;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id: ");
		sb.append(getId());
		sb.append(" amount: ");
		sb.append(getAmount());
		sb.append(" timestamp: ");
		sb.append(getTimestamp());
		sb.append(" verified: ");
		sb.append(isVerified());
		sb.append(" address: ");
		sb.append(getBtcAddress());
		sb.append(" received: ");
		sb.append(isReceived());
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		//TODO: mehmet is this needed??
		if (o == null)
			return false;

		if (o == this)
			return true;

//		if (!(o instanceof ))
//			return false;

		ServerTransaction other = (ServerTransaction) o;
		return new EqualsBuilder().append(getId(), other.getId()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(47, 83).append(getId())
				.append(getTimestamp())
				.append(getBtcAddress())
				.append(isVerified())
				.append(getAmount())
				.append(isReceived())
				.append(getTransactionID())
				.toHashCode();
	}
}