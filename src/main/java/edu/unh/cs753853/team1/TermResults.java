package edu.unh.cs753853.team1;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.TermQuery;

import java.util.ArrayList;
import java.util.HashMap;

public class TermResults {
    private HashMap<TermQuery, ArrayList<DocumentResults>> termResults;

    QueryResults()
    {

    }

    public ArrayList<DocumentResults> getResultsList(TermQuery tq)
    {
        return termResults.get(tq);
    }

    public DocumentResults getDocResult(TermQuery tq, int i)
    {
        return termResults.get(tq).get(i);
    }

    public DocumentResults getDocResults(TermQuery tq, )
}
