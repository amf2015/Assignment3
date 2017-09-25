package edu.unh.cs753853.team1;
import java.io.*;
import java.util.*;

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import edu.unh.cs.treccartool.read_data.DeserializeData.RuntimeCborException;

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
	static final public String INDEX_DIRECTORY = "index";
	static final public String Cbor_FILE ="test200.cbor/train.test200.cbor.paragraphs";
	static final public String Cbor_OUTLINE ="test200.cbor/train.test200.cbor.outlines";
	static final public String OUTPUT_DIR = "output";


	private void indexAllParagraphs() throws CborException, IOException {
		Directory indexdir = FSDirectory.open((new File(INDEX_DIRECTORY)).toPath());
		IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
		conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter iw = new IndexWriter(indexdir, conf);
		for (Data.Paragraph p : DeserializeData.iterableParagraphs(new FileInputStream(new File(Cbor_FILE)))) {
			this.indexPara(iw, p);
		}
		iw.close();
	}

	private void indexPara(IndexWriter iw, Data.Paragraph para) throws IOException {
		Document paradoc = new Document();
		paradoc.add(new StringField("paraid", para.getParaId(), Field.Store.YES));
		paradoc.add(new TextField("parabody", para.getTextOnly(), Field.Store.YES));
		iw.addDocument(paradoc);
	}

	/*
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
	*/

	private void customScore(boolean custom) throws IOException {
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
	private void rankParas(Data.Page page, int n, String filename) throws IOException, ParseException {
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

	private ArrayList<Data.Page> getPageListFromPath(String path){
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

			/*
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
			*/

			TFIDF_lnc_ltn tfidf = new TFIDF_lnc_ltn(pagelist, 100);

			tfidf.dumpScoresTo(OUTPUT_DIR + "/not-yet-implemented");


		} catch (CborException | IOException | ParseException e) {
			e.printStackTrace();
		}


	}

}

