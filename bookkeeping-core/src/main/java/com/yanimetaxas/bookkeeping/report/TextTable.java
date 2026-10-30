package com.yanimetaxas.bookkeeping.report;

/** Box-drawn text table used by ledger and trial-balance reports. */
public final class TextTable {

  private TextTable() {}

  public static String separator(int... widths) {
    StringBuilder separator = new StringBuilder("+");
    for (int width : widths) {
      separator.append("-".repeat(width + 2)).append("+");
    }
    return separator.toString();
  }

  public static String row(String format, String... cells) {
    return String.format(format, (Object[]) cells);
  }
}
