package com.solace.psg.queueBrowser;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.queueBrowser.util.MessageRestoreUtil.ParsedMessage;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * Utility class for publishing messages to queues using JCSMP
 */
public class MessagePublisher {
	private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class.getName());
	
	private Broker broker;
	private JCSMPSession session;
	private XMLMessageProducer producer;
	
	public MessagePublisher(Broker broker) throws BrokerException {
		this.broker = broker;
		initializeSession();
	}
	
	/**
	 * Initialize JCSMP session and producer
	 */
	private void initializeSession() throws BrokerException {
		try {
			JCSMPProperties properties = new JCSMPProperties();
			properties.setProperty(JCSMPProperties.HOST, broker.messagingHost);
			properties.setProperty(JCSMPProperties.VPN_NAME, broker.msgVpnName);
			properties.setProperty(JCSMPProperties.USERNAME, broker.messagingClientUsername);
			properties.setProperty(JCSMPProperties.PASSWORD, broker.messagingPw);
			
			session = JCSMPFactory.onlyInstance().createSession(properties);
			session.connect();
			
			// Create producer with event handler
			producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
				@Override
				public void responseReceivedEx(Object key) {
					// Handle publish confirmation
					logger.debug("Message published successfully: " + key);
				}
				
				@Override
				public void handleErrorEx(Object key, JCSMPException e, long timestamp) {
					// Handle publish errors
					logger.error("Error publishing message: " + key, e);
				}
			});
			
			logger.info("MessagePublisher initialized successfully");
		} catch (JCSMPException e) {
			String errorMsg = "Failed to initialize JCSMP session for publishing: " + e.getMessage();
			logger.error(errorMsg, e);
			throw new BrokerException(errorMsg);
		}
	}
	
	/**
	 * Publish a single message to a queue
	 * @param queueName Target queue name
	 * @param parsedMessage Parsed message data
	 * @throws BrokerException If publishing fails
	 */
	public void publishMessage(String queueName, ParsedMessage parsedMessage) throws BrokerException {
		try {
			BytesXMLMessage message = reconstructMessage(parsedMessage);
			Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
			producer.send(message, queue);
			logger.info("Published message " + parsedMessage.messageId + " to queue " + queueName);
		} catch (JCSMPException e) {
			String errorMsg = "Failed to publish message " + parsedMessage.messageId + " to queue " + queueName + ": " + e.getMessage();
			logger.error(errorMsg, e);
			throw new BrokerException(errorMsg);
		}
	}
	
	/**
	 * Publish multiple messages to a queue
	 * @param queueName Target queue name
	 * @param messages List of parsed messages
	 * @return Number of successfully published messages
	 * @throws BrokerException If all messages fail
	 */
	public int publishMessages(String queueName, List<ParsedMessage> messages) throws BrokerException {
		int successCount = 0;
		int failCount = 0;
		
		for (ParsedMessage parsedMessage : messages) {
			try {
				publishMessage(queueName, parsedMessage);
				successCount++;
			} catch (BrokerException e) {
				failCount++;
				logger.error("Failed to publish message " + parsedMessage.messageId, e);
			}
		}
		
		if (successCount == 0 && failCount > 0) {
			throw new BrokerException("All " + failCount + " messages failed to publish");
		}
		
		logger.info("Published " + successCount + " messages successfully, " + failCount + " failed");
		return successCount;
	}
	
	/**
	 * Reconstruct BytesXMLMessage from parsed message data
	 * @param parsedMessage Parsed message data
	 * @return BytesXMLMessage ready to publish
	 * @throws BrokerException If reconstruction fails
	 */
	private BytesXMLMessage reconstructMessage(ParsedMessage parsedMessage) throws BrokerException {
		try {
			BytesXMLMessage message;
			
			// Determine message type - prefer TextMessage for text payloads
			// Check if payload looks like text (contains printable characters)
			boolean isText = isTextPayload(parsedMessage.payload);
			
			if (isText) {
				TextMessage textMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
				textMsg.setText(parsedMessage.payload);
				message = textMsg;
			} else {
				BytesMessage bytesMsg = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
				bytesMsg.setData(parsedMessage.payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				message = bytesMsg;
			}
			
			// Set headers from parsed headers
			setHeaders(message, parsedMessage.headers);
			
			// Set user properties
			setUserProperties(message, parsedMessage.userProps);
			
			// Set delivery mode to persistent by default
			message.setDeliveryMode(DeliveryMode.PERSISTENT);
			
			return message;
		} catch (Exception e) {
			throw new BrokerException("Failed to reconstruct message: " + e.getMessage());
		}
	}
	
	/**
	 * Check if payload is text (contains mostly printable characters)
	 */
	private boolean isTextPayload(String payload) {
		if (payload == null || payload.isEmpty()) {
			return true;
		}
		
		int printableCount = 0;
		int totalChars = Math.min(payload.length(), 1000); // Sample first 1000 chars
		
		for (int i = 0; i < totalChars; i++) {
			char c = payload.charAt(i);
			if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || 
				(c >= 32 && c <= 126)) { // Printable ASCII
				printableCount++;
			}
		}
		
		// Consider it text if >80% printable
		return (printableCount * 100 / totalChars) > 80;
	}
	
	/**
	 * Set message headers from parsed headers map
	 */
	private void setHeaders(BytesXMLMessage message, Map<String, String> headers) {
		if (headers == null) {
			return;
		}
		
		// Set TTL if available
		if (headers.containsKey("Time-To-Live (TTL)")) {
			try {
				long ttl = Long.parseLong(headers.get("Time-To-Live (TTL)"));
				message.setTimeToLive(ttl);
			} catch (NumberFormatException e) {
				logger.warn("Invalid TTL value: " + headers.get("Time-To-Live (TTL)"));
			}
		}
		
		// Set Correlation ID if available
		if (headers.containsKey("Correlation ID") && !headers.get("Correlation ID").isEmpty()) {
			message.setCorrelationId(headers.get("Correlation ID"));
		}
		
		// Set Reply-To Destination if available
		if (headers.containsKey("Reply-To Destination") && !headers.get("Reply-To Destination").isEmpty()) {
			try {
				String replyTo = headers.get("Reply-To Destination");
				Destination dest = JCSMPFactory.onlyInstance().createTopic(replyTo);
				message.setReplyTo(dest);
			} catch (Exception e) {
				logger.warn("Failed to set Reply-To destination: " + headers.get("Reply-To Destination"), e);
			}
		}
		
		// Note: Some headers like Message ID, Redelivery Flag, etc. are set by the broker
		// and cannot be set on new messages
	}
	
	/**
	 * Set user properties from parsed user properties map
	 */
	private void setUserProperties(BytesXMLMessage message, Map<String, String> userProps) {
		if (userProps == null || userProps.isEmpty()) {
			return;
		}
		
		try {
			SDTMap properties = JCSMPFactory.onlyInstance().createMap();
			for (Map.Entry<String, String> entry : userProps.entrySet()) {
				properties.putString(entry.getKey(), entry.getValue());
			}
			message.setProperties(properties);
		} catch (SDTException e) {
			logger.error("Failed to set user properties", e);
		}
	}
	
	/**
	 * Close the session and producer
	 */
	public void close() {
		try {
			if (producer != null) {
				producer.close();
			}
			if (session != null) {
				session.closeSession();
			}
			logger.info("MessagePublisher closed");
		} catch (Exception e) {
			logger.error("Error closing MessagePublisher", e);
		}
	}
}

