
package de.unkrig.javashell.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.unkrig.commons.file.FileUtil;
import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.io.Readers;
import de.unkrig.javashell.CharFilter;
import de.unkrig.javashell.JavaShell;

public
class TestJavaShell {
	
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private static final File TMP = new File("tmp");
	
	@Before public void
	setUp() throws IOException {
		if (TMP.exists()) FileUtil.deleteRecursively(TMP);
		mkdir(TMP);
	}
	
	@After public void
	tearDown() throws IOException {
		if (TMP.exists()) FileUtil.deleteRecursively(TMP);
	}

	@Test public void
	testExpand() throws IOException, InterruptedException {

		assertTrue(JavaShell.expand("lknjdkl").isEmpty());

		String tmpPath = TMP.getPath().replace(File.separatorChar, '/');

		fe(JavaShell.expand(tmpPath),         		TMP);
		fe(JavaShell.expand(tmpPath + "/*"));
		fe(JavaShell.expand(tmpPath + "/**"));
		fe(JavaShell.expand(tmpPath + "/***"));

		File dir1            = mkdir(new File(TMP,  "dir1"));
		File dir1_dir2       = mkdir(new File(dir1, "dir2"));
		File file1           = mkfile(new File(TMP,       "file1"), "HELLO");
		File file2           = mkfile(new File(TMP,       "file2"), "HELLO");
		File dir1_file1      = mkfile(new File(dir1,      "file1"), "HELLO");
		File dir1_dir2_file1 = mkfile(new File(dir1_dir2, "file1"), "HELLO");
		fe(JavaShell.expand(tmpPath),              TMP);
		fe(JavaShell.expand(tmpPath + "/*"),       dir1, file1, file2);
		fe(JavaShell.expand(tmpPath + "/**"),      dir1, dir1_dir2, dir1_dir2_file1, dir1_file1, file1, file2);
		fe(JavaShell.expand(tmpPath + "/dir1"),    dir1);
		fe(JavaShell.expand(tmpPath + "/dir1/*"),  dir1_dir2, dir1_file1);
		fe(JavaShell.expand(tmpPath + "/dir1/**"), dir1_dir2, dir1_dir2_file1, dir1_file1);
	}

	@Test public void
	testCp() throws IOException {

		File foo = new File(TMP, "foo");
		File bar = new File(TMP, "bar");

		assertFileContents("HELLO", mkfile(foo, "HELLO"));
		
		JavaShell.cp(foo, bar);
		
		assertFileContents("HELLO", bar);
	}

	@Test public void
	testByteFilter1A() throws IOException {
		
		InputStream           in  = inputStream("Hallo");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		JavaShell.byteFilter(in, out, JavaShell.sedSubstituteAll_(Pattern.compile("(.)"), "$1$1").asByteFilter());

		assertEquals("HHaalllloo", new String(out.toByteArray()));
	}
	
	@Test public void
	testByteFilter1B() throws IOException {
		
		InputStream           in  = inputStream("Hallo");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		JavaShell.byteFilter_(JavaShell.sedSubstituteAll_(Pattern.compile("(.)"), "$1$1").asByteFilter()).execute(in, out);
		
		assertEquals("HHaalllloo", new String(out.toByteArray()));
	}

	@Test public void
	testCharFilter2A() throws IOException {
		
		assertEquals("Drii Chinisin", TestJavaShell.runCharFilter2(
			new StringReader("Drei Chinesen"),
			JavaShell.sedSubstituteAll_(Pattern.compile("[aeiou]"), "i")
		));
	}

	@Test public void
	testCharFilter2B() throws IOException {
		
		assertEquals("Drii Chinisin", runCharFilter2(
			new StringReader("Drei Chinesen"),
			JavaShell.sedSubstituteAll_(Pattern.compile("[aeiou]"), "i"),
			JavaShell.cat_()
		));
	}
	
	@Test public void
	testCharFilter2C() throws IOException {
		
		assertEquals("6" + LINE_SEPARATOR, runCharFilter2(
			new StringReader("Drei Chinesen"),
			JavaShell.sedSubstituteAll_(Pattern.compile("[aeiou]"), "i"),
			JavaShell.sedSubstituteAll_(Pattern.compile("i"), "$0\n"),
			JavaShell.wcL()
		));
	}
	
	@Test public void
	testExec() throws IOException, InterruptedException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		assertTrue(JavaShell.exec(
			System.out, // stdout
			baos,       // stderr
			System.getProperty("java.home") + "/bin/java",
			"-version"
		));
		String s = Readers.readAll(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
		assertTrue(s.contains("java"));
	}

	// ----------------------------------------------------------------------------------------------------------------

	private void
	assertFileContents(String expected, File actual) throws IOException {
		try (InputStream is = new FileInputStream(actual)) {
			assertEquals(expected, InputStreams.readAll(is, Charset.forName("UTF-8"), false));
		}
	}
	
	private File
	mkdir(File dir) throws IOException {
		assertTrue(dir.mkdir());
		return dir;
	}

	private File
	mkfile(File file, String contents) throws IOException {
		try (Writer w = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"))) {
			w.write(contents);
		}
		return file;
	}

	private static void
	fe(List<File> actual, File... expected) {
		assertArrayEquals(expected, actual.toArray(new File[actual.size()]));
	}

	private static InputStream
	inputStream(String string) { return new ByteArrayInputStream(string.getBytes()); }

	/**
	 * @return The output of the last pipe element
	 */
	private static String
	runCharFilter2(StringReader in, CharFilter<?>... pipes) throws IOException {

		// "CharFilter2.execute()" returns when the first pipe segment completes, which is typically earlier than when
		// the *last* pipe segment has completely written its output. Thus we must use a piped character stream in
		// order to realiably detect the end-of-output:
		PipedWriter pw = new PipedWriter();
		PipedReader pr = new PipedReader();
		pr.connect(pw);
		
		JavaShell.charFilter2_(
			true, // closeIn
			true, // closeOut  <= Important, so we can determine the end-of-output!
			pipes
		).execute(in, pw);

		return Readers.readAll(pr);
	}
}
