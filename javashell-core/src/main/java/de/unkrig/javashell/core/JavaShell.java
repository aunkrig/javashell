
package de.unkrig.javashell.core;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.io.InputStreams;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.ProcessUtil;
import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.lang.protocol.ProducerUtil;
import de.unkrig.commons.text.pattern.Pattern2;
import de.unkrig.commons.text.pattern.PatternUtil;

public class JavaShell {

	private static final File   ROOT_DIRECTORY    = new File("/");
	private static final File   WORKING_DIRECTORY = new File(".");
	private static final String LINE_TERMINATOR   = String.format("%n");

	// ----------------------------------------------------------------------------------------------------------------
	
	@SafeVarargs public static <T> T
	byteFilter(ByteFilter<? extends T>... pipes) throws IOException {
		return JavaShell.byteFilter_(pipes).execute(System.in, System.out);
	}
	
	@SafeVarargs public static <T> T
	byteFilter(InputStream in, ByteFilter<? extends T>... pipes) throws IOException {
		return JavaShell.byteFilter_(pipes).execute(in, System.out);
	}
	
	@SafeVarargs public static <T> T
	byteFilter(OutputStream out, ByteFilter<? extends T>... pipes) throws IOException {
		return JavaShell.byteFilter_(pipes).execute(System.in, out);
	}

	@SafeVarargs public static <T> T
	byteFilter(InputStream in, OutputStream out, ByteFilter<? extends T>... pipes) throws IOException {
		return JavaShell.byteFilter_(pipes).execute(in, out);
	}

	/**
	 * Creates background threads that execute {@code pipes[0...n-2]}, then executes {@code pipe[n-1]} and
	 * returns. If all pipes read their input until EOI (or return), then all background threads will have
	 * terminated when this method returns.
	 * <p>
	 *   Notice that the return values and exceptions produced by <var>pipes</var>{@code [0...N-2]} are ignored.
	 * </p>
	 * 
	 * @param closeIn           Whether <var>in</var> should be closed when <var>pipes</var>{@code [0]} completes
	 * @param closeOut          Whether <var>out</var> should be closed when <var>pipes</var>{@code [N-1]} completes
	 * @return                  The return value produced by <var>pipes</var>{@code [N-1]}
	 * @throws RuntimeException The exception produced by <var>pipes</var>{@code [N-1]}
	 * @see                     #byteFilter2_(boolean, boolean, ByteFilter[])
	 */
	@SafeVarargs public static <T> T
	byteFilter(InputStream in, boolean closeIn, OutputStream out, boolean closeOut, ByteFilter<? extends T>... pipes) throws IOException {
		return JavaShell.byteFilter_(closeIn, closeOut, pipes).execute(in, out);
	}
	
	/**
	 * Equivalent with {@link #byteFilter_(boolean, boolean, ByteFilter[]) byteFilter(true, false, pipes)}.
	 */
	@SafeVarargs public static <T> ByteFilter<? extends T>
	byteFilter_(ByteFilter<? extends T>... pipes) throws IOException {
		return byteFilter_(/*closeIn*/ true, /*closeOut*/ false, pipes);
	}

	/**
	 * Creates background threads that execute {@code pipes[0...n-2]}, then executes {@code pipes[n-1]} and
	 * returns. If all pipes read their input until EOI (or return), then all background threads will have
	 * terminated when this method returns.
	 * <p>
	 *   Notice that the return values and exceptions produced by <var>pipes</var>{@code [0...N-2]} are ignored.
	 * </p>
	 * 
	 * @param closeIn           Whether <var>in</var> should be closed when <var>pipes</var>{@code [0]} completes
	 * @param closeOut          Whether <var>out</var> should be closed when <var>pipes</var>{@code [N-1]} completes
	 * @return                  The return value produced by <var>pipes</var>{@code [N-1]}
	 * @throws RuntimeException The exception produced by <var>pipes</var>{@code [N-1]}
	 * @see                     #byteFilter2_(boolean, boolean, ByteFilter[])
	 */
	@SafeVarargs public static <T> ByteFilter<? extends T>
	byteFilter_(boolean closeIn, boolean closeOut, ByteFilter<? extends T>... pipes) throws IOException {
	
		switch (pipes.length) {
		
		case 0:
			return cp_();
			
		case 1:
			return close(pipes[0], closeIn, closeOut);
			
		default:
			return (in, out) -> {
				int i = 0;
	
				in = pipeAsInputStream(pipes[i++], in, closeIn);
	
				while (i < pipes.length - 1) in = pipeAsInputStream(pipes[i++], in, true);
	
				try {
					return pipes[i].execute(in, out);
				} finally {
					if (closeOut) close(out);
				}
			};
		}
	}

	/**
	 * Equivalent with {@link #byteFilter2_(boolean, boolean, ByteFilter[]) pipe2(true, false, pipes)}.
	 */
	@SafeVarargs public static <T> ByteFilter<? extends T>
	byteFilter2_(ByteFilter<? extends T>... pipes) throws IOException {
		return byteFilter2_(/*closeIn*/ true, /*closeOut*/ false, pipes);
	}

	/**
	 * Creates background threads that execute {@code pipes[1...]}, then executes {@code pipe[0]} and
	 * returns. The background threads typically still execute when this method returns.
	 * <p>
	 *   Notice that the return values and exceptions produced by <var>pipes</var>{@code [1...N-1]} are ignored.
	 * </p>
	 * 
	 * @param closeIn           Whether <var>in</var> should be closed when <var>pipes</var>{@code [0]} completes
	 * @param closeOut          Whether <var>out</var> should be closed when <var>pipes</var>{@code [N-1]} completes
	 * @return                  The return value produced by <var>pipes</var>{@code [0]}
	 * @throws RuntimeException The exception produced by <var>pipes</var>{@code [0]}
	 * @see                     #byteFilter_(boolean, boolean, ByteFilter[])
	 */
	@SafeVarargs public static <T> ByteFilter<? extends T>
	byteFilter2_(boolean closeIn, boolean closeOut, ByteFilter<? extends T>... pipes) throws IOException {
	
		switch (pipes.length) {
		
		case 0:
			return cp_();
			
		case 1:
			return close(pipes[0], closeIn, closeOut);
			
		default:
			return (in, out) -> {
				int i = pipes.length - 1;
				
				out = pipeAsOutputStream(pipes[i--], out, closeOut);
				
				while (i > 0) out = pipeAsOutputStream(pipes[i--], out, true);
				
				try {
					return pipes[i].execute(in, out);
				} finally {
					if (closeIn) close(in);
				}
			};
		}
	}

	// ----------------------------------------------------------------------------------------------------------------
	
	public static void
	cat(InputStream[] in, OutputStream out) throws IOException {
		for (InputStream in2 : in) IoUtil.copy(in2, /*closeInputStream*/ false, out, /*closeOutputStream*/ false);
	}
	
	public static <T> T
	cat(Reader[] in, Writer out) throws IOException {
		for (Reader in2 : in) IoUtil.copy(in2, /*closeReader*/ false, out, /*closeWriter*/ false);
		return null;
	}

	public static void
	cat(File[] files, OutputStream os) throws IOException {
		for (File file : files) IoUtil.copy(file, os, /*closeOutputStream*/ false);
	}
	
	@SuppressWarnings("unchecked") public static <T> CharFilter<T>
	cat_() throws IOException { return (CharFilter<T>) CAT_; }

	public static final CharFilter<?> CAT_ = (in, out) -> JavaShell.cat(new Reader[] { in }, out);

	// ----------------------------------------------------------------------------------------------------------------

	public static void
	cd() throws IOException, InterruptedException {
		String userHome = System.getProperty("user.home");
		assert userHome != null;
		System.setProperty("user.dir", userHome);
	}

	public static void
	cd(File dir) throws IOException, InterruptedException {
		if (!dir.isDirectory()) throw new FileNotFoundException(dir.toString());
		System.setProperty("user.dir", dir.getAbsolutePath());
	}

	// ----------------------------------------------------------------------------------------------------------------
	
	@SafeVarargs public static <T> T
	charFilter(CharFilter<? extends T>... pipes) throws IOException {
		return JavaShell.charFilter_(pipes).execute(System.in, System.out);
	}
	
	@SafeVarargs public static <T> T
	charFilter(Reader in, CharFilter<? extends T>... pipes) throws IOException {
		return JavaShell.charFilter_(pipes).execute(in, System.out);
	}
	
	@SafeVarargs public static <T> T
	charFilter(Writer out, CharFilter<? extends T>... pipes) throws IOException {
		return JavaShell.charFilter_(pipes).execute(System.in, out);
	}

	@SafeVarargs public static <T> T
	charFilter(Reader in, Writer out, CharFilter<? extends T>... pipes) throws IOException {
		return JavaShell.charFilter_(pipes).execute(in, out);
	}

	/**
	 * Creates background threads that execute {@code pipes[0...n-2]}, then executes {@code pipe[n-1]} and
	 * returns. If all pipes read their input until EOI (or return), then all background threads will have
	 * terminated when this method returns.
	 * <p>
	 *   Notice that the return values and exceptions produced by <var>pipes</var>{@code [0...N-2]} are ignored.
	 * </p>
	 * 
	 * @param closeIn           Whether <var>in</var> should be closed when <var>pipes</var>{@code [0]} completes
	 * @param closeOut          Whether <var>out</var> should be closed when <var>pipes</var>{@code [N-1]} completes
	 * @return                  The return value produced by <var>pipes</var>{@code [N-1]}
	 * @throws RuntimeException The exception produced by <var>pipes</var>{@code [N-1]}
	 * @see                     #charFilter2_(boolean, boolean, CharFilter[])
	 */
	@SafeVarargs public static <T> T
	charFilter(Reader in, boolean closeIn, Writer out, boolean closeOut, CharFilter<? extends T>... pipes) throws IOException {
		return JavaShell.charFilter_(closeIn, closeOut, pipes).execute(in, out);
	}

	/**
	 * Equivalent with {@link #charFilter_(boolean, boolean, CharFilter[]) charFilter(true, false, pipes)}.
	 */
	@SafeVarargs public static <T> CharFilter<? extends T>
	charFilter_(CharFilter<? extends T>... pipes) throws IOException {
		return JavaShell.charFilter_(/*closeIn*/ true, /*closeOut*/ false, pipes);
	}

	/**
	 * Creates background threads that execute {@code pipes[0...n-2]}, then executes {@code pipe[n-1]} and
	 * returns. If all pipes read their input until EOI (or return), then all background threads will have
	 * terminated when this method returns.
	 * <p>
	 *   Notice that the return values and exceptions produced by <var>pipes</var>{@code [0...N-2]} are ignored.
	 * </p>
	 * 
	 * @param closeIn           Whether <var>in</var> should be closed when <var>pipes</var>{@code [0]} completes
	 * @param closeOut          Whether <var>out</var> should be closed when <var>pipes</var>{@code [N-1]} completes
	 * @return                  The return value produced by <var>pipes</var>{@code [N-1]}
	 * @throws RuntimeException The exception produced by <var>pipes</var>{@code [N-1]}
	 * @see                     #charFilter2_(boolean, boolean, CharFilter[])
	 */
	@SafeVarargs public static <T> CharFilter<? extends T>
	charFilter_(boolean closeIn, boolean closeOut, CharFilter<? extends T>... pipes) throws IOException {
	
		switch (pipes.length) {
		
		case 0:
			return cat_();
			
		case 1:
			return close(pipes[0], closeIn, closeOut);
	
		default:
			
			return (in, out) -> {
				int i = 0;
				
				in = pipeAsReader(pipes[i++], in, closeIn);
				
				while (i < pipes.length - 1) {
					in = pipeAsReader(pipes[i++], in, true);
				}
				
				try {
					return pipes[i].execute(in, out);
				} finally {
					if (closeOut) JavaShell.close(out);
				}
			};
		}
	}

	/**
	 * Equivalent with {@link #charFilter2_(boolean, boolean, CharFilter[]) charFilter2(true, false, pipes)}.
	 */
	@SafeVarargs public static <T> T
	charFilter2(Reader in, Writer out, CharFilter<? extends T>... pipes) throws IOException {
		return JavaShell.charFilter2_(pipes).execute(in, out);
	}

	/**
	 * Creates background threads that execute {@code pipes[0...n-2]}, then executes {@code pipe[n-1]} and
	 * returns. If all pipes read their input until EOI (or return), then all background threads will have
	 * terminated when this method returns.
	 * <p>
	 *   Notice that the return values and exceptions produced by <var>pipes</var>{@code [0...N-2]} are ignored.
	 * </p>
	 * 
	 * @param closeIn           Whether <var>in</var> should be closed when <var>pipes</var>{@code [0]} completes
	 * @param closeOut          Whether <var>out</var> should be closed when <var>pipes</var>{@code [N-1]} completes
	 * @return                  The return value produced by <var>pipes</var>{@code [N-1]}
	 * @throws RuntimeException The exception produced by <var>pipes</var>{@code [N-1]}
	 * @see                     #charFilter2_(boolean, boolean, CharFilter[])
	 */
	@SafeVarargs public static <T> T
	charFilter2(Reader in, boolean closeIn, Writer out, boolean closeOut, CharFilter<? extends T>... pipes) throws IOException {
		return JavaShell.charFilter2_(closeIn, closeOut, pipes).execute(in, out);
	}

	/**
	 * Equivalent with {@link #charFilter2_(boolean, boolean, CharFilter[]) pipe(true, false, pipes)}.
	 */
	@SafeVarargs public static <T> CharFilter<? extends T>
	charFilter2_(CharFilter<? extends T>... pipes) throws IOException {
		return charFilter2_(/*closeIn*/ true, /*closeOut*/ false, pipes);
	}

	/**
	 * Creates background threads that execute {@code pipes[1...]}, then executes {@code pipe[0]} and
	 * returns. The background threads typically still execute when this method returns.
	 * <p>
	 *   Notice that the return values and exceptions produced by <var>pipes</var>{@code [1...N-1]} are ignored.
	 * </p>
	 * 
	 * @param closeIn           Whether <var>in</var> should be closed when <var>pipes</var>{@code [0]} completes
	 * @param closeOut          Whether <var>out</var> should be closed when <var>pipes</var>{@code [N-1]} completes
	 * @return                  The return value produced by <var>pipes</var>{@code [0]}
	 * @throws RuntimeException The exception produced by <var>pipes</var>{@code [0]}
	 * @see                     #charFilter_(boolean, boolean, CharFilter[])
	 */
	@SafeVarargs public static <T> CharFilter<? extends T>
	charFilter2_(boolean closeIn, boolean closeOut, CharFilter<? extends T>... pipes) throws IOException {
	
		switch (pipes.length) {
		
		case 0:
			return cat_();
			
		case 1:
			return close(pipes[0], closeIn, closeOut);
			
		default:
			return (in, out) -> {
				int i = pipes.length - 1;
				
				out = pipeAsWriter(pipes[i--], out, closeOut);
				
				while (i > 0) out = pipeAsWriter(pipes[i--], out, true);
				
				try {
					return pipes[i].execute(in, out);
				} finally {
					if (closeIn) JavaShell.close(in);
					JavaShell.close(out);
				}
			};
		}
	}

	// ----------------------------------------------------------------------------------------------------------------

	public static <T> T
	cp(InputStream in, OutputStream out) throws IOException {
		IoUtil.copy(in, out);
		return null;
	}
	
	public static <T> T
	cp(String fromGlob, File toFileOrDir) throws IOException, InterruptedException {
		return cp(expand(fromGlob), toFileOrDir);
	}
	
	public static <T> T
	cpToDir(String fromGlob, File toDir) throws IOException, InterruptedException {
		return cpToDir(expand(fromGlob), toDir);
	}

	public static <T> T
	cp(Collection<File> fromFiles, File toFile) throws IOException {
		if (fromFiles.size() == 1 && !toFile.isDirectory()) {
			cpToFile(fromFiles.iterator().next(), toFile);
		} else {
			for (File fromFile : fromFiles) cpToDir(fromFile, toFile);
		}
		return null;
	}
	
	public static <T> T
	cpToDir(Collection<File> fromFiles, File toDir) throws IOException {
		for (File fromFile : fromFiles) cpToDir(fromFile, toDir);
		return null;
	}
	
	public static Long
	cp(File fromFile, File toFileOrDir) throws IOException {
		if (toFileOrDir.isDirectory()) {
			return cpToDir(fromFile, toFileOrDir);
		} else {
			return cpToFile(fromFile, toFileOrDir);
		}
	}

	public static Long
	cpToDir(File fromFile, File toDir) throws IOException { return cp(fromFile, new File(toDir, fromFile.getName())); }
	
	public static Long
	cpToFile(File fromFile, File toFile) throws IOException { return IoUtil.copy(fromFile, toFile); }
	
	@SuppressWarnings("unchecked") public static <T> ByteFilter<T>
	cp_() { return (ByteFilter<T>) CP_; }
	
	public static final ByteFilter<?> CP_ = (in, out) -> cp(in, out);

	// ----------------------------------------------------------------------------------------------------------------
	
	public static <T> T
	echo(String word, Appendable out) throws IOException {
		out.append(word).append(LINE_TERMINATOR);
		return null;
	}
	
	public static <T> T
	echo(Appendable out, String... words) throws IOException { return JavaShell.echo(words, out); }

	public static <T> T
	echo(String[] words, Appendable out) throws IOException {

		if (words.length > 0) {
			out.append(words[0]);
			for (int i = 1; i < words.length; i++) out.append(' ').append(words[i]);
		}
		
		out.append(LINE_TERMINATOR);
		
		return null;
	}
	
	public static <T> CharFilter<T>
	echo_(String word) { return (in, out) -> JavaShell.echo(word, out); }
	
	public static <T> CharFilter<T>
	echo_(String... words) { return (in, out) -> JavaShell.echo(words, out); }
	
	// ----------------------------------------------------------------------------------------------------------------

	// VARARGS variants:
	public static boolean exec(String... command)                                                              throws IOException, InterruptedException { return JavaShell.exec(Arrays.asList(command)); } 
	public static boolean exec(OutputStream stdout, String... command)                                         throws IOException, InterruptedException { return JavaShell.exec(Arrays.asList(command), stdout); } 
	public static boolean exec(OutputStream stdout, OutputStream stderr, String... command)                    throws IOException, InterruptedException { return JavaShell.exec(Arrays.asList(command), stdout, stderr); } 
	public static boolean exec(InputStream stdin, OutputStream stdout, OutputStream stderr, String... command) throws IOException, InterruptedException { return JavaShell.exec(stdin, Arrays.asList(command), stdout, stderr); }

	public static boolean
	exec(List<String> command) throws IOException, InterruptedException {
		return JavaShell.exec(command, System.out);
	}
	
	public static boolean
	exec(List<String> command, OutputStream out) throws IOException, InterruptedException {
		return JavaShell.exec(command, out, System.err);
	}
	
	public static boolean
	exec(List<String> command, OutputStream stdout, OutputStream stderr) throws IOException, InterruptedException {
		return JavaShell.exec(InputStreams.EMPTY, command, stdout, stderr);
	}

	public static boolean
	exec(
		InputStream  stdin,
		List<String> command,
		OutputStream stdout,
		OutputStream stderr
	) throws IOException, InterruptedException {
		return JavaShell.exec(
			stdin,      // stdin
			false,      // closeStdin
			command,    // command
			null,       // environment
			null,       // workingDirectory
			stdout,     // stdout
			false,      // closeStdout
			stderr,     // stderr
			false       // closeStderr
		);
	}

	public static boolean
	exec(
		InputStream         stdin,
		boolean             closeStdin,
		List<String>        command,
		Map<String, String> environment,
		File                workingDirectory,
		OutputStream        stdout,
		boolean             closeStdout,
		OutputStream        stderr,
		boolean             closeStderr
	) throws IOException, InterruptedException {

		return ProcessUtil.execute(
	        command,
	        workingDirectory,
	        stdin,
	        closeStdin,
	        stdout,
	        closeStdout,
	        stderr,
	        closeStderr
	    );
	}

	public static ByteFilter<Boolean>
	exec_(
		List<String>        command,
		Map<String, String> environment,
		File                workingDirectory,
		OutputStream        stderr
	) {
		return (in, out) -> {
			try {
				return JavaShell.exec(
					in,               // stdin
					false,            // closeStdin
					command,          // command
					environment,      // environment
					workingDirectory, // workingDirectory
					out,              // stdout,
					false,            // closeStdout
					stderr,           // stderr
					false             // closeStderr
				);
			} catch (InterruptedException e) {
				return false;
			}
		};
	}
	
	// ----------------------------------------------------------------------------------------------------------------
	
	public static <T> T
	ls(Appendable out) throws IOException { return JavaShell.ls(new File("."), out); }
	
	public static <T> T
	ls(File file, Appendable out) throws IOException { return JavaShell.ls(new File[] { file }, out); }

	public static <T> T
	ls(File[] files, Appendable out) throws IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				if (files.length > 1) out.append(LINE_TERMINATOR).append(file.getPath()).append(':').append(LINE_TERMINATOR);
				for (String memberName : file.list()) out.append(memberName).append(LINE_TERMINATOR);
			} else
			if (file.isFile()) {
				JavaShell.lsD(file, out);
			} else
			{
				throw new FileNotFoundException(file.toString());
			}
		}
		return null;
	}
	
	public static CharFilter<Void> ls_()             throws FileNotFoundException { return (in, out) -> JavaShell.ls(out); } 
	public static CharFilter<Void> ls_(File file)    throws FileNotFoundException { return (in, out) -> JavaShell.ls(file, out); } 
	public static CharFilter<Void> ls_(File[] files) throws FileNotFoundException { return (in, out) -> JavaShell.ls(files, out); } 

	// ----------------------------------------------------------------------------------------------------------------
	
	public static <T> T
	lsD(File file, Appendable out) throws IOException {
		out.append(file.getPath()).append(LINE_TERMINATOR);
		return null;
	}
	
	public static CharFilter<Void>
	lsD_(File file) {
		return (in, out) -> {
			out.append(file.getPath()).append(LINE_TERMINATOR);
			return null;
		};
	}

	// ----------------------------------------------------------------------------------------------------------------

	public static File
	pwd() throws IOException {
		String userDir = System.getProperty("user.dir");
		assert userDir != null;
		return new File(userDir);
	}
	
	public static CharFilter<String>
	pwd_() {
		return (in, out) -> { String pwd = pwd().getPath(); out.append(pwd).append(LINE_TERMINATOR); return pwd; };
	}

	// ----------------------------------------------------------------------------------------------------------------
	
	public static int
	sedSubstituteAll(Reader in, Pattern pattern, String replacementString, Appendable out) throws IOException {
		return (int) PatternUtil.replaceAll(in, pattern, replacementString, out);
	}

	public static CharFilter<Integer>
	sedSubstituteAll_(Pattern pattern, String replacementString) throws IOException {
		return (in, out) -> JavaShell.sedSubstituteAll(in, pattern, replacementString, out);
	}
	
	// ----------------------------------------------------------------------------------------------------------------
	
	public static int
	sedSubstituteFirst(Reader in, Pattern pattern, String replacementString, Appendable out) throws IOException {
		
        FunctionWhichThrows<MatchResult, String, ? extends RuntimeException>
        rsmr = PatternUtil.<RuntimeException>replacementStringMatchReplacer(replacementString);

        ProducerUtil.BooleanProducer once = ProducerUtil.once();
        
		return PatternUtil.replaceSome(
            in,
            pattern,
            mr -> once.produce() ? rsmr.call(mr) : null, 
            out,
            8192
        );
	}

	public static CharFilter<Integer>
	sedSubstituteFirst_(Pattern pattern, String replacementString) throws IOException {
		return (in, out) -> JavaShell.sedSubstituteFirst(in, pattern, replacementString, out);
	}
	
	// ----------------------------------------------------------------------------------------------------------------

	public static int
	wcL(Reader in) throws IOException {
		BufferedReader br = in instanceof BufferedReader ? (BufferedReader) in : new BufferedReader(in);
		
		int lineCount = 0;
		while (br.readLine() != null) lineCount++;
		return lineCount;
	}

	public static CharFilter<Integer>
	wcL() {
		return (in, out) -> {
			int lineCount = wcL(in);
			out.append(Integer.toString(lineCount)).append(LINE_TERMINATOR);
			return lineCount;
		};
	}

	// *********************************************************************************************
	
	/**
	 * Expands a path name pattern to a set of {@link File}s.
	 * <p>
	 *   Notice: The only recognized file separator is {@code "/"}.
	 * </p>
	 */
	public static List<File>
	expand(String glob) throws IOException, InterruptedException {
		List<File> files = new ArrayList<>();
		Pattern pattern = Pattern2.compile(glob, Pattern2.WILDCARD);
		
		if (pattern.matcher("/").lookingAt()) {
			JavaShell.expand(ROOT_DIRECTORY, pattern, files);
		} else
		{
			String[] memberNames = WORKING_DIRECTORY.list();
			Arrays.sort(memberNames);
			for (String memberName : memberNames) {
				expand(new File(memberName), pattern, files);
			}
		}
		return files;
	}

	private static void
	expand(File file, Pattern pattern, Collection<File> result) {
		
		if (pattern.matcher(file.getPath()).matches()) result.add(file);
		
		Matcher m = pattern.matcher(file.getPath() + "/");
		if ((m.matches() || m.hitEnd()) && file.isDirectory()) {
			String[] memberNames = file.list();
			Arrays.sort(memberNames);
			for (String memberName : memberNames) {
				expand(new File(file, memberName), pattern, result);
			}
		}
	}

	public static Pattern
	regex(String regex) { return Pattern.compile(regex); }
	
	public static Pattern
	regex(String regex, int flags) { return Pattern.compile(regex, flags); }
	
	public static void
	executeRunnableInBackground(Runnable runnable) {
		
		Thread t = new Thread(runnable);
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Creates a background thread that executes the <var>pipe</var> and returns.
	 * <p>
	 *   Notice that the return value and any exceptions produced by the <var>pipe</var> are ignored.
	 * </p>
	 * 
	 * @param in      Where the <var>pipe</var> reads from
	 * @param closeIn Whether <var>in</var> should be closed when the <var>pipe</var> completes
	 * @return        Reads the bytes written by the <var>pipe</var>
	 */
	public static <T> InputStream
	pipeAsInputStream(ByteFilter<T> pipe, InputStream in, boolean closeIn) throws IOException {
		
		PipedInputStream  pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream();
		pis.connect(pos);

		executePipeInBackground(pipe, in, closeIn, pos, true);

		return pis;
	}

	/**
	 * Creates a background thread that executes the <var>pipe</var> and returns.
	 * <p>
	 *   Notice that the return value and any exceptions produced by the <var>pipe</var> are ignored.
	 * </p>
	 * 
	 * @param out      Where the <var>pipe</var> writes to
	 * @param closeOut Whether <var>out</var> should be closed when the <var>pipe</var> completes
	 * @return         Bytes written here are read by the <var>pipe</var>
	 */
	public static <T> OutputStream
	pipeAsOutputStream(ByteFilter<T> pipe, OutputStream out, boolean closeOut) throws IOException {

		PipedInputStream  pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream();
		pis.connect(pos);
		
		executePipeInBackground(pipe, pis, true, out, closeOut);
		
		return pos;
	}

	private static <T> ByteFilter<? extends T>
	close(ByteFilter<? extends T> delegate, boolean closeIn, boolean closeOut) {
		
		if (!closeIn && !closeOut) return delegate;
		
		return (in, out) -> {
			try {
				return delegate.execute(in, out);
			} finally {
				if (closeIn)  close(in);
				if (closeOut) close(out);
			}
		};
	}

	/**
	 * Creates a background thread that executes the <var>pipe</var> asynchronously, and returns.
	 * <p>
	 *   Notice that the return value and any exceptions produced by the <var>pipe</var> are ignored.
	 * </p>
	 * 
	 * @param in       Where the <var>pipe</var> reads from
	 * @param closeIn  Whether <var>in</var> should be closed when the <var>pipe</var> completes
	 * @param out      Where the <var>pipe</var> writes to
	 * @param closeOut Whether <var>out</var> should be closed when the <var>pipe</var> completes
	 */
	public static <T> void
	executePipeInBackground(ByteFilter<T> pipe, InputStream in, boolean closeIn, OutputStream out, boolean closeOut) {
		
		JavaShell.executeRunnableInBackground(new Runnable() {

			@Override public void
			run() {
				try {
					pipe.execute(in, out);
				} catch (IOException e) {
					;
				} finally {
					if (closeIn)  close(in);
					if (closeOut) close(out);
				}
			}
		});
	}

	/**
	 * Creates a background thread that executes the <var>pipe</var> and returns.
	 * <p>
	 *   Notice that the return value and any exceptions produced by the <var>pipe</var> are ignored.
	 * </p>
	 * 
	 * @param in      Where the <var>pipe</var> reads from
	 * @param closeIn Whether <var>in</var> should be closed when the <var>pipe</var> completes
	 * @return        Reads the bytes written by the <var>pipe</var>
	 */
	public static <T> Reader
	pipeAsReader(CharFilter<T> pipe, Reader in, boolean closeIn) throws IOException {
		
		PipedReader pr = new PipedReader();
		PipedWriter pw = new PipedWriter();
		pr.connect(pw);
		
		executePipeInBackground(pipe, in, closeIn, pw, true);
		
		return pr;
	}

	/**
	 * Creates a background thread that executes the <var>pipe</var> and returns.
	 * <p>
	 *   Notice that the return value and any exceptions produced by the <var>pipe</var> are ignored.
	 * </p>
	 * 
	 * @param out      Where the <var>pipe</var> writes to
	 * @param closeOut Whether <var>out</var> should be closed when the <var>pipe</var> completes
	 * @return         Bytes written here are read by the <var>pipe</var>
	 */
	public static <T> Writer
	pipeAsWriter(CharFilter<T> pipe, Writer out, boolean closeOut) throws IOException {
		
		PipedReader pis = new PipedReader();
		PipedWriter pos = new PipedWriter();
		pis.connect(pos);
		
		executePipeInBackground(pipe, pis, true, out, closeOut);
		
		return pos;
	}

	private static <T> CharFilter<? extends T>
	close(CharFilter<? extends T> delegate, boolean closeIn, boolean closeOut) {
		
		if (!closeIn && !closeOut) return delegate;
		
		return (in, out) -> {
			try {
				return delegate.execute(in, out);
			} finally {
				if (closeIn)  JavaShell.close(in);
				if (closeOut) JavaShell.close(out);
			}
		};
	}

	/**
	 * Creates a background thread that executes the <var>pipe</var> asynchronously, and returns.
	 * <p>
	 *   Notice that the return value and any exceptions produced by the <var>pipe</var> are ignored.
	 * </p>
	 * 
	 * @param in       Where the <var>pipe</var> reads from
	 * @param closeIn  Whether <var>in</var> should be closed when the <var>pipe</var> completes
	 * @param out      Where the <var>pipe</var> writes to
	 * @param closeOut Whether <var>out</var> should be closed when the <var>pipe</var> completes
	 */
	public static <T> void
	executePipeInBackground(CharFilter<T> pipe, Reader in, boolean closeIn, Writer out, boolean closeOut) {
		
		JavaShell.executeRunnableInBackground(new Runnable() {
			
			@Override public void
			run() {
				try {
					pipe.execute(in, out);
				} catch (IOException e) {
					;
				} finally {
					if (closeIn)  JavaShell.close(in);
					if (closeOut) JavaShell.close(out);
				}
			}
		});
	}

	static void
	close(Closeable closeable) {
		try { closeable.close(); } catch (Exception e) {}
	}
}
