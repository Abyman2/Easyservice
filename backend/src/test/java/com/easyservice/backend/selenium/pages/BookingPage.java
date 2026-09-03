package com.easyservice.backend.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class BookingPage {
    private final WebDriver driver;

    private final By quantityField = By.id("quantityInput");
    private final By promoCodeField = By.id("promoCodeInput");
    private final By confirmBookingBtn = By.id("confirmBookingBtn");
    private final By bookingStatusText = By.id("bookingStatusLabel");

    public BookingPage(WebDriver driver) {
        this.driver = driver;
    }

    public void enterQuantity(int quantity) {
        driver.findElement(quantityField).clear();
        driver.findElement(quantityField).sendKeys(String.valueOf(quantity));
    }

    public void enterPromoCode(String promoCode) {
        driver.findElement(promoCodeField).sendKeys(promoCode);
    }

    public void clickConfirmBooking() {
        driver.findElement(confirmBookingBtn).click();
    }

    public String getBookingStatus() {
        return driver.findElement(bookingStatusText).getText();
    }
}
