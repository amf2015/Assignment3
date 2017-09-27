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


/*
 * 
 */
class ResultComparator implements Comparator<DocumentResults>
{
    public int compare(DocumentResults d2, DocumentResults d1)
    {
        if(d1.getScore() < d2.getScore())
            return -1;
        if(d1.getScore() == d2.getScore())
            return 0;
        return 1;
    }
}


/*
 * the indexing and querying of documents
 * uses tf-idf: bnn.bnn
 * 
 */
public class TFIDF_bnn_bnn {
	
	private IndexSearcher indexSearcher = null;
	private QueryParser queryParser = null;
	
	// query pages
	private ArrayList<Data.Page> queryPages;
	
	// num docs to return for a query
	private int numDocs = 100;
		
	// map of queries to document results with scores
	HashMap<Query, ArrayList<DocumentResults> > queryResults;
	
	
	// directory  structure..
	static final private String INDEX_DIRECTORY = "index";
	static final private String OUTPUT_DIR = "output";
	
	private String runFile = "/bnn_bnn_scores";
	
	/*
	 * @param pageList
	 * @param maxDox
	 */
	TFIDF_bnn_bnn(ArrayList<Data.Page> pageList, int maxDox) throws IOException, ParseException {
		queryPages = pageList;
		numDocs = maxDox;
		
		queryParser = new QueryParser("parabody", new StandardAnalyzer());
		
		indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));
		
		SimilarityBase bnn = new SimilarityBase() {
			protected float score(BasicStats stats, float freq, float decLen) {
				return freq > 0 ? 1 : 0;
			}
			@Override
			public String toString() {
				return null;
			}
		};
		indexSearcher.setSimilarity(bnn);
	}

	/*
	 *  method to go through and score docs for each query
	 *  @throws ParseException
	 */
	public void doScoring() throws ParseException, IOException {
		queryResults = new HashMap<>();
		
		// run through cbor.outlines for queries
		for(Data.Page page: queryPages) {
			
			HashMap<Document, Float> docScores = new HashMap<>();
			HashMap<Document, DocumentResults> docMap = new HashMap<>();
			HashMap<TermQuery, Float> queryWeights = new HashMap<>(); 
			ArrayList<DocumentResults> docResults = new ArrayList<>();
			ArrayList<TermQuery> queryTerms = new ArrayList<>();  
			PriorityQueue<DocumentResults> docQueue = new PriorityQueue<>(new ResultComparator());
			
            Query qry = queryParser.parse(page.getPageName());      
            String qid = page.getPageId();
			
            
			for(String term: page.getPageName().split(" ")) {
				TermQuery cur = new TermQuery(new Term("parabody", term));
				queryTerms.add(cur);
//				float termFreq = 1;
//				queryWeights.put(cur, termFreq);
			}
			for(TermQuery term: queryTerms) {
				IndexReader indexReader = indexSearcher.getIndexReader();
				// rule of bnn
				float df = 1;
				
				float qWeight = 1;
//				float qWeight = df * (float)(queryWeights.get(term) == null ? 0 : 1); // always 1
				
//				queryWeights.put(term, qWeight); // always 1
				
				TopDocs topDocs = indexSearcher.search(term, numDocs);
				for(int i = 0; i < topDocs.scoreDocs.length; i++) {
					Document doc = indexSearcher.doc(topDocs.scoreDocs[i].doc);
//					float docScore = (float)((topDocs.scoreDocs[i].score == -1) ? 0 : 1);
					float docScore = 1.0f;
					
					DocumentResults res = docMap.get(doc);
					if(res == null) {
						res = new DocumentResults(doc);
						res.queryId(qid);
						res.paragraphId(doc.getField("paraid").stringValue());
						res.teamName("team1");
						res.methodName("tf.idf_bnn_bnn");
					}
//					
					float prevScore = (res.getScore());
					res.score((docScore + prevScore));
					docMap.put(doc, res);
					System.out.println("docScore: " + prevScore + " : " + docScore);
					
					docScores.put(doc, prevScore+docScore);
				}
			}
			
			for(Map.Entry<Document, Float> entry: docScores.entrySet()) {
				Document doc = entry.getKey();
				docQueue.add(docMap.get(doc));
			}
			int curRank = 0;
			DocumentResults curRes;
			while((curRes = docQueue.poll()) != null) {
				curRes.rank(curRank++);
				docResults.add(curRes);
			}
			queryResults.put(qry, docResults);
			
		}
		writeResults();
		
	}
	
	/* 
	 * 
	 */
	private void writeResults() throws IOException {
		System.out.println("Writing...");
		FileWriter writer = new FileWriter(new File(OUTPUT_DIR + runFile));
		for(Map.Entry<Query, ArrayList<DocumentResults>> results: queryResults.entrySet()) {
			ArrayList<DocumentResults> cur = results.getValue();
			for(int i = 0; i < cur.size(); i++) {
				DocumentResults res = cur.get(i);
				writer.write(res.getRunfileString());
			}
		}
		
	}
}
