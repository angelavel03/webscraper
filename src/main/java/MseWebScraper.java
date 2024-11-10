import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MseWebScraper {
    private static boolean issuerContainsDigit(String issuerName) {
        for (int i = 0; i < issuerName.length(); i++) {
            if (Character.isDigit(issuerName.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // Filter 1 (implementation)
    public static List<String> fetchAllIssuerNames(Document document) throws IOException {
        Elements issuers = document.select("select#Code > option");
        List<String> issuerNames = new ArrayList<>();
        for (Element issuer : issuers) {
            String issuerName = issuer.text();
            if (issuerContainsDigit(issuerName)) {
                issuerNames.add(issuerName);
            }
        }
        return issuerNames;
    }

    public static String convertDate(String cellDate) throws ParseException {
        SimpleDateFormat americanFormat = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat macedonianFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date date = americanFormat.parse(cellDate);
        return macedonianFormat.format(date);
    }

    // Filter 2 (implementation)
    public static void writeToDatabase(String issuerName, Elements rows) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("database/%s.csv", issuerName)));
        bw.write("Date;Last trade price;Max;Min;Avg. Price;%chg.;Volume;Turnover in BEST in denars;Total turnover in denars\n");

        List<String> cells = new ArrayList<>();
        for (Element row : rows) {
            String cellText = row.text().trim();

            if(cellText.contains("/")) {
                try {
                    cellText = convertDate(cellText);
                } catch (ParseException e) {
                    e.printStackTrace(System.out);
                }
            }

            cells.add(cellText);

            if(cells.size() == 9) {
                bw.append(String.join(";", cells)).append("\n");
                cells = new ArrayList<>();
            }
        }

        bw.close();
    }

    public static List<String> getLastUpdatedDate(List<String> issuerNames) throws IOException {
        List<String> dates = new ArrayList<>();
        for (String issuerName : issuerNames) {
            BufferedReader br = new BufferedReader(new FileReader(String.format("database/%s.csv", issuerName)));
            br.readLine();
            String date = br.readLine().split(";")[0];
            dates.add(date);
        }
        return dates;
    }

    public static void main(String[] args) throws IOException {
        String url = "https://www.mse.mk/en/stats/symbolhistory/";
        Document document = Jsoup.connect(url + "MPT").get();

        // Filter 1 (method call)
        List<String> issuerNames = fetchAllIssuerNames(document);

        // <-- Filter 2 (method call, little code added) -->
        for (String issuerName : issuerNames) {
            Document documentPerIssuer = Jsoup.connect(url + issuerName).get();
            Elements rows = documentPerIssuer.select("table#resultsTable td");
            writeToDatabase(issuerName, rows);
        }

        System.out.println(getLastUpdatedDate(issuerNames));
        // <-- Filter 2 (method call, little code added) /-->
    }
}
