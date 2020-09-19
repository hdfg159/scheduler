package io.github.hdfg159.scheduler.function;

@FunctionalInterface
public interface Consumer<T> {
	/**
	 * Performs this operation on the given argument.
	 *
	 * @param t
	 * 		the input argument
	 *
	 * @throws Exception
	 * 		execute error throw exception
	 */
	void accept(T t) throws Exception;
}
