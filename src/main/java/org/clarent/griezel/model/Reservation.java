package org.clarent.griezel.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

/**
 * @author Guy Mahieu
 * @since 2017-09-25
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column
    private String firstname;

    @Column
    private String lastname;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "nr_of_children")
    private Integer nrOfChildren;

    @Column(name = "nr_of_children_halal")
    private Integer nrOfChildrenHalal;

    @Column(name = "nr_of_children_veggie")
    private Integer nrOfChildrenVeggie;

    @Column(name = "nr_of_adults")
    private Integer nrOfAdults;

    @Column(name = "nr_of_adults_halal")
    private Integer nrOfAdultsHalal;

    @Column(name = "nr_of_adults_veggie")
    private Integer nrOfAdultsVeggie;

    @Column(name = "halal")
    private boolean halal;

    @Column(name = "paid")
    private boolean paid;

    @Column(name = "time_block")
    @Enumerated(EnumType.STRING)
    private TimeBlock timeBlock;

    @Column(name = "reference")
    private String reference;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column
    private boolean deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public TimeBlock getTimeBlock() {
        return timeBlock;
    }

    public void setTimeBlock(TimeBlock timeBlock) {
        this.timeBlock = timeBlock;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Integer getNrOfChildren() {
        return Optional.ofNullable(nrOfChildren).orElse(0);
    }

    public void setNrOfChildren(Integer nrOfChildren) {
        this.nrOfChildren = nrOfChildren;
    }

    public Integer getNrOfAdults() {
        return Optional.ofNullable(nrOfAdults).orElse(0);
    }

    public void setNrOfAdults(Integer nrOfAdults) {
        this.nrOfAdults = nrOfAdults;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isHalal() {
        return halal;
    }

    public void setHalal(boolean halal) {
        this.halal = halal;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }


    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReference() {
        return reference;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getNrOfChildrenHalal() {
        return Optional.ofNullable(nrOfChildrenHalal).orElse(0);
    }

    public void setNrOfChildrenHalal(Integer nrOfChildrenHalal) {
        this.nrOfChildrenHalal = nrOfChildrenHalal;
    }

    public Integer getNrOfChildrenVeggie() {
        return Optional.ofNullable(nrOfChildrenVeggie).orElse(0);
    }

    public void setNrOfChildrenVeggie(Integer nrOfChildrenVeggie) {
        this.nrOfChildrenVeggie = nrOfChildrenVeggie;
    }

    public Integer getNrOfAdultsHalal() {
        return Optional.ofNullable(nrOfAdultsHalal).orElse(0);
    }

    public void setNrOfAdultsHalal(Integer nrOfAdultsHalal) {
        this.nrOfAdultsHalal = nrOfAdultsHalal;
    }

    public Integer getNrOfAdultsVeggie() {
        return Optional.ofNullable(nrOfAdultsVeggie).orElse(0);
    }

    public void setNrOfAdultsVeggie(Integer nrOfAdultsVeggie) {
        this.nrOfAdultsVeggie = nrOfAdultsVeggie;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", nrOfChildren=" + nrOfChildren +
                ", nrOfChildrenHalal=" + nrOfChildrenHalal +
                ", nrOfChildrenVeggie=" + nrOfChildrenVeggie +
                ", nrOfAdults=" + nrOfAdults +
                ", nrOfAdultsHalal=" + nrOfAdultsHalal +
                ", nrOfAdultsVeggie=" + nrOfAdultsVeggie +
                ", halal=" + halal +
                ", timeBlock=" + timeBlock +
                ", reference='" + reference + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", totalAmount=" + totalAmount +
                ", createdAt=" + createdAt +
                '}';
    }
}
