package edu.unh.cs753853.team1;
import java.util.HashMap;
import java.util.ArrayList;

public class NDCGCalculation_5 {
    private HashMap<String, ArrayList<String>>Outputmap;
    private HashMap<String, ArrayList<String>> Relevantmap;

    public void NDCGinitialization(HashMap<String, ArrayList<String>> output, HashMap<String, ArrayList<String>> relevant) {
        Outputmap = output;
        Relevantmap = relevant;
    }

    public float calculateNDCG20(String docId ){
        ArrayList<String> arrOutput = Outputmap.get(docId);
        ArrayList<String> arrRelevant = Relevantmap.get(docId);

        float dcg = 0;
        float idcg = 0;
        int ranking = 0;
        String paraId;

		/* dcg@20 calculation */
		/* using formula  */
        for (int index = 0; index < 20; index++ ) {

            paraId = arrOutput.get(index);
            ranking++;
            if ( index > arrRelevant.size()) {
                break;
            }
            if ( arrRelevant.contains(paraId)) {
                dcg = (float) (dcg + (1 / ( Math.log10(ranking+1)/Math.log10(2) )));
            }


        }
        //again ranking 0 for idcg calculation.
        ranking = 0;
		/* idcg calculation  */
        for (int index = 0; index < 20; index++ ) {
            paraId = arrOutput.get(index);
            if ( index > arrRelevant.size()) {
                break;
            }
            if ( arrRelevant.contains(paraId)) {
                ranking++;
                idcg =(float) (idcg+ 1 / ( Math.log10(ranking+1)/Math.log10(2) ));
            }


        }

        return dcg/idcg;
    }


}
