/**
 * 
 */
package edu.unh.cs753853;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import edu.unh.cs.treccartool.read_data.DeserializeData.RuntimeCborException;

/**
 * @author Bindu Kumari ,Austin FishBaugh ,Daniel Lamkin
 * 
 */
public class Spearmencoefficient {

	/**
	 * @param args
	 */
	private IndexSearcher is = null;
	private QueryParser qp = null;
	private boolean customScore = false;
	static float ndcgValue;

	HashMap<String, HashMap<String, Integer>> luceneSpearManMap = new HashMap<String, HashMap<String, Integer>>();
	HashMap<String, HashMap<String, Integer>> CustomSpearManMap = new HashMap<String, HashMap<String, Integer>>();

	// for keeping difference

	HashMap<String, HashMap<String, Integer>> luceneCustomDifference = new HashMap<String, HashMap<String, Integer>>();

	Query q;
	TopDocs topdocs;
	static ScoreDoc[] returnedDocs;
	String filename;

	static final String Output_Directory = "output";
	static final String Index_Directory = "index/dir";
	static final String Cbor_File = "test200.cbor/train.test200.cbor.paragraphs";
	static final String Cbor_Outline = "test200.cbor/train.test200.cbor.outlines";
	static final String Qrels_File = "test200.cbor/train.test200.cbor.article.qrels";

	private static final float NaN = 0;
	Map<String, List<String>> spearManMap = new HashMap<String, List<String>>();

	/* For indexing paragraph */
	public void indexAllParagraphs() throws CborException, IOException {
		Directory indexdirectory = FSDirectory.open((new File(Index_Directory))
				.toPath());
		IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
		conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter iw = new IndexWriter(indexdirectory, conf);
		for (Data.Paragraph p : DeserializeData
				.iterableParagraphs(new FileInputStream(new File(Cbor_File)))) {
			this.indexPara(iw, p);
		}
		iw.close();
	}

	public void indexPara(IndexWriter iw, Data.Paragraph para)
			throws IOException {
		Document paradoc = new Document();
		paradoc.add(new StringField("paraid", para.getParaId(), Field.Store.YES));
		paradoc.add(new TextField("parabody", para.getTextOnly(),
				Field.Store.YES));
		iw.addDocument(paradoc);
	}

	/***************************************** For search *****************************************************************/

	public void performSearch(String queryString, int n) throws IOException,
			ParseException {
		if (is == null) {
			is = new IndexSearcher(DirectoryReader.open(FSDirectory
					.open((new File(Index_Directory).toPath()))));
		}

		if (customScore) {
			SimilarityBase mySimiliarity = new SimilarityBase() {
				protected float score(BasicStats stats, float freq, float docLen) {
					return freq;
				}

				@Override
				public String toString() {
					return null;
				}
			};
			is.setSimilarity(mySimiliarity);
		}

		if (qp == null) {
			qp = new QueryParser("parabody", new StandardAnalyzer());
		}

		System.out.println("Query String: " + queryString);
		q = qp.parse(queryString);
		topdocs = is.search(q, n);
		returnedDocs = topdocs.scoreDocs;
		Document d;
		for (int i = 0; i < returnedDocs.length; i++) {
			d = is.doc(returnedDocs[i].doc);
			System.out.println("Doc " + i);
			System.out.println("Score " + topdocs.scoreDocs[i].score);
			System.out.println(d.getField("paraid").stringValue());
			System.out.println(d.getField("parabody").stringValue() + "\n");

		}
	}

	public void customScore(boolean custom) throws IOException {
		customScore = custom;
	}

	/**
	 * 
	 * @param page
	 * @param number
	 * @param filename
	 * @throws IOException
	 * @throws ParseException
	 */

	/********************* For Ranking Paragraphs *****************************************************/
	public void rankParas(Data.Page page, int number, String filename)
			throws IOException, ParseException {
		if (is == null) {
			is = new IndexSearcher(DirectoryReader.open(FSDirectory
					.open((new File(Index_Directory).toPath()))));
		}

		if (customScore) {
			SimilarityBase mySimiliarity = new SimilarityBase() {
				protected float score(BasicStats stats, float freq, float docLen) {
					return freq;
				}

				@Override
				public String toString() {
					return null;
				}
			};
			is.setSimilarity(mySimiliarity);
		}

		if (qp == null) {
			qp = new QueryParser("parabody", new StandardAnalyzer());
		}

		System.out.println(" Entered Query: " + page.getPageName());
		q = qp.parse(page.getPageName());
		topdocs = is.search(q, number);
		returnedDocs = topdocs.scoreDocs;
		Document d;

		ArrayList<String> runStringsperPage = new ArrayList<String>();

		String method = "lucene-default";
		if (customScore)
			method = "custom-";
		for (int i = 0; i < returnedDocs.length; i++) {
			d = is.doc(returnedDocs[i].doc);

			String runFile = page.getPageId() + " Q0 "
					+ d.getField("paraid").stringValue() + " " + i + " "
					+ topdocs.scoreDocs[i].score + " team1-" + method;

			runStringsperPage.add(runFile);

			if (method.equalsIgnoreCase("custom-")) {

				HashMap<String, Integer> innerCustomMap = CustomSpearManMap
						.get(page.getPageId());

				if (innerCustomMap == null)

				{

					innerCustomMap = new HashMap<String, Integer>();
				}

				innerCustomMap.put(d.getField("paraid").stringValue(), i);
				CustomSpearManMap.put(page.getPageId(), innerCustomMap);

				/*
				 * if (CustomSpearManMap.containsKey(page.getPageId())) {
				 * CustomSpearManMap .put(page.getPageId(),
				 * Integer.toString(i));
				 * wt_list.add(d.getField("paraid").stringValue());
				 * wt_list.add(Integer.toString(i));
				 * CustomSpearManMap.put(page.getPageId(), wt_list); } else {
				 * ArrayList<String> wt_list = new ArrayList<String>();
				 * wt_list.add(d.getField("paraid").stringValue());
				 * wt_list.add(Integer.toString(i));
				 * CustomSpearManMap.put(page.getPageId(), wt_list);
				 * 
				 * }
				 */

			}

			else {

				HashMap<String, Integer> innerluceneMap = luceneSpearManMap
						.get(page.getPageId());
				if (innerluceneMap == null)

				{

					innerluceneMap = new HashMap<String, Integer>();
				}
				innerluceneMap.put(d.getField("paraid").stringValue(), i);
				luceneSpearManMap.put(page.getPageId(), innerluceneMap);

				/*
				 * if (luceneSpearManMap.containsKey(page.getPageId())) {
				 * ArrayList<String> wt_list = luceneSpearManMap.get(page
				 * .getPageId());
				 * wt_list.add(d.getField("paraid").stringValue());
				 * wt_list.add(Integer.toString(i));
				 * luceneSpearManMap.put(page.getPageId(), wt_list); } else {
				 * ArrayList<String> wt_list = new ArrayList<String>();
				 * wt_list.add(d.getField("paraid").stringValue());
				 * wt_list.add(Integer.toString(i));
				 * luceneSpearManMap.put(page.getPageId(), wt_list);
				 * 
				 * }
				 */
			}

		}

		FileWriter fw = new FileWriter(QueryParagraphs.Output_Directory + "/"
				+ filename, true);
		for (String runString : runStringsperPage)
			fw.write(runString + "\n");
		fw.close();

	}

	/* Function to print spear man map Values */

	public void SpearmanMatrixPrint() throws FileNotFoundException {

		System.out.println(" Lucene SpearMan Map values ");

		for (Map.Entry<String, HashMap<String, Integer>> entry : luceneSpearManMap
				.entrySet()) {
			String key = entry.getKey();
			HashMap<String, Integer> lucenevalues = entry.getValue();
			// System.out.println("Key = " + key);
			// System.out.println("Values = " + lucenevalues);

		}

		System.out.println(" Custom SpearMan Map values ");

		for (Map.Entry<String, HashMap<String, Integer>> entry : CustomSpearManMap
				.entrySet()) {
			String key = entry.getKey();
			HashMap<String, Integer> customvalues = entry.getValue();
			// System.out.println("Key = " + key);
			// System.out.println("Values = " + customvalues);

		}

	}

	void CompareParagraphValue() throws FileNotFoundException {
		PrintStream out = new PrintStream(new FileOutputStream(
				"spearmanOutput2.txt"));
		System.setOut(out);
		int cParaRank = 0, lparaRank = 0;
		String lParaId = null, luceneQueryId = null;
		for (Map.Entry<String, HashMap<String, Integer>> luceneEntry : luceneSpearManMap
				.entrySet()) {
			luceneQueryId = luceneEntry.getKey();
			System.out.println(" Lucene Query Id is:" + luceneQueryId);

			for (Map.Entry<String, Integer> luceneparaId : luceneEntry
					.getValue().entrySet()) {

				lParaId = luceneparaId.getKey();

				lparaRank = luceneparaId.getValue();

				{

					for (Map.Entry<String, HashMap<String, Integer>> customEntry : CustomSpearManMap
							.entrySet()) {
						String customQueryId = customEntry.getKey();

						if (luceneQueryId.equalsIgnoreCase(customQueryId)) {

							for (Map.Entry<String, Integer> customparaId : customEntry
									.getValue().entrySet()) {
								String CParaId = customparaId.getKey();

								if (lParaId.equalsIgnoreCase(CParaId)) {

									cParaRank = customparaId.getValue();

									int finalDistance = (int) (lparaRank - cParaRank);
									System.out
											.println("Final Distance for QueryId  ="
													+ luceneQueryId
													+ "ParagraphId ="
													+ (String) lParaId
													+ " is "
													+ (finalDistance));
								} else {
									continue;
								}

							}

						} else
							continue;

					}
				}
			}
		}

	}

	/******************************** Retrieve Page List from Given Path *********************************************/

	public ArrayList<Data.Page> RetrievePageListFromPath(String path) {
		ArrayList<Data.Page> pageList = new ArrayList<Data.Page>();
		try {
			FileInputStream fis = new FileInputStream(new File(path));
			for (Data.Page page : DeserializeData.iterableAnnotations(fis)) {
				pageList.add(page);

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeCborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pageList;
	}

	/**
	 * 
	 * @param runfile
	 *            The filename of the runfile containing the queries we wish to
	 *            check
	 * @throws IOException
	 * @return HashMap<String, Double> A mapping of queryId to P@R for that
	 *         queryId
	 */
	public static HashMap<String, Double> getPrecisionAtR(String runfile)
			throws IOException {
		// Maps queryId to P@R for that queryId
		HashMap<String, Double> precisions = new HashMap<>();

		// Statistics needed to calculate P@R for all
		int totalTruePositives = 0;
		int totalRelevant = 0;

		// Map of returned docs and Map or relevant docs, for comparison
		LinkedHashMap<String, LinkedHashSet<String>> qDocs = getReturnedDocs(runfile);
		LinkedHashMap<String, LinkedHashSet<String>> rDocs = getRelevantDocs();

		// Pretty Print a table
		// System.out.format("%20s%44s\n", "queryId", "P@R");
		// System.out.println("-----------------------------------------------------------------");

		// For every mapping of queryId to returned document set...
		for (Map.Entry<String, LinkedHashSet<String>> queryDoc : qDocs
				.entrySet()) {
			// Get the queryId
			String queryId = queryDoc.getKey();

			// Get the corresponding documents
			LinkedHashSet<String> queryDocs = queryDoc.getValue();

			// Counter for number of relevant documents found
			int relevant = 0;

			// For every docId in our returned documents for queryId...
			for (String docId : queryDocs) {
				// Check if our document is relevant
				if (rDocs.get(queryId).contains(docId)) {
					// update statistics
					relevant++;
					totalTruePositives++;
				}
			}

			// Calculate P@R and push it to our Map
			double pAtR = (double) relevant
					/ (double) rDocs.get(queryId).size();
			precisions.put(queryId, pAtR);

			// Update the total number of relevant docs we have
			totalRelevant += rDocs.get(queryId).size();

			// Print our calculated P@R for queryId
			// System.out.format("%-60s%1.3f\n", queryId, pAtR);
		}

		// Calculate P@R for the whole set (r for all queries / total relevant)
		double total_pAtR = (double) totalTruePositives
				/ (double) totalRelevant;

		// Add our P@R for the whole set to our output map
		precisions.put("all", total_pAtR);
		System.out.format("%-60s%1.3f\n", "all", total_pAtR);

		return precisions;
	}

	/**
	 * 
	 * @param runfile
	 *            The filename of the runfile containing the queries we wish to
	 *            check
	 * @return LinkedHashMap<String, LinkedHashSet<String>> Maps queryId to the
	 *         set of matching documents
	 */
	public static LinkedHashMap<String, LinkedHashSet<String>> getReturnedDocs(
			String runfile) {
		// Create our HashMap to return if try statement succeeds
		LinkedHashMap<String, LinkedHashSet<String>> qDocs = new LinkedHashMap<>();

		// Attempt to open our qrels file
		try (BufferedReader reader = new BufferedReader(new FileReader(runfile))) {
			// Read our file line by line
			String line;
			while ((line = reader.readLine()) != null) { // get lines until EOF

				// Split our line into components
				String[] parts = line.split(" ");

				// Get the fields we care about
				String queryId = parts[0];
				String docId = parts[2];

				// Get/create our document set for the current queryId
				LinkedHashSet<String> docs = qDocs.get(queryId);
				if (docs == null) {
					docs = new LinkedHashSet<>();
					qDocs.put(queryId, docs);
				}

				// Add our new document to the queryId
				docs.add(docId);
				qDocs.replace(queryId, docs);
			}

			// Make sure we properly close the file
			reader.close();
		} catch (IOException ex) {
			// Handle any exceptions
			// System.out.println("getRelevantDocs(): Unable to read documents from .qrels file");
		}

		// return our [queryId -> returned documents] map
		return qDocs;
	}

	/**
	 * @return LinkedHashMap<String, LinkedHashSet<String>> Maps queryId to the
	 *         set of relevant documents
	 */
	public static LinkedHashMap<String, LinkedHashSet<String>> getRelevantDocs() {
		// Create our HashMap to return if try statement succeeds
		LinkedHashMap<String, LinkedHashSet<String>> rDocs = new LinkedHashMap<>();

		// Attempt to open our qrels file
		try (BufferedReader reader = new BufferedReader(new FileReader(
				Qrels_File))) {
			// Read our file line by line
			String line;
			while ((line = reader.readLine()) != null) { // get lines until EOF

				// Split our line into components
				String[] parts = line.split(" ");

				// Get the fields we care about
				String queryId = parts[0];
				String docId = parts[2];

				// Get/create our document set for the current queryId
				LinkedHashSet<String> docs = rDocs.get(queryId);
				if (docs == null) {
					docs = new LinkedHashSet<>();
					rDocs.put(queryId, docs);
				}

				// Add our new document to the queryId
				docs.add(docId);
				rDocs.replace(queryId, docs);
			}

			// Make sure we properly close the file
			reader.close();
		} catch (IOException ex) {
			// Handle any exceptions
			System.out
					.println("getRelevantDocs(): Unable to read documents from .qrels file");
		}

		// return our [queryId -> relevant documents] map
		return rDocs;
	}

	public static HashMap<String, ArrayList<String>> RetrieveQrelsMap(
			String runPath) {
		HashMap<String, ArrayList<String>> qrelsMap = new HashMap<String, ArrayList<String>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(runPath));
			String line, qid;
			ArrayList<String> paraList;
			while ((line = br.readLine()) != null) {
				qid = line.split(" ")[0];
				if (qrelsMap.keySet().contains(qid))
					qrelsMap.get(qid).add(line.split(" ")[2]);
				else {
					paraList = new ArrayList<String>();
					paraList.add(line.split(" ")[2]);
					qrelsMap.put(qid, paraList);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return qrelsMap;
	}

	// For mean Precision

	private static HashMap<String, ArrayList<String>> OutputMap;
	private static HashMap<String, ArrayList<String>> RelevantMap;

	public static void Precision(HashMap<String, ArrayList<String>> output,
			HashMap<String, ArrayList<String>> relevant) {
		OutputMap = output;
		RelevantMap = relevant;
	}

	public static double getPrecision(String docId) {
		ArrayList<String> arrOutput = OutputMap.get(docId);
		ArrayList<String> arrRelevant = RelevantMap.get(docId);
		int prec = 0;
		String paraId;

		if (arrOutput == null || arrRelevant == null)
			return 1.0;

		for (int i = 0; i < arrOutput.size(); i++) {
			paraId = arrOutput.get(i);
			if (arrRelevant.contains(paraId))
				prec++;
		}

		float precision = (float) (prec / arrOutput.size());

		return precision;

	}

	/*
	 * public static void NDCGInitialization( HashMap<String, ArrayList<String>>
	 * output, HashMap<String, ArrayList<String>> relevant) { OutputMap =
	 * output; RelevantMap = relevant; }
	 */

	/*
	 * public static float CalculateNDCG20(String docId) { ArrayList<String>
	 * arrOutput = OutputMap.get(docId); ArrayList<String> arrRelevant =
	 * RelevantMap.get(docId);
	 * 
	 * float dcg = 0; float idcg = 0; int rank = 0; String paraId;
	 * 
	 * // performing dcg@20 for (int i = 0; i < 20; i++) {
	 * 
	 * if (arrOutput == null || arrRelevant == null) continue; rank++;
	 * 
	 * if (i < arrOutput.size()) paraId = arrOutput.get(i);
	 * 
	 * else continue;
	 * 
	 * if (i > arrRelevant.size()) { break; }
	 * 
	 * if (arrRelevant.contains(paraId)) { dcg += (1 / (Math.log10(rank + 1) /
	 * Math.log10(2))); }
	 * 
	 * }
	 * 
	 * rank = 0; // performing idcg for (int i = 0; i < 20; i++) { if (arrOutput
	 * == null || arrRelevant == null) continue; if (i < arrOutput.size())
	 * paraId = arrOutput.get(i); else continue;
	 * 
	 * if (i > arrRelevant.size()) { break; }
	 * 
	 * if (arrRelevant.contains(paraId)) { rank++; idcg += 1 / (Math.log10(rank
	 * + 1) / Math.log10(2)); }
	 * 
	 * }
	 * 
	 * return dcg / idcg; }
	 */

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Spearmencoefficient q = new Spearmencoefficient();
		int topSearch = 100;

		try {
			q.indexAllParagraphs();

			ArrayList<Data.Page> pagelist = q
					.RetrievePageListFromPath(QueryParagraphs.Cbor_Outline);
			String runFileString = "";

			for (Data.Page page : pagelist) {
				q.rankParas(page, 100, "result-lucene.run");
			}

			q.customScore(true);

			for (Data.Page page : pagelist) {
				q.rankParas(page, 100, "result-custom.run");
			}
			/******************************************************** Precision at R *****************************************/
			// Get our precisions at R for query results from lucene scoring
			// function
			getPrecisionAtR(Output_Directory + "/result-lucene.run");

			// Get our precisions at R for query results from custom scoring
			// function
			getPrecisionAtR(Output_Directory + "/result-custom.run");

			System.err.println("Printing Spearman value");

			q.SpearmanMatrixPrint();
			q.CompareParagraphValue();

			/*
			 * // Compute Mean Average Precision
			 * 
			 * HashMap<String, ArrayList<String>> luceneMAPOutput =
			 * QueryParagraphs .RetrieveQrelsMap(Output_Directory +
			 * "/result-lucene.run"); HashMap<String, ArrayList<String>>
			 * customMAPOutput = QueryParagraphs
			 * .RetrieveQrelsMap(Output_Directory + "/result-custom.run");
			 * 
			 * Precision(customMAPOutput, luceneMAPOutput);
			 * 
			 * System.err .println(
			 * "*********************************************Mean Average Precision**************************************************"
			 * ); { int counter = 0; double totalmeanAvgPrecision = 0.0; double
			 * averageprecision = 0.0; for (Data.Page p : pagelist) {
			 * averageprecision = averageprecision +
			 * getPrecision(p.getPageId()); totalmeanAvgPrecision =
			 * totalmeanAvgPrecision + averageprecision; counter++; }
			 * System.out.println((double) (totalmeanAvgPrecision / pagelist
			 * .size())); }
			 * 
			 * NDCGInitialization(customMAPOutput, luceneMAPOutput); System.err
			 * .println(
			 * "*********************************************NDCG Value*****************************************"
			 * ); for (Data.Page p : pagelist) {
			 * 
			 * ndcgValue = CalculateNDCG20(p.getPageId()); if
			 * (Double.isNaN(ndcgValue)) { ndcgValue = 0; }
			 * 
			 * System.out.format("%-60s%1.3f\n", p.getPageId(), ndcgValue);
			 * 
			 * }
			 * 
			 * }
			 */
		} catch (CborException | IOException | ParseException e) {
			e.printStackTrace();
		}

	}
}
