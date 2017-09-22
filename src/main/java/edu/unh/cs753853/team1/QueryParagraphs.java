package edu.unh.cs753854.team1;
import java.io.*;
import java.util.*;

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import edu.unh.cs.treccartool.read_data.DeserializeData.RuntimeCborException;

import edu.unh.cs753853.team1.NDCGCalculation_5;
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



public class QueryParagraphs {

	private IndexSearcher is = null;
	private QueryParser qp = null;
	private boolean customScore = false;


	// directory  structure..
	static final String INDEX_DIRECTORY = "index";
	static final String Cbor_FILE ="test200.cbor/train.test200.cbor.paragraphs";
	static final String Cbor_OUTLINE ="test200.cbor/train.test200.cbor.outlines";
	static final String Cbor_QRELS = "test200.cbor/train.test200.cbor.article.qrels";
	static final String OUTPUT_DIR = "output";
	static Map<String, Set<String>> rel_docs = new HashMap<String, Set<String> >();


	public void indexAllParagraphs() throws CborException, IOException {
		Directory indexdir = FSDirectory.open((new File(INDEX_DIRECTORY)).toPath());
		IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
		conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter iw = new IndexWriter(indexdir, conf);
		for (Data.Paragraph p : DeserializeData.iterableParagraphs(new FileInputStream(new File(Cbor_FILE)))) {
			this.indexPara(iw, p);
		}
		iw.close();
	}

	public void indexPara(IndexWriter iw, Data.Paragraph para) throws IOException {
		Document paradoc = new Document();
		paradoc.add(new StringField("paraid", para.getParaId(), Field.Store.YES));
		paradoc.add(new TextField("parabody", para.getTextOnly(), Field.Store.YES));
		iw.addDocument(paradoc);
	}

	public void doSearch(String qstring, int n) throws IOException, ParseException {
		if ( is == null ) {
			is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));
		}

		if ( customScore ) {
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

		Query q;
		TopDocs tds;
		ScoreDoc[] retDocs;

		System.out.println("Query: " + qstring);
		q = qp.parse(qstring);
		tds = is.search(q, n);
		retDocs = tds.scoreDocs;
		Document d;
		for (int i = 0; i < retDocs.length; i++) {
			d = is.doc(retDocs[i].doc);
			System.out.println("Doc " + i);
			System.out.println("Score " + tds.scoreDocs[i].score);
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
	 * @param n
	 * @param filename
	 * @throws IOException
	 * @throws ParseException
	 */
	public void rankParas(Data.Page page, int n, String filename) throws IOException, ParseException {
		if ( is == null ) {
			is = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));
		}

		if ( customScore ) {
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

		Query q;
		TopDocs tds;
		ScoreDoc[] retDocs;

		System.out.println("Query: " + page.getPageName());
		q = qp.parse(page.getPageName());
		tds = is.search(q, n);
		retDocs = tds.scoreDocs;
		Document d;
		ArrayList<String> runStringsForPage = new ArrayList<String>();
		String method = "lucene-score";
		if(customScore)
			method = "custom-score";
		for (int i = 0; i < retDocs.length; i++) {
			d = is.doc(retDocs[i].doc);
			System.out.println("Doc " + i);
			System.out.println("Score " + tds.scoreDocs[i].score);
			System.out.println(d.getField("paraid").stringValue());
			System.out.println(d.getField("parabody").stringValue() + "\n");

			// runFile string format $queryId Q0 $paragraphId $rank $score $teamname-$methodname
			String runFileString = page.getPageId()+" Q0 "+d.getField("paraid").stringValue()
					+" "+i+" "+tds.scoreDocs[i].score+" team1-"+method;
			runStringsForPage.add(runFileString);
		}


		FileWriter fw = new FileWriter(QueryParagraphs.OUTPUT_DIR+"/"+filename, true);
		for(String runString:runStringsForPage)
			fw.write(runString+"\n");
		fw.close();
	}

	public ArrayList<Data.Page> getPageListFromPath(String path){
		ArrayList<Data.Page> pageList = new ArrayList<Data.Page>();
		try {
			FileInputStream fis = new FileInputStream(new File(path));
			for(Data.Page page: DeserializeData.iterableAnnotations(fis)) {
				pageList.add(page);
				System.out.println(page.toString());

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
	 * @param 	runfile		The filename of the runfile containing the queries we wish to check
	 * @throws	IOException
	 * @return HashMap<String, Double>	A mapping of queryId to P@R for that queryId
	 */
	public static HashMap<String, Double> getPrecisionAtR(String runfile) throws IOException {
		// Maps queryId to P@R for that queryId
		HashMap<String, Double> precisions = new HashMap<>();

		// Statistics needed to calculate P@R for all
		int totalTruePositives = 0;
		int totalRelevant = 0;

		// Map of returned docs and Map or relevant docs, for comparison
		LinkedHashMap<String, LinkedHashSet<String>> qDocs = getReturnedDocs(runfile);
		LinkedHashMap<String, LinkedHashSet<String>> rDocs = getRelevantDocs();

		// Pretty Print a table
		System.out.format("%20s%44s\n", "queryId", "P@R");
		System.out.println("-----------------------------------------------------------------");

		// For every mapping of queryId to returned document set...
		for (Map.Entry<String, LinkedHashSet<String>> queryDoc : qDocs.entrySet()) {
			// Get the queryId
			String queryId = queryDoc.getKey();

			// Get the corresponding documents
			LinkedHashSet<String> queryDocs = queryDoc.getValue();

			// Counter for number of relevant documents found
			int relevant = 0;

			// For every docId in our returned documents for queryId...
			for(String docId: queryDocs)
			{
				// Check if our document is relevant
				if(rDocs.get(queryId).contains(docId))
				{
					// update statistics
					relevant++;
					totalTruePositives++;
				}
			}

			// Calculate P@R and push it to our Map
			double pAtR = (double)relevant / (double)rDocs.get(queryId).size();
			precisions.put(queryId, pAtR);

			// Update the total number of relevant docs we have
			totalRelevant += rDocs.get(queryId).size();

			// Print our calculated P@R for queryId
			System.out.format("%-60s%1.3f\n", queryId, pAtR);
		}

		// Calculate P@R for the whole set (r for all queries / total relevant)
		double total_pAtR = (double)totalTruePositives/(double)totalRelevant;

		// Add our P@R for the whole set to our output map
		precisions.put("all", total_pAtR);
		System.out.format("%-60s%1.3f\n", "all", total_pAtR);

		return precisions;
	}

	/**
	 *
	 * @param runfile	The filename of the runfile containing the queries we wish to check
     * @return LinkedHashMap<String, LinkedHashSet<String>>		Maps queryId to the set of matching documents
	 */
	public static LinkedHashMap<String, LinkedHashSet<String>> getReturnedDocs(String runfile) {
		// Create our HashMap to return if try statement succeeds
		LinkedHashMap<String, LinkedHashSet<String>> qDocs = new LinkedHashMap<>();

		// Attempt to open our qrels file
		try (BufferedReader reader = new BufferedReader(new FileReader(runfile)))
		{
			// Read our file line by line
			String line;
			while((line = reader.readLine()) != null) { // get lines until EOF

				// Split our line into components
				String[] parts = line.split(" ");

				// Get the fields we care about
				String queryId = parts[0];
				String docId = parts[2];

				// Get/create our document set for the current queryId
				LinkedHashSet<String> docs = qDocs.get(queryId);
				if(docs == null) {
					docs = new LinkedHashSet<>();
					qDocs.put(queryId, docs);
				}

				// Add our new document to the queryId
				docs.add(docId);
				qDocs.replace(queryId, docs);
			}

			// Make sure we properly close the file
			reader.close();
		}
		catch(IOException ex) {
			// Handle any exceptions
			System.out.println("getRelevantDocs(): Unable to read documents from .qrels file");
		}

		// return our [queryId -> returned documents] map
		return qDocs;
	}

	/**
     * @return LinkedHashMap<String, LinkedHashSet<String>>		Maps queryId to the set of relevant documents
	 */
	public static LinkedHashMap<String, LinkedHashSet<String>> getRelevantDocs() {
		// Create our HashMap to return if try statement succeeds
		LinkedHashMap<String, LinkedHashSet<String>> rDocs = new LinkedHashMap<>();

		// Attempt to open our qrels file
		try (BufferedReader reader = new BufferedReader(new FileReader(Cbor_QRELS)))
		{
			// Read our file line by line
			String line;
			while((line = reader.readLine()) != null) { // get lines until EOF

				// Split our line into components
				String[] parts = line.split(" ");

				// Get the fields we care about
				String queryId = parts[0];
				String docId = parts[2];

				// Get/create our document set for the current queryId
				LinkedHashSet<String> docs = rDocs.get(queryId);
				if(docs == null) {
					docs = new LinkedHashSet<>();
					rDocs.put(queryId, docs);
				}

				// Add our new document to the queryId
				docs.add(docId);
				rDocs.replace(queryId, docs);
			}

			// Make sure we properly close the file
			reader.close();
		}
		catch(IOException ex) {
			// Handle any exceptions
			System.out.println("getRelevantDocs(): Unable to read documents from .qrels file");
		}

		// return our [queryId -> relevant documents] map
		return rDocs;
	}


	public static HashMap<String, ArrayList<String>> MapToQRels(String source)
	{
		HashMap<String, ArrayList<String>> qrels_document = new HashMap<String, ArrayList<String>>();
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(source));
			String qrelId,line;
			ArrayList<String> paragraphList;
			while((line = br.readLine())!=null)
			{
				qrelId = line.split(" ")[0];
				if(qrels_document.keySet().contains(qrelId))
					qrels_document.get(qrelId).add(line.split(" ")[2]);
				else
				{
					paragraphList=new ArrayList<String>();
					paragraphList.add(line.split(" ")[2]);
					qrels_document.put(qrelId, paragraphList);
				}
			}
		}
		catch (IOException e)
		{
// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return qrels_document;
	}


	public static void main(String[] args) {
		QueryParagraphs q = new QueryParagraphs();
		int topSearch = 100;
		String[] queryArr = {"power nap benefits", "whale vocalization production of sound", "pokemon puzzle league"};

		try {
			q.indexAllParagraphs();
			/*
			for(String qstring:queryArr) {
				a.doSearch(qstring, topSearch);
			}

			System.out.println(StringUtils.repeat("=", 300));

			a.customScore(true);
			for(String qstring:queryArr) {
				a.doSearch(qstring, topSearch);
			}
			*/
			ArrayList<Data.Page> pagelist = q.getPageListFromPath(QueryParagraphs.Cbor_OUTLINE);
			String runFileString = "";

			File f = new File(OUTPUT_DIR + "/result-lucene.run");
			if(f.exists())
			{
				FileWriter createNewFile = new FileWriter(f);
				createNewFile.write("");
			}
			for(Data.Page page:pagelist){

				q.rankParas(page, 100, "result-lucene.run");
			}

			q.customScore(true);
			f = new File(OUTPUT_DIR + "/result-custom.run");
			if(f.exists())
			{
				FileWriter createNewFile = new FileWriter(f);
				createNewFile.write("");
			}
			for(Data.Page page:pagelist){

				q.rankParas(page, 100, "result-custom.run");
			}


			// Get our precisions at R for query results from lucene scoring function
			getPrecisionAtR(OUTPUT_DIR + "/result-lucene.run");

			// Get our precisions at R for query results from custom scoring function
            getPrecisionAtR(OUTPUT_DIR + "/result-custom.run");


			/*
			HashMap<String, ArrayList<String>> luceneMAPResult = MapToQRels(OUTPUT_DIR+"/result-lucene.run");
			HashMap<String, ArrayList<String>> customMAPResult = MapToQRels(OUTPUT_DIR+"/result-custom.run");

			System.out.println("\n\nNDCG : \n");
			NDCGCalculation_5 ndcg = new NDCGCalculation_5();
			ndcg .NDCGinitialization(customMAPResult, luceneMAPResult);
			for (Data.Page p:pagelist)
			{
				System.out.println(p.getPageId() + " : " + ndcg.calculateNDCG20(p.getPageId()));
			}
			*/


		} catch (CborException | IOException | ParseException e) {
			e.printStackTrace();
		}


	}

}

