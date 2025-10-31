package com.example.studiondonationstokvel;

public class Institution {
    private String name;
    private String whatsAppLink;
    private String accountNumber;

    public Institution() {}

    public Institution(String name, String whatsAppLink, String accountNumber) {
        this.name = name;
        this.whatsAppLink = whatsAppLink;
        this.accountNumber = accountNumber;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getWhatsAppLink() { return whatsAppLink; }

    public void setWhatsAppLink(String whatsAppLink) { this.whatsAppLink = whatsAppLink; }

    public String getAccountNumber() { return accountNumber; }

    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
}