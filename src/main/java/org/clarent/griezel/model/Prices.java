package org.clarent.griezel.model;

import java.math.BigDecimal;

/**
 * @author Guy Mahieu
 * @since 2017-09-26
 */
public enum Prices {

    CHILD(new BigDecimal("5.00")),
    ADULT(new BigDecimal("7.00"));

    private final BigDecimal price;

    Prices(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getPrice(int nrOfParticipants) {
        return price.multiply(new BigDecimal(nrOfParticipants));
    }

}
