package edu.unh.cs753853.team1;

import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.*;



public class TFIDF_lnc_ltn {
    // Lucene tools
    private IndexSearcher searcher;
    private QueryParser parser;

    // List of pages to query
    private ArrayList<Data.Page> pageList;

    // Number of documents to return
    private int numDocs;

    // Map of queries to map of Documents to scores for that query
    private HashMap<Query, ArrayList<DocumentResults>> queryResults;


    TFIDF_lnc_ltn(ArrayList<Data.Page> pl, int n) throws ParseException, IOException
    {

        numDocs = n; // Get the (max) number of documents to return
        pageList = pl; // Each page title will be used as a query

        // Parse the parabody field using StandardAnalyzer
        parser = new QueryParser("parabody", new StandardAnalyzer());

        // Create an index searcher
        String INDEX_DIRECTORY = "index";
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));

        // Set our own similarity class which computes tf[t,d]
        SimilarityBase lnc_ltn = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float docLen) {
                return (float)(1 + Math.log10(freq));
            }

            @Override
            public String toString() {
                return null;
            }
        };
        searcher.setSimilarity(lnc_ltn);
    }

    /**
     *
     * @param runfile   The name of the runfile to output to
     * @throws IOException
     * @throws ParseException
     */
    public void dumpScoresTo(String runfile) throws IOException, ParseException
    {
        queryResults = new HashMap<>(); // Maps query to map of Documents with TF-IDF score

        for(Data.Page page:pageList)
        {   // For every page in .cbor.outline
            // We need...
            TermResults results = new TermResults(searcher.getIndexReader());          // Mapping of each Document to its score
            ArrayList<TermQuery> terms = new ArrayList<>();             // List of every term in the query
            Query q = parser.parse(page.getPageName());                 // The full query containing all terms
            String qid = page.getPageId();

            for(String term: page.getPageName().split(" "))
            {   // For every word in page name...
                // Take word as query term for parabody
                TermQuery tq = new TermQuery(new Term("parabody", term));
                terms.add(tq);

                // Add one to our term weighting every time it appears in the query
                results.addTermQuery(tq);
            }
            for(TermQuery query: terms)
            {   // For every Term

                // Get the top 100 documents that match our query
                TopDocs tpd = searcher.search(query, numDocs);
                for(int i = 0; i < tpd.scoreDocs.length; i++)
                {   // For every returned document...
                    Document doc = searcher.doc(tpd.scoreDocs[i].doc);                  // Get the document
                    double score = tpd.scoreDocs[i].score * results.termWeight(query);    // Calculate TF-IDF for document

                    DocumentResults dResults = results.getByDocument(query, doc);
                    if(dResults == null)
                    {
                        results.put(query, new DocumentResults(doc));
                        dResults = results.getByDocument(query, doc);
                    }
                    float prevScore = dResults.getScore();
                    dResults.score((float)(prevScore+score));
                    dResults.queryId(qid);
                    dResults.paragraphId(doc.getField("paraid").stringValue());
                    dResults.teamName("team1");
                    dResults.methodName("tf.idf_lnc_ltn");

                    // Store score for later use
                    results.put(query, dResults);
                }
            }

            results.combineTermScores();
            results.normalizeScores();

            // Map our Documents and scores to the corresponding query
            results.rankResults();
            queryResults.put(q, results.getCombinedScores());
        }


        FileWriter runfileWriter = new FileWriter(new File(runfile));
        for(Map.Entry<Query, ArrayList<DocumentResults>> results: queryResults.entrySet())
        {
            ArrayList<DocumentResults> list = results.getValue();
            for(int i = 0; i < list.size(); i++)
            {
                DocumentResults dr = list.get(i);
                //runfileWriter.write(dr.getRunfileString());
                //System.out.println(dr.getRunfileString());
            }
        }


    }



}
