package edu.unh.cs753853.team1.a1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

/** Simple command-line based search demo. */
public class SearchFiles {

    private SearchFiles() {}

    /** Simple command-line based search demo. */
    public static void main(String[] args) throws Exception
    {
        String usage =
                "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

        // Defaults
        String index = "index";
        String field = "paragraphContent";
        String queries = null;
        int repeat = 0;
        boolean raw = false;
        String queryString = null;
        int hitsPerPage = 10;

        // Check for command-line arguments specified by user
        for(int i = 0;i < args.length;i++) {
            if ("-index".equals(args[i])) {
                index = args[i+1];
                i++;
            } else if ("-field".equals(args[i])) {
                field = args[i+1];
                i++;
            } else if ("-queries".equals(args[i])) {
                queries = args[i+1];
                i++;
            } else if ("-query".equals(args[i])) {
                queryString = args[i+1];
                i++;
            } else if ("-repeat".equals(args[i])) {
                repeat = Integer.parseInt(args[i+1]);
                i++;
            } else if ("-raw".equals(args[i])) {
                raw = true;
            } else if ("-paging".equals(args[i])) {
                hitsPerPage = Integer.parseInt(args[i+1]);
                if (hitsPerPage <= 0) {
                    System.err.println("There must be at least 1 hit per page.");
                    System.exit(1);
                }
                i++;
            }
        }

        // Pass our index to the reader
        // Pass the reader to our searcher
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);

        searcher.setSimilarity(new SimilarityBase() {
            @Override
            protected float score(BasicStats stats, float freq, float docLen) {
                return (freq);
            }

            @Override
            public String toString() {
                return null;
            }
        });
        Analyzer analyzer = new StandardAnalyzer();

        // If no queries specified
        BufferedReader in = null;
        if (queries != null) { // if queries file specified, read queries file
            in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
        } else { // else read queries from System.in from the user
            in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }

        // Parse the specified field using the analyzer
        QueryParser parser = new QueryParser(field, analyzer);
        while (true) {
            // Prompt the user if no queries specified
            if (queries == null && queryString == null) {
                System.out.println("Enter query: ");
            }

            // If queryString is null, read a line from the user
            // else line is queryString
            String line = queryString != null ? queryString : in.readLine();

            // Quit if line is null
            if (line == null || line.length() == -1) {
                break;
            }

            // If line is all whitespace, quit
            line = line.trim();
            if (line.length() == 0) {
                break;
            }

            // Interpret the line as a query
            Query query = parser.parse(line);
            System.out.println("Searching for: " + query.toString(field));


            if (repeat > 0) {                           // repeat & time as benchmark
                Date start = new Date();
                for (int i = 0; i < repeat; i++) {
                    searcher.search(query, 100);
                }
                Date end = new Date();
                System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
            }

            // Displays results in pages containing hitsPerPage results
            doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);

            if (queryString != null) {
                break;
            }
        }
        reader.close();
    }

    /**
     139   * This demonstrates a typical paging search scenario, where the search engine presents
     140   * pages of size n to the user. The user can then go to the next page if interested in
     141   * the next hits.
     142   *
     143   * When the query is executed for the first time, then only enough results are collected
     144   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
     145   * is executed another time and all hits are collected.
     146   *
     147   */
    public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query,
                                      int hitsPerPage, boolean raw, boolean interactive) throws IOException {

        // Collect enough docs to show 5 pages
        TopDocs results = searcher.search(query, 5 * hitsPerPage);
        ScoreDoc[] hits = results.scoreDocs;

        // Get the number of total matching documents
        int numTotalHits = results.totalHits;
        System.out.println(numTotalHits + " total matching documents");

        // Define our start and end points for the current visible page
        int start = 0;
        int end = Math.min(numTotalHits, hitsPerPage);

        while (true) {
            // If we have reached the end of the results
            if (end > hits.length) {
                System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
                System.out.println("Collect more (y/n) ?");
                String line = in.readLine();
                if (line.length() == 0 || line.charAt(0) == 'n') {
                    break;
                }

                hits = searcher.search(query, numTotalHits).scoreDocs;
            }

            // Redefine our end point for the current page
            end = Math.min(hits.length, start + hitsPerPage);

            for (int i = start; i < end; i++) {
                // If raw is specified
                if (raw) {                              // output raw format
                    System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
                    continue;
                }

                // Output the raw score and document number
                System.out.println("Score: "+hits[i].score);

                // Get the current document at index i
                Document doc = searcher.doc(hits[i].doc);
                String paragraphId = doc.get("paragraphID");
                if (paragraphId != null) { // If we have a paragraphID
                    // Print the ID
                    System.out.println((i+1) + ". " +"Id: " +paragraphId);
                    String paragraphContent = doc.get("paragraphContent");
                    if (paragraphContent != null) { // If we have paragraphContent
                        // Print the content matching the paragraphID
                         System.out.println("   Content: " + paragraphContent);
                    }
                } else { // If we have no paragraphID
                    System.out.println((i+1) + ". " + "No path for this document");
                }

            }

            if (!interactive || end == 0) { // !(queries == null && queryString == null) || (end == 0)
                break;
            }

            // If we have not reached the end of our results
            // Print paging options
            if (numTotalHits >= end) {
                boolean quit = false;
                while (true) {
                    System.out.print("Press ");
                    if (start - hitsPerPage >= 0) { // If there are previous pages
                        System.out.print("(p)revious page, ");
                    }
                    if (start + hitsPerPage < numTotalHits) { // If there are more pages of results
                        System.out.print("(n)ext page, ");
                    }
                    System.out.println("(q)uit or enter number to jump to a page.");

                    // Get input from the user
                    String line = in.readLine();
                    if (line.length() == 0 || line.charAt(0)=='q') { // quit
                        quit = true;
                        break;
                    }
                    if (line.charAt(0) == 'p') { // load previous page of results
                        start = Math.max(0, start - hitsPerPage);
                        break;
                    } else if (line.charAt(0) == 'n') { // load next page of results
                        if (start + hitsPerPage < numTotalHits) {
                            start+=hitsPerPage;
                        }
                        break;
                    } else { // check if user entered a page number
                        int page = Integer.parseInt(line);
                        if ((page - 1) * hitsPerPage < numTotalHits) { // if the number is a valid page
                            start = (page - 1) * hitsPerPage;
                            break;
                        } else { // if the number is not a valid page
                            System.out.println("No such page");
                        }
                    }
                }
                if (quit) break;
                // Redefine the end point for our current page
                end = Math.min(numTotalHits, start + hitsPerPage);
            }
        }
    }
}
