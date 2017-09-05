package ru.ovchinnikov.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;

@XmlRootElement
public class Transaction {
    @XmlElement
    private final long from;
    @XmlElement
    private final long to;
    @XmlElement
    private final BigDecimal amount;
    @XmlElement
    private final String description;
    @XmlElement
    private final long timestamp;

    public Transaction() {
        this.from = -1;
        this.to = -1;
        this.amount = null;
        this.description = "";
        this.timestamp = -1;
    }

    private Transaction(long from, long to, BigDecimal amount, String description, long timestamp) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
    }

    public static Transaction create(long from, long to, BigDecimal amount, String description, long timestamp) {
        return new Transaction(from, to, amount, description, timestamp);
    }

    public long from() {
        return from;
    }

    public long to() {
        return to;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String description() {
        return description;
    }

    public long timestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transaction that = (Transaction) o;

        if (from != that.from) return false;
        if (to != that.to) return false;
        if (amount != null ? !amount.equals(that.amount) : that.amount != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (from ^ (from >>> 32));
        result = 31 * result + (int) (to ^ (to >>> 32));
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "from=" + from +
                ", to=" + to +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
