package edu.unh.cs753853.team1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;

public class IndexParagraphs {
	
	

    // Empty constructor for IndexParagraphs
    public IndexParagraphs() {

    }

    // Main function, so the program can be run from the command line
    public static void main(String[] args) throws Exception {
        // Relative path definitions
        String indexDir = "index";
        String paragraphFile = "train.test200.cbor.paragraphs";
        
      
    	
  	  // Defaults
      String index = "index";
      String field = "paragraphContent";
      String queries = null;
      int repeat = 0;
      boolean raw = false;
      String queryString = null;
      int hitsPerPage = 10;
      //close

        // Record start time to track how long indexing took
        Date startDate = new Date();
        try {
            // Print the name of the directory that we are indexing into
            System.out.println("Indexing to directory '" + indexDir + "'...");

            // Turn string paths into Path and Dir objects
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            final Path paragraphPath = Paths.get(paragraphFile);

            // Get our analyzer and our IndexWriterConfig
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            // Create a new index every time index is run
            iwc.setOpenMode(OpenMode.CREATE);

            // Get our IndexWriter and index the document
            IndexWriter writer = new IndexWriter(dir, iwc);
            indexParagraphs(writer, paragraphPath);

            // Make sure we close our writer
            writer.close();

            // Get our end time and calculate time to index
            Date end = new Date();
            System.out.println(end.getTime() - startDate.getTime() + " total milliseconds");
        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
        
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

        Analyzer analyzer = new StandardAnalyzer();

        // If no queries specified
        BufferedReader in = null;
        if (queries != null) { // if queries file specified, read queries file
            in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
        } else { // else read queries from System.in from the user
            in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }


        System.out.println();
        // Parse the specified field using the analyzer
        QueryParser parser = new QueryParser(field, analyzer);
        while (true) {
            // Ask the user what kind of scoring function they would like to use
            System.out.println("Which scoring function? (0: default, 1: Term Frequency): ");
            int scoretype = Integer.parseInt(in.readLine());
            if(scoretype == 1)
            {
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
            }
            else if (scoretype == 0) {
                searcher.setSimilarity(IndexSearcher.getDefaultSimilarity());
            }
            else
            {
                System.out.println("Invalid scoring type.");
                continue;
            }
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

    static void indexParagraphs(IndexWriter writer, Path paragraphFile) throws IOException, Exception {
        // Try to open our file for reading
        int count = 0;
        System.out.println("Indexing paragraphs:");
        try(InputStream stream = Files.newInputStream(paragraphFile)){
            // For each paragraph, index it
            for(Data.Paragraph p: DeserializeData.iterableParagraphs(stream)) {
                indexDoc(writer, p);
                count++;
            }
        }
        System.out.print(count + " paragraphs indexed in ");
    }

   //  indexDoc
     //       index the <file> using the given IndexWriter <writer>
     
    static void indexDoc(IndexWriter writer, Data.Paragraph p) throws IOException, Exception {
        // Create our new document
        Document document = new Document();

        // Add the necessary Fields, ID and paragraph content
        Field idField = new StringField("paragraphID", p.getParaId(), Field.Store.YES);
        Field contentField = new TextField("paragraphContent", p.getTextOnly(), Field.Store.YES);

        // Add those fields to our document
        document.add(idField);
        document.add(contentField);

        // Display IDs

        System.out.println(" Paragraph ID:  " + p.getParaId());

        // Add document to the document writer
        writer.addDocument(document);
    }
    
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
