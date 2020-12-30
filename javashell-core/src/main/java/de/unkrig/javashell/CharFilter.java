
package de.unkrig.javashell;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * <p>
 *   "The pipe completes" means: {@link CharFilter#execute(InputStream, OutputStream)} returns.
 * </p>
 * <p>
 *   "The pipe produces a return value" means: {@link CharFilter#execute(InputStream, OutputStream)} returns a
 *   value.
 * </p>
 * <p>
 *   "The pipe produces an exception" means: {@link CharFilter#execute(InputStream, OutputStream)} throws an
 *   exception.
 * </p>
 */
public interface CharFilter<T> {

	T
	execute(Reader in, Writer out) throws IOException;

	default T
	execute(InputStream in, Writer out) throws IOException {
		return this.execute(new InputStreamReader(in), out);
	}
	
	default T
	execute(InputStream in, Charset inCharset, Writer out) throws IOException {
		return this.execute(new InputStreamReader(in, inCharset), out);
	}

	default T
	execute(Reader in, OutputStream out) throws IOException {
		OutputStreamWriter w = new OutputStreamWriter(out);
		try {
			return this.execute(in, w);
		} finally {
			w.flush();
		}
	}
	
	default T
	execute(Reader in, OutputStream out, Charset outCharset) throws IOException {
		OutputStreamWriter w = new OutputStreamWriter(out, outCharset);
		try {
			return this.execute(in, w);
		} finally {
			w.flush();
		}
	}

	default T
	execute(InputStream in, OutputStream out) throws IOException {
		return this.execute(new InputStreamReader(in), out);
	}
	
	default T
	execute(InputStream in, Charset inCharset, OutputStream out, Charset outCharset) throws IOException {
		return this.execute(new InputStreamReader(in, inCharset), out, outCharset);
	}

	default ByteFilter<T>
	asByteFilter() {
		return (in, out) -> CharFilter.this.execute(in, out);
	}
	
	default ByteFilter<T>
	asByteFilter(Charset charset) {
		return (in, out) -> CharFilter.this.execute(in, charset, out, charset);
	}
	
	default ByteFilter<T>
	asByteFilter(Charset inCharset, Charset outCharset) {
		return (in, out) -> CharFilter.this.execute(in, inCharset, out, outCharset);
	}

	default T execute()                 throws IOException { return this.execute(System.in, System.out); }
	default T execute(Reader in)        throws IOException { return this.execute(in, System.out); }
	default T execute(InputStream in)   throws IOException { return this.execute(in, System.out); }
	default T execute(Writer out)       throws IOException { return this.execute(System.in, out); }
	default T execute(OutputStream out) throws IOException { return this.execute(System.in, out); }
}
