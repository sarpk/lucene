package similarity;

import org.apache.lucene.search.similarities.DefaultSimilarity;

public class CustomSimilarity extends DefaultSimilarity {
	  @Override
	  public float sloppyFreq(int distance) {
	    return 1.0f / (distance + 1);
	  }
	  
	  @Override
	  public String toString() {
	    return "CustomSimilarity";
	  }
}
