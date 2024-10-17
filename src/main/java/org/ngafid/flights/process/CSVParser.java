package org.ngafid.flights.process;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CSVParser {
  private static final Logger LOG = Logger.getLogger(CSVParser.class.getName());

  private CharBuffer data;

  public CSVParser(byte[] data) {
    var bytes = ByteBuffer.wrap(data);
    this.data = StandardCharsets.UTF_8.decode(bytes);
  }

  public List<List<CharBuffer>> parse() {
    var rows = new ArrayList<List<CharBuffer>>(this.data.length() / 1024);

    var longestRowLength = 32;

    var currentRow = new ArrayList<CharBuffer>(longestRowLength);
    int i = 0;
    int start = 0;

    while (i < data.length()) {
      switch (data.get(i)) {
        case '\n':
          currentRow.add(data.subSequence(start, i));
          rows.add(currentRow);
          longestRowLength = Math.max(longestRowLength, currentRow.size());
          currentRow = new ArrayList<CharBuffer>(longestRowLength);
          i += 1;
          start = i;
          continue;
        case ',':
          currentRow.add(data.subSequence(start, i));
          start = i + 1;
        default:
          i += 1;
      }
    }

    if (currentRow.size() != 0) {
      currentRow.add(data.subSequence(start, i));
      rows.add(currentRow);
    }

    return rows;
  }
}
