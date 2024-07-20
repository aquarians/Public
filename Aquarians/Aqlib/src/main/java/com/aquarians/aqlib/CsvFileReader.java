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
		String line = readLine();
		if (null == line) {
			return null;
		}

		return line.split(",");
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
