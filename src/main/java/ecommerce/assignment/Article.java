package ecommerce.assignment;

public class Article {

    private String url;
    private String title;
    private String content;
    private String imagePath;
    private String englishTitle;

    public Article() {
    }

    public Article(String url, String title, String content, String imagePath) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.imagePath = imagePath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getEnglishTitle() {
        return englishTitle;
    }

    public void setEnglishTitle(String englishTitle) {
        this.englishTitle = englishTitle;
    }
}