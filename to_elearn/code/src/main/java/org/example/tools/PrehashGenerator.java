package org.example.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public class PrehashGenerator {
    private static final long MAGIC = 0x7072656861736821L; // arbitrary
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PrehashGenerator <dictionary.txt> <out.prehash>");
            System.exit(2);
        }
        Path dict = Paths.get(args[0]);
        Path out = Paths.get(args[1]);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        List<Pair> list = new ArrayList<>();

        try (BufferedReader r = Files.newBufferedReader(dict, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String w = line.trim();
                if (w.isEmpty()) continue;
                byte[] digest = md.digest(w.getBytes(StandardCharsets.UTF_8));
                // copy digest (md reused)
                byte[] dcopy = Arrays.copyOf(digest, digest.length);
                list.add(new Pair(dcopy, w));
            }
        }

        // sort by raw digest bytes lexicographically
        list.sort(Comparator.comparing(p -> new ByteArrayKey(p.digest)));

        // reserve offsets
        long headerSize = 8 + 4 + 8; // magic + version + count
        long indexSize = (long) list.size() * 8L;
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(out)))) {
            dos.writeLong(MAGIC);
            dos.writeInt(1); // version
            dos.writeLong(list.size());
            // write placeholder zeroes for offsets
            for (int i = 0; i < list.size(); i++) dos.writeLong(0L);

            long offset = headerSize + indexSize;
            for (Pair p : list) {
                // update offset in file by seeking? We'll collect then rewrite offsets after writing records
                // Simpler: buffer into memory (offsets[]) then write header+offsets+records in two passes:
            }
        }

        // Simpler two-pass: create offsets array and write in one final pass:
        long[] offsets = new long[list.size()];
        long curOffset = headerSize + indexSize;
        // prepare records byte[] for each (could be memory heavy for huge diction)
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(out)))) {
            dos.writeLong(MAGIC);
            dos.writeInt(1);
            dos.writeLong(list.size());
            // reserve offsets
            for (int i = 0; i < list.size(); i++) dos.writeLong(0L);

            for (int i = 0; i < list.size(); i++) {
                Pair p = list.get(i);
                offsets[i] = curOffset;
                dos.write(p.digest); // 32 bytes
                byte[] pw = p.password.getBytes(StandardCharsets.UTF_8);
                dos.writeInt(pw.length);
                dos.write(pw);
                curOffset += 32 + 4 + pw.length;
            }
        }

        // Now patch offsets into file (rewrite them)
        try (RandomAccessFile raf = new RandomAccessFile(out.toFile(), "rw")) {
            raf.seek(8 + 4 + 8); // after magic + version + count
            for (long off : offsets) raf.writeLong(off);
        }
        System.out.println("Wrote prehash cache: " + out + " entries=" + list.size());
    }

    static final class Pair { final byte[] digest; final String password; Pair(byte[] d, String p){this.digest=d;this.password=p;} }
    static final class ByteArrayKey implements Comparable<ByteArrayKey> {
        private final byte[] b;
        ByteArrayKey(byte[] bb){this.b=bb;}
        public int compareTo(ByteArrayKey o){
            for (int i=0;i<b.length;i++){
                int a = b[i] & 0xff, c = o.b[i] & 0xff;
                if (a != c) return Integer.compare(a, c);
            }
            return 0;
        }
    }
} 
