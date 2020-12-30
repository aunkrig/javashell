
package de.unkrig.javashell;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

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
	execute(Reader in, OutputStream out) throws IOException {
		OutputStreamWriter w = new OutputStreamWriter(out);
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

	default ByteFilter<T>
	asBytePipe() {
		return (in, out) -> CharFilter.this.execute(in, out);
	}

	default T execute()                 throws IOException { return this.execute(System.in, System.out); }
	default T execute(Reader in)        throws IOException { return this.execute(in, System.out); }
	default T execute(InputStream in)   throws IOException { return this.execute(in, System.out); }
	default T execute(Writer out)       throws IOException { return this.execute(System.in, out); }
	default T execute(OutputStream out) throws IOException { return this.execute(System.in, out); }
}
