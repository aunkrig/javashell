# JavaShell

`JavaShell` is a library that makes it possible to write compact, BASH-like scripts in Java.

## Introduction

The Java ["Single-File Source-Code Programs" feature](https://openjdk.java.net/jeps/330) that first appeared in
Java 11 makes it easy to write instantly-executing, single-source-file Java programs like this:

    #!/usr/bin/java --source 11

    // This file is "./myscript.sh".

    public class Main {
        public static void main(String[] args) {
            System.out.println("HELLO WORLD");
        }
    }


    $ ./myscript.sh
    HELLO WORLD
    $

Wow! You have the full power of the Java universe at your fingertips with zero project setup time and zero workaround.
Complex script tasks that were difficult to implement with BASH or some common scripting language are now easy with
Java, the programming language that you know inside out and use every day.

However, BASH and the UNIX command utilities provide lots of funtionality that are tedious to implement, say, copying
a set of files, or feeding text through a sequence of "filter" commands. This is where `JavaShell` jumps in: A
compact, easy-to-use framework that greatly simplifies the "normal" tasks that you typically execute in a shell script.

## Examples

### Commands

`cp foo bar`:

    #!/usr/bin/java --source 11 -cp ./javashell-core-0.0.1-SNAPSHOT-jar-with-dependencies.jar --source 11
    
    import java.io.*;
    import static de.unkrig.javashell.core.JavaShell.*;

    cp(new File("foo"), new File("bar"));

### Pipelines

`JavaShell` implements pipelines with `ByteFilter`s and `CharFilter`s, which can be chained and adapted to
one another.

`tr a b <foo | tr c d | tr e f >bar`:

    #!/usr/bin/java --source 11 -cp ./javashell-core-0.0.1-SNAPSHOT-jar-with-dependencies.jar --source 11
    
    import java.io.*;
    import static de.unkrig.javashell.core.JavaShell.*;

    try (InputStream in = new FileInputStream("foo")) {
        try (OutputStream out = new FileOutputStream("bar")) {
            byteFilter(in, out, tr_('a', 'b'), tr_('c', 'd'), tr_('e', 'f'));
        }
    }
