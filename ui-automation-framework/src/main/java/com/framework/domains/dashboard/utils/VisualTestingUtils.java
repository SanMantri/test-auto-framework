package com.framework.domains.dashboard.utils;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * VisualTestingUtils - Utilities for visual regression testing
 *
 * Provides functionality for:
 * - Screenshot comparison
 * - Baseline management
 * - Diff image generation
 * - Pixel-level comparison with tolerance
 */
@Slf4j
public class VisualTestingUtils {

    private static final String BASELINE_DIR = "src/test/resources/visual-baselines";
    private static final String ACTUAL_DIR = "target/visual-actual";
    private static final String DIFF_DIR = "target/visual-diffs";

    private final Page page;
    private double pixelTolerance = 0.1;  // 10% tolerance by default
    private double diffThreshold = 0.01;  // 1% max diff allowed

    public VisualTestingUtils(Page page) {
        this.page = page;
        createDirectories();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets pixel tolerance for color comparison (0.0 to 1.0).
     */
    public VisualTestingUtils withPixelTolerance(double tolerance) {
        this.pixelTolerance = tolerance;
        return this;
    }

    /**
     * Sets maximum allowed difference percentage (0.0 to 1.0).
     */
    public VisualTestingUtils withDiffThreshold(double threshold) {
        this.diffThreshold = threshold;
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCREENSHOT CAPTURE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Captures a full page screenshot.
     */
    public byte[] captureFullPage() {
        log.debug("Capturing full page screenshot");
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    /**
     * Captures viewport screenshot.
     */
    public byte[] captureViewport() {
        log.debug("Capturing viewport screenshot");
        return page.screenshot();
    }

    /**
     * Captures element screenshot.
     */
    public byte[] captureElement(String selector) {
        log.debug("Capturing element screenshot: {}", selector);
        return page.locator(selector).screenshot();
    }

    /**
     * Captures element screenshot.
     */
    public byte[] captureElement(Locator locator) {
        log.debug("Capturing element screenshot");
        return locator.screenshot();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BASELINE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Saves screenshot as baseline.
     */
    public void saveBaseline(String name, byte[] screenshot) throws IOException {
        Path baselinePath = getBaselinePath(name);
        Files.createDirectories(baselinePath.getParent());
        Files.write(baselinePath, screenshot);
        log.info("Saved baseline: {}", baselinePath);
    }

    /**
     * Loads baseline screenshot.
     */
    public byte[] loadBaseline(String name) throws IOException {
        Path baselinePath = getBaselinePath(name);
        if (!Files.exists(baselinePath)) {
            throw new IOException("Baseline not found: " + baselinePath);
        }
        return Files.readAllBytes(baselinePath);
    }

    /**
     * Checks if baseline exists.
     */
    public boolean baselineExists(String name) {
        return Files.exists(getBaselinePath(name));
    }

    /**
     * Updates baseline with new screenshot.
     */
    public void updateBaseline(String name, byte[] screenshot) throws IOException {
        saveBaseline(name, screenshot);
        log.info("Updated baseline: {}", name);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPARISON
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Compares current screenshot with baseline.
     * Returns comparison result with diff percentage.
     */
    public ComparisonResult compare(String baselineName, byte[] actual) throws IOException {
        log.info("Comparing screenshot with baseline: {}", baselineName);

        // Save actual for debugging
        Path actualPath = getActualPath(baselineName);
        Files.createDirectories(actualPath.getParent());
        Files.write(actualPath, actual);

        // Load baseline
        byte[] baseline;
        try {
            baseline = loadBaseline(baselineName);
        } catch (IOException e) {
            log.warn("Baseline not found, saving current as baseline: {}", baselineName);
            saveBaseline(baselineName, actual);
            return ComparisonResult.builder()
                .baselineName(baselineName)
                .passed(true)
                .diffPercent(0.0)
                .message("Baseline created")
                .build();
        }

        // Compare images
        BufferedImage baselineImg = ImageIO.read(new ByteArrayInputStream(baseline));
        BufferedImage actualImg = ImageIO.read(new ByteArrayInputStream(actual));

        // Check dimensions
        if (baselineImg.getWidth() != actualImg.getWidth() ||
            baselineImg.getHeight() != actualImg.getHeight()) {

            log.warn("Image dimensions don't match. Baseline: {}x{}, Actual: {}x{}",
                baselineImg.getWidth(), baselineImg.getHeight(),
                actualImg.getWidth(), actualImg.getHeight());

            return ComparisonResult.builder()
                .baselineName(baselineName)
                .passed(false)
                .diffPercent(100.0)
                .message(String.format("Dimension mismatch. Baseline: %dx%d, Actual: %dx%d",
                    baselineImg.getWidth(), baselineImg.getHeight(),
                    actualImg.getWidth(), actualImg.getHeight()))
                .build();
        }

        // Pixel comparison
        int width = baselineImg.getWidth();
        int height = baselineImg.getHeight();
        int totalPixels = width * height;
        int diffPixels = 0;

        BufferedImage diffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int baselinePixel = baselineImg.getRGB(x, y);
                int actualPixel = actualImg.getRGB(x, y);

                if (!pixelsMatch(baselinePixel, actualPixel)) {
                    diffPixels++;
                    diffImg.setRGB(x, y, 0xFFFF0000); // Red for diff
                } else {
                    // Fade the matching pixels
                    int grayValue = toGray(actualPixel);
                    diffImg.setRGB(x, y, (0xFF << 24) | (grayValue << 16) | (grayValue << 8) | grayValue);
                }
            }
        }

        double diffPercent = (double) diffPixels / totalPixels * 100;
        boolean passed = diffPercent <= (diffThreshold * 100);

        log.info("Comparison result: {}% diff, passed={}", String.format("%.2f", diffPercent), passed);

        // Save diff image if there are differences
        if (diffPixels > 0) {
            Path diffPath = getDiffPath(baselineName);
            Files.createDirectories(diffPath.getParent());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(diffImg, "png", baos);
            Files.write(diffPath, baos.toByteArray());

            // Attach diff to Allure report
            Allure.addAttachment(baselineName + " - Diff", "image/png", new ByteArrayInputStream(baos.toByteArray()), "png");
        }

        return ComparisonResult.builder()
            .baselineName(baselineName)
            .passed(passed)
            .diffPercent(diffPercent)
            .diffPixelCount(diffPixels)
            .totalPixels(totalPixels)
            .message(passed ? "Visual comparison passed" : "Visual comparison failed")
            .baselinePath(getBaselinePath(baselineName).toString())
            .actualPath(actualPath.toString())
            .diffPath(diffPixels > 0 ? getDiffPath(baselineName).toString() : null)
            .build();
    }

    /**
     * Compares element screenshot with baseline.
     */
    public ComparisonResult compareElement(String baselineName, String selector) throws IOException {
        byte[] actual = captureElement(selector);
        return compare(baselineName, actual);
    }

    /**
     * Compares full page screenshot with baseline.
     */
    public ComparisonResult compareFullPage(String baselineName) throws IOException {
        byte[] actual = captureFullPage();
        return compare(baselineName, actual);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASSERTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Asserts visual comparison passes.
     */
    public void assertVisualMatch(String baselineName, byte[] actual) throws IOException {
        ComparisonResult result = compare(baselineName, actual);
        if (!result.passed) {
            throw new VisualComparisonException(result);
        }
    }

    /**
     * Asserts element matches baseline.
     */
    public void assertElementMatches(String baselineName, String selector) throws IOException {
        ComparisonResult result = compareElement(baselineName, selector);
        if (!result.passed) {
            throw new VisualComparisonException(result);
        }
    }

    /**
     * Asserts full page matches baseline.
     */
    public void assertPageMatches(String baselineName) throws IOException {
        ComparisonResult result = compareFullPage(baselineName);
        if (!result.passed) {
            throw new VisualComparisonException(result);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MULTI-ELEMENT COMPARISON
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Compares multiple elements and returns aggregated results.
     */
    public Map<String, ComparisonResult> compareElements(Map<String, String> elementsToCompare) throws IOException {
        Map<String, ComparisonResult> results = new HashMap<>();

        for (Map.Entry<String, String> entry : elementsToCompare.entrySet()) {
            String name = entry.getKey();
            String selector = entry.getValue();
            results.put(name, compareElement(name, selector));
        }

        return results;
    }

    /**
     * Asserts all elements match their baselines.
     */
    public void assertAllElementsMatch(Map<String, String> elementsToCompare) throws IOException {
        Map<String, ComparisonResult> results = compareElements(elementsToCompare);

        StringBuilder failures = new StringBuilder();
        for (Map.Entry<String, ComparisonResult> entry : results.entrySet()) {
            if (!entry.getValue().passed) {
                failures.append(String.format("%s: %.2f%% diff\n",
                    entry.getKey(), entry.getValue().diffPercent));
            }
        }

        if (failures.length() > 0) {
            throw new VisualComparisonException("Visual comparison failures:\n" + failures);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean pixelsMatch(int pixel1, int pixel2) {
        int r1 = (pixel1 >> 16) & 0xFF;
        int g1 = (pixel1 >> 8) & 0xFF;
        int b1 = pixel1 & 0xFF;

        int r2 = (pixel2 >> 16) & 0xFF;
        int g2 = (pixel2 >> 8) & 0xFF;
        int b2 = pixel2 & 0xFF;

        double tolerance = pixelTolerance * 255;

        return Math.abs(r1 - r2) <= tolerance &&
               Math.abs(g1 - g2) <= tolerance &&
               Math.abs(b1 - b2) <= tolerance;
    }

    private int toGray(int pixel) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    private Path getBaselinePath(String name) {
        return Paths.get(BASELINE_DIR, sanitizeFileName(name) + ".png");
    }

    private Path getActualPath(String name) {
        return Paths.get(ACTUAL_DIR, sanitizeFileName(name) + ".png");
    }

    private Path getDiffPath(String name) {
        return Paths.get(DIFF_DIR, sanitizeFileName(name) + "-diff.png");
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(BASELINE_DIR));
            Files.createDirectories(Paths.get(ACTUAL_DIR));
            Files.createDirectories(Paths.get(DIFF_DIR));
        } catch (IOException e) {
            log.warn("Could not create visual testing directories", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESULT CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    @lombok.Data
    @lombok.Builder
    public static class ComparisonResult {
        private String baselineName;
        private boolean passed;
        private double diffPercent;
        private int diffPixelCount;
        private int totalPixels;
        private String message;
        private String baselinePath;
        private String actualPath;
        private String diffPath;
    }

    public static class VisualComparisonException extends RuntimeException {
        private final ComparisonResult result;

        public VisualComparisonException(ComparisonResult result) {
            super(String.format("Visual comparison failed for '%s': %.2f%% difference (threshold: %.2f%%)",
                result.baselineName, result.diffPercent, result.diffPercent));
            this.result = result;
        }

        public VisualComparisonException(String message) {
            super(message);
            this.result = null;
        }

        public ComparisonResult getResult() {
            return result;
        }
    }
}
