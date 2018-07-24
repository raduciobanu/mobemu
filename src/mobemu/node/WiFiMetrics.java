package mobemu.node;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for the WiFi metrics of a node.
 * 
 * @author raduioanciobanu
 */
public class WiFiMetrics {
	private int node;
	private WiFiDataGroup cachedData = null;
	private long cachedDataStart = 0;
	private long cachedDataEnd = 0;

	private static List<WiFiData> measurements;
	private static long traceStart;
	private static long traceEnd;
	private static long apDataTraceStart;
	private static long apDataTraceEnd;
	private static boolean parsed = false;

	private static final String DEFAULT_FILE_LOCATION = "traces" + File.separator + "ap-data" + File.separator
			+ "APs.txt";

	/**
	 * Creates a {@code WiFiMetrics} object.
	 * 
	 * @param node
	 *            ID of the current node
	 * @param traceStart
	 *            start timestamp of the trace
	 * @param traceEnd
	 *            end timestamp of the trace
	 */
	public WiFiMetrics(int node, long traceStart, long traceEnd) {
		this(node, traceStart, traceEnd, DEFAULT_FILE_LOCATION);
	}

	/**
	 * Creates a {@code WiFiMetrics} object.
	 * 
	 * @param node
	 *            ID of the current node
	 * @param traceStart
	 *            start timestamp of the trace
	 * @param traceEnd
	 *            end timestamp of the trace
	 * @param filePath
	 *            path of the file with the WiFi traces
	 */
	public WiFiMetrics(int node, long traceStart, long traceEnd, String filePath) {
		this.node = node;

		if (!parsed) {
			WiFiMetrics.traceStart = traceStart;
			WiFiMetrics.traceEnd = traceEnd;

			measurements = new ArrayList<>();
			parseData(filePath);
			parsed = true;
		}
	}

	/**
	 * Gets the SSID of the Wi-Fi network the current node is connected to at the
	 * given timestamp.
	 * 
	 * @param timestamp
	 *            current timestamp
	 * @return SSID of the Wi-Fi network the current node is connected to at the
	 *         given timestamp
	 */
	public String getSsid(long timestamp) {
		return getData(timestamp).ssid;
	}

	/**
	 * Gets the RSSI of the Wi-Fi network the current node is connected to at the
	 * given timestamp.
	 * 
	 * @param timestamp
	 *            current timestamp
	 * @return RSSI of the Wi-Fi network the current node is connected to at the
	 *         given timestamp
	 */
	public int getRssi(long timestamp) {
		return getData(timestamp).rssi;
	}

	/**
	 * Gets the connectivity of the Wi-Fi network the current node is connected to
	 * at the given timestamp.
	 * 
	 * @param timestamp
	 *            current timestamp
	 * @return connectivity of the Wi-Fi network the current node is connected to at
	 *         the given timestamp
	 */
	public boolean isConnectable(long timestamp) {
		return getData(timestamp).isConnectable;
	}

	/**
	 * Gets the capabilities of the Wi-Fi network the current node is connected to
	 * at the given timestamp.
	 * 
	 * @param timestamp
	 *            current timestamp
	 * @return capabilities of the Wi-Fi network the current node is connected to at
	 *         the given timestamp
	 */
	public List<String> getCapabilities(long timestamp) {
		return getData(timestamp).capabilities;
	}

	/**
	 * Gets Wi-Fi data for the current node at the given timestamp.
	 * 
	 * @param timestamp
	 *            current timestamp
	 * @return Wi-Fi data for the current node at the given timestamp
	 */
	private WiFiDataGroup getData(long timestamp) {
		if (!canUseCachedData(timestamp)) {
			getNewData(timestamp);
		}

		return cachedData;
	}

	/**
	 * Gets cached Wi-Fi data for the current node at the given timestamp.
	 * 
	 * @param timestamp
	 *            current timestamp
	 * @return cached Wi-Fi data for the current node at the given timestamp
	 */
	private boolean canUseCachedData(long timestamp) {
		return cachedData != null && cachedDataStart <= timestamp && cachedDataEnd >= timestamp;
	}

	/**
	 * Gets new Wi-Fi data for the current node at the given timestamp.
	 * 
	 * @param timestamp
	 *            current timestamp
	 * @return new Wi-Fi data for the current node at the given timestamp
	 */
	private void getNewData(long timestamp) {
		WiFiData data = measurements.get(0);

		for (WiFiData d : measurements) {
			if (d.start <= timestamp && d.end >= timestamp) {
				data = d;
				break;
			}
		}

		cachedDataStart = data.start;
		cachedDataEnd = data.end;
		cachedData = data.data.get(node % data.data.size());
	}

	/**
	 * Parses Wi-Fi data.
	 * 
	 * @param filePath
	 *            path of the trace file
	 */
	private static void parseData(String filePath) {
		try (FileInputStream fstream = new FileInputStream(filePath);
				DataInputStream in = new DataInputStream(fstream)) {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line;
			long lastTimestamp = 0;
			WiFiData currentData = null;
			int lineCount = 0;

			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(",");
				long timestamp = Long.parseLong(tokens[0]);

				if (lineCount == 0) {
					apDataTraceStart = timestamp;
				}

				if (timestamp - lastTimestamp > 8 * 60 * 1000L) {
					if (currentData != null) {
						currentData.end = timestamp;
						measurements.add(currentData);
					}

					currentData = new WiFiData(timestamp);
				}

				lastTimestamp = timestamp;

				currentData.addData(new WiFiDataGroup(tokens[4], Integer.parseInt(tokens[5]),
						Boolean.parseBoolean(tokens[7]), tokens[6]));
				lineCount++;
			}

			currentData.end = lastTimestamp;
			measurements.add(currentData);

			apDataTraceEnd = lastTimestamp;

			br.close();
			in.close();
			fstream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (WiFiData data : measurements) {
			data.translateToTrace(traceStart, traceEnd, apDataTraceStart, apDataTraceEnd);
		}
	}

	/**
	 * Class for Wi-Fi data collected for a given interval.
	 * 
	 * @author raduioanciobanu
	 */
	private static class WiFiData {
		/**
		 * Start timestap of this data.
		 */
		private long start;
		/**
		 * End timestamp of this data (to be set explicitly).
		 */
		private long end;
		private List<WiFiDataGroup> data;

		/**
		 * Instantiates a {@code WiFiData} object.
		 * 
		 * @param start
		 *            start timestamp of this data
		 */
		public WiFiData(long start) {
			this.start = start;
			this.data = new ArrayList<>();
		}

		/**
		 * Adds data to this dataset.
		 * 
		 * @param group
		 *            data to be added
		 */
		public void addData(WiFiDataGroup group) {
			data.add(group);
		}

		/**
		 * Translated data timestamps to trace timestamps.
		 * 
		 * @param traceStart
		 *            start timestamp of the trace data
		 * @param traceEnd
		 *            end timestamp of the trace data
		 * @param apDataTraceStart
		 *            start timestamp of the Wi-Fi data
		 * @param apDataTraceEnd
		 *            end timestamp of the Wi-Fi data
		 */
		public void translateToTrace(long traceStart, long traceEnd, long apDataTraceStart, long apDataTraceEnd) {
			start = traceStart
					+ (start - apDataTraceStart) * (traceEnd - traceStart) / (apDataTraceEnd - apDataTraceStart);
			end = traceStart + (end - apDataTraceStart) * (traceEnd - traceStart) / (apDataTraceEnd - apDataTraceStart);
		}
	}

	/**
	 * Class for per-AP Wi-Fi data.
	 * 
	 * @author raduioanciobanu
	 */
	private static class WiFiDataGroup {
		private String ssid;
		private int rssi;
		private boolean isConnectable;
		private List<String> capabilities;

		/**
		 * Instantiates a {@code WiFiDataGroup} object.
		 * 
		 * @param ssid
		 *            SSID of the AP
		 * @param rssi
		 *            RSSI of the AP
		 * @param isConnectable
		 *            specifies if the AP is connectable
		 * @param capabilities
		 *            capabilities of the AP
		 */
		public WiFiDataGroup(String ssid, int rssi, boolean isConnectable, String capabilities) {
			this.ssid = ssid;
			this.rssi = rssi;
			this.isConnectable = isConnectable;

			String[] res = capabilities.split("[|\\\\]");
			this.capabilities = new ArrayList<>(res.length);
			this.capabilities.addAll(Arrays.asList(res));
		}
	}
}
