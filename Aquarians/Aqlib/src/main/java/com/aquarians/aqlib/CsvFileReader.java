package com.aquarians.aqlib;

import java.io.BufferedReader;
import java.io.FileReader;

public class CsvFileReader {

	private final String file;
	BufferedReader reader;
    private int lineNumber = 0;

	public CsvFileReader(String file) {
		this.file = file;
		try {
            reader = new BufferedReader(new FileReader(file));
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

    public String readLine() {
        try {
            return reader.readLine();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

	public String[] readRecord() {
        lineNumber++;
		String line = null;
		try {
			line = reader.readLine();
		} catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);		}

		if (null == line) {
			return null;
		}

		// Process quoted text, like "1,234.56"
		int posQuote = line.indexOf("\"");
		while (posQuote >= 0) {
			int nextQuote = line.indexOf("\"", posQuote + 1);
			if (nextQuote < 0) {
				throw new RuntimeException("Invalid quoted text: " + line);
			}
			String text = line.substring(posQuote + 1, nextQuote);

			// Remove comma character from text
			text = text.replace(",", "");

			// Replace quoted text with unquoted one
			String prefix = line.substring(0, posQuote);
			String suffix = line.substring(nextQuote + 1);
			line = prefix + text + suffix;
			posQuote = line.indexOf("\"");
		}

		String[] values = line.split(",");
		return values;
	}

	public void close() {
		try {
            reader.close();
		} catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
		}
	}

    public String getFile() {
        return file;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
