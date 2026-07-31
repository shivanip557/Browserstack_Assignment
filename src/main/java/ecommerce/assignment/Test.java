package ecommerce.assignment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Test {
	static WebDriver driver;
	static WebDriverWait wait;

	/**
	 * Translates Spanish text to English using the free MyMemory API
	 * (libretranslate.com's public endpoint now requires an API key, which is
	 * why the previous implementation always returned null).
	 * Falls back to the original Spanish text if translation fails, so callers
	 * never receive null.
	 */
	public static String translateTitle(String spanishTitle) {

		try {
			Response response = RestAssured
					.given()
					.queryParam("q", spanishTitle)
					.queryParam("langpair", "es|en")
					.get("https://api.mymemory.translated.net/get");

			String translated = response.jsonPath().getString("responseData.translatedText");

			if (translated == null || translated.isBlank()) {
				System.out.println("Translation returned empty for: " + spanishTitle);
				return spanishTitle;
			}

			return translated;

		} catch (Exception e) {
			System.out.println("Translation failed for: \"" + spanishTitle + "\" (" + e.getMessage() + ")");
			return spanishTitle; // never return null - keeps downstream code NPE-safe
		}
	}

	/**
	 * Picks the actual headline link for an article card.
	 * The first <a> tag inside an <article> is usually the section/kicker tag
	 * (e.g. a small "Opinión" label that links back to the section index), NOT
	 * the headline. That was the cause of every "article" resolving back to
	 * the section page (h1 == "Opinión", duplicate content across articles).
	 * We target the headline anchor (inside h2) directly, with a fallback.
	 */
	private static WebElement getHeadlineLink(WebElement articleCard) {
		try {
			return articleCard.findElement(By.cssSelector("h2 a"));
		} catch (Exception e) {
			List<WebElement> anchors = articleCard.findElements(By.tagName("a"));
			if (anchors.isEmpty()) {
				throw new RuntimeException("No links found in article card");
			}
			// Headline link is typically the last anchor in the card, not the first
			return anchors.get(anchors.size() - 1);
		}
	}

	public static void main(String[] args) throws TimeoutException, IOException {

		driver = new ChromeDriver();
		// Open El Pais
		driver.get("https://elpais.com");
		driver.manage().window().maximize();
		wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		WebElement acceptBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='Accept']")));

		acceptBtn.click();
		System.out.println("Cookie banner accepted.");
		String alltitle = driver.getTitle();
		System.out.println(alltitle);
		String language = driver.findElement(By.tagName("html"))
				.getAttribute("lang");

		System.out.println("Language : " + language);
		// Go to Opinion
		driver.findElement(By.xpath("//*[@id=\"csw\"]/div[1]/nav/div/a[2]")).click();

		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article")));

		List<String> articleLinks = new ArrayList<>();

		List<WebElement> articles = driver.findElements(By.cssSelector("article"));

		// Collect first five URLs - using the fixed headline-link selector
		for (int i = 0; i < 5 && i < articles.size(); i++) {

			WebElement link = getHeadlineLink(articles.get(i));
			String href = link.getAttribute("href");

			if (href != null && !href.isBlank()) {
				articleLinks.add(href);
			}
		}

		List<String> translatedTitles = new ArrayList<>();

		for (int i = 0; i < articleLinks.size(); i++) {

			driver.get(articleLinks.get(i));

			wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));

			String title = driver.findElement(By.tagName("h1")).getText();

			// Safety guard: if we somehow still landed on the section page instead of
			// an actual article, skip it rather than silently reprinting the index.
			if (title == null || title.isBlank() || title.trim().equalsIgnoreCase("Opinión")) {
				System.out.println("Skipping article " + (i + 1) + " - landed on section page instead of an article (link: " + articleLinks.get(i) + ")");
				System.out.println("=================================================");
				continue;
			}

			String englishTitle = translateTitle(title);
			translatedTitles.add(englishTitle);

			System.out.println("Spanish Title : " + title);
			System.out.println("English Title : " + englishTitle);
			System.out.println();

			System.out.println("=================================================");
			System.out.println("Article " + (i + 1));
			System.out.println("Title : " + title);
			System.out.println();

			// =========================
			// Read Content
			// =========================

			List<WebElement> paragraphs = driver.findElements(By.cssSelector("article p"));

			StringBuilder content = new StringBuilder();

			for (WebElement paragraph : paragraphs) {
				content.append(paragraph.getText()).append("\n");
			}

			System.out.println("Content");
			System.out.println(content.toString());

			// =========================
			// Download Image
			// =========================

			try {
				WebElement image = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.cssSelector("figure img")));

				String imageURL = image.getAttribute("src");

				URL url = new URL(imageURL);

				File directory = new File("images");

				if (!directory.exists()) {
					directory.mkdir();
				}

				FileUtils.copyURLToFile(
						url,
						new File("images/article" + (i + 1) + ".jpg"));

				System.out.println("Image Downloaded Successfully");

			} catch (Exception e) {
				System.out.println("No Cover Image Found");
			}

			System.out.println("=================================================");
		}

		Map<String, Integer> wordCount = new HashMap<>();

		for (String title : translatedTitles) {

			if (title == null) {
				continue; // defense-in-depth; translateTitle no longer returns null anyway
			}

			String[] words = title
					.toLowerCase()
					.replaceAll("[^a-z ]", "")
					.split("\\s+");

			for (String word : words) {

				if (word.isBlank())
					continue;

				wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
			}
		}

		System.out.println("\nRepeated Words:");

		for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {

			if (entry.getValue() > 2) {
				System.out.println(entry.getKey() + " -> " + entry.getValue());
			}
		}

		driver.quit();
	}
}