package engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import wordnet.WordNetQueryProcessor;

public class SearchDocuments {
	private IndexSearcher searcher = null;
	private BooleanQuery bquery = null;
	public SearchDocuments(String collectNewsFeeds, String qString, int hits) {
		String index = collectNewsFeeds + "/index";
		String field = "contents";
		String line = qString;
		int repeat = 0;
		boolean raw = false;
		// String queryString = null;
		int hitsPerPage = hits;
		IndexReader reader = null;
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(index)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_44);

		QueryParser parser = new QueryParser(Version.LUCENE_44, field, analyzer);

		Query query = null;
		try {
			query = parser.parse(line);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		WordNetQueryProcessor queryProcess = null;
		try {
			queryProcess = new WordNetQueryProcessor(parser, line);
		} catch (ParseException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Searching for: " + query.toString(field));

		if (repeat > 0) { // repeat & time as benchmark
			Date start = new Date();
			for (int i = 0; i < repeat; i++) {
				try {
					searcher.search(query, null, 100);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Date end = new Date();
			System.out.println("Time: " + (end.getTime() - start.getTime())
					+ "ms");
		}
		bquery = queryProcess.getBooleanQuery();
		this.searcher = searcher;
		/*try {
			//doPagingSearch(searcher, bquery, hitsPerPage, raw);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	/*	try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	
	
	
	
	
	
	
	
	
	public List<String> getPages(int startIndex, int endIndex) {
		TopDocs results = null;
		try {
			results = searcher.search(bquery, 5);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ScoreDoc[] hits = results.scoreDocs;
		int numTotalHits = results.totalHits;
		int minStart = Math.min(startIndex, numTotalHits);
		int minEnd = Math.min(endIndex, numTotalHits);
		//System.out.println("Total results are " + numTotalHits);
		List<String> getRes = new ArrayList<String>();
		for (int i = minStart; i < minEnd; i++) {
			Document doc = null;
			try {
				doc = searcher.doc(hits[i].doc);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String path = doc.get("path");
			getRes.add(path);
		}
		return getRes;
	}
	
	
	static void doPagingSearch(IndexSearcher searcher,
			BooleanQuery bquery, int hitsPerPage, boolean raw)
			throws IOException {
		/*
		 * query2.setBoost(0.1f); // Collect enough docs to show 5 pages
		 * BooleanQuery booleanQuery = new BooleanQuery();
		 * booleanQuery.add(query, BooleanClause.Occur.MUST);
		 * booleanQuery.add(query2, BooleanClause.Occur.SHOULD);
		 */
		TopDocs results = searcher.search(bquery, 5 );
		ScoreDoc[] hits = results.scoreDocs;

		int numTotalHits = results.totalHits;
		System.out.println(numTotalHits + " total matching documents");

		int start = 0;
		int end = Math.min(numTotalHits, hitsPerPage);

		if (end > hits.length) {
			System.out.println("Only results 1 - " + hits.length + " of "
					+ numTotalHits + " total matching documents collected.");
			System.out.println("Collect more (y/n) ?");

			hits = searcher.search(bquery, numTotalHits).scoreDocs;
		}
		end = Math.min(hits.length, start + hitsPerPage);

		for (int i = start; i < end; i++) {
			if (raw) { // output raw format
				System.out.println("doc=" + hits[i].doc + " score="
						+ hits[i].score);
				continue;
			}

			Document doc = searcher.doc(hits[i].doc);
			// System.out.println(doc);
			String path = doc.get("path");
			if (path != null) {

				System.out.println((i + 1) + ". " + path);
				// System.out.println(searcher.explain(bquery,
				// hits[i].doc).toString());

				String title = doc.get("title");
				if (title != null) {
					System.out.println("   Title: " + doc.get("title"));
				}
			} else {
				System.out
						.println((i + 1) + ". " + "No path for this document");
			}

		}
	}
}
