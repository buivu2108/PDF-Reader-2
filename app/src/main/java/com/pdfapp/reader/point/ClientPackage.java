package com.pdfapp.reader.point;

import com.android.billingclient.api.ProductDetails;

public class ClientPackage {
    private String packageId;
    private int point;
    private String description;
    private String productId;
    private String price;
    private double amount;
    private String currency;
    private String serverPrice;
    private String text;

    private ProductDetails productDetails;

    public ClientPackage(String packageId, int point, String productId) {
        super();
        this.packageId = packageId;
        this.point = point;
        this.productId = productId;
    }

    /**
     * For Api 135 ListPointActionPacket
     */
    public ClientPackage(String packageId, int point, String description,
                         String productId, String text) {
        super();
        this.packageId = packageId;
        this.point = point;
        this.description = description;
        this.productId = productId;
        this.text = text;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public ProductDetails getProductDetails() {
        return productDetails;
    }

    public void setProductDetails(ProductDetails productDetails) {
        this.productDetails = productDetails;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getServerPrice() {
        return serverPrice;
    }

    public void setServerPrice(String serverPrice) {
        this.serverPrice = serverPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductId() {
        return productId;
    }

    public int getPoint() {
        return point;
    }

    public String getPackageId() {
        return packageId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
