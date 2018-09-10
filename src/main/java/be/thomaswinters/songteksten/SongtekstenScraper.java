package be.thomaswinters.songteksten;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SongtekstenScraper {

    private final static String BASE_URL = "https://songteksten.nl";

    public static void main(String[] args) throws IOException {
        new SongtekstenScraper().downloadAllFrom(4402, new File(".downloaded//samson"));
    }

    public void downloadAllFrom(int artistId, File toDirectory) throws IOException {
        getAllLyricsUrls(artistId)
                .forEach(url -> {
                    try {
                        List<String> lyrics = Arrays.asList(scrapeLyricsPage(url).split("\n"));
                        String fileName = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")) + ".txt";
                        if (toDirectory.exists() || toDirectory.mkdir()) {
                            Path file = new File(toDirectory, fileName).toPath();
                            Files.write(file, lyrics, Charset.forName("UTF-8"));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public List<String> getAllLyricsUrls(int artistId) throws IOException {
        return getAllLyricsUrls("https://songteksten.nl/artiest/" + artistId);
    }

    public List<String> getAllLyricsUrls(String url) throws IOException {
        Document doc = Jsoup.connect(url).followRedirects(true).timeout(10 * 1000).get();

        return doc.getElementsByClass("table")
                .stream()
                .flatMap(table -> table.getElementsByTag("a").stream())
                .map(a -> a.attr("href"))
                .map(href -> BASE_URL + href)
                .collect(Collectors.toList());

    }


    public String scrapeLyricsPage(String pageUrl) throws IOException {
        Document doc = Jsoup.connect(pageUrl).timeout(10 * 1000).get();

        // Preserve linebreaks & spacing
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        doc.select("br").append("\\n");

        List<String> content = doc
                .getElementsByClass("single")
                .stream()
                .flatMap(
                        element -> element.getElementsByTag("p")
                                .stream()
                                .filter(p -> p.hasAttr("style"))
                )
                .map(Element::text)
                .map(e -> e.replaceAll("\\\\n", "\n"))
                .map(e -> e.replaceAll("\\n", "\n"))
                .map(e -> e.replaceAll("\n ", "\n"))
//                .collect(Collectors.joining("\n"));
                .collect(Collectors.toList());
        System.out.println("content:" + content.stream().collect(Collectors.joining("\n////\n")));
        return correctCommonMistakes(content.stream().collect(Collectors.joining("\n")));
    }

    public String correctCommonMistakes(String lyrics) {
        return lyrics.replaceAll("íƒÆ’í‚©", "é");
    }

}
