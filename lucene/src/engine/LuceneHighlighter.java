package engine;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Example of Lucene Highlighter
 * @author Mubin Shrestha
 */
public class LuceneHighlighter {

	public static void main(String[] args) throws IOException, ParseException, InvalidTokenOffsetsException {
		LuceneHighlighter l = new LuceneHighlighter();
		l.highLighter();
		
	}
	
    public void highLighter() throws IOException, ParseException, InvalidTokenOffsetsException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File("C:\\crawl\\experiment\\index")));
        Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_44);
        IndexSearcher searcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(Version.LUCENE_44 , "contents", analyzer);
        Query query = parser.parse("search engine technologies");
        TopDocs hits = searcher.search(query, reader.maxDoc());
        System.out.println(hits.totalHits);
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
        int maxRead = 3;
        for (int i = 0; i < maxRead; i++) {
            int id = hits.scoreDocs[i].doc;
            Document doc = searcher.doc(id);
            String text = doc.get("contents");
            TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "contents", analyzer);
            TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, false, 2);
            for (int j = 0; j < frag.length; j++) {
                if ((frag[j] != null) && (frag[j].getScore() > 0)) {
                    System.out.println((frag[j].toString()));
                }
            }
            System.out.println("FIRST IS DONE\n\n\n");
            //Term vector
            /*text = doc.get("contents");
            tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), hits.scoreDocs[i].doc, "contents", analyzer);
            frag = highlighter.getBestTextFragments(tokenStream, text, false, 4);
            for (int j = 0; j < frag.length; j++) {
                if ((frag[j] != null) && (frag[j].getScore() > 0)) {
                    System.out.println((frag[j].toString()));
                }
            }*/
        }
    }
}