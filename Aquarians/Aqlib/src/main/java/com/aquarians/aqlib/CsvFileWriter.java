package com.aquarians.aqlib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class CsvFileWriter {

	private final File file;
	private final FileOutputStream outputStream;
	private final BufferedWriter bufferedWriter;

	public CsvFileWriter(String fileName) {
		file = new File(fileName);
		try {
			outputStream = new FileOutputStream(file);
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public void close() {
		try {
			bufferedWriter.close();
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public void write(String[] records) {
		for (int i = 0; i < records.length; ++i) {
			String record = (0 == i ? "" : ",") + records[i];
			try {
				bufferedWriter.write(record);
			} catch (Exception ex) {
				throw new RuntimeException(ex.getMessage());
			}
		}

		try {
			bufferedWriter.newLine();
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public void write(List<String> records) {
		for (int i = 0; i < records.size(); ++i) {
			String record = (0 == i ? "" : ",") + records.get(i);
			try {
				bufferedWriter.write(record);
			} catch (Exception ex) {
				throw new RuntimeException(ex.getMessage());
			}
		}

		try {
			bufferedWriter.newLine();
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	public void flush() {
        try {
            bufferedWriter.flush();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
	}

}
