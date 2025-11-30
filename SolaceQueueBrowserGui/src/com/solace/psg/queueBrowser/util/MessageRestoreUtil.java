package com.solace.psg.queueBrowser.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for parsing message ZIP files and extracting metadata
 */
public class MessageRestoreUtil {
	private static final Logger logger = LoggerFactory.getLogger(MessageRestoreUtil.class.getName());
	
	/**
	 * Represents a parsed message from a ZIP file
	 */
	public static class ParsedMessage {
		public String messageId;
		public String timestamp;
		public String payload;
		public Map<String, String> headers;
		public Map<String, String> userProps;
		public File zipFile;
		
		public ParsedMessage() {
			this.headers = new HashMap<>();
			this.userProps = new HashMap<>();
		}
	}
	
	/**
	 * Represents source metadata extracted from directory path
	 */
	public static class SourceMetadata {
		public String host;
		public String vpn;
		public String queue;
		public String directoryPath;
		
		public SourceMetadata(String host, String vpn, String queue, String directoryPath) {
			this.host = host;
			this.vpn = vpn;
			this.queue = queue;
			this.directoryPath = directoryPath;
		}
	}
	
	/**
	 * Extract source metadata from directory path
	 * Assumes format: .../host/vpn/queue-name/
	 * @param directoryPath The directory path
	 * @return SourceMetadata or null if path doesn't match expected format
	 */
	public static SourceMetadata extractSourceMetadata(String directoryPath) {
		try {
			Path path = Paths.get(directoryPath).normalize();
			List<String> parts = new ArrayList<>();
			for (Path element : path) {
				parts.add(element.toString());
			}
			
			if (parts.size() < 3) {
				logger.warn("Directory path does not have enough levels: " + directoryPath);
				return null;
			}
			
			// Last 3 parts should be: queue-name, vpn, host
			String queue = parts.get(parts.size() - 1);
			String vpn = parts.get(parts.size() - 2);
			String host = parts.get(parts.size() - 3);
			
			// Unsanitize queue and VPN names (replace _ back to / and spaces)
			queue = queue.replace("_", "/");
			vpn = vpn.replace("_", "/");
			
			logger.info("Extracted metadata - Host: " + host + ", VPN: " + vpn + ", Queue: " + queue);
			return new SourceMetadata(host, vpn, queue, directoryPath);
		} catch (Exception e) {
			logger.error("Error extracting source metadata from path: " + directoryPath, e);
			return null;
		}
	}
	
	/**
	 * Extract message ID and timestamp from ZIP filename
	 * Format: msg-<id>-<timestamp>.zip
	 * @param zipFileName The ZIP file name
	 * @return Array with [messageId, timestamp] or null if format doesn't match
	 */
	public static String[] extractMessageIdAndTimestamp(String zipFileName) {
		Pattern pattern = Pattern.compile("msg-(\\d+)-(\\d{14})\\.zip");
		Matcher matcher = pattern.matcher(zipFileName);
		
		if (matcher.matches()) {
			return new String[]{matcher.group(1), matcher.group(2)};
		}
		
		logger.warn("ZIP filename does not match expected format: " + zipFileName);
		return null;
	}
	
	/**
	 * Find all message ZIP files in a directory
	 * @param directoryPath The directory to search
	 * @return List of ZIP files matching msg-*-*.zip pattern
	 */
	public static List<File> findMessageZipFiles(String directoryPath) {
		List<File> zipFiles = new ArrayList<>();
		try {
			File dir = new File(directoryPath);
			if (!dir.exists() || !dir.isDirectory()) {
				logger.error("Directory does not exist or is not a directory: " + directoryPath);
				return zipFiles;
			}
			
			File[] files = dir.listFiles();
			if (files == null) {
				return zipFiles;
			}
			
			Pattern pattern = Pattern.compile("msg-\\d+-\\d{14}\\.zip");
			for (File file : files) {
				if (file.isFile() && pattern.matcher(file.getName()).matches()) {
					zipFiles.add(file);
				}
			}
			
			logger.info("Found " + zipFiles.size() + " message ZIP files in " + directoryPath);
		} catch (Exception e) {
			logger.error("Error finding ZIP files in directory: " + directoryPath, e);
		}
		
		return zipFiles;
	}
	
	/**
	 * Parse a single ZIP file and extract message data
	 * @param zipFile The ZIP file to parse
	 * @return ParsedMessage or null if parsing fails
	 */
	public static ParsedMessage parseZipFile(File zipFile) {
		ParsedMessage message = new ParsedMessage();
		message.zipFile = zipFile;
		
		// Extract message ID and timestamp from filename
		String[] idAndTimestamp = extractMessageIdAndTimestamp(zipFile.getName());
		if (idAndTimestamp == null) {
			logger.error("Failed to extract message ID and timestamp from: " + zipFile.getName());
			return null;
		}
		message.messageId = idAndTimestamp[0];
		message.timestamp = idAndTimestamp[1];
		
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry;
			Map<String, byte[]> fileContents = new HashMap<>();
			
			// Read all entries from ZIP
			while ((entry = zis.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					String fileName = entry.getName();
					byte[] buffer = new byte[1024];
					java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
					int len;
					while ((len = zis.read(buffer)) > 0) {
						baos.write(buffer, 0, len);
					}
					fileContents.put(fileName, baos.toByteArray());
				}
				zis.closeEntry();
			}
			
			// Extract payload
			byte[] payloadBytes = fileContents.get("payload.txt");
			if (payloadBytes != null) {
				message.payload = new String(payloadBytes, StandardCharsets.UTF_8);
			} else {
				logger.warn("payload.txt not found in ZIP: " + zipFile.getName());
				message.payload = "";
			}
			
			// Parse headers
			byte[] headersBytes = fileContents.get("headers.txt");
			if (headersBytes != null) {
				String headersText = new String(headersBytes, StandardCharsets.UTF_8);
				message.headers = parseKeyValuePairs(headersText);
			} else {
				logger.warn("headers.txt not found in ZIP: " + zipFile.getName());
			}
			
			// Parse user properties
			byte[] userPropsBytes = fileContents.get("userProps.txt");
			if (userPropsBytes != null) {
				String userPropsText = new String(userPropsBytes, StandardCharsets.UTF_8);
				message.userProps = parseKeyValuePairs(userPropsText);
			} else {
				logger.warn("userProps.txt not found in ZIP: " + zipFile.getName());
			}
			
			logger.info("Successfully parsed ZIP file: " + zipFile.getName() + ", Message ID: " + message.messageId);
			return message;
			
		} catch (IOException e) {
			logger.error("Error parsing ZIP file: " + zipFile.getName(), e);
			return null;
		}
	}
	
	/**
	 * Parse multiple ZIP files
	 * @param zipFiles List of ZIP files to parse
	 * @return List of ParsedMessage objects
	 */
	public static List<ParsedMessage> parseZipFiles(List<File> zipFiles) {
		List<ParsedMessage> messages = new ArrayList<>();
		for (File zipFile : zipFiles) {
			ParsedMessage message = parseZipFile(zipFile);
			if (message != null) {
				messages.add(message);
			}
		}
		return messages;
	}
	
	/**
	 * Parse key:value pairs from text (one per line)
	 * Format: "Key: Value\n"
	 * @param text The text to parse
	 * @return Map of key-value pairs
	 */
	private static Map<String, String> parseKeyValuePairs(String text) {
		Map<String, String> map = new HashMap<>();
		if (text == null || text.trim().isEmpty()) {
			return map;
		}
		
		String[] lines = text.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}
			
			// Split on first ": " to handle values that contain colons
			int colonIndex = line.indexOf(": ");
			if (colonIndex > 0) {
				String key = line.substring(0, colonIndex).trim();
				String value = line.substring(colonIndex + 2).trim();
				map.put(key, value);
			}
		}
		
		return map;
	}
	
	/**
	 * Format timestamp for display
	 * Input format: yyyyMMddHHmmss
	 * Output format: yyyy-MM-dd HH:mm:ss
	 */
	public static String formatTimestamp(String timestamp) {
		if (timestamp == null || timestamp.length() != 14) {
			return timestamp;
		}
		try {
			return timestamp.substring(0, 4) + "-" + 
				   timestamp.substring(4, 6) + "-" + 
				   timestamp.substring(6, 8) + " " + 
				   timestamp.substring(8, 10) + ":" + 
				   timestamp.substring(10, 12) + ":" + 
				   timestamp.substring(12, 14);
		} catch (Exception e) {
			return timestamp;
		}
	}
}

