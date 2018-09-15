package model;

public class CreditCard {

    private final String userId;
    private final String cName;
    private final String cNumber;
    private final String expDate;
    private final String backNumber;

    protected CreditCard(String userId, String cName, String cNumber, String expDate, String backNumber) {
	this.userId = userId;
	this.cName = cName;
	this.cNumber = cNumber;
	this.expDate = expDate;
	this.backNumber = backNumber;
    }
}