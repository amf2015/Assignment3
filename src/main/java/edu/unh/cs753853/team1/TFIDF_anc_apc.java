package edu.unh.cs753853.team1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class TFIDF_anc_apc {

	static final private String INDEX_DIRECTORY = "index";
	static final private String query_str = "Infield fly rule"; // For testing.
	static private QueryParser parser = null;
	static private Integer docNum = 100;

	// public static void main(String[] args) throws IOException {
	// HashMap<String, Float> testMap = new HashMap<String, Float>();
	// // IndexReader ir = getInedexReader(INDEX_DIRECTORY);
	// testMap = getRankedDocuments(query_str);
	// System.out.println(testMap);
	//
	// for (Entry<String, Float> entry : testMap.entrySet()) {
	// String key = entry.getKey();
	// Float value = entry.getValue();
	// System.out.println("Key = " + key);
	// System.out.println("Values = " + value);
	// }
	// }

	public static IndexReader getInedexReader(String path) throws IOException {
		return DirectoryReader.open(FSDirectory.open((new File(path).toPath())));
	}

	public void retrieveAllAncApcResults(ArrayList<String> queryList) {
		String method = "AncApc";
		ArrayList<String> runFileStrList = new ArrayList<String>();
		if (queryList != null) {
			for (String queryStr : queryList) {
				HashMap<String, Float> result_map = getRankedDocuments(queryStr);
				int i = 0;
				for (Entry<String, Float> entry : result_map.entrySet()) {

					String runFileString = queryList + " Q0 " + entry.getKey() + " " + i + " " + entry.getValue()
							+ " team1-" + method;
					runFileStrList.add(runFileString);
					i++;
				}

			}
		}

		// Call wirte run file function

	}

	// Go through every term in each document, and find the highest term freq
	// Return a map with doc id, and its highest term freq.
	// Document: paraId,parabody
	public static HashMap<String, Integer> getMapOfDocWithMaxTF(IndexReader ir) throws IOException {

		HashMap<String, Integer> result_map = new HashMap<String, Integer>();

		System.out.println(ir.maxDoc());
		// iterate through documents in index
		for (int i = 0; i < ir.maxDoc(); i++) {
			Document doc = ir.document(i);
			String docId = doc.get("paraid");
			// System.out.println(docId);

			Terms terms = ir.getTermVector(i, "content");// ir.getTermVector(i,
			if (terms != null) {

				TermsEnum itr = terms.iterator();
				BytesRef term = null;
				PostingsEnum postings = null;

				ArrayList<Integer> tfList = new ArrayList<Integer>();
				while ((term = itr.next()) != null) {
					try {
						String termText = term.utf8ToString();
						postings = itr.postings(postings, PostingsEnum.FREQS);
						int num = postings.nextDoc();
						int freq = postings.freq();

						tfList.add(freq);
						// System.out.println("doc:" + docId + ", term: " +
						// termText
						// + ", termFreq = " + freq);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				int max_tf = Collections.max(tfList);

				result_map.put(docId, max_tf);
			}
		}

		return result_map;
	}

	// Get normalized weight for each query term. (apc)
	public static HashMap<Term, Float> getNormMapForEachQueryTerm(IndexReader ir, String queryStr) throws IOException {

		HashMap<Term, Integer> term_tf = new HashMap<Term, Integer>();

		HashMap<Term, Float> term_wt = new HashMap<Term, Float>();

		HashMap<Term, Float> term_norm = new HashMap<Term, Float>();
		int num_N = ir.maxDoc();

		// Get unique terms from query, and the term freq.
		if (queryStr != null) {
			for (String termStr : queryStr.split(" ")) {
				Term term = new Term("parabody", termStr);
				if (term_tf.containsKey(term)) {
					int freq = term_tf.get(term) + 1;
					term_tf.put(term, freq);
				} else {
					term_tf.put(term, 1);
				}
			}
		}

		int max_tf = Collections.max(term_tf.values());

		ArrayList<Float> wt_list = new ArrayList<Float>();

		for (Term term : term_tf.keySet()) {
			float wt;
			int tf = term_tf.get(term);
			int df = (ir.docFreq(term) == 0) ? 1 : ir.docFreq(term);

			float a = getAugmentedWt(tf, max_tf);

			float p = (float) (Math.log10((num_N - df) / df));
			if (p < 0) {
				p = 0;
			}

			wt = (float) a * p;
			wt_list.add(wt);
			term_wt.put(term, wt);
		}

		float c = getCosine(wt_list);

		for (Term term : term_wt.keySet()) {
			float norm = (float) term_wt.get(term) * c;
			term_norm.put(term, norm);
		}
		return term_norm;
	}

	// Retrieve ranked result with score for one query string.
	public static HashMap<String, Float> getRankedDocuments(String queryStr) {

		HashMap<Term, Float> qTerm_norm = new HashMap<Term, Float>();
		HashMap<String, Integer> doc_maxTF = new HashMap<String, Integer>();

		HashMap<String, ArrayList<Float>> doc_wtList = new HashMap<String, ArrayList<Float>>();
		HashMap<String, Float> doc_cos = new HashMap<String, Float>();

		HashMap<String, Float> doc_score = new HashMap<String, Float>();

		try {
			IndexReader ir = getInedexReader(INDEX_DIRECTORY);
			IndexSearcher se = new IndexSearcher(ir);
			se.setSimilarity(getAncApcSimilarityBase());
			parser = new QueryParser("parabody", new StandardAnalyzer());

			doc_maxTF = getMapOfDocWithMaxTF(ir);
			qTerm_norm = getNormMapForEachQueryTerm(ir, queryStr);
			System.out.println(qTerm_norm);
			// Get Cosine value.
			for (Term qTerm : qTerm_norm.keySet()) {
				Query q = parser.parse(qTerm.text());

				TopDocs topDocs = se.search(q, docNum);

				ScoreDoc[] hits = topDocs.scoreDocs;
				// System.out.println("got : " + hits.length);

				for (int i = 0; i < hits.length; i++) {
					Document doc = se.doc(hits[i].doc);
					// System.out.println(
					// (i + 1) + ". " + doc.get("paraid") + " (" + hits[i].score
					// + ") " + doc.get("parabody"));
					String docId = doc.get("paraid");

					int tf = (int) hits[i].score;
					int max_tf = doc_maxTF.get(docId);

					float a = getAugmentedWt(tf, max_tf);

					if (doc_wtList.containsKey(docId)) {
						ArrayList<Float> wt_list = doc_wtList.get(docId);
						wt_list.add(a);
						doc_wtList.put(docId, wt_list);
					} else {
						ArrayList<Float> wt_list = new ArrayList<Float>();
						wt_list.add(a);
						doc_wtList.put(docId, wt_list);
					}
				}
			}

			for (String docId : doc_wtList.keySet()) {

				doc_cos.put(docId, getCosine(doc_wtList.get(docId)));

			}

			// To ensure the accuracy, doing another search.

			for (Term qTerm : qTerm_norm.keySet()) {
				Query q = parser.parse(qTerm.text());

				TopDocs topDocs = se.search(q, docNum);

				ScoreDoc[] hits = topDocs.scoreDocs;

				float q_norm = qTerm_norm.get(qTerm); // apc

				for (int i = 0; i < hits.length; i++) {
					Document doc = se.doc(hits[i].doc);
					// System.out.println(
					// (i + 1) + ". " + doc.get("paraid") + " (" + hits[i].score
					// + ") " + doc.get("parabody"));
					String docId = doc.get("paraid");

					int tf = (int) hits[i].score;
					int max_tf = doc_maxTF.get(docId);

					float a = getAugmentedWt(tf, max_tf);
					float c = doc_cos.get(docId);

					if (doc_score.containsKey(docId)) {
						float score = doc_score.get(docId) + a * c * q_norm;
						doc_score.put(docId, score);
					} else {
						float score = a * c * q_norm;
						doc_score.put(docId, score);
					}
				}
			}

		} catch (Throwable e) {
			e.printStackTrace();
		}

		return sortByValue(doc_score);
	}

	public static SimilarityBase getAncApcSimilarityBase() throws IOException {

		SimilarityBase ancApcSim = new SimilarityBase() {

			@Override
			protected float score(BasicStats stats, float freq, float docLen) {
				return freq;
			}

			// protected void fillBasicStats(BasicStats stats,
			// CollectionStatistics collectionStats,
			// TermStatistics termStats) {
			//
			// }

			@Override
			public String toString() {
				// TODO Auto-generated method stub
				return null;
			}

		};

		return ancApcSim;
	}

	// Augmented Wt
	public static float getAugmentedWt(int tf, int max_tf) {

		return (float) (0.5 + (0.5 * tf / max_tf));
	}

	// Get consine value fro weight list.
	public static float getCosine(List<Float> wt_list) {
		float c = 0;

		float pow_sum = 0;

		for (float f : wt_list) {

			pow_sum = (float) (pow_sum + Math.pow(f, 2));

		}

		c = (float) (1.0 / Math.sqrt(pow_sum));

		return c;
	}

	public static void writeStrListToRunFile(ArrayList<String> strList, String path) {
		// write to run file.
	}

	// Sort Descending HashMap<String, Float>Map by its value
	private static HashMap<String, Float> sortByValue(Map<String, Float> unsortMap) {

		List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(unsortMap.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
			public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		HashMap<String, Float> sortedMap = new LinkedHashMap<String, Float>();
		for (Map.Entry<String, Float> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

}
