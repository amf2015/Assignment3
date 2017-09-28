/**
 * 
 */
package edu.unh.cs753853;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
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
public class Spearmancoefficient {

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

	// Spearman relation

	HashMap<String, Integer> FinalSpearManMap = new HashMap<String, Integer>();

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

		}

		System.out.println(" Custom SpearMan Map values ");

		for (Map.Entry<String, HashMap<String, Integer>> entry : CustomSpearManMap
				.entrySet()) {
			String key = entry.getKey();
			HashMap<String, Integer> customvalues = entry.getValue();

		}

	}

	void CompareParagraphValue() throws FileNotFoundException {
		PrintStream out = new PrintStream(new FileOutputStream(
				"spearmanOutput2.txt"));
		System.setOut(out);
		int cParaRank = 0, lparaRank = 0, SumofDSquare = 0, dSquare = 0, n = 0, np = 0, srcc, temp = 0;
		String lParaId = null, luceneQueryId = null;
		for (Map.Entry<String, HashMap<String, Integer>> luceneEntry : luceneSpearManMap
				.entrySet()) {
			luceneQueryId = luceneEntry.getKey();

			SumofDSquare = 0;

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
									n = n + 1;

									cParaRank = customparaId.getValue();

									int finalDistance = Math
											.abs((lparaRank - cParaRank));

									dSquare = finalDistance * finalDistance;
									SumofDSquare = SumofDSquare + dSquare;

								} else {
									np = np + 1;
									continue;

								}

							}

						} else
							continue;

					}

				}

			}
			srcc = 1 - (6 * SumofDSquare) / (n * n * n - n);
			//System.out.format("%30s%40s%20s\n", luceneQueryId, SumofDSquare,srcc);

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

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Spearmancoefficient q = new Spearmancoefficient();
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
			// getPrecisionAtR(Output_Directory + "/result-lucene.run");

			// Get our precisions at R for query results from custom scoring
			// function
			// getPrecisionAtR(Output_Directory + "/result-custom.run");

			System.err.println("Printing Spearman value");

			q.SpearmanMatrixPrint();
			q.CompareParagraphValue();

		} catch (CborException | IOException | ParseException e) {
			e.printStackTrace();
		}

	}
}
