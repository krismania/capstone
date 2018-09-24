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

    public String getUserId() {
	return this.userId;
    }

    public String getCName() {
	return this.cName;
    }

    public String getCNumber() {
	return this.cNumber;
    }

    public String getExpDate() {
	return this.expDate;
    }

    public String getBackNumber() {
	return this.backNumber;
    }
}
