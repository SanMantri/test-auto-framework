package com.framework.core.base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * BasePage - Foundation for all Page Objects
 *
 * Provides:
 * - Common wait methods
 * - Element interaction utilities
 * - Screenshot helpers
 * - Allure step logging
 */
@Slf4j
public abstract class BasePage {

    protected final Page page;
    protected final String baseUrl;

    // Default timeouts
    protected static final int DEFAULT_TIMEOUT = 30000;
    protected static final int SHORT_TIMEOUT = 5000;
    protected static final int LONG_TIMEOUT = 60000;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public BasePage(Page page) {
        this.page = page;
        this.baseUrl = System.getProperty("framework.base-url", "http://localhost:3000");
    }

    public BasePage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ABSTRACT METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Implement to define how to wait for page readiness.
     * Called after navigation and reload.
     */
    protected abstract void waitForPageLoad();

    /**
     * Implement to return page-specific validation.
     */
    public boolean isDisplayed() {
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WAIT UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    protected void waitForVisible(String selector) {
        page.waitForSelector(selector,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT));
    }

    protected void waitForVisible(String selector, int timeout) {
        page.waitForSelector(selector,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeout));
    }

    protected void waitForHidden(String selector) {
        page.waitForSelector(selector,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(DEFAULT_TIMEOUT));
    }

    protected void waitForHidden(String selector, int timeout) {
        page.waitForSelector(selector,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(timeout));
    }

    protected void waitForEnabled(String selector) {
        page.locator(selector).waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE));
        page.waitForFunction(
            String.format("!document.querySelector('%s').disabled", selector));
    }

    protected void waitForNetworkIdle() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    protected void waitForDomContentLoaded() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    protected void waitForText(String selector, String text) {
        page.locator(selector).waitFor();
        page.waitForFunction(
            String.format("document.querySelector('%s')?.textContent?.includes('%s')",
                selector.replace("'", "\\'"), text.replace("'", "\\'")));
    }

    protected void waitForUrl(String urlPattern) {
        page.waitForURL(Pattern.compile(urlPattern));
    }

    protected void waitForUrl(String urlPattern, int timeout) {
        page.waitForURL(Pattern.compile(urlPattern),
            new Page.WaitForURLOptions().setTimeout(timeout));
    }

    protected void waitFor(int milliseconds) {
        page.waitForTimeout(milliseconds);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ELEMENT INTERACTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Click on {selector}")
    protected void click(String selector) {
        log.debug("Click: {}", selector);
        page.click(selector);
    }

    @Step("Click on {selector} and wait for {waitForSelector}")
    protected void clickAndWait(String selector, String waitForSelector) {
        log.debug("Click: {} and wait for: {}", selector, waitForSelector);
        page.click(selector);
        waitForVisible(waitForSelector);
    }

    @Step("Fill {selector} with value")
    protected void fill(String selector, String value) {
        log.debug("Fill: {} with: {}", selector, maskSensitive(value));
        page.fill(selector, value);
    }

    @Step("Clear and fill {selector}")
    protected void clearAndFill(String selector, String value) {
        log.debug("Clear and fill: {} with: {}", selector, maskSensitive(value));
        page.locator(selector).clear();
        page.fill(selector, value);
    }

    @Step("Type {selector} with value (character by character)")
    protected void type(String selector, String value) {
        log.debug("Type: {} with: {}", selector, maskSensitive(value));
        page.locator(selector).type(value);
    }

    @Step("Select option {value} in {selector}")
    protected void selectOption(String selector, String value) {
        log.debug("Select: {} in: {}", value, selector);
        page.selectOption(selector, value);
    }

    @Step("Select option by label {label} in {selector}")
    protected void selectOptionByLabel(String selector, String label) {
        log.debug("Select by label: {} in: {}", label, selector);
        page.selectOption(selector, new SelectOption().setLabel(label));
    }

    @Step("Check {selector}")
    protected void check(String selector) {
        log.debug("Check: {}", selector);
        page.check(selector);
    }

    @Step("Uncheck {selector}")
    protected void uncheck(String selector) {
        log.debug("Uncheck: {}", selector);
        page.uncheck(selector);
    }

    protected String getText(String selector) {
        return page.locator(selector).textContent();
    }

    protected String getInnerText(String selector) {
        return page.locator(selector).innerText();
    }

    protected String getValue(String selector) {
        return page.inputValue(selector);
    }

    protected String getAttribute(String selector, String attribute) {
        return page.locator(selector).getAttribute(attribute);
    }

    protected boolean isVisible(String selector) {
        return page.locator(selector).isVisible();
    }

    protected boolean isEnabled(String selector) {
        return page.locator(selector).isEnabled();
    }

    protected boolean isChecked(String selector) {
        return page.locator(selector).isChecked();
    }

    protected boolean isEditable(String selector) {
        return page.locator(selector).isEditable();
    }

    protected int count(String selector) {
        return page.locator(selector).count();
    }

    protected List<String> getAllTexts(String selector) {
        return page.locator(selector).allTextContents();
    }

    protected List<String> getAllInnerTexts(String selector) {
        return page.locator(selector).allInnerTexts();
    }

    protected Locator locator(String selector) {
        return page.locator(selector);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Navigate to {path}")
    protected void navigateTo(String path) {
        String fullUrl = path.startsWith("http") ? path : baseUrl + path;
        log.info("Navigate to: {}", fullUrl);
        page.navigate(fullUrl);
        waitForPageLoad();
    }

    @Step("Reload page")
    protected void reload() {
        log.debug("Reload page");
        page.reload();
        waitForPageLoad();
    }

    @Step("Go back")
    protected void goBack() {
        log.debug("Go back");
        page.goBack();
        waitForPageLoad();
    }

    @Step("Go forward")
    protected void goForward() {
        log.debug("Go forward");
        page.goForward();
        waitForPageLoad();
    }

    protected String getCurrentUrl() {
        return page.url();
    }

    protected String getTitle() {
        return page.title();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCREENSHOTS
    // ═══════════════════════════════════════════════════════════════════════════

    protected byte[] captureScreenshot() {
        return page.screenshot();
    }

    protected byte[] captureFullPageScreenshot() {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    protected byte[] captureElementScreenshot(String selector) {
        return page.locator(selector).screenshot();
    }

    @Step("Capture screenshot: {name}")
    protected void attachScreenshot(String name) {
        byte[] screenshot = captureFullPageScreenshot();
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), ".png");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KEYBOARD & MOUSE
    // ═══════════════════════════════════════════════════════════════════════════

    protected void pressKey(String key) {
        page.keyboard().press(key);
    }

    protected void pressEnter() {
        page.keyboard().press("Enter");
    }

    protected void pressEscape() {
        page.keyboard().press("Escape");
    }

    protected void pressTab() {
        page.keyboard().press("Tab");
    }

    protected void hover(String selector) {
        page.hover(selector);
    }

    protected void doubleClick(String selector) {
        page.dblclick(selector);
    }

    protected void rightClick(String selector) {
        page.click(selector, new Page.ClickOptions().setButton(MouseButton.RIGHT));
    }

    protected void dragAndDrop(String source, String target) {
        page.dragAndDrop(source, target);
    }

    protected void scrollToElement(String selector) {
        page.locator(selector).scrollIntoViewIfNeeded();
    }

    protected void scrollToBottom() {
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
    }

    protected void scrollToTop() {
        page.evaluate("window.scrollTo(0, 0)");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FRAMES & DIALOGS
    // ═══════════════════════════════════════════════════════════════════════════

    protected FrameLocator frameLocator(String selector) {
        return page.frameLocator(selector);
    }

    protected void acceptDialog() {
        page.onDialog(Dialog::accept);
    }

    protected void dismissDialog() {
        page.onDialog(Dialog::dismiss);
    }

    protected void handleDialog(String promptText) {
        page.onDialog(dialog -> dialog.accept(promptText));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JAVASCRIPT EXECUTION
    // ═══════════════════════════════════════════════════════════════════════════

    protected Object evaluate(String script) {
        return page.evaluate(script);
    }

    protected Object evaluate(String script, Object arg) {
        return page.evaluate(script, arg);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    private String maskSensitive(String value) {
        if (value == null) return null;
        if (value.length() <= 4) return "****";
        // Check if it looks like a password or sensitive data
        return value.length() > 8 ? value.substring(0, 2) + "****" + value.substring(value.length() - 2) : value;
    }

    protected Page getPage() {
        return page;
    }
}
