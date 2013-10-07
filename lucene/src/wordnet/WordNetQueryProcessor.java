package wordnet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class WordNetQueryProcessor {
	private Query originalQuery = null;
	private BooleanQuery booleanQuery = null;
	private static float boostVal = 0.1f;
	private static String wnhome = "C:\\Program Files (x86)\\WordNet\\2.1\\";
	public WordNetQueryProcessor(QueryParser parser, String queryString) throws ParseException, IOException {
		// set the query
		originalQuery = parser.parse(queryString);
		//Split queries first
		String[] parsedQueriesStr = originalQuery.toString(parser.getField()).split(" ");
		String[] nonParsedQueriesStr = queryString.split(" ");
		
		HashSet<String> allSynonyms = new HashSet<String>();
				
		for(String parsedQuery : parsedQueriesStr) {
			//Try the parsed query first
			HashSet<String> querySynonyms = getAllSynonyms(parsedQuery);
			if(querySynonyms == null) {
				for (String nonParsedQuery : nonParsedQueriesStr) {
					//Iterate until word contains other one(skipping stopwords)
					if (nonParsedQuery.contains(parsedQuery)) {
						querySynonyms = getAllSynonyms(nonParsedQuery);
					}
				}
			}
			if(querySynonyms != null) {
				allSynonyms.addAll(querySynonyms);
			}
		}
		
		setBooleanQuery(parser, allSynonyms);
	
		
	}
	
	private void setBooleanQuery(QueryParser parser, HashSet<String> synonyms) throws ParseException{
		booleanQuery = new BooleanQuery();
		booleanQuery.add(originalQuery, BooleanClause.Occur.MUST);
		if (!synonyms.isEmpty()) {
			for (String eachSynonym : synonyms) {
				Query synonymQuery = parser.parse(eachSynonym);
				synonymQuery.setBoost(boostVal);
				booleanQuery.add(synonymQuery, BooleanClause.Occur.SHOULD);
			}
		}
	}
	
	private HashSet<String> getAllSynonyms(String expandingWord) throws IOException{
		String path = wnhome + File.separator + "dict";
		URL url = null;
		try{ url = new URL("file", null, path); } 
		catch(MalformedURLException e){ e.printStackTrace(); }
		if(url == null) return null;
		
		// construct the dictionary object and open it
		IDictionary dict = new Dictionary(url);
		dict.open();
		
		IIndexWord idxNoun = null, idxVerb = null;
		try{
			idxNoun = dict.getIndexWord(expandingWord, POS.NOUN);
			idxVerb = dict.getIndexWord(expandingWord, POS.VERB);
		}
		catch(NullPointerException e){ 
			e.printStackTrace(); 
		}
		if(idxNoun == null && idxVerb == null) {
			return null;
		}
		
		List <IWordID> wordIDs = new ArrayList<IWordID>();
		HashSet<String> allWords = new HashSet<String>();
		//add all nouns if they exist
		if(idxNoun != null) {
			wordIDs.addAll(idxNoun.getWordIDs());
		}
		//add all verbs if they exist
		if(idxVerb != null) {
			wordIDs.addAll(idxVerb.getWordIDs());
		}
		
		IWordID firstWordID = wordIDs.get(0);
		IWord firstword = dict.getWord(firstWordID);
		for (IWordID wordID: wordIDs) {
			IWord word = dict.getWord(wordID);
			ISynset synset = word.getSynset();
			// iterate over words associated with the synset
			for(IWord w : synset.getWords()) {
				if (!firstword.getLemma().equals(w.getLemma())) {
					String currentLemma = w.getLemma();
					currentLemma = currentLemma.replace("_", " ");
					allWords.add(currentLemma);
				}
			}
		}
		
		/*for(String s : allWords) {
			System.out.println(s);
		}*/
		return allWords;
	}
	
	public Query getOriginalQuery() {
		return originalQuery;
	}
	
	public BooleanQuery getBooleanQuery(){
		return booleanQuery;
	}

}
