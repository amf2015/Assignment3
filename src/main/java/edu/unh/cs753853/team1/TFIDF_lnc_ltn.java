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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TFIDF_lnc_ltn {
    // Lucene tools
    private IndexSearcher searcher;
    private QueryParser parser;

    // Paths to important file locations
    final private String INDEX_DIRECTORY = "index";

    // List of pages to query
    private ArrayList<Data.Page> pagelist;

    // Number of documents to return
    private int numDocs;

    // Map of queries to map of Documents to scores for that query
    HashMap<Query, HashMap<Document, Float>> queryScores;

    TFIDF_lnc_ltn(ArrayList<Data.Page> pl, int n) throws ParseException, IOException
    {
        numDocs = n;
        pagelist = pl;
        parser = new QueryParser("parabody", new StandardAnalyzer());
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));
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

    public void dumpScoresTo(String runfile) throws IOException, ParseException
    {
        queryScores = new HashMap<>();

        HashMap<Query, HashMap<Document, String>> runFileInfo = new HashMap<>();
        for(Data.Page page:pagelist)
        {
            HashMap<Document, Float> scores = new HashMap<>();
            HashMap<TermQuery, Float> queryweights = new HashMap<>();
            ArrayList<TermQuery> queries = new ArrayList<>();
            Query q = parser.parse(page.getPageName());
            for(String term: page.getPageName().split(" "))
            {
                TermQuery tq = new TermQuery(new Term("parabody", term));
                queries.add(tq);
                queryweights.put(tq, queryweights.getOrDefault(tq, 0.0f)+1.0f);
            }
            for(TermQuery query: queries)
            {
                IndexReader reader = searcher.getIndexReader();
                float DF = 0.0f;
                if(reader.docFreq(query.getTerm()) == 0)
                {
                   DF = 1;
                }
                else
                {
                    DF = reader.docFreq(query.getTerm());
                }
                float qTF = (float)(1 + Math.log10(queryweights.get(query)));
                float qIDF = (float)(Math.log10(reader.numDocs()/DF));
                float qWeight = qTF * qIDF;
                queryweights.put(query, qWeight);
                TopDocs tpd = searcher.search(query, numDocs);
                for(int i = 0; i < tpd.scoreDocs.length; i++)
                {
                    Document doc = searcher.doc(tpd.scoreDocs[i].doc);
                    double score = tpd.scoreDocs[i].score * queryweights.get(query);
                    scores.put(doc, (float)(scores.getOrDefault(doc, 0.0f)+score));
                }
            }
            for(Map.Entry<Document, Float> entry: scores.entrySet())
            {
                Document doc = entry.getKey();
                Float score = entry.getValue();
                scores.put(doc, score/scores.size());
            }
            queryScores.put(q, scores);
        }

        for(Map.Entry<Query, HashMap<Document, Float>> queryScore: queryScores.entrySet())
        {
            String query = queryScore.getKey().toString();
            HashMap<Document, Float> scores = queryScore.getValue();
            for(Map.Entry<Document, Float> docScore: scores.entrySet())
            {
                Document d = docScore.getKey();
                Float f = docScore.getValue();
                System.out.println("Query\t" + query);
                System.out.println("Paraid\t" + d.getField("paraid").stringValue());
                System.out.println("Score\t" + f);
            }
        }
    }

}
