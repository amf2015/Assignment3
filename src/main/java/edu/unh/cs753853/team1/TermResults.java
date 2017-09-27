package edu.unh.cs753853.team1;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.util.*;

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

public class TermResults {
    private HashMap<TermQuery, ArrayList<DocumentResults>> termResults;
    private ArrayList<DocumentResults> combinedScores;
    private HashMap<Document, Float> lengths;
    private PriorityQueue<DocumentResults> descendingResults;
    private HashMap<TermQuery, Integer> termFreq;
    private IndexReader reader;

    TermResults(IndexReader r)
    {
        termResults = new HashMap<>();
        descendingResults = new PriorityQueue<>(new ResultComparator());
        termFreq = new HashMap<>();
        combinedScores = new ArrayList<>();
        lengths = new HashMap<>();
        reader = r;
    }

    public Set<Map.Entry<TermQuery, ArrayList<DocumentResults>>> entrySet()
    {
        return termResults.entrySet();
    }

    public ArrayList<DocumentResults> getAll(TermQuery tq)
    {
        return termResults.get(tq);
    }

    public DocumentResults getByRank(TermQuery tq, int rank)
    {
        rankResults();
        for(DocumentResults results: getAll(tq))
        {
            if(results.getRank() == rank)
                return results;
        }
        return null;
    }

    public DocumentResults getByDocument(TermQuery tq, Document d)
    {
        for(DocumentResults documentResults: getAll(tq))
        {
            if(documentResults.getDoc() == d)
            {
                return documentResults;
            }
        }
        return null;
    }


    public Float termWeight(TermQuery tq) throws IOException
    {
        float DF = (reader.docFreq(tq.getTerm()) == 0) ? 1 : reader.docFreq(tq.getTerm());

        // Calculate TF-IDF for the query vector
        float qTF = (float)(1 + Math.log10(termFreq.get(tq)));   // Logarithmic term frequency
        float qIDF = (float)(Math.log10(reader.numDocs()/DF));          // Logarithmic inverse document frequency
        float qWeight = qTF * qIDF;                                     // Final calculation

        // Store query weight for later calculations
        return qWeight;
    }

    public Boolean find(TermQuery tq, Document d)
    {
        for(DocumentResults documentResults: getAll(tq))
        {
            if(documentResults.getDoc() == d)
            {
                return true;
            }
        }
        return false;
    }

    public Boolean find(TermQuery tq)
    {
        return termResults.containsKey(tq);
    }

    public void addTermQuery(TermQuery tq)
    {
        if(!termResults.containsKey(tq)) {
            termFreq.put(tq, 0);
            termResults.put(tq, new ArrayList<>());
        }
        termFreq.put(tq, termFreq.get(tq)+1);
    }

    public void put(TermQuery tq, DocumentResults dr)
    {
        if(!termResults.containsKey(tq)) {
            addTermQuery(tq);
        }
        ArrayList<DocumentResults> list = termResults.get(tq);
        ListIterator<DocumentResults> it = list.listIterator();
        while(it.hasNext()) {
            DocumentResults results = it.next();
            if (results.getDoc() == dr.getDoc()) {
                it.remove();
            }
        }
        termResults.get(tq).add(dr);

    }

    public void rankResults()
    {
        descendingResults.addAll(combinedScores);
        combinedScores.clear();
        int rank = 0;
        DocumentResults results;
        while((results = descendingResults.poll()) != null)
        {
            results.rank(rank++);
            putCombined(results);
            if(rank >= 100)
                break;
        }
        if(descendingResults.size() != 0)
            descendingResults.clear();
    }

    public Float getLength(Document d)
    {
        return lengths.get(d);
    }

    private void calculateLengths()
    {
        for(Map.Entry<TermQuery, ArrayList<DocumentResults>> entry: termResults.entrySet())
        {
            ArrayList<DocumentResults> docList = entry.getValue();

            for(DocumentResults docRes: docList)
            {
                Document doc = docRes.getDoc();
                Float score = docRes.getScore();
                Float prevScore = lengths.get(doc);
                if(prevScore == null)
                {
                    lengths.put(doc, 0.0f);
                    prevScore = lengths.get(doc);
                }
                lengths.put(doc, (float)(prevScore + Math.pow(score, 2)));
            }

        }
        for(Map.Entry<Document, Float> entry: lengths.entrySet())
        {
            Document doc = entry.getKey();
            Float summation = entry.getValue();
            lengths.put(doc, (float)(Math.sqrt(summation)));
        }
    }

    public void combineTermScores()
    {
        for(Map.Entry<TermQuery, ArrayList<DocumentResults>> entry: termResults.entrySet())
        {
            for(DocumentResults results: entry.getValue())
            {
                addToCombined(results);
            }
        }
    }

    public void normalizeScores()
    {
        calculateLengths();
        for(DocumentResults results: combinedScores)
        {
            System.out.print(results.getScore() + " ");
            results.score(results.getScore()/getLength(results.getDoc()));
            System.out.println(results.getScore());
        }
    }

    public void putCombined(DocumentResults d)
    {
        for(DocumentResults results: combinedScores) {
            if (results.getDoc() == d.getDoc()) {
                combinedScores.remove(d);
            }
        }
        combinedScores.add(d);
    }

    public void addToCombined(DocumentResults d)
    {
        ListIterator<DocumentResults> it = combinedScores.listIterator();
        while(it.hasNext())
        {
            DocumentResults results = it.next();
            if (results.getDoc() == d.getDoc()) {
                Float score = results.getScore();
                it.remove();
                d.score(d.getScore()+score);
                it.add(d);
                return;
            }
        }
        combinedScores.add(d);
    }


    public ArrayList<DocumentResults> getCombinedScores()
    {
        return combinedScores;
    }
}
