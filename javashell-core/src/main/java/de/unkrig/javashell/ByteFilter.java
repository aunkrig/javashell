
package de.unkrig.javashell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>
 *   "The pipe completes" means: {@link ByteFilter#execute(InputStream, OutputStream)} returns.
 * </p>
 * <p>
 *   "The pipe produces a return value" means: {@link ByteFilter#execute(InputStream, OutputStream)} returns a
 *   value.
 * </p>
 * <p>
 *   "The pipe produces an exception" means: {@link ByteFilter#execute(InputStream, OutputStream)} throws an
 *   exception.
 * </p>
 */
public
interface ByteFilter<T> {

	T
	execute(InputStream in, OutputStream out) throws IOException;

	default T execute()                 throws IOException { return this.execute(System.in, System.out); }
	default T execute(InputStream in)   throws IOException { return this.execute(in, System.out); }
	default T execute(OutputStream out) throws IOException { return this.execute(System.in, out); }
}
