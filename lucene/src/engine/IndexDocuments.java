package engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class IndexDocuments {

	public IndexDocuments(String collectNewsFeeds, String webRoot) {
		String indexPath = collectNewsFeeds + "/" + webRoot + "/index";
		String docsPath = collectNewsFeeds + "/" + webRoot + "/download";
		;
		boolean create = true;
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out
					.println("Document directory '"
							+ docDir.getAbsolutePath()
							+ "' does not exist or is not readable, please check the path");
		} else {

			// Date start = new Date();
			try {
				System.out.println("Indexing to directory '" + indexPath
						+ "'...");

				Directory dir = FSDirectory.open(new File(indexPath));
				Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_44);
				IndexWriterConfig iwc = new IndexWriterConfig(
						Version.LUCENE_44, analyzer);

				if (create) {
					// Create a new index in the directory, removing any
					// previously indexed documents:
					iwc.setOpenMode(OpenMode.CREATE);
				} else {
					// Add new documents to an existing index:
					iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
				}

				// Optional: for better indexing performance, if you
				// are indexing many documents, increase the RAM
				// buffer. But if you do this, increase the max heap
				// size to the JVM (eg add -Xmx512m or -Xmx1g):
				//
				// iwc.setRAMBufferSizeMB(256.0);

				IndexWriter writer = new IndexWriter(dir, iwc);
				indexDocs(writer, docDir);

				// NOTE: if you want to maximize search performance,
				// you can optionally call forceMerge here. This can be
				// a terribly costly operation, so generally it's only
				// worth it when your index is relatively static (ie
				// you're done adding documents to it):
				//
				// writer.forceMerge(1);

				writer.close();

				// Date end = new Date();
				// System.out.println(end.getTime() - start.getTime()
				// + " total milliseconds");

			} catch (IOException e) {
				System.out.println(" caught a " + e.getClass()
						+ "\n with message: " + e.getMessage());
			}
		}
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	static void indexDocs(IndexWriter writer, File file) throws IOException {
		// do not try to index files that cannot be read
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						indexDocs(writer, new File(file, files[i]));
					}
				}
			} else {

				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException fnfe) {
					// at least on windows, some temporary files raise this
					// exception with an "access denied" message
					// checking if the file can be read doesn't help
					return;
				}

				try {
					if (file.toString().endsWith(".dat")) {
						return;
					}
					// make a new, empty document
					Document doc = new Document();

					// Add the path of the file as a field named "path". Use a
					// field that is indexed (i.e. searchable), but don't
					// tokenize
					// the field into separate words and don't index term
					// frequency
					// or positional information:
					Field pathField = new StringField("path", file.getPath(),
							Field.Store.YES);
					doc.add(pathField);
					// Add the last modified date of the file a field named
					// "modified".
					// Use a LongField that is indexed (i.e. efficiently
					// filterable with
					// NumericRangeFilter). This indexes to milli-second
					// resolution, which
					// is often too fine. You could instead create a number
					// based on
					// year/month/day/hour/minutes/seconds, down the resolution
					// you require.
					// For example the long value 2011021714 would mean
					// February 17, 2011, 2-3 PM.
					doc.add(new LongField("modified", file.lastModified(),
							Field.Store.NO));

					// Add the contents of the file to a field named "contents".
					// Specify a Reader,
					// so that the text of the file is tokenized and indexed,
					// but not stored.
					// Note that FileReader expects the file to be in UTF-8
					// encoding.
					// If that's not the case searching for special characters
					// will fail.
					Field textField = new TextField("contents",
							new BufferedReader(new InputStreamReader(fis,
									"UTF-8")));
					doc.add(textField);

					if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
						// New index, so we just add the document (no old
						// document can be there):

						// System.out.println("adding " + file);

						/*
						 * String contents = readFile(file.toString(),
						 * StandardCharsets.UTF_8).toLowerCase(); if
						 * (contents.contains("citation")) {
						 * textField.setBoost(0.001f); } if
						 * (contents.contains("publisher")) {
						 * textField.setBoost(0.001f); } if
						 * (contents.contains("publication")) {
						 * textField.setBoost(0.001f); } if
						 * (contents.contains("search results")) {
						 * textField.setBoost(0.01f); }
						 */
						writer.addDocument(doc);
					} else {
						// Existing index (an old copy of this document may have
						// been indexed) so
						// we use updateDocument instead to replace the old one
						// matching the exact
						// path, if present:
						System.out.println("updating " + file);
						writer.updateDocument(new Term("path", file.getPath()),
								doc);
					}

				} finally {
					fis.close();
				}
			}
		}
	}

}
