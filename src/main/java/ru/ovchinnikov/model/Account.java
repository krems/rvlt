package ru.ovchinnikov.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@XmlRootElement
public class Account {
    private static final AtomicLong ID_SEQUENCE = new AtomicLong();
    @XmlElement
    private final long id;
    @XmlElement
    private BigDecimal balance = new BigDecimal(0);

    public Account() {
        this.id = -1;
    }

    private Account(long id) {
        this.id = id;
    }

    public static Account create(long id) {
        return new Account(id);
    }

    public static Account create() {
        return new Account(ID_SEQUENCE.incrementAndGet());
    }

    public long id() {
        return id;
    }

    public BigDecimal balance() {
        return balance;
    }

    public void recharge(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Recharging with negative amount! " + amount);
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Withdrawing with negative amount! " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", balance=" + balance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        return id == account.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
