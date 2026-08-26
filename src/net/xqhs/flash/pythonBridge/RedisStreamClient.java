package net.xqhs.flash.pythonBridge;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A small Redis client, speaking just enough of the RESP protocol for
 * {@link PythonMqBridgeDriver} to read results off a stream ({@code XREAD}).
 */
public class RedisStreamClient implements AutoCloseable {

    protected static final byte[]	CRLF			= { '\r', '\n' };
    /** Extra socket read timeout on top of the requested block time */
    protected static final int		TIMEOUT_MARGIN_MS	= 5000;

    protected final Socket			socket;
    protected final OutputStream	out;
    protected final InputStream		in;

    public RedisStreamClient(String host, int port, int blockMillis) throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(blockMillis + TIMEOUT_MARGIN_MS);
        out = socket.getOutputStream();
        in = new BufferedInputStream(socket.getInputStream());
    }

    /** Sends PING; used to check that a Redis server is actually reachable before relying on it. */
    public boolean ping() {
        try {
            return "PONG".equals(String.valueOf(command("PING")));
        } catch(IOException e) {
            return false;
        }
    }

    /** One entry read off a stream: its id and its field/value pairs. */
    public static class StreamEntry {
        public final String						    id;
        public final java.util.Map<String, String>	fields	= new java.util.LinkedHashMap<>();

        protected StreamEntry(String id) {
            this.id = id;
        }
    }

    /**
     * Blocking read of new entries on one stream.
     *
     * @param stream
     *            stream key, e.g. {@code results:agent-1}
     * @param lastId
     *            id to read after; {@code $} means "only entries added from now on"
     * @param blockMillis
     *            how long the server should block before returning nothing
     * @return the entries read, in order; empty if the block expired with nothing new
     */
    public List<StreamEntry> xread(String stream, String lastId, int blockMillis) throws IOException {
        Object reply = command("XREAD", "BLOCK", String.valueOf(blockMillis), "STREAMS", stream, lastId);
        List<StreamEntry> entries = new ArrayList<>();
        if(!(reply instanceof List))
            return entries; // nil reply: the block expired with nothing new
        for(Object perStream : (List<?>) reply) {
            // each element is [streamKey, [[id, [field, value, field, value, ...]], ...]]
            if(!(perStream instanceof List) || ((List<?>) perStream).size() < 2)
                continue;
            Object rawEntries = ((List<?>) perStream).get(1);
            if(!(rawEntries instanceof List))
                continue;
            for(Object rawEntry : (List<?>) rawEntries) {
                if(!(rawEntry instanceof List) || ((List<?>) rawEntry).size() < 2)
                    continue;
                String id = String.valueOf(((List<?>) rawEntry).get(0));
                Object rawFields = ((List<?>) rawEntry).get(1);
                StreamEntry entry = new StreamEntry(id);
                if(rawFields instanceof List) {
                    List<?> fields = (List<?>) rawFields;
                    for(int i = 0; i + 1 < fields.size(); i += 2)
                        entry.fields.put(String.valueOf(fields.get(i)), String.valueOf(fields.get(i + 1)));
                }
                entries.add(entry);
            }
        }
        return entries;
    }

    // ==================== RESP ====================

    /** Writes one command as a RESP array of bulk strings and reads back the reply. */
    protected synchronized Object command(String... args) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(('*' + String.valueOf(args.length)).getBytes(StandardCharsets.UTF_8));
        buffer.write(CRLF);
        for(String arg : args) {
            byte[] raw = arg.getBytes(StandardCharsets.UTF_8);
            buffer.write(('$' + String.valueOf(raw.length)).getBytes(StandardCharsets.UTF_8));
            buffer.write(CRLF);
            buffer.write(raw);
            buffer.write(CRLF);
        }
        out.write(buffer.toByteArray());
        out.flush();
        return readReply();
    }

    /** Reads one reply: simple string, error, integer, bulk string (possibly nil), or (nested) array. */
    protected Object readReply() throws IOException {
        int type = in.read();
        if(type < 0)
            throw new IOException("Redis connection closed.");
        switch(type) {
            case '+':
                return readLine();
            case '-':
                throw new IOException("Redis error: " + readLine());
            case ':':
                return Long.valueOf(readLine());
            case '$': {
                int length = Integer.parseInt(readLine());
                if(length < 0)
                    return null;
                byte[] data = new byte[length];
                int read = 0;
                while(read < length) {
                    int n = in.read(data, read, length - read);
                    if(n < 0)
                        throw new IOException("Redis connection closed mid-value.");
                    read += n;
                }
                in.read();
                in.read(); // trailing CRLF
                return new String(data, StandardCharsets.UTF_8);
            }
            case '*': {
                int count = Integer.parseInt(readLine());
                if(count < 0)
                    return null;
                List<Object> items = new ArrayList<>(count);
                for(int i = 0; i < count; i++)
                    items.add(readReply());
                return items;
            }
            default:
                throw new IOException("Unexpected RESP type byte: " + (char) type);
        }
    }

    protected String readLine() throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int c;
        while((c = in.read()) >= 0) {
            if(c == '\r') {
                in.read(); // consume '\n'
                break;
            }
            line.write(c);
        }
        return new String(line.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}